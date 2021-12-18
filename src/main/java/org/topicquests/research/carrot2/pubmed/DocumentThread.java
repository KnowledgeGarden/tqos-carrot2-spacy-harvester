/*
 * Copyright 2021 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2.pubmed;

import java.util.ArrayList;
import java.util.List;

import org.topicquests.asr.general.document.api.IDocumentClient;
import org.topicquests.hyperbrane.ConcordanceDocument;
import org.topicquests.hyperbrane.api.IDocument;
import org.topicquests.research.carrot2.Environment;
import org.topicquests.research.carrot2.nlp.ElasticSearch;
import org.topicquests.research.carrot2.ocean.OceanThread;
import org.topicquests.research.carrot2.pubmed.ParserThread.Worker;
import org.topicquests.support.api.IResult;

/**
 * @author jackpark
 *
 */
public class DocumentThread {
	private Environment environment;
	private IDocumentClient documentDatabase;
	private ElasticSearch es;
	private OceanThread ocean;
	private List<JSONDocumentObject> docs;
	private boolean isRunning = true;
	private Worker worker;
	/**
	 * 
	 */
	public DocumentThread(Environment env) {
		environment = env;
		es = environment.getElasticSearch();
		documentDatabase = environment.getDocumentDatabase();
		docs = new ArrayList<JSONDocumentObject>();
		ocean = new OceanThread(environment);
		isRunning = true;
		worker = new Worker();
		worker.start();	
	}

	public void addDoc(JSONDocumentObject doc) {
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
			JSONDocumentObject doc = null;
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
		
		
		void processDoc(JSONDocumentObject doc) {
			environment.logDebug("DocThread\n"+doc);
			IDocument d = new ConcordanceDocument(environment, doc);
			String docId = d.getId();
			String pmid = d.getPMID();
			String pmcid = d.getPMCID();
			String url = null;
			List<String>labels = d.listLabels();
			String label = labels.get(0);
			IResult r  = documentDatabase.put(docId, pmid, pmcid, url, label, d.getData());
			es.addDoc(d);
			ocean.addDoc(d);
			environment.logDebug("DocThread+ "+r.getErrorString());
		}
	}
}
