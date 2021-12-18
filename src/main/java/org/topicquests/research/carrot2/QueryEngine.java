/*
 * Copyright 2020 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2;

import java.util.HashMap;
import java.util.Map;
import java.io.*;

import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.ProcessingResult;
import org.carrot2.core.attribute.CommonAttributesDescriptor;
import org.topicquests.research.carrot2.impl.PubMedDocumentSource;


/**
 * @author park
 * based on Carrot2 example: ClusteringDataFromPubMed
 */
public class QueryEngine {
	private Environment environment;
	private final String outputPath;
	private Controller controller;
	private StringBuilder buf;
	private Accountant accountant;
	private final int HOW_MANY, COUNT;
	/**
	 * 
	 */
	public QueryEngine(Environment env) {
		environment = env;
		outputPath = environment.getStringProperty("ClusterDataPath");
		accountant = environment.getAccountant();
		buf = new StringBuilder();
		String x =  environment.getStringProperty("MAX_COUNT");
		HOW_MANY = Integer.parseInt(x);
		x = environment.getStringProperty("COUNT");
		COUNT = Integer.parseInt(x);
	}

	public void runQuery(String query) {
		environment.logDebug("QE "+query);
		environment.getAccountant().newQuery(query);
		if (query.equals(""))
			return;
		controller = ControllerFactory.createSimple();
		Map<String, Object> attributes = new HashMap<String, Object>();
		int total = COUNT;
		int start = 0;
		int count = 0;
		long howMany = 0;
		int i = 0;
        while (true) {
        	//create some attributes
            CommonAttributesDescriptor
            .attributeBuilder(attributes)
            .query(query)
            .results(total)
            .start(start);
            buf.setLength(0);
    		buf = buf.append(query).append(i);
    		File f = new File(environment.queryToFileName(outputPath, buf.toString()));
    		System.out.println(f.getAbsolutePath());
    		if (!f.exists()) {
	        	ProcessingResult result = runProcess(attributes, query, f);
	        	if (result == null) // try one more time
	        		result = runProcess(attributes, query, f);
	        	i++;
	        	count = result.getDocuments().size();
	        	//update start
	        	start += count;
	        	howMany += (long)count;
	        	System.out.println(count+" "+howMany);
	        	if (count < total)
	        		break;
	        	if (howMany >= HOW_MANY)
	        		break;
	        	synchronized(buf) {
		        	try {
		        		buf.wait(4000); // 4 second delay
		        	} catch (Exception e) {}
	        	}
    		} else {
    			f = null;
    			break;
    		}
    		f = null;
        }
		environment.getAccountant().endQuery(query);

	}
	
	private ProcessingResult runProcess(Map<String, Object> attributes, String query, File f) {
		System.out.println(attributes);
		ProcessingResult result = null;
		try {
			result =  controller.process(attributes,
	            PubMedDocumentSource.class, LingoClusteringAlgorithm.class);
		} catch (Exception e) {
			System.out.println("Second Try");
        	try {
        		buf.wait(30000); // 30 second delay
        	} catch (Exception x) {}
        	result =  controller.process(attributes,
    	            PubMedDocumentSource.class, LingoClusteringAlgorithm.class);
		}
		if (result != null) {
			//don't save cluster results if there are no clusters -- that can happen!
			//See example below of a bad cluster document
			if (result.getDocuments().size() > 0) {
				try {
		        	
		        	FileOutputStream fos = new FileOutputStream(f);
		        	result.serialize(fos, true, true);
		        	fos.flush();
		        	fos.close();
		        } catch (Exception e) {
		        	e.printStackTrace();
		        } 
			}
		}
		System.gc();
        return result;
	}
	
}

/** Example of a cluster file with no data
<searchresult>
   <query>acrylamide desensitizes</query>
   <attribute key="MultilingualClustering.languageCounts">
      <value>
         <wrapper class="org.carrot2.util.simplexml.MapSimpleXmlWrapper">
            <map/>
         </wrapper>
      </value>
   </attribute>
   <attribute key="processing-time-algorithm">
      <value type="java.lang.Long" value="0"/>
   </attribute>
   <attribute key="start">
      <value type="java.lang.Integer" value="0"/>
   </attribute>
   <attribute key="SearchEngineBase.compressed">
      <value type="java.lang.Boolean" value="false"/>
   </attribute>
   <attribute key="SearchEngineStats.queries">
      <value type="java.lang.Integer" value="0"/>
   </attribute>
   <attribute key="processing-time-source">
      <value type="java.lang.Long" value="428"/>
   </attribute>
   <attribute key="SearchEngineStats.pageRequests">
      <value type="java.lang.Integer" value="0"/>
   </attribute>
   <attribute key="MultilingualClustering.majorityLanguage">
      <value value=""/>
   </attribute>
   <attribute key="processing-time-total">
      <value type="java.lang.Long" value="428"/>
   </attribute>
   <attribute key="results">
      <value type="java.lang.Integer" value="150"/>
   </attribute>
   <attribute key="results-total">
      <value type="java.lang.Long" value="0"/>
   </attribute>
</searchresult>
 */
