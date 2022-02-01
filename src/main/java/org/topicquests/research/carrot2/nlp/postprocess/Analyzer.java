/**
 * 
 */
package org.topicquests.research.carrot2.nlp.postprocess;

import java.util.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * @author jackpark
 *
 */
public class Analyzer {
	private PostProcessor environment;
	private static final String
		SUBSTANCES 	= "substances",
		TAGS		= "tags",
		RAW			= "raw",
		XML			= "xml",
		PMID		= "pmid",
		TITLE		= "title",
		ABSTRACT	= "abstract",
		LANG		= "lang",
		RESULTS		= "results", 	// outer and nested
		MODEL		= "model",
		TEXT		= "text", 		// usually the abstract - also each sentence - repeats for each modeel
		CONCEPTS	= "concepts", 	// ontology stuff
			// concepts repeat on the same abstract for each model
			// TODO - are they the same concepts?
		MARKUP		= "markup", 	//html for displaying results
		SENTENCES	= "sentences", 	// array of each sentence in abstract
		NODES		= "nodes", 		//POS - some include concepts
		TREE		= "tree",		// parse tree
		TREES		= "trees",		// collection of trees by model
		ID			= "id", 		// sentence id
		DOC_ID		= "docId";		// usually same as pmid
	///////////////////////////////////////////////////
	// We start with an abstract == one to many sentences
	// We pass that abstract through several models
	// We get back, among other things, POS and parse trees for each model
	// A goal here is to aggregate on sentences
	//	"sentences": [
	//		{ "text": ...
	//		  "nodes": [
	//			{ "model": ...,
	//		  	  "id": ... // sentence id
	//		  	  "nodes": ... }...]
	//		  "trees": [
	//			{ "model": ...
	//			  "tree": .., }...]
	//	
	//		} ...]
	// And bulk concepts
	//  
	//	"concepts": [
	//		
	//
	///////////////////////////////////////////////////
	/**
	 * 
	 */
	public Analyzer(PostProcessor env) {
		environment = env;
	}

	/**
	 * 
	 * @param doc = input
	 */
	public JSONObject analyzeDocument(JSONObject doc) {
		JSONObject result = new JSONObject();
		result.put(PMID, doc.getAsString(PMID));
		Object x = doc.get(SUBSTANCES);
		if (x != null)
			result.put(SUBSTANCES, x);
		x = doc.get(TAGS);
		if (x != null)
			result.put(TAGS, x);
		result.put(TITLE, doc.getAsString(TITLE));
		result.put(ABSTRACT, doc.getAsString(ABSTRACT));
		result.put(LANG, doc.getAsString(LANG));
		result.put(XML, doc.getAsString(RAW));

		//first level results, one for each model
		JSONArray ja = (JSONArray)doc.get("results");
		Iterator<Object> itr = ja.iterator();
		JSONObject jo;
		while (itr.hasNext()) {
			jo = (JSONObject)itr.next();
			processModel(jo, doc, result);
		}
		
		return result;
	}
	
	void processModel(JSONObject modelObject, JSONObject doc, JSONObject result) {
		String myModel = modelObject.getAsString(MODEL);
		//we ignore the text field since it is the abstract
		//The abstract's parse for this model
		JSONObject abstractResults = (JSONObject)modelObject.get(RESULTS);
		//Ontology concepts for this abstract
		JSONObject bulkConcepts = (JSONObject)abstractResults.get(CONCEPTS);
		//All the sentences for this abstract
		JSONArray sentences = (JSONArray)abstractResults.get(SENTENCES);
		processSentences(myModel, sentences, result);
		
	}
	
	void processSentences(String myModel, JSONArray sentences, JSONObject result) {
		Iterator<Object> itr = sentences.iterator();
		while (itr.hasNext()) 
			processSentence(myModel, (JSONObject)itr.next(), result);
	}
	
	// TODO huge issue here
	// we need a sentence object which is the sentence, then a mynodes and mytrees arrays
	//
	void processSentence(String myModel, JSONObject sentence, JSONObject result) {
		JSONArray mySentences = (JSONArray)result.get(SENTENCES);
		// mySentences should have { "text", "nodes"[], "trees[]" } objects
		JSONArray myNodes, myTrees;
		JSONObject so, jo;
		if (mySentences == null) {
			mySentences = new JSONArray();
			result.put(SENTENCES, mySentences);
		}
		// In the beginning, the sentence object
		so = new JSONObject();
		// First, the nodes
		jo = new JSONObject();
		jo.put(MODEL, myModel);
		jo.put(NODES, sentence.get(NODES));
		mySentences.add(jo);
		// Then, the trees
		// TODO
	}
}
