/**
 *  (c) 2013 Tom Hedges
 *  System for analysing crawl file and generating statistics and Page Rank values
 */
package code;

import java.io.FileNotFoundException;
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

		adjacencyMatrix = adjacencyMatrix.substring(0, adjacencyMatrix.length()-1) + System.getProperty("line.separator");
		
	    for (String url : urlsInCrawlFile) {
	    	adjacencyMatrix = adjacencyMatrix + url + System.getProperty("line.separator");
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