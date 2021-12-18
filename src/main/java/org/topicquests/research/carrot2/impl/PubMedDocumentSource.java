
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2015, Dawid Weiss, Stanisław Osiński.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

//package org.carrot2.source.pubmed;
package org.topicquests.research.carrot2.impl;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpStatus;
import org.carrot2.core.Document;
import org.carrot2.core.LanguageCode;
import org.carrot2.core.attribute.Init;
import org.carrot2.core.attribute.Internal;
import org.carrot2.core.attribute.Processing;
import org.carrot2.source.SearchEngineResponse;
import org.carrot2.source.SimpleSearchEngine;
import org.carrot2.util.StringUtils;
import org.carrot2.util.attribute.Attribute;
import org.carrot2.util.attribute.AttributeLevel;
import org.carrot2.util.attribute.Bindable;
import org.carrot2.util.attribute.DefaultGroups;
import org.carrot2.util.attribute.Group;
import org.carrot2.util.attribute.Input;
import org.carrot2.util.attribute.Label;
import org.carrot2.util.attribute.Level;
import org.carrot2.util.attribute.constraint.IntRange;
import org.carrot2.util.httpclient.HttpClientFactory;
import org.carrot2.util.httpclient.HttpRedirectStrategy;
import org.carrot2.util.httpclient.HttpUtils;
import org.topicquests.research.carrot2.Environment;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Performs searches on the PubMed database using its on-line e-utilities:
 * http://eutils.ncbi.nlm.nih.gov/entrez/query/static/eutils_help.html
 */
@Bindable(prefix = "PubMedDocumentSource")
public class PubMedDocumentSource extends SimpleSearchEngine
{
	private Environment environment;

	private final String path  = "data/output/";
    /** PubMed search service URL */
    public static final String E_SEARCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";

    /** PubMed fetch service URL */
    public static final String E_FETCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";

    /** HTTP timeout for pubmed services.*/
    public static final int PUBMED_TIMEOUT = HttpClientFactory.DEFAULT_TIMEOUT * 3;

    /**
     * Tool name, if registered.
     * @see "http://www.ncbi.nlm.nih.gov"
     */
    @Init
    @Input
    @Attribute
    @Label("EUtils Registered Tool Name")
    @Level(AttributeLevel.ADVANCED)
    @Group(DefaultGroups.QUERY)
    public String toolName = "Carrot Search";

    /**
     * Maximum results to fetch. No more than the specified number of results
     * will be fetched from PubMed, regardless of the requested number of results. 
     */
    @Processing
    @Input
    @Attribute
    @IntRange(min = 1)
    @Internal(configuration = true)
    @Label("Maximum results")
    @Level(AttributeLevel.ADVANCED)
    @Group(DefaultGroups.QUERY)
    public int maxResults = 150;
    
    /**
     * HTTP redirect response strategy (follow or throw an error).
     */
    @Input
    @Processing
    @Attribute
    @Label("HTTP redirect strategy")
    @Level(AttributeLevel.MEDIUM)
    @Group(SimpleSearchEngine.SERVICE)
    @Internal
    public HttpRedirectStrategy redirectStrategy = HttpRedirectStrategy.NO_REDIRECTS; 

    @Override
    protected SearchEngineResponse fetchSearchResponse() throws Exception
    {
    	if (environment == null)
    		environment = Environment.getInstance();
    	environment.logDebug("PMDS.fetch");
    	System.out.println("FF "+start+" "+environment);
        PubMedIdSearchHandler idResponse = getPubMedIds(query, results, start);
    	System.out.println("FF-1 "+idResponse.getMatchCount());
        SearchEngineResponse response = getPubMedAbstracts(idResponse.getPubMedPrimaryIds());
    	System.out.println("FF-2 "+response.getResultsTotal());
        response.metadata.put(SearchEngineResponse.RESULTS_TOTAL_KEY, idResponse.getMatchCount());
        return response;
    }

    @Override
    protected void afterFetch(SearchEngineResponse response)
    {
        for (Document document : response.results)
        {
            document.setLanguage(LanguageCode.ENGLISH);
        }
    }

    /**
     * Gets PubMed entry ids matching the query.
     */
    private PubMedIdSearchHandler getPubMedIds(final String query, final int requestedResults, final int offset)
        throws Exception
    {
        final XMLReader reader = newXmlReader();
        PubMedIdSearchHandler searchHandler = new PubMedIdSearchHandler();
        reader.setContentHandler(searchHandler);
        StringBuilder buf = new StringBuilder(E_SEARCH_URL)
        	.append("?db=pubmed")
        	.append("&usehistory=n&")
        	.append("&term=").append(StringUtils.urlEncodeWrapException(query, "UTF-8"))
        	.append("&retmax=").append(Integer.toString(Math.min(requestedResults, maxResults)))
        	.append("&retstart=").append(Integer.toString(offset))
        	.append("&tool=").append(StringUtils.urlEncodeWrapException(toolName, "UTF-8"));
        final String url = buf.toString();

        final HttpUtils.Response response = HttpUtils.doGET(
            url, 
            null, 
            null,
            null, null, 
            PUBMED_TIMEOUT,
            redirectStrategy.value());

        // Get document IDs
        if (response.status == HttpStatus.SC_OK)
        {
            reader.parse(new InputSource(response.getPayloadAsStream()));
        }
        else
        {
            throw new IOException("PubMed returned HTTP Error: " + response.status
                + ", HTTP payload: " + new String(response.payload, "iso8859-1"));
        }

        return searchHandler;
    }

    String getPMID(String xml) {
    	String result = "";
    	int where = xml.indexOf("<PMID");
	    	if (where > -1) {
	    		result = xml.substring(where);
	    	where = result.indexOf('>');
	    	if (where > -1)
	    		result = result.substring(where+1);
	    	where = result.indexOf('<');
	    	if (where > -1) 
	    		result = result.substring(0,where);
    	}
    	return result;
    }
    
    List<String> splitDocs(String docs) {
    	
    	List<String>result = new ArrayList<String>();
    	String source = docs.trim();
    	String aDoc = "";
    	//System.out.println(docs);
    	int where = -1;
    	int where2 = -1;
    	boolean first = true;
    	while ((where = source.indexOf("<PubmedArticle>")) > -1) {
    		//System.out.println(where);	
			aDoc = source.substring(where);
			//look for next one sitting in aDoc
			where2 = aDoc.indexOf("<PubmedArticle>",5);
			if (where2 > -1) {
				//it's there, so strip it off
    			aDoc = aDoc.substring(0, where2).trim();
    			result.add(aDoc);
    			// bump source
    			where2 = aDoc.length();
    			if (source.length() > where2)
    				source = source.substring(where2).trim();
			} else {
				//there are no trailing docs
				where = aDoc.indexOf("</PubmedArticleSet>");
				if (where > -1)
					aDoc = aDoc.substring(0, where);
				result.add(aDoc.trim());
				break;
			}	
    	}
    	return result;
    }
    
    /**
     * Send {@code xml} to the parser for processing
     * and save as a file locally
     * @param xml
     * @throws Exception
     */
    private void processDocument(String xml) throws Exception {
    	environment.logDebug("PMDS.process");
    	String pmid = getPMID(xml);
    	if (pmid.equals("")) return; // no content
    	environment.addPubMedAbstract(pmid, xml);
    	StringBuilder buf = new StringBuilder(path).append(pmid).append(".xml");
/*
    	
    	String filePath = buf.toString();
    	File f = new File(filePath);
    	if (!f.exists()) {
	    	buf.setLength(0);;
	    	buf = buf.append("<?xml version=\"1.0\"?>\n")
	    			.append("<!DOCTYPE PubmedArticleSet PUBLIC \"-//NLM//DTD PubMedArticle, 1st January 2014//EN\" \"http://www.ncbi.nlm.nih.gov/corehtml/query/DTD/pubmed_140101.dtd\">\n")
	    			.append(xml);
	    	System.out.println("PD: "+f.getAbsolutePath());
	    	//FileOutputStream fos = new FileOutputStream(f);
	    	try {
	    	PrintWriter out = new PrintWriter(f, StandardCharsets.UTF_8.toString());
	    	out.print(buf.toString());
	    	System.out.println("PD-1: ");
	    	out.flush();
	    	out.close();
	    	} catch (Exception e) {
	    		System.out.println("DANG!");
	    		e.printStackTrace();
	    	}
    	}
    	*/
    }
    
    private void captureDocument(InputStream is) throws Exception {
    	StringBuilder buf = new StringBuilder();
    	int c;
    	String pmid;
    	while ((c = is.read()) > -1) {
    		buf.append((char)c);
    		
    	}
    	String xml = buf.toString();
    	List<String> docs = splitDocs(xml);
    	int len = docs.size();
    	for (int i=0;i<len;i++) {
    		processDocument(docs.get(i));
    	}
    }
    /**
     * Gets PubMed abstracts corresponding to the provided ids.
     */
    private SearchEngineResponse getPubMedAbstracts(List<String> ids) throws Exception
    {
    	System.out.println("SER "+ids);
        if (ids.isEmpty()) 
        {
            return new SearchEngineResponse();
        }
        
        final XMLReader reader = newXmlReader();
        final PubMedContentHandler fetchHandler = new PubMedContentHandler();
        reader.setContentHandler(fetchHandler);
        StringBuilder buf = new StringBuilder(E_FETCH_URL)
        	.append("?db=pubmed")
        	.append("&retmode=xml" )
        	.append("&rettype=abstract")
        	.append("&id=").append(getIdsString(ids))
        	.append("&tool=").append(StringUtils.urlEncodeWrapException(toolName, "UTF-8"));
        final String url =  buf.toString();
    	System.out.println("SER-1 "+url);

        final HttpUtils.Response response = HttpUtils.doGET(
            url, 
            null, null,
            null, null, 
            PUBMED_TIMEOUT,
            redirectStrategy.value());
    	System.out.println("SER-2 "+response.status);

        // Get document contents
        // No URL logging here, as the url can get really long
        if (response.status == HttpStatus.SC_OK)
        {
        	InputStream is = response.getPayloadAsStream();
        	captureDocument(is);
            reader.parse(new InputSource(response.getPayloadAsStream()));
        }
        else
        {
            throw new IOException("PubMed returned HTTP Error: " + response.status
                + ", HTTP payload: " + new String(response.payload, "iso8859-1"));
        }
        return fetchHandler.getResponse();
    }

    static XMLReader newXmlReader()
        throws SAXException, ParserConfigurationException
    {
        XMLReader reader = SAXParserFactory.newInstance()
            .newSAXParser()
            .getXMLReader();
        reader.setFeature("http://xml.org/sax/features/validation", false);
        reader.setFeature("http://xml.org/sax/features/namespaces", true);
        reader.setEntityResolver(new EmptyEntityResolver());
        return reader;
    }

    private String getIdsString(List<String> ids)
    {
        final StringBuilder buf = new StringBuilder();
        for (String id : ids)
        {
            buf.append(id);
            buf.append(",");
        }

        if (buf.length() > 0)
        {
            return buf.substring(0, buf.length() - 1);
        }
        else
        {
            return "";
        }
    }
}
