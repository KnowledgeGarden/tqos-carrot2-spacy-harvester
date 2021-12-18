/*
 * Copyright 2021 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2.search;

import java.util.*;

import org.topicquests.os.asr.info.InformationEnvironment;
import org.topicquests.os.asr.info.api.IInfoOcean;
import org.topicquests.pg.PostgresConnectionFactory;
import org.topicquests.pg.api.IPostgresConnection;
import org.topicquests.research.carrot2.Environment;
import org.topicquests.research.carrot2.nlp.ElasticSearch;
import org.topicquests.support.ResultPojo;
import org.topicquests.support.api.IResult;

import com.tinkerpop.blueprints.Edge;

import net.minidev.json.JSONObject;

/**
 * @author jackpark
 *
 */
public class VagabondThread {
	private Environment environment;
	private InformationEnvironment oceanEnvironment;
	private PostgresConnectionFactory database = null;

	private IInfoOcean dsl;
	private ElasticSearch es;
	private List<String> queries;
	private boolean isRunning = true;
	private Worker worker = null;
	
	/**
	 * @param env
	 */
	public VagabondThread(Environment env) {
		environment = env;
		
		oceanEnvironment = new InformationEnvironment();
		database = oceanEnvironment.getWordGramEnvironment().getSqlGraph().getProvider();
		dsl = oceanEnvironment.getDSL();
		es = environment.getElasticSearch();
		queries = new ArrayList<String>();
	}

	public void addDoc(String query) {
		if (worker == null) {
			worker = new Worker();
			worker.start();
		}
		synchronized(queries) {
			queries.add(query);
			queries.notify();
		}
	}
	
	public void shutDown() {
		synchronized(queries) {
			isRunning = false;
			queries.notify();
		}
	}
	
	class Worker extends Thread {
		
		public void run() {
			String doc = null;
			while (isRunning) {
				synchronized(queries) {
					if (queries.isEmpty()) {
						try {
							queries.wait();
						} catch (Exception e) {}
						
					} else {
						doc = queries.remove(0);
					}
				}
				if (isRunning && doc != null) {
					processQuery(doc);
					doc = null;
				}
			}
		}
		
		void processQuery(String query) {
			int begin = 0;
			int count = -1; // ignore cound
			////////
			//TODO
			// If we want an exhaustic query, might limit to 100 rather than -1
			// and repeat until it returns less that count
			////////
			IResult r  = es.get(query, begin, count);
			Object o = r.getResultObject();
			environment.logDebug("VagabondThread.XX "+o);
			Set<JSONObject> hits = (Set<JSONObject>)r.getResultObject();
			if (hits != null && !hits.isEmpty()) {
				Iterator<JSONObject> itr = hits.iterator();
				while (itr.hasNext())
					processHit(itr.next(), query);
			}
		}
		
		void processHit(JSONObject doc, String query) {
			System.out.println("QQQ "+query);
			environment.logDebug("VagabondThread.ph "+query);
			IPostgresConnection conn = null;
		    IResult r = new ResultPojo();
	        try {
	        	conn = database.getConnection();
	           	conn.setProxyRole(r);
	            conn.beginTransaction(r);
	            IResult x = dsl.processString(query, "SystemUser", null);
				String gramId = (String)x.getResultObject();
				environment.logDebug("VagabondThread.ph-1 "+query+" | "+gramId);
				// get the wordGramId
				String docId = doc.getAsString("id");
				Edge d = dsl.connectKeyWordGramToDocument(conn, gramId, docId, r);
				environment.logDebug("VagabondThread.ph+ "+docId+" | "+d);
				//NOTE: d will == null if edge already exists
	            conn.endTransaction(r);
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	environment.logError(e.getMessage(), e);
	        } finally {
		    	conn.closeConnection(r);
	        } 
			// get the wordgramID
		}
	}


}
