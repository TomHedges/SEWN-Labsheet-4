/**
 *  (c) 2013 Tom Hedges
 *  System for analysing crawl file and generating statistics and Page Rank values
 */
package code;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * @author Tom
 *
 */
public class LinkAnalysisPageRank {
	// ******************************************************************************************
	// ** INSTANCE VARIABLES.                                                                  **
	// ******************************************************************************************

	private String crawlFileURL;
	private String adjacencyMatrix;
	private ArrayList<String> urlsInCrawlFile;
	private InputStream isCrawlData;
	
	public static void main(String[] args) {
		System.out.println("start");
		LinkAnalysisPageRank lapr = new LinkAnalysisPageRank("http://www.dcs.bbk.ac.uk/~martin/sewn/ls4/sewn-crawl-2013.txt");
		
		lapr.buildAdjacencyMatrix();
		
		lapr.outputURLsInCrawlFile();

		System.out.println("end1");

		lapr.outputTextFile("matrix.csv", lapr.adjacencyMatrix);

		System.out.println("end2");
		//System.out.println(lapr.adjacencyMatrix);
	}

	private void outputURLsInCrawlFile() {
	    for (String url : urlsInCrawlFile) {
			System.out.println(url);
	    }
	}

	public LinkAnalysisPageRank(String fileToParse) {
		this.crawlFileURL = fileToParse;
		this.adjacencyMatrix = "";
		this.urlsInCrawlFile = new ArrayList<String>();
	}

	private void buildAdjacencyMatrix() {
		Scanner crawlData = new Scanner(this.accessURL());

		adjacencyMatrix = "ORIGIN POINT,";
		
		while (crawlData.hasNextLine()) {
			String strCrawlFileLine = crawlData.nextLine();
			
			// if t x
			if (strCrawlFileLine.startsWith("Visited:")) {
				String url = strCrawlFileLine.replace("Visited: ", "");
				urlsInCrawlFile.add(url);
				adjacencyMatrix = adjacencyMatrix + url + ",";
			}
		}

		adjacencyMatrix = adjacencyMatrix + "Dangling URLs" + System.getProperty("line.separator");

		String zeroBuilder = "";
	    for (int loopCounter = 0; loopCounter < urlsInCrawlFile.size()+1; loopCounter++) {
	    	zeroBuilder = zeroBuilder + ",0";
	    }
		
	    for (String url : urlsInCrawlFile) {
	    	adjacencyMatrix = adjacencyMatrix + url + zeroBuilder + System.getProperty("line.separator");
	    }

		crawlData = new Scanner(this.accessURL());
		int iVisitedPos = 0;
		int iLinkPos = 0;
		String strVisitedURL = "";
		String strLinkURL = "";
		
		while (crawlData.hasNextLine()) {
			String strCrawlFileLine = crawlData.nextLine();
			
			// if t x
			if (strCrawlFileLine.trim().startsWith("Link:")) {
				strLinkURL = strCrawlFileLine.trim().replace("Link: ", "");

				iLinkPos = urlsInCrawlFile.indexOf(strLinkURL);
				
				// if link url is not in visited list, then this url is 'dangling' 
				if (iLinkPos == -1) {
					//adjacencyMatrix.indexOf(strVisitedURL, 1);
					
					int linkCounter = 0;
					
					try {
						BufferedReader reader = new BufferedReader(new FileReader("matrix.csv"));
						
						String temp = "";
						String newFileText = "";
						int loopCounter = 0;
						
						//for (int loopCounter = 0; loopCounter <= iVisitedPos+1; loopCounter++) {
						while ((temp = reader.readLine()) != null) {	
							//temp = reader.readLine();
							loopCounter++;
							
							if (loopCounter == (iVisitedPos+2)) {
								linkCounter = Integer.parseInt(temp.substring(temp.lastIndexOf(",")+1));
								//temp = temp.substring(temp.lastIndexOf(",")+1);
								
								linkCounter = linkCounter + 1;

				                String tokens[] = temp.split(",");
				                tokens[tokens.length-1] = String.valueOf(linkCounter);
				                
				                temp = "";
				                for (String token : tokens) {
				                	temp = temp + token + ",";
				                }
				                temp = temp.substring(0, temp.length()-1);
							}
							
							newFileText = newFileText + temp + System.getProperty("line.separator");
						}
						
						reader.close();

						this.outputTextFile("matrix.csv", newFileText);
						
						
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
				} else {
					
				}
				
			} else {
				strVisitedURL = strCrawlFileLine.replace("Visited: ", "");
				iVisitedPos = urlsInCrawlFile.indexOf(strVisitedURL);
			}
		}
	}

	// Read URL and return inputstream 
	private InputStream accessURL() {
		try {
			URL url = new URI(this.crawlFileURL).toURL();
			this.isCrawlData = url.openStream();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.out.println(e);
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		} catch (URISyntaxException e) {
			System.out.println(e);
			System.exit(1);
		}

		return this.isCrawlData;
	}
	
	// output text file using given variables
	private void outputTextFile(String fileName, String textToOutput) {
		try {
			PrintWriter pwOutput = new PrintWriter(fileName);
			pwOutput.println(textToOutput);
			pwOutput.close();
		} catch (FileNotFoundException e) {
			System.out.println(e);
			System.exit(1);
		}
	}
}