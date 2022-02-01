/**
 * 
 */
package org.topicquests.research.carrot2.nlp.postprocess;

/**
 * @author jackpark
 *
 */
public class PostFileHandler {
	private PostProcessor environment;
	private Analyzer analyzer;
	private final String
		IN_PATH,
		OUT_PATH;
	/**
	 * 
	 */
	public PostFileHandler(PostProcessor env) {
		environment = env;
		IN_PATH = environment.getStringProperty("NlpPath");
		OUT_PATH = environment.getStringProperty("PostPath");
		analyzer = new Analyzer(environment);
				
	}

	/**
	 * Kickstart processing from the source
	 * feed JSON documents to analyzer
	 */
	public void beginProcessing() {
		
	}
}
