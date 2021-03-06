/*
 * Copyright 2021 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2.pubmed;

import java.util.ArrayList;
import java.util.List;

import org.topicquests.research.carrot2.Environment;
import org.topicquests.research.carrot2.nlp.SpaCyThread;

import net.minidev.json.JSONObject;

/**
 * @author jackpark
 * <p>
 * Stack up (cache) XML strings from Carrot2 search and pass them, one at a time
 * to the PubMedPullParser. Its output returns here where it is then sent on to
 * the SpaCy agent.
 * To do that, we have to take apart all <em>sentences> from the abstracts and title
 * in a given document, and send those, one at a time.
 * You need to keep track of PMID and accumulate parse "results" in a JSON object.
 * We begin with an XML abstract document.
 * We then get a JSONObject from the pull parser
 * 	We add the raw xml to that JSONObject for future reference
 * From each SpaCy pass, we get parse "results".
 * We need to accumulate all of those into a giant JSONObject which
 * we will then persist in a gzip file.
 * <em>FailSafe</em>
 * We need to track all processed objects.
 * If the system fails for any reason, it should be able to start again where it 
 * left off without losing any data.
 * The work flow is:
 * <ol><li>Read a query from a text file</li>
 * <li>Send to Carrot2 and wait for result --> XML string</li>
 * <li>XML string sent (not saved) to ParserThread queue</li>
 * <li> Note the opportunity to lose all those in the queue</li>
 * <li>XML string sent to PullParser --> JSONObject</li>
 * <li>JSONObject is the text to be sent to SpaCy, one sentence at a time</li>
 * <li>SpaCy takes time - its queue handles the lags</li>
 * <li>  More opportunity to lose queued objects</li>
 * <li>When all sentences in a given PMID are parsed by SpaCy, the final
 *   JSONObject which accumulates everything is then stored as a gzip file</li>
 * <li>At that time, the PMID is marked finished, but the query remains open until
 *  all PMIDs associated with it are completed</li></ol>
 *  @deprecated
 */
public class DocumentThread {
	private Environment environment;
	private SpaCyThread spacy;
	private List<JSONObject> docs;
	private boolean isRunning = true;
	private Worker worker;
	/**
	 * 
	 */
	public DocumentThread(Environment env) {
		environment = env;
		spacy = new SpaCyThread(environment);
		docs = new ArrayList<JSONObject>();
		isRunning = true;
		worker = new Worker();
		worker.start();	
	}

	/**
	 * From {@code ParserThread}
	 * Should include "raw" abstract + pull parser results
	 * @param doc
	 */
	public void addDoc(JSONObject doc) {
		environment.logDebug("DocThread.add "+doc);
		synchronized(docs) {
			docs.add(doc);
			docs.notify();
		}
	}

	public void shutDown() {
		synchronized(docs) {
			isRunning = false;
			docs.notify();
		}
	}
	
	class Worker extends Thread {
		
		public void run() {
			JSONObject doc = null;
			while (isRunning) {
				synchronized(docs) {
					if (docs.isEmpty()) {
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
		 * We get back a pmid doc to now process to spaCy
		 * @param doc 
		 */
		void processDoc(JSONObject doc) {
			environment.logDebug("DocThread\n"+doc);
		//	spacy.addDoc(doc);
		}
	}
}
