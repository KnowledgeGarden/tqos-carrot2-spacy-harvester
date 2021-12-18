/*
 * Copyright 2021 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2.nlp;

import java.util.ArrayList;
import java.util.List;

import org.topicquests.os.asr.driver.sp.SpacyDriverEnvironment;
import org.topicquests.research.carrot2.Environment;
import org.topicquests.research.carrot2.FileManager;
import org.topicquests.support.api.IResult;

import net.minidev.json.JSONObject;

/**
 * @author jackpark
 *
 */
public class SpaCyThread {
	private Environment environment;
	private FileManager fileManager;
	private SpacyDriverEnvironment spaCy;
	private List<JSONObject> paragraphs;
	private boolean isRunning = true;
	private Worker worker = null;
	private boolean wasRunning = false;

	/**
	 * 
	 */
	public SpaCyThread(Environment env) {
		environment = env;
		spaCy = new SpacyDriverEnvironment();
		fileManager = environment.getFileManager();
		paragraphs = new ArrayList<JSONObject>();
		environment.logDebug("SpaCyThread.boot");
	}

	public void addDoc(String docId, String paragraph) {
		if (worker == null) {
			worker = new Worker();
			worker.start();
		}
		JSONObject d = new JSONObject();
		d.put("id", docId);
		d.put("text", paragraph);

		synchronized(paragraphs) {
			environment.logDebug("SpaCyThread.add "+docId+" "+paragraphs.size());
			paragraphs.add(d);
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
			environment.logDebug("ParserThread.starting");
			JSONObject doc = null;
			while (isRunning) {
				synchronized(paragraphs) {
					environment.logDebug("SpaCyThread-XX "+paragraphs.size());
					if (paragraphs.isEmpty()) {
						if (wasRunning) {
							wasRunning = false;
							fileManager.persistSpaCy(null, null);
						}
						try {
							paragraphs.wait();
						} catch (Exception e) {}
						
					} else {
						doc = paragraphs.remove(0);
					}
				}
				if (isRunning && doc != null) {
					wasRunning = true;
					processDoc(doc);
					doc = null;
					environment.logDebug("SpaCyThread-R "+isRunning);
				}
			}
		}
		
		void processDoc(JSONObject paragraph) {
			int ps = 0;
			long startTime = System.currentTimeMillis();
			synchronized(paragraphs) {
				ps = paragraphs.size();
			}
			environment.logDebug("SpaCyThread-x "+ps);
			String docId = paragraph.getAsString("id");
			System.out.println("STp "+docId+" "+ps);
			String text = paragraph.getAsString("text");
			text = cleanParagraph(text);
			IResult r = spaCy.processSentence(text);
			JSONObject jo = (JSONObject)r.getResultObject();
			jo.put("docId", docId);
			long delta = System.currentTimeMillis() - startTime;
			environment.nlpTiming(docId, delta); // instrument
			System.out.println("STp+ "+jo.size());
			environment.logDebug("SpaCyThread+ "+jo.size());
			fileManager.persistSpaCy(docId, jo.toJSONString());
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
	}
}
