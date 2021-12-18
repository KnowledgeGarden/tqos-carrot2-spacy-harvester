/*
 * Copyright 2021 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2.pubmed;

import java.util.ArrayList;
import java.util.List;

import org.topicquests.research.carrot2.Environment;
import org.topicquests.research.carrot2.pubmed.ParserThread.Worker;
import org.topicquests.support.api.IResult;

import net.minidev.json.JSONObject;

/**
 * @author jackpark
 *
 */
public class DocumentThread {
	private Environment environment;
	private List<JSONObject> docs;
	private boolean isRunning = true;
	private Worker worker;
	/**
	 * 
	 */
	public DocumentThread(Environment env) {
		environment = env;
		docs = new ArrayList<JSONObject>();
		isRunning = true;
		worker = new Worker();
		worker.start();	
	}

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
		
		
		void processDoc(JSONObject doc) {
			environment.logDebug("DocThread\n"+doc);
/*			IDocument d = new ConcordanceDocument(environment, doc);
			String docId = d.getId();
			String pmid = d.getPMID();
			String pmcid = d.getPMCID();
			String url = null;
			List<String>labels = d.listLabels();
			String label = labels.get(0);
			IResult r  = documentDatabase.put(docId, pmid, pmcid, url, label, d.getData());
			es.addDoc(d);
			ocean.addDoc(d); */
			environment.logDebug("DocThread+ "+r.getErrorString());
		}
	}
}
