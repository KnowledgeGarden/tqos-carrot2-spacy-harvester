/*
 * Copyright 2021 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;
/**
 * @author jackpark
 *
 */
public class FileManager {
	private Environment environment;
	private final String pubMedPath;
	private final String nlpPath;

	/**
	 * 
	 */
	public FileManager(Environment env) {
		environment = env;
		pubMedPath = environment.getStringProperty("PubMedAbstractPath");
		nlpPath = environment.getStringProperty("NlpPath");
	}

	/**
	 * Persist abstracts in groups of 100 in gzip files
	 * @param pmid 
	 * @param xml
	 */
	public void persistAbstract(String pmid, String xml) {
	   	String filePath = this.pubMedPath+pmid+".xml.gz";
    	File f = new File(filePath);
    	try {
	    	if (!f.exists()) {
		    	StringBuilder buf = new StringBuilder();
		    	buf = buf.append("<?xml version=\"1.0\"?>\n")
		    			.append("<!DOCTYPE PubmedArticleSet PUBLIC \"-//NLM//DTD PubMedArticle, 1st January 2014//EN\" \"http://www.ncbi.nlm.nih.gov/corehtml/query/DTD/pubmed_140101.dtd\">\n")
		    			.append(xml);
		    	System.out.println("PD: "+f.getAbsolutePath());
		    	FileOutputStream fos = new FileOutputStream(f);
		    	GZIPOutputStream gos = new GZIPOutputStream(fos);
		    	PrintWriter out = new PrintWriter(gos);
		    	out.print(buf.toString());
		    	System.out.println("PD-1: ");
		    	out.flush();
		    	out.close();
	    	}
    	} catch (Exception e) {
    		environment.logError(e.getMessage(), e);
    		System.out.println("DANG!");
    		e.printStackTrace();
    	}
	}
	
	/** Not thread safe artifacts */
	private List<String> abstracts = new ArrayList<String>();
	private String currentPMID = null;
	/**
	 * Persist SpaCy results in groups of 100 in gzip files
	 * @param pmid if {@code null}, just save what's in the queue
	 * @param json
	 */
	public void persistSpaCy(String pmid, String json) {
		String thisPMID;
		environment.logDebug("FM.ps "+pmid+" "+abstracts.size());
		List<String> them;
		if (pmid != null) {
			if (currentPMID == null) {
		   		currentPMID = pmid;
		   		abstracts.clear();
		   		abstracts.add(json);
		   		return;
		   	} else if (currentPMID.equals(pmid)) {
		   		abstracts.add(json);
		   		return;
		   	}
		   	//otherwise go ahead and save this stuff and
			// get ready for this new pmid
			thisPMID = currentPMID;
		   	them = abstracts;
		   	abstracts = new ArrayList<String>();
		   	currentPMID = pmid;
	    	abstracts.add(json);
	   	} else {
	   		thisPMID = currentPMID;
	   		them = abstracts;
	   	}
	   	
		environment.logDebug("FM.ps-1 "+thisPMID+" "+abstracts.size());
	
		
		String filePath = this.nlpPath+thisPMID+".json.gz";
    	File f = new File(filePath);
    	try {
	    	if (!f.exists()) {		
	    		environment.logDebug("FM.ps-2 "+f.getAbsolutePath());
		    	FileOutputStream fos = new FileOutputStream(f);
	    		environment.logDebug("FM.ps-3");
		    	GZIPOutputStream gos = new GZIPOutputStream(fos);
	    		environment.logDebug("FM.ps-4");
		    	PrintWriter out = new PrintWriter(gos);
	    		//environment.logDebug("FM.ps-5\n"+them);
		    	Iterator<String>itr = them.iterator();
	    		environment.logDebug("FM.ps-6");
		    	while (itr.hasNext())
		    		out.println(itr.next());
	    		environment.logDebug("FM.ps-7");
		    	System.out.println("PD-y: ");
		    	out.flush();
	    		environment.logDebug("FM.ps-8");
		    	out.close();
	    		environment.logDebug("FM.ps-9");
	    	}
	    	
    	} catch (Exception e) {
    		environment.logDebug("FM.ps-10");
    		environment.logError(e.getMessage(), e);
    		System.out.println("SHEUTTT!");
    		e.printStackTrace();
    	}	
		environment.logDebug("FM.ps+ "+abstracts.size());
   }
}
