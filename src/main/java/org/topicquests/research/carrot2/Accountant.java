/*
 * Copyright 2021 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2;

import java.util.*;
import java.io.*;
/**
 * @author jackpark
 * A class for <em>keeping track of stuff</em>
 */
public class Accountant {
	private Environment environment;
	private List<String> docs;
	private List<String> busy;
	private Map<String, Long>queries;
	private Map<String, Long>history;
	private final String
		DOCS_FILE	= "data/doclist";
	/**
	 * 
	 */
	public Accountant(Environment env) {
		environment = env;
		docs = new ArrayList<String>();
		busy = new ArrayList<String>();
		queries = new HashMap<String, Long>();
		history = new HashMap<String, Long>();
		bootDocs();
		environment.logDebug("SeenDocs "+docs.size());
	}
	
	/**
	 * A signal that the ParserThread is empty;
	 * use that to safe files, etc
	 */
	public void bump() {
		saveDocs();
	}
	/**
	 * Prevent duplicate document processing
	 * @param pmid
	 * @return
	 */
	public boolean seenBefore(String pmid) {
		synchronized(docs) {
			boolean result = docs.contains(pmid);
			if (!result) {
				result |= busy.contains(pmid);
				if (!result)
					busy.add(pmid);
			}
			return result;
		}
	}
	
	
	/**
	 * Successfully processed {@code pmid}
	 * @param pmid
	 */
	public void haveSeen(String pmid) {
		synchronized(docs) {
			docs.add(pmid);
			busy.remove(pmid);
			saveDocs();
		}
	}
	
	void bootDocs() {
		File f = new File(DOCS_FILE);
		if (!f.exists())
			return;
				
		try
        {
            FileInputStream fis = new FileInputStream(DOCS_FILE);
            ObjectInputStream ois = new ObjectInputStream(fis);
 
            docs = (ArrayList) ois.readObject();
 
            ois.close();
            fis.close();
        } 
        catch (Exception ioe) 
        {
        	environment.logError(ioe.getMessage(), ioe);
            ioe.printStackTrace();
        } 
	}
	
	void saveDocs() {
		try
        {
            FileOutputStream fos = new FileOutputStream(DOCS_FILE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(docs);
            oos.close();
            fos.close();
        } 
        catch (IOException ioe) 
        {
        	environment.logError(ioe.getMessage(), ioe);
            ioe.printStackTrace();
        }
	}
	
	///////////////////////
	// Instrumentation
	// Timing queries
	// QuerySession:
	//		A session begins when the query queue is
	//			filled
	//		It ends when the last query in that queue finished
	//		This gives an overall sense of time to complete a queue
	// NewQuery
	//		Starts a clock against that particular query
	//		Ends with either another NewQuery or endQuerySession
	///////////////////////
	
	/**
	 * When a PubMed query que begins to fill up
	 */
	public void beginQuerySession() {
		//TODO
	}
	
	/**
	 * When the last PubMed query has ended
	 */
	public void endQuerySession() {
		//TODO
	}
	
	public void newQuery(String queryString) {
		queries.put(queryString, new Long(System.currentTimeMillis()));
	}
	
	public void endQuery(String queryString) {
		Long x = queries.get(queryString);
		long delta = System.currentTimeMillis() - x.longValue();
		environment.queryTiming(queryString, delta);
		history.put(queryString, new Long(delta));
		environment.logDebug("History\n"+history);
	}
	
	public void shutDown() {
		saveDocs();
		environment.logDebug("HistoryFinal\n"+history);
	}
}
