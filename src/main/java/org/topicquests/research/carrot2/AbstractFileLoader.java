/*
 * Copyright 2021 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import org.topicquests.research.carrot2.pubmed.ParserThread;

/**
 * @author jackpark
 *
 */
public class AbstractFileLoader {
	private Environment environment;
	private final String pubMedPath;
	private ParserThread parser;

	/**
	 * 
	 */
	public AbstractFileLoader(Environment env) {
		environment = env;
		pubMedPath = environment.getStringProperty("PubMedAbstractPath");
		parser = environment.getParserThread();
	}

	//////////////////////////////////////
	// We need to load abstracts in groups of, say, 6 at a time
	// because we want to send a large number of abstracts to the server
	//  - one at a time - 
	// against the same model; that reduces model swapping at the server
	//////////////////////////////////////

	/**
	 * Load files from {@code pubMedPath} as XML files, then pass first
	 * to the PubMedPullParser, then to SpaCy
	 */
	public void bootFiles() {
		File root = new File(pubMedPath);
		if (!root.exists())
			throw new RuntimeException("Crap!");
		try {
			String [] names = root.list();
			int len = names.length;
			String nx;
			for (int i=0;i<len; i++) {
				nx = names[i];
				if (nx.endsWith(".gz"))
					bootFile(pubMedPath+nx);
				else
					bootDirectory(pubMedPath+nx);
			}
			
		} catch (Exception e) {
			environment.logError(e.getMessage(), e);
			e.printStackTrace();
		}
		environment.filesLoaded();
	}
	
	/**
	 * We only nest one level from root - we don't recurse here (we could)
	 * @param path
	 * @throws Exception
	 */
	void bootDirectory(String path) throws Exception {
		File f = new File(path);
		if (f.exists() && f.isDirectory()) {
			String [] names = f.list();
			String nx;
			int len = names.length;
			for (int i=0;i<len;i++) {
				bootFile(path+names[i]);
			}
		}

	}
	
	void bootFile(String path) throws Exception {
		File f = new File(path);
		if (f.exists()) {
			environment.logDebug("LOADING "+path);
			processFile(f);
		}
	}
	
	void processFile(File abst) throws Exception {
		try {
			FileInputStream fis = new FileInputStream(abst);
			GZIPInputStream gis = new GZIPInputStream(fis);
			InputStreamReader rdr = new InputStreamReader(gis, "UTF-8");
			BufferedReader brdr = new BufferedReader(rdr);
			StringBuilder buf = new StringBuilder();
			String line;
			while ((line = brdr.readLine()) != null)
				buf.append(line);
			String xml = buf.toString().trim();
			// we now have an xml file.
			// send it to the parser
			environment.logDebug("AbstractFileLoader got "+xml.length());
			parser.addDoc(xml);
			
		} catch (Exception e) {
			e.printStackTrace();
			environment.logError(e.getMessage(), e);
		}
	}
}
