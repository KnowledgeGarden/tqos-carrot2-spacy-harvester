/*
 * Copyright 2021 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.topicquests.os.asr.driver.sp.SpacyDriverEnvironment;
import org.topicquests.research.carrot2.Environment;
import org.topicquests.research.carrot2.FileManager;
import org.topicquests.support.api.IResult;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * @author jackpark
 *
 */
public class SpaCyThread {
	private Environment environment;
	private FileManager fileManager;
	private SpacyDriverEnvironment spaCy;
	private List<List<JSONObject>> paragraphs; //todo rename to pubmedDocs
	private boolean isRunning = true;
	private Worker worker = null;
	//private boolean wasRunning = false;

	/**
	 * 
	 */
	public SpaCyThread(Environment env) {
		environment = env;
		spaCy = new SpacyDriverEnvironment();
		fileManager = environment.getFileManager();
		paragraphs = new ArrayList<List<JSONObject>>();
		environment.logDebug("SpaCyThread.boot");
		worker = new Worker();
		worker.start();
		isRunning = true;
	}

	//////////////////////////////////////
	// In theory, this needs to receive a block of docs, say 6 at a time.
	// So, we are talking about a List<JSONObject> coming in, and
	// sequestering a List<List<JSONObject>> for thread handling
	//////////////////////////////////////

	/**
	 * Abstracts of a given {@code pubmedDoc} are accumulated in a {@code List}
	 * @param pubmedDoc
	 */
	public void addDoc(List<JSONObject> pubmedDoc) { //(String docId, String paragraph) {
		environment.logDebug("ADDING "+pubmedDoc.size());
			synchronized(paragraphs) {
				environment.logDebug("SpaCyThread.add "+" "+paragraphs.size());
				paragraphs.add(pubmedDoc);
				paragraphs.notify();
			}
		
	}
	
	public void shutDown() {
		environment.logDebug("SpaCyThread.shutDown ");
		synchronized(paragraphs) {
			isRunning = false;
			paragraphs.notify();
		}
	}
	
	class Worker extends Thread {
		
		public void run() {
			environment.logDebug("SpaCyThread.starting");
			List<JSONObject> doc = null;
			while (isRunning) {
				synchronized(paragraphs) {
					environment.logDebug("SpaCyThread-XX "+paragraphs.size());
					if (paragraphs.isEmpty()) {
						try {
							paragraphs.wait();
						} catch (Exception e) {}
						
					} else {
						doc = paragraphs.remove(0);
						environment.logDebug("SpaCyThread.pong "+doc);
					}
				}
				if (isRunning && doc != null) {
					processDoc(doc);
					doc = null;
					environment.logDebug("SpaCyThread-R "+isRunning);
				}
				environment.logDebug("SpaCyThread.ping");

			}
			environment.logDebug("SpaCyThread.ending");
		}
		
		/**
		 * We get a block of abstracts, e.g. 6 at a time.
		 * @param pubmedDoc [{id, paragraphs }, ...]
		 */
		void processDoc(List<JSONObject> pubmedDoc) {
			environment.logDebug("SpaCyThread- "+pubmedDoc);
			// From one pass per model on each pubmedDoc
			// we populate collection. The collection can then be persisted
			// either separately or as a single gz file
			// for debugging, best to take it apart
			JSONObject collection = new JSONObject();
			//int ps = 0;
			
			List<String> models = this.spaCyModels();
			Iterator<String> itr = models.iterator();
			Iterator<JSONObject> itjo;
			JSONObject jo, hit, jx;
			JSONArray ja;
			String model, paragraph, text, docId;
			IResult r;
			long startTime = System.currentTimeMillis();
			while (itr.hasNext()) {
				//For each model
				model = itr.next();
				itjo = pubmedDoc.iterator();
				while (itjo.hasNext()) {
					//For each abstract
					jo = itjo.next();
					paragraph = jo.getAsString("text");
					text = cleanParagraph(paragraph);
					docId = jo.getAsString("id");
					//put it in collection if not there yet
					jx = (JSONObject)collection.get(docId);
					if (jx == null)
						collection.put(docId, jo); 
					environment.logDebug("SpaCyThread-AAA "+docId+"/n"+text);
					r = spaCy.processParagraph(text, model);
					hit = (JSONObject)r.getResultObject();
					environment.logDebug("SpaCyThread-AAA "+docId+"/n"+text);
					// we got a hit for a PMID and MODEL
					// jo is the core doc
					jx = (JSONObject)collection.get(docId);
					//add this new hit
					ja = (JSONArray)jx.get("results");
					if (ja == null) ja = new JSONArray();
					ja.add(hit);  // new result for existing collection
					jx.put("results", ja);
						// in theory, collection is now up to date
				}
			}
			

			/*IResult r = 
			//we get back a list of json objects, one for each model for the given  paragraph
			List<JSONObject> jo = (List<JSONObject>)r.getResultObject();
			environment.logDebug("SpaCyThread-BBB "+jo);

			pubmedDoc.put("abstract", text);
			pubmedDoc.put("results", jo);*/

			long delta = System.currentTimeMillis() - startTime;
			//environment.nlpTiming(docId, delta); // instrument
			//System.out.println("STp+ "+jo.size());
			environment.logDebug("SpaCyThread+ "+delta);
			fileManager.quickSaveSpaCy(collection);
			//persistSpaCy(docId, jo.toJSONString());
		}
		
		
		String cleanParagraph(String paragraph) {
			StringBuilder buf = new StringBuilder();
			int len = paragraph.length();
			char c;
			boolean found = false;
			boolean didSpace = false;
			for (int i=0;i<len;i++) {
				c = paragraph.charAt(i);
				if (!found && c == '(') {
					found = true;
					didSpace = true; // remember you did one before
					// we don't want to accumulate spaces from around ()
				} else if (found && c == ')') {
					found = false;
				} else if (!found) {
					if (c == ' ') {
						if (didSpace)
							didSpace = false;
						else {
							buf.append(c);
						}
					} else {
						buf.append(c);
						didSpace = false;
					}
				}
			}
			return buf.toString().trim();
		}
		
		/**
		 * List of known models available at the server.<br/>
		 * We are tasked to send blocks of abstracts, one model at a time
		 * @return
		 */
		List<String> spaCyModels() {
			List<String> models = new ArrayList<String>();
			models.add("en_ner_jnlpba_md");
			models.add("en_ner_bc5cdr_md");
			models.add("en_ner_bionlp13cg_md"); //
			models.add("en_ner_craft_md"); //
			models.add("en_core_web_lg");  //
			models.add("en_core_sci_lg"); //
			models.add("en_core_web_trf"); //
			models.add("en_core_sci_scibert"); //
			models.add("stanza;craft;anatem"); //
			models.add("stanza;craft;bc5cdr"); //
			models.add("stanza;craft;jnlbpa"); //
			models.add("stanza;craft;bionlp13cg"); //
			models.add("stanza;craft;ncbi_disease"); //
			return models;
		}
	}
	

	
}
