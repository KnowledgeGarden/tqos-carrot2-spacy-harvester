/**
 * 
 */
package org.topicquests.research.carrot2.nlp.postprocess;

import org.topicquests.support.RootEnvironment;

/**
 * @author jackpark
 * Read json.gz files from the NlpPath, process them, and
 * return them to the PostPath as json.gz files
 */
public class PostProcessor extends RootEnvironment {
	private PostFileHandler filer;

	/**
	 * 
	 */
	public PostProcessor() {
		super("config-props.xml", "logger.properties");
		filer = new PostFileHandler(this);
		System.out.println("Booted "+filer);
		//begin processing
		filer.beginProcessing();
	}

	@Override
	public void shutDown() {
		// TODO Auto-generated method stub

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new PostProcessor();
	}

}
