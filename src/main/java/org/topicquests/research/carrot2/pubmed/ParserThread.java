/*
 * Copyright 2021 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2.pubmed;

import java.util.*;
import org.topicquests.research.carrot2.Environment;
import org.topicquests.research.carrot2.nlp.SpaCyThread;
import org.topicquests.support.api.IResult;

import net.minidev.json.JSONObject;

/**
 * @author jackpark
 *
 */
public class ParserThread {
	private Environment environment;
	//private DocumentThread docThread;
	private SpaCyThread spacy;

	private PubMedReportPullParser parser;
	private List<String> docs;
	private List<JSONObject> docBlock;
	private boolean isRunning = true;
	private boolean hasBeenRunning = false;
	private Worker worker;
	private final int BLOCK_SIZE = 2;

	/**
	 * 
	 */
	public ParserThread(Environment env) {
		environment = env;
		parser = new PubMedReportPullParser(environment);
		//docThread = new DocumentThread(environment);
		spacy = new SpaCyThread(environment);

		docs = new ArrayList<String>();
		docBlock = new ArrayList<JSONObject>(BLOCK_SIZE);
		isRunning = true;
		worker = new Worker();
		worker.start();
		environment.logDebug("ParserThread");
	}
	
	/**
	 * Called by way of {@code Environment} when all files have
	 * been loaded, which means that if we don't have a full block,
	 * send it anyway.
	 */
	public void filesLoaded() {
		
	}
	/**
	 * From Carrot2 search by way of {@code Environment}
	 * @param xml
	 */
	public void addDoc(String xml) {
		environment.logDebug("PT.add");
		synchronized(docs) {
			docs.add(xml);
			docs.notify();
		}
	}

	public void shutDown() {
		synchronized(docs) {
			isRunning = false;
			docs.notify();
		}
		//docThread.shutDown();
		spacy.shutDown();
	}
	class Worker extends Thread {
		
		public void run() {
			environment.logDebug("ParserThread.starting");
			String doc = null;
			while (isRunning) {
				synchronized(docs) {
					if (docs.isEmpty()) {
						if (hasBeenRunning) {
							environment.queueEmpty();
							hasBeenRunning  = false;
						}
						try {
							docs.wait();
						} catch (Exception e) {}
						
					} else {
						doc = docs.remove(0);
					}
				}
				if (isRunning && doc != null) {
					processDoc(doc);
					doc = null;
				}
			}
		}
		
		/**
		 * This receives {@code xml} and will get back
		 * an object which must then be sent off to 
		 * another thread for turning into an IDocument
		 * @param xml
		 */
		void processDoc(String xml) {
			environment.logDebug("PT.process "+parser);
			hasBeenRunning = true;
			IResult r = parser.parseXML(xml);
			JSONObject j = (JSONObject)r.getResultObject();
			//add raw XML to j
			j.put("raw", xml);
			String pmid = j.getAsString("pmid");
			environment.getAccountant().haveSeen(pmid);
			environment.logDebug("PT+");
			processBlock(j);
			environment.logDebug("PT++");
		}
		
		void processBlock(JSONObject pubmedDoc) {
			String docId = pubmedDoc.getAsString("pmid");
			environment.logDebug("SpaCyThread.adding "+docId);
			List<String> abstracts = (List<String>)pubmedDoc.get("abstract");
			StringBuilder buf = new StringBuilder();
			if (abstracts != null && !abstracts.isEmpty()) {
				Iterator<String> itr = abstracts.iterator();
				while (itr.hasNext())
					buf.append(itr.next()+"/n");
			}
			pubmedDoc.put("id", docId);
			pubmedDoc.put("text", buf.toString());

			docBlock.add(pubmedDoc);
			if (docBlock.size() >= BLOCK_SIZE) {
				sendBlock();
				docBlock = new ArrayList<JSONObject>();
			}
		}
		void sendBlock() {
			spacy.addDoc(docBlock);
		}
	}
}
