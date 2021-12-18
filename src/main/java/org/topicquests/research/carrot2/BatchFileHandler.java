/*
 * Copyright 2020 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2;

import java.util.*;

import org.topicquests.support.util.TextFileHandler;

import java.io.*;

/**
 * @author park
 *
 */
public class BatchFileHandler {
	private Environment environment;
	private QueryEngine engine;
	private final String QUERY_DIR_PATH = "data/query/";
	private final String SOURCE_DIR_PATH = "data/source/";
	private final String SIMPLE_SOURCE_DIR_PATH = "data/simplesource/";
	private Set<String> queries;
	private Set<String> sources;
	private Set<String> simplesources;
	private TextFileHandler handler;
	private StringBuilder buf;
	/**
	 * 
	 */
	public BatchFileHandler(Environment env) {
		environment = env;
		engine = environment.getQueryEngine();
		queries = new HashSet<String>();
		handler = new TextFileHandler();
		buf = new StringBuilder();
	}
	
	/**
	 * Run queries against the combination of sources and queries
	 */
	public void runBatchQueries() {
		sources = new HashSet<String>();
		long starttime = System.currentTimeMillis();
		fetchQueries();
		System.out.println(queries);
		System.out.println(sources);
		fetchSources();
		Iterator<String> itr = sources.iterator();
		while (itr.hasNext())
			processSource(itr.next());
		long tt = (System.currentTimeMillis() - starttime)/1000;
		System.out.println("Finished: "+tt);
	}
	
	/**
	 * Just run whatever queries are in simplesources
	 */
	public void runSimpleBatchQueries() {
		System.out.println("BFH.rsbq ");
		simplesources = new HashSet<String>();
		fetchSimpleSources();
		environment.logDebug("BFH "+simplesources);
		System.out.println("BFH.rsbq-1 "+simplesources);
		Iterator<String> itr = simplesources.iterator();
		while (itr.hasNext())
			engine.runQuery(itr.next());
	}
	
	private void processSource(String src) {
		engine.runQuery(src);
		Iterator<String>itr  = queries.iterator();
		String q;
		while (itr.hasNext()) {
			q = itr.next();
			buf.setLength(0);
			buf = buf.append(src).append(" ").append(q);
			engine.runQuery(buf.toString());
			
		}
	}

	private void fetchQueries() {
		File dir = new File(QUERY_DIR_PATH);
		File [] f = dir.listFiles();
		String line;
		int len = f.length;
		for (int i=0;i<len;i++) {
			line = handler.readFirstLine(f[i]);
			while (line != null) {
				line = line.trim();
				if (!line.equals(""))
					queries.add(line);
				line = handler.readNextLine();
			}
		}
	}
	private void fetchSources() {
		File dir = new File(SOURCE_DIR_PATH);
		File [] f = dir.listFiles();
		String line;
		int len = f.length;
		for (int i=0;i<len;i++) {
			line = handler.readFirstLine(f[i]);
			while (line != null) {
				line = line.trim();
				//"#" comments out a line
				if (!line.startsWith("#") && !line.equals(""))
					sources.add(line.trim());
				line = handler.readNextLine();
			}
		}
	}
	
	/**
	 * Fill <code>simplesources</code> with everything there
	 */
	private void fetchSimpleSources() {
		File dir = new File(SIMPLE_SOURCE_DIR_PATH);
		File [] f = dir.listFiles();
		String line;
		int len = f.length;
		System.out.println("BFH.fss "+len);
		for (int i=0;i<len;i++) {
			System.out.println("BFH.fss-1 "+f[i].getName());
			if (f[i].getName().endsWith(".txt")) {
				line = handler.readFirstLine(f[i]);
				while (line != null) {
					line = line.trim();
					//"#" comments out a line
					if (!line.startsWith("#") && !line.equals(""))
						simplesources.add(line);
					line = handler.readNextLine();
				}
			}
		}
	}

}
