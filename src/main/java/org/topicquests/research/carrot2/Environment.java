/*
 * Copyright 2020 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.*;
import org.topicquests.research.carrot2.pubmed.ParserThread;
import org.topicquests.research.carrot2.search.VagabondThread;
import org.topicquests.support.RootEnvironment;

import net.minidev.json.JSONObject;

/**
 * @author park
 *
 */
public class Environment extends RootEnvironment {
	private static Environment instance;
	//NOT thread safe
	private StringBuilder buf;
	private QueryEngine engine;
	private BatchFileHandler batcher;
	private ParserThread parserThread;
	private Accountant accountant;
	private FileManager fileManager;
	private VagabondThread vagabondThread;
	
	private JSONObject queryInstrumentation;
	private JSONObject nlpInstrumentation;
	private Set<String> keywordInstrumentation;
	private final String STATS_PATH;
	
	/**
	 * 
	 */
	public Environment() {
		super("config-props.xml", "logger.properties");
		logDebug("Environment ");
		buf = new StringBuilder();
		accountant = new Accountant(this);
		fileManager = new FileManager(this);
		engine = new QueryEngine(this);
		STATS_PATH = getStringProperty("StatsPath");
		logDebug("Environment- "+engine);
		System.out.println("E1");
		batcher = new BatchFileHandler(this);
		String schemaName = getStringProperty("DatabaseSchema");
		System.out.println("E2 "+schemaName);
		logDebug("Environment-1 "+schemaName);
		System.out.println("E5");
		parserThread = new ParserThread(this);
		vagabondThread = new VagabondThread(this);
		System.out.println("E6");
		logDebug("Environment-2 "+parserThread);
		queryInstrumentation = new JSONObject();
		nlpInstrumentation  = new JSONObject();
		keywordInstrumentation = new HashSet<String>();
		instance = this;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			
			@Override
			public void run() {
				shutDown();
			}
		});
		logDebug("Environment booted");
		System.out.println("E7");
	}
	
	/////////////////////
	// DSL
	/////////////////////
	/**
	 * Called from PubMedDocumentSource when it has
	 * a new PubMed Doc for processing
	 * @param pmid
	 * @param xml
	 */
	public void addPubMedAbstract(String pmid, String xml) {
		logDebug("Env.add");
		if (!accountant.seenBefore(pmid))
			parserThread.addDoc(xml);
		fileManager.persistAbstract(pmid, xml);
	}
	
	/**
	 * Callback from ParserThread when its
	 * queue runs empty
	 */
	public void queueEmpty() {
		accountant.bump();
		//fileManager.bump();
	}
	
	/////////////////////
	// Support
	/////////////////////
	
	public VagabondThread getVagabondThread() {
		return vagabondThread;
	}
	
	public FileManager getFileManager() {
		return fileManager;
	}
	public Accountant getAccountant() {
		return accountant;
	}
	
	public static Environment getInstance() {
		return instance;
	}

	public BatchFileHandler getBatchFileHandler() {
		return batcher;
	}
	public QueryEngine getQueryEngine() {
		return engine;
	}
	

	/**
	 * Utility to convert a Carrot2 <code>query</code> to a file name
	 * @param path
	 * @param query
	 * @return
	 */
	public String queryToFileName(String path, String query) {
		String x = query.replaceAll(" ", "_");
		buf.setLength(0);
		buf = buf.append(path).append(x).append(".xml");
		return buf.toString();
	}
	
	//////////////////////////
	// Instrumentation
	/////////////////////////
	
	public void queryTiming(String query, long delta) {
		queryInstrumentation.put(query, new Long(delta));
	}
	
	public void nlpTiming(String pmid, long delta) {
		nlpInstrumentation.put(pmid, new Long(delta));
	}
	
	public void keywords(List<String> keywords) {
		if (keywords != null && !keywords.isEmpty())
			keywordInstrumentation.addAll(keywords);
	}
	
	void saveInstrumentation() {
		PrintWriter out;
		FileOutputStream fos;
		//query
		try
        {
			fos = new FileOutputStream(new File(STATS_PATH+"Query.json"));
			out = new PrintWriter(fos);
			out.write(queryInstrumentation.toJSONString());
			out.flush();
			out.close();
        } catch (Exception e) {
        	logError(e.getMessage(), e);
        	e.printStackTrace();
        }
		//nlp
		try
        {
			fos = new FileOutputStream(new File(STATS_PATH+"NLP.json"));
			out = new PrintWriter(fos);
			out.write(nlpInstrumentation.toJSONString());
			out.flush();
			out.close();
        } catch (Exception e) {
        	logError(e.getMessage(), e);
        	e.printStackTrace();
        }
		//keywords
		try
        {
            fos = new FileOutputStream(STATS_PATH+"MyKeywords");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(keywordInstrumentation);
            oos.close();
            fos.close();
        } 
        catch (IOException ioe) 
        {
        	logError(ioe.getMessage(), ioe);
            ioe.printStackTrace();
        }
		//TODO
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Environment();
	}

	@Override
	public void shutDown() {
		logDebug("Environment.shuttingDown");
		System.out.println("Environment.shutDown");
		saveInstrumentation();
		parserThread.shutDown();
	}

}
