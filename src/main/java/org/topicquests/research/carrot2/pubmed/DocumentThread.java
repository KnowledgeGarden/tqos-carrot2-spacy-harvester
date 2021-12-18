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

		}
	}
}
