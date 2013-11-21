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
	private ArrayList<Integer> numberOfInlinks;
	private ArrayList<Integer> numberOfOutlinks;
	private InputStream isCrawlData;
	private String currentVisitURL;
	private String currentVisitRootURL;

	private boolean generateMatrix = false;
	private boolean calculateStats = true;
	
	public static void main(String[] args) {
		System.out.println("start");
		LinkAnalysisPageRank lapr = new LinkAnalysisPageRank("http://www.dcs.bbk.ac.uk/~martin/sewn/ls4/sewn-crawl-2013.txt");
		
		lapr.buildListOfVisitedURLs();
		
		if (lapr.generateMatrix) {
			lapr.buildAdjacencyMatrix();
			
			lapr.outputURLsInCrawlFile();

			System.out.println("end1");

			lapr.outputTextFile("matrix.csv", lapr.adjacencyMatrix);
		}
		

		if (lapr.calculateStats) {
			lapr.generateStatistics();
		}
		
		System.out.println("end2");
	}

	private void generateStatistics() {
		this.generateNumberOfOutlinks();
		this.generateNumberOfInlinks();
		this.generateSummaryFigures(this.numberOfOutlinks, "outlinks-stats.csv");
		this.generateSummaryFigures(this.numberOfInlinks, "inlinks-stats.csv");
		this.generateDegreeDistribution(this.numberOfOutlinks, "outlinks-degree-distribution.csv");
		this.generateDegreeDistribution(this.numberOfInlinks, "inlinks-degree-distribution.csv");
		
	}
	
	private void generateDegreeDistribution(ArrayList<Integer> listOfNumbers, String outputFilename) {
		String output = "# outlinks,# pages" + System.getProperty("line.separator");

		int linkCounter = 0;
		int loopCounter = 0;
		int numberSought = 0;
		while (linkCounter < listOfNumbers.size()) {
			int numberOfHits = 0;
			while (loopCounter < listOfNumbers.size()) {
				if (listOfNumbers.get(loopCounter) == numberSought) {
					numberOfHits++;
					linkCounter++;
				}
				loopCounter++;
			}
			
			if (numberSought > 0) {
				output = output + numberSought + "," + numberOfHits + System.getProperty("line.separator");
			}

			numberOfHits = 0;
			loopCounter = 0;
			numberSought++;
		}
		
		this.outputTextFile(outputFilename, output);
	}

	private void generateSummaryFigures(ArrayList<Integer> listOfNumbers, String outputFilename) {
		double mean = 0;
		double variance = 0;
		double standardDeviation = 0;
		String output = "";
		
		double runningTotal = 0;
		for (int number : listOfNumbers) {
			runningTotal = runningTotal + number;
		}
		
		mean = runningTotal / listOfNumbers.size();
		
		runningTotal = 0;
		for (int number : listOfNumbers) {
			runningTotal = runningTotal + ((number - mean)*(number - mean));
		}
		
		variance = runningTotal / listOfNumbers.size();
		
		standardDeviation = Math.sqrt(variance);
		
		output="Mean Number of Outlinks,Variance,Standard Deviation" + System.getProperty("line.separator") + mean + "," + variance + "," + standardDeviation;

		this.outputTextFile(outputFilename, output);
	}

	private void generateNumberOfInlinks() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader("matrix.csv"));

			for (int counter = 0; counter<this.urlsInCrawlFile.size(); counter++) {
				this.numberOfInlinks.add(0);
			}
			
			String matrixLine = "";
			String inLinksTable = "Visited URL,Number of Inlinks" + System.getProperty("line.separator");
			int loopCounter = 0;
			int inLinkTotal;
			
			matrixLine = reader.readLine();
			
			while (((matrixLine = reader.readLine()) != null) && matrixLine.length()>0 ) {
				inLinkTotal = 0;
				loopCounter++;
				
                String tokens[] = matrixLine.split(",");
                                	
            	// -1 stops it covering final column for dangling rows!
                for (int columnCounter = 1; columnCounter < tokens.length-1; columnCounter++) {
                	int cellValue = Integer.parseInt(tokens[columnCounter]);
                	if (cellValue > 0) {
                		this.numberOfInlinks.set(columnCounter-1,this.numberOfInlinks.get(columnCounter-1)+1);
                	}
                }
			}
			
			reader.close();

			for (int counter = 0; counter<this.urlsInCrawlFile.size(); counter++) {
				inLinksTable = inLinksTable + this.urlsInCrawlFile.get(counter) + "," + this.numberOfInlinks.get(counter) + System.getProperty("line.separator");
			}

			this.outputTextFile("inlinks.csv", inLinksTable);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	
	
	
	private void generateNumberOfOutlinks() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader("matrix.csv"));
			
			String matrixLine = "";
			String outLinksTable = "Visited URL,Number of Outlinks" + System.getProperty("line.separator");
			int loopCounter = 0;
			int outLinkTotal;
			
			matrixLine = reader.readLine();
			
			while (((matrixLine = reader.readLine()) != null) && matrixLine.length()>0 ) {
				outLinkTotal = 0;
				loopCounter++;
				
                String tokens[] = matrixLine.split(",");
                String urlCheck = tokens[0];
                outLinksTable = outLinksTable + urlCheck + ",";
                
                if (!(urlCheck.equals(urlsInCrawlFile.get(loopCounter-1)))) {
                	System.out.println("ERROR - URL discrepancy! " + urlCheck + " : " + loopCounter);
                } else {
                	
                	// -1 stops it covering final column for dangling rows!
                    for (int columnCounter = 1; columnCounter < tokens.length-1; columnCounter++) {
                    	int cellValue = Integer.parseInt(tokens[columnCounter]);
                    	if (cellValue > 0) {
                    		outLinkTotal = outLinkTotal + 1;
                    	}
                    }
                    
                    this.numberOfOutlinks.add(outLinkTotal);
                    outLinksTable = outLinksTable + outLinkTotal + System.getProperty("line.separator");
                    //System.out.println(urlCheck + ":" + outLinkTotal);
                }
			}
			
			reader.close();

			this.outputTextFile("outlinks.csv", outLinksTable);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		this.numberOfOutlinks = new ArrayList<Integer>();
		this.numberOfInlinks = new ArrayList<Integer>();
	}

	private void buildListOfVisitedURLs() {
		Scanner crawlData = new Scanner(this.accessURL());

		adjacencyMatrix = "ORIGIN POINT,";
		
		while (crawlData.hasNextLine()) {
			String strCrawlFileLine = crawlData.nextLine();
			
			// if t x
			if (strCrawlFileLine.startsWith("Visited:")) {
				String url = strCrawlFileLine.replace("Visited: ", "");
				
				urlsInCrawlFile.add(url);
				adjacencyMatrix = adjacencyMatrix + url + ",";
				
				//COME BACK TO THIS ONCE THE REST OF THE ASSIGNEMNT IS WORKIGN BETTER!
				//adjacencyMatrix = adjacencyMatrix + calculateFullURL(url, true);
			}
		}		
	}
	
	private void buildAdjacencyMatrix() {
		Scanner crawlData = new Scanner(this.accessURL());
		
		adjacencyMatrix = adjacencyMatrix + "Dangling URLs" + System.getProperty("line.separator");

		String zeroBuilder = "";
	    for (int loopCounter = 0; loopCounter < urlsInCrawlFile.size()+1; loopCounter++) {
	    	zeroBuilder = zeroBuilder + ",0";
	    }
		
	    for (String url : urlsInCrawlFile) {
	    	adjacencyMatrix = adjacencyMatrix + url + zeroBuilder + System.getProperty("line.separator");
	    }

		this.outputTextFile("matrix.csv", adjacencyMatrix);
	    
		System.out.println("adjacency matrix output!");
		
		crawlData = new Scanner(this.accessURL());
		int iVisitedPos = 0;
		int iLinkPos = 0;
		String strVisitedURL = "";
		String strLinkURL = "";
		int lineCounter = 0;
		
		while (crawlData.hasNextLine()) {
			String strCrawlFileLine = crawlData.nextLine();
			lineCounter++;
			System.out.println("Processing crawl file line: " + lineCounter + " : " + strCrawlFileLine);
			
			// if t x
			if (strCrawlFileLine.trim().startsWith("Link:")) {
				strLinkURL = strCrawlFileLine.trim().replace("Link: ", "");

				iLinkPos = urlsInCrawlFile.indexOf(strLinkURL) + 2;
				if (iLinkPos == 1) {
					iLinkPos = urlsInCrawlFile.size() + 2;
				}
				
				//FOR FUTURE REFINEMENT!
				//this.setMatrixPoint(iVisitedPos, iLinkPos, this.getMatrixPoint(iVisitedPos, iLinkPos) + 1);
				
				// if link url is not in visited list, then this url is 'dangling' 
				
				
				
				
				//NEED TO REMOVE END "/"
				//NEED TO REMOVE ANCHORS (#)
				//NEED TO REMOVE INDEX.HTM/INDEX.HTML   ???
				//NEED TO HANDLE RELATIVE LINKS!!!!
				
				
				
				
				try {
					BufferedReader reader = new BufferedReader(new FileReader("matrix.csv"));
					
					String matrixLine = "";
					String newFileText = "";
					int loopCounter = 0;
					int linkCounter = 0;
					
					while ((matrixLine = reader.readLine()) != null) {
						loopCounter++;
						
						if (loopCounter == (iVisitedPos)) {
			                String tokens[] = matrixLine.split(",");
			                linkCounter = Integer.parseInt(tokens[iLinkPos-1]);
							linkCounter = linkCounter + 1;
			                tokens[iLinkPos-1] = String.valueOf(linkCounter);

							//linkCounter = Integer.parseInt(matrixLine.substring(matrixLine.lastIndexOf(",")+1));
							//temp = temp.substring(temp.lastIndexOf(",")+1);
							
			                matrixLine = "";
			                for (String token : tokens) {
			                	matrixLine = matrixLine + token + ",";
			                }
			                matrixLine = matrixLine.substring(0, matrixLine.length()-1);
						}
						
						newFileText = newFileText + matrixLine + System.getProperty("line.separator");
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
				strVisitedURL = strCrawlFileLine.replace("Visited: ", "");
				iVisitedPos = urlsInCrawlFile.indexOf(strVisitedURL) + 2;
			}
		}
	}


	private String calculateFullURL(String url, boolean isVisitedPage) {
		//String baseRelativeURL = originalURL;
		char[] charactersInURL;
		charactersInURL = url.toCharArray();
		int loopCounter = charactersInURL.length-1;
		boolean dotEncountered = false;

		do
		{
			if (charactersInURL[loopCounter] == '/')
			{
				// checks if last character in array is a slash
				if (loopCounter == charactersInURL.length-1)
				{
					// if so, then 'originalURL' is already base relative path - so we are finished!
					url = url.substring(0, url.length()-2);
					return url;
				}

				// if dot not yet encountered, then 'originalURL' was base relative path
				if (!dotEncountered)
				{
					return url;
				}
				else
				{
					// as '.' was encountered before '/' in 'originalURL', it must have been a full filepath - so need to perform sum on character position to get correct base URL
					url = url.substring(0, loopCounter+1);
					return url;
				}
			}

			if (charactersInURL[loopCounter] == '.')
			{
				dotEncountered=true;
			}

			loopCounter--;
		} while (loopCounter>0);

		// if URL is well formed, should never reach this return statement
		return url;
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