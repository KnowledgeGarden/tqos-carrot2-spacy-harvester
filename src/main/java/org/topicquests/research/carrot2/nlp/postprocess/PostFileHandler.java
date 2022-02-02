/**
 * 
 */
package org.topicquests.research.carrot2.nlp.postprocess;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.topicquests.support.util.TextFileHandler;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

/**
 * @author jackpark
 *
 */
public class PostFileHandler {
	private PostProcessor environment;
	private Analyzer analyzer;
	private TextFileHandler handler;
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
		handler = new TextFileHandler();
	}

	/**
	 * Kickstart processing from the source
	 * feed JSON documents to analyzer
	 */
	public void beginProcessing() {
		File dir = new File(IN_PATH);
		System.out.println("Starting on "+dir);
		File [] f = dir.listFiles();
		String name;
		int len = f.length;
		
		for (int i=0;i<len;i++) {
			name = f[i].getName();
			System.out.println("Reading "+name);

			if (name.endsWith(".gz")) 
				processGzFile(f[i]);
			else if (f[i].isDirectory())
				handleNestedDirectory(f[i]);
			// otherewise ignore
		}
	}
	
	void handleNestedDirectory(File dir) {
		
		File [] f = dir.listFiles();
		String name;
		int len = f.length;
		for (int i=0;i<len;i++) {
			name = f[i].getName();
			if (name.endsWith(".gz"))
				processGzFile(f[i]);
			// otherewise ignore
		}
	}
	
	void processGzFile(File gzFile) {
		System.out.println(gzFile.getName());
		try {
			InputStream is = new FileInputStream(gzFile);
			GZIPInputStream gzr = new GZIPInputStream(is);
			
			JSONParser p = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
			JSONObject jo = (JSONObject)p.parse(gzr);
			environment.logDebug("Analyzing "+jo.getAsString(Analyzer.ID));
			JSONObject result = analyzer.analyzeDocument(jo);
			saveObject(result);
		} catch (Exception e) {
			e.printStackTrace();
			environment.logError(e.getMessage(), e);
		}
	}
	
	void saveObject(JSONObject doc) throws Exception {
		String id = doc.getAsString(Analyzer.PMID);
		id = id+".json.gz";
		String path = OUT_PATH+id;
		System.out.println("Saving "+path);
		File f = new File(path);
		FileOutputStream fos = new FileOutputStream(f);
		GZIPOutputStream gos = new GZIPOutputStream(fos);
		PrintWriter pw = new PrintWriter(gos);
		pw.print(doc.toJSONString());
		pw.flush();
		pw.close();
	}
}
