/*
 * Copyright 2020 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2;


/**
 * @author jackpark
 *
 */
public class SimpleTest {
	private Environment environment;
	private AbstractFileLoader loader;
	/**
	 * 
	 */
	public SimpleTest() {
		environment = new Environment();
		environment.logDebug("ST");
		//BatchFileHandler h = environment.getBatchFileHandler();
		//h.runSimpleBatchQueries();
		loader = new AbstractFileLoader(environment);
		loader.bootFiles();
	}

	public static void main(String[] args) {
		new SimpleTest();
	}

}
