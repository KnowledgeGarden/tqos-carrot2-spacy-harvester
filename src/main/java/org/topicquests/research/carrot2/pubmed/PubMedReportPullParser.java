/*
 * Copyright 2020 TopicQuests Foundation
 *  This source code is available under the terms of the Affero General Public License v3.
 *  Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
 */
package org.topicquests.research.carrot2.pubmed;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.topicquests.research.carrot2.Environment;
import org.topicquests.support.ResultPojo;
import org.topicquests.support.api.IResult;
import org.topicquests.support.util.TextFileHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import net.minidev.json.JSONObject;

/**
 * @author park
 *
 */
public class PubMedReportPullParser {
	private Environment environment;
	
//	private CarrotClusterImporterEnvironment environment;
//	private PubMedEnvironment pubMedEnvironment;

	/**
	 * 
	 */
	public PubMedReportPullParser(Environment env) {
		environment = env;
	}
	
	/**
	 * Returns an instance of {@link JSONDocumentObject}
	 * @param xmlFile 
	 * @return
	 */
	public IResult parseXML(String xml) {
//		environment.logDebug("PubMedReportPullParser- "+foo.length());
		IResult result = new ResultPojo();
		//NOW, parse it
		try {
			InputStream ins = new ByteArrayInputStream(xml.getBytes());
			BufferedInputStream bis = new BufferedInputStream(ins);
			
			parse(bis, result);
		} catch (Exception e) {
			e.printStackTrace();
			result.addErrorString(e.getMessage());
		}
//		environment.logDebug("PubMedReportPullParser+ "+result.hasError()+" "+result.getResultObject());
		return result;
	}

	/**
	 * Clean up the xml input
	 * @param inString
	 * @return instance of {@link JSONDocumentObject}
	 */
	String cleanXML(String inString) {
		StringBuilder buf = new StringBuilder();
		int len = inString.length();
		int lll = (int)'l';
		int ggg = (int)'g';
		int ttt = (int)'t';
		int aaa = (int)'&';
				
		int c = 0, x,y,z;
		for (int i=0;i<len;i++) {
			c = inString.charAt(i);
			if (c == aaa) {
				x = inString.charAt(i+1);
				if (x == lll || x == ggg) {
					y = inString.charAt(i+2);
					if (y == ttt) {
						if (x == lll) 
							buf.append('<');
						else
							buf.append('>');
						i +=3;
						c=-1;
					}
				}
			}
			if (c > -1)
				buf.append((char)c);
		}
		return buf.toString();
	}
	
	void parse(InputStream ins, IResult result) {
	    try {
	         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	         factory.setNamespaceAware(false);
	         XmlPullParser xpp = factory.newPullParser();

	         BufferedReader in = new BufferedReader(new InputStreamReader(ins));
	         xpp.setInput(in);
	         
	        //the working document
	         JSONObject theDocument = null;
	         String temp = null;
	         String text = null;
	         String label = null;
	         String category = null;
	         String refType = null;
	         String lastName = null, firstName = null, initials = null, affiliation = null;
	         String grantId=null, agency=null, country = null;
	         String pages=null, pubVolume=null, pubYear=null, pubMonth=null, pubTitle=null;
	         String pubName=null, pubLoc=null, pubDate=null, pubISSN=null, pubIsoAbbrev=null;
	         String articleIdType = null;
	         boolean isJournal = false;
	         boolean isValid = false;
	         boolean isAuthor = false;
	         boolean isRefType = false;
	         boolean isGrant = true;
	         HashMap<String,String> props;
	         int eventType = xpp.getEventType();
	         boolean isStop = false;
	         while (!(isStop || eventType == XmlPullParser.END_DOCUMENT)) {
	        	 Thread.yield();
	            temp = xpp.getName();
	            if(eventType == XmlPullParser.START_DOCUMENT) {
	                System.out.println("PM Start document");
	            } else if(eventType == XmlPullParser.END_DOCUMENT) {
	                System.out.println("PM End document");
	                //TODO Temporary
	                
	            } else if(eventType == XmlPullParser.START_TAG) {
	                System.out.println("PM Start tag "+temp);
	                props = getAttributes(xpp);
	                articleIdType = props.get("IdType");
	                if (temp.equalsIgnoreCase("Journal")) {
	                	isJournal = true;
		  
	                } else if (temp.equalsIgnoreCase("Author")) {
	                	//inconsistencies: some <author> tags don't include ValidYN
	                	//////////////////////////////////////////////////////
	                	//<Author>
	                    //<LastName>Kaufman</LastName>
	                    //<ForeName>Adam C</ForeName>
	                    //<Initials>AC</Initials>
	                    //<AffiliationInfo>
	                    //    <Affiliation>Department of Cellular Neuroscience, Neurodegeneration, and Repair, Yale University School of Medicine, New Haven, CT.</Affiliation>
	                    //</AffiliationInfo>
	                	//</Author>
	                	//////////////////////////////////////////////////////
	                
	                	String aix = props.get("ValidYN");
	                	if (aix == null) {
	                		isValid = true;
	                		isAuthor = true;
	                	} else if (aix.equals("Y")) {
	                		isValid = true;
	                		isAuthor = true;
	                	} else {
	                		isValid = false;
	                		isAuthor = false;
	                	}
	                } else if (temp.equalsIgnoreCase("Grant")) {
	                	isGrant = true;
	                } else if(temp.equalsIgnoreCase("AbstractText")) {
	                	// <AbstractText Label="BACKGROUND AND OBJECTIVES" NlmCategory="OBJECTIVE">
	                	label = (String)props.get("Label");
	                	category = (String)props.get("NlmCategory");
	                } else if(temp.equalsIgnoreCase("CommentsCorrections")) {
	                	///////////////////////////////////
	                	//<CommentsCorrections RefType="CommentOn">
	                    //<RefSource>Blood. 2015 Jan 22;125(4):619-28</RefSource>
	                    //<PMID Version="1">25416276</PMID>
	                	//</CommentsCorrections>
	                	///////////////////////////////////
	                	refType = (String)props.get("RefType");
	                	isRefType = true;
	                } else if (temp.equalsIgnoreCase("PubmedArticle")) {
	                	theDocument = new JSONObject();
	                	result.setResultObject(theDocument);
	                	environment.logDebug("PMRPP.start");
	                } else if (temp.equalsIgnoreCase("PubmedArticleSet")) {
	                	if (theDocument == null)
	                		isStop = true;
	                	//We must leave -- that's because the system builds
	                	// empty files with <PubmedArticleSet> and nothing else
	                	// if document is missing.
	                }
//DescriptorName  (mesh heading)
//
	                
	            } else if(eventType == XmlPullParser.END_TAG) {
	                System.out.println("PM End tag "+temp+" // "+text);
	                if (temp.equalsIgnoreCase("ArticleTitle")) {
	                	theDocument.put("title", text);
	                } else if(temp.equalsIgnoreCase("AbstractText")) {
	                	//TODO deal with abstracts
	                	String foo = cleanText(text);
	                	if (label == null)
	                		addAbstract(theDocument, foo); 
	                	else 
	                		addAbstract(theDocument, label+". "+foo);
	                } else if (temp.equalsIgnoreCase("Language")) {
	                	theDocument.put("lang", text);
	                } else if (temp.equalsIgnoreCase("MedlineDate")) {
	                	pubDate = text;
	                } else if (temp.equalsIgnoreCase("Journal")) {
	                	isJournal = false;
	                	
	                } else if (temp.equalsIgnoreCase("ISOAbbreviation")) {
	                	if (isJournal)
	                		pubIsoAbbrev = text;

	                } else if (temp.equalsIgnoreCase("Title")) {
	                	if (isJournal)
	                		pubName = text;
	                } else if (temp.equalsIgnoreCase("Volume")) {
	                	if (isJournal)
	                		pubVolume = text;
	                } else if (temp.equalsIgnoreCase("MedlinePgn")) {
	                	//theDocument.getPublication().setPages(text);
	                } else if (temp.equalsIgnoreCase("Year")) {
	                	if (isJournal)
	                		pubYear = text;
	                } else if (temp.equalsIgnoreCase("PMID")) {
	                	if (!isRefType) {
	                		theDocument.put("pmid", text);
	                		//have we seen this before?
	                		//if (pubMedEnvironment.pmidIsVisited(text)) {
	                		//	result.setResultObjectA(PubMedEnvironment.PMID_IS_VISITED);
	                		//	break;
	                		//}
	                		//we are seeing it now
	                		//pubMedEnvironment.visitingPMID(text);
	                		//theDocument = new JSONDocumentObject(IHarvestingOntology.CARROT2_AGENT_USER);
	                		//theDocument.setPMID(text);
	                		//theDocument.setPublicationType("JournalArticle"); //just in case it's not set later
	                		//result.setResultObject(theDocument);
	              //  		environment.logDebug("PMID "+text+" "+theDocument);
	                	} //else if (refType.equals("Cites"))
	                	//	theDocument.addCitation(text);
		            } else if (temp.equalsIgnoreCase("ArticleId")) {
		            	//theDocument.addCitation(articleIdType, text); 

	                } else if (temp.equalsIgnoreCase("Month")) {
	                	if (isJournal)
	                		pubMonth = text;
	                } else if (temp.equalsIgnoreCase("Title")) {
	                	if (isJournal)
	                		pubTitle =text ;
	                } else if (temp.equalsIgnoreCase("Author")) {
	                	/*environment.logDebug("PMRPP.author "+isValid); //+"\n"+theDocument);
	                	if (isValid) {
	                		IAuthor a = new AuthorPojo();
	                		a.setAuthorLastName(lastName);
	                		if (firstName != null)
	                			a.setAuthorNickName(firstName);
	                		if (initials != null)
	                			a.setAuthorInitials(initials);
	                		if (affiliation != null)
	                			a.addAffiliationName(trimAffiliation(affiliation));
		                	environment.logDebug("PMRPP.author-1\n"+a);
	                		theDocument.addAuthor(a);
	                	}
	                	lastName = null;
	                	firstName = null;
	                	initials = null;
	                	affiliation = null;*/
	                } else if (temp.equalsIgnoreCase("PublicationType")) {
	                	/*String pt = theDocument.getPublication().getPublicationType();
	                	if (pt == null)
	                		pt = text;
	                	else
	                		pt += " | "+text;
	                	theDocument.getPublication().setPublicationType(pt);*/
	                } else if (temp.equalsIgnoreCase("NameOfSubstance")) {
	                	addTag(theDocument, text);
	                	addSubstance(theDocument, text);
	                } else if (temp.equalsIgnoreCase("DescriptorName")) {
	                	addTag(theDocument, text);
	                } else if (temp.equalsIgnoreCase("QualifierName")) {
	                	addTag(theDocument, text);
	                } else if (temp.equalsIgnoreCase("Keyword")) {
	                	environment.logDebug("PMRPP.addTag "+text);
	                	addTag(theDocument, text);
	                } else if(temp.equalsIgnoreCase("CommentsCorrections")) {
	                	isRefType = false;
	                } else if(temp.equalsIgnoreCase("LastName")) { //TODO
	                	if (isAuthor)
	                		lastName = text;
	                } else if(temp.equalsIgnoreCase("ForeName")) { //TODO
	                	if (isAuthor)
	                		firstName = text;
	                } else if (temp.equalsIgnoreCase("MedlineTA")) {
	                	pubName = text;
	                } else if (temp.equalsIgnoreCase("Country")) {
	                	if (!isGrant)
	                		pubLoc = text;
	                	else
	                		country = text;
	                } else if (temp.equalsIgnoreCase("ISSN")) {
	                	pubISSN = text;
	                } else if (temp.equalsIgnoreCase("Affiliation")) {
	                	affiliation = text;
	                } else if (temp.equalsIgnoreCase("Initials")) {
	                	initials = text;
	                } else if (temp.equalsIgnoreCase("GrantID")) {
	                	grantId = text;
	                } else if (temp.equalsIgnoreCase("Agency")) {
	                	agency = text;
	                } else if (temp.equalsIgnoreCase("Grant")) {
	                	///////////////////////////////////////////
	                	//<Grant>
	                    //<GrantID>R01 AG034924</GrantID>
	                    //<Acronym>AG</Acronym>
	                    //<Agency>NIA NIH HHS</Agency>
	                    //<Country>United States</Country>
	                	//</Grant>
	                	//////////////////////////////////////////
	                	/*theDocument.addGrant(grantId, agency, country);
	                	grantId = null;
	                	agency = null;
	                	country = null;
	                	isGrant = false;*/
	                } else if (temp.equalsIgnoreCase("CopyrightInformation")) {
	                	//theDocument.setCopyright(text);
	                }
	            } else if(eventType == XmlPullParser.TEXT) {
	                text = xpp.getText().trim();
	             } else if(eventType == XmlPullParser.CDSECT) {
	                text = xpp.getText().trim();
	            }
	            eventType = xpp.next();
	          }
	      } catch (Exception e) {
	      		environment.logError(e.getMessage(), e);
	      		result.addErrorString(e.getMessage());
	      } 		
	}
	
	void addAbstract(JSONObject doc, String text) {
		List<String> l = (List<String>) doc.get("abstract");
		if (l == null) l = new ArrayList<String>();
		l.add(text);
		doc.put("abstract", l);
	}
	void addTag(JSONObject doc, String tag) {
		List<String> l = (List<String>) doc.get("tags");
		if (l == null) l = new ArrayList<String>();
		l.add(tag);
		doc.put("tags", l);
	}
	void addSubstance(JSONObject doc, String text) {
		List<String> l = (List<String>) doc.get("substances");
		if (l == null) l = new ArrayList<String>();
		l.add(text);
		doc.put("substances", l);
	}
	
	String trimAffiliation(String affiliation) {
		int len = affiliation.length();
		//if (len > 200)
		//	len = 200;
		StringBuilder buf = new StringBuilder();
		char c;
		for (int i=0;i<len;i++) {
			c = affiliation.charAt(i);
			if (canUse(c))
				buf.append(c);
		}
		return buf.toString().trim();
	}
	
	boolean canUse(char c) {
		if ( c == '"' ||
			 c == ',' ||
			 c == '.' ||
			 c == ':' ||
			 c == ';')
			return false;
		return true;
	}

	/**
	 * </p>@see http://www.ncbi.nlm.nih.gov/pubmed/23329350 for
	 * an abstract that has wild characters '0''9'</p>
	 * <p>@see http://www.ncbi.nlm.nih.gov/pubmed/23325918 uses:
	 * "Expt." for experiment</p>
	 * <p>Bad break:
	 * Trichophyton rubrum (T.
 rubrum) represents the most important agent of dermatophytosis in humans.
	 * </p>
	 * <p> (p???&lt;???0.001),  bad sentence break here, and 0009 and &lg;
	 *   has UTF-8 characters</p>
	 * @param inString
	 * @return
	 * Note: grand possiblility of outOfBounds errors here
	 */
	String cleanText(String inString) {
		int lparen = (int)'(';
		String foo = inString;
		foo = foo.replace("Expt.", "Experiment"); //worked!
		StringBuilder buf = new StringBuilder();
		int len = foo.length();
		int c = 0;
		boolean blockNewLine = false;
		for (int i=0;i<len;i++) {
			c = foo.charAt(i);
			//case of 23329350 
			//TODO did not work
			if (blockNewLine) {
				if (c != 0x0D && c != 0x0A) {
					//we just passed the newline line feed we wanted to ignore
					blockNewLine = false;
					buf.append((char)c);
				}
			}
			if (c == 0 && foo.charAt(i+1) == 9)
				i++; // skip those
			else if (c == lparen) {
				//bad sentence 
				//TODO did not work
				////////////////////////////////
				// outof bounds at i+2 and i+3
				if (i+3 < foo.length() && foo.charAt(i+2)==(int)'.' &&
					foo.charAt(i+3)==0x0D) {
					blockNewLine = true;
				}
				buf.append((char)c);
			}
			else
				buf.append((char)c);
		}
		return buf.toString();
	}
	/**
     * does not return null if no attributes
     */
    HashMap<String,String> getAttributes(XmlPullParser p) {
      HashMap <String,String>result =  new HashMap<String,String>();;
      int count = p.getAttributeCount();
      if (count > 0) {
        String name = null;
        for (int i = 0; i < count; i++) {
          name = p.getAttributeName(i);
          result.put(name,p.getAttributeValue(i));
        }
      }
      return result;
    }		

}
