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
	private String matrixFilename;
	private String localCopyOfFileToParseFilename;
	private ArrayList<String> urlsInCrawlFile;
	private ArrayList<Integer> posOfVisitedURLInCrawlFile;
	private ArrayList<Integer> numberOfInlinks;
	private ArrayList<Integer> numberOfOutlinks;
	private InputStream isCrawlData;
	private String currentVisitRootURL;

	// To alter whether the program should build the matrix, or simply calculate stats
	private boolean generateMatrix = true;
	private boolean calculateStats = true;

	private int maxNumberOfIterations;
	private int finalNumberOfIterations = 0;
	private boolean convergenceAchieved = false;

	private static final String NEWLINE = System.getProperty("line.separator");

	public static void main(String[] args) {
		System.out.println("start");
		LinkAnalysisPageRank lapr = new LinkAnalysisPageRank("http://www.dcs.bbk.ac.uk/~martin/sewn/ls4/sewn-crawl-2013.txt", "sewn-crawl-2013.txt", "matrix-full-counts.csv", 100);

		lapr.makeLocalCopyOfFileToParse();

		lapr.buildListOfVisitedURLs();

		if (lapr.generateMatrix) {
			lapr.buildAdjacencyMatrix();

			System.out.println("end1");
		}


		if (lapr.calculateStats) {
			lapr.generateStatistics();
		}

		System.out.println("end2");
		System.exit(0);
	}


	// ******************************************************************************************
	// ** CONSTRUCTOR.                                                                         **
	// ******************************************************************************************
	
	public LinkAnalysisPageRank(String fileToParse, String localCopyOfFileToParseFilename, String matrixFilename, int maxIterations) {
		this.crawlFileURL = fileToParse;
		this.localCopyOfFileToParseFilename = localCopyOfFileToParseFilename;
		this.matrixFilename = matrixFilename;
		this.adjacencyMatrix = "";
		this.urlsInCrawlFile = new ArrayList<String>();
		this.posOfVisitedURLInCrawlFile = new ArrayList<Integer>();
		this.numberOfOutlinks = new ArrayList<Integer>();
		this.numberOfInlinks = new ArrayList<Integer>();
		this.currentVisitRootURL = "";
	}



	// ******************************************************************************************
	// ** ADJACENCY MATRIX BUILDER.                                                            **
	// ******************************************************************************************
	
	// Construct adjacency matrix as CSV file
	private void buildAdjacencyMatrix() {
		adjacencyMatrix = adjacencyMatrix + "Dangling URLs" + NEWLINE;

		String zeroBuilder = "";
		for (int loopCounter = 0; loopCounter < urlsInCrawlFile.size()+1; loopCounter++) {
			zeroBuilder = zeroBuilder + ",0";
		}

		for (String url : urlsInCrawlFile) {
			adjacencyMatrix = adjacencyMatrix + url + zeroBuilder + NEWLINE;
		}

		this.outputTextFile(matrixFilename, adjacencyMatrix);

		System.out.println("blank adjacency matrix output!");

		int iVisitedRowInMatrix = 0;
		int iLinkColInMatrix = 0;
		String strVisitedURL = "";
		String strLinkURL = "";
		int lineCounter = 0;
		boolean skipVisit = false;

		try { 
			BufferedReader crawlFileReader = new BufferedReader(new FileReader(localCopyOfFileToParseFilename));
			String strCrawlFileLine = "";
			while ((strCrawlFileLine = crawlFileReader.readLine()) != null && strCrawlFileLine.length() > 0) {


				lineCounter++;
				System.out.println("Processing crawl file line: " + lineCounter + " : " + strCrawlFileLine);

				if (strCrawlFileLine.trim().startsWith("Link:")) {
					if (!skipVisit) {
						strLinkURL = strCrawlFileLine.trim().replace("Link: ", "");

						strLinkURL = getFullLinkURL(strLinkURL);
						System.out.println("Equates to: " + strLinkURL);

						// if link url is not in visited list, then this url is 'dangling' 
						iLinkColInMatrix = urlsInCrawlFile.indexOf(strLinkURL) + 2;
						if (iLinkColInMatrix == 1) {
							iLinkColInMatrix = urlsInCrawlFile.size() + 2;
						}

						if (!(urlsInCrawlFile.indexOf(strVisitedURL) == urlsInCrawlFile.indexOf(strLinkURL))) {
							// url is linking to itself, so treat as dangling!
							iLinkColInMatrix = -1;
						}

						try { 
							BufferedReader reader = new BufferedReader(new FileReader(matrixFilename));

							String matrixLine = "";
							String newFileText = "";

							int matrixLineCounter = 0;
							int linkCounter = 0;

							matrixLine = reader.readLine();
							newFileText = matrixLine;
							matrixLineCounter++; 
							while ((matrixLine = reader.readLine()) != null && matrixLine.length() > 0) {
								matrixLineCounter++;

								if (matrixLineCounter == (iVisitedRowInMatrix)) {
									String tokens[] = matrixLine.split(",");
									linkCounter = Integer.parseInt(tokens[iLinkColInMatrix-1]);
									linkCounter = linkCounter + 1;
									tokens[iLinkColInMatrix-1] = String.valueOf(linkCounter);

									matrixLine = "";
									for (String token : tokens) {
										matrixLine = matrixLine + token + ",";
									}
									matrixLine = matrixLine.substring(0, matrixLine.length()-1);
								}

								newFileText = newFileText + NEWLINE + matrixLine;
							}

							reader.close();

							this.outputTextFile(matrixFilename, newFileText);
							this.adjacencyMatrix = newFileText;

						} catch (FileNotFoundException e) {
							System.out.println(e);
							System.exit(1);
						} catch (IOException e) {
							System.out.println(e);
							System.exit(1);
						}
					} else {
						System.out.println("SKIPPING crawl file line: " + lineCounter + " : " + strCrawlFileLine);
					}

				} else {
					if (!(posOfVisitedURLInCrawlFile.indexOf(lineCounter) == -1)) {
						skipVisit = false;
						strVisitedURL = urlsInCrawlFile.get((posOfVisitedURLInCrawlFile.indexOf(lineCounter)));
						System.out.println("Equates to: " + (posOfVisitedURLInCrawlFile.indexOf(lineCounter)+1) + " : " + strVisitedURL);
						iVisitedRowInMatrix = urlsInCrawlFile.indexOf(strVisitedURL) + 2;
						this.currentVisitRootURL = convertToRootURL(strVisitedURL);
					} else {
						skipVisit = true;
					}
				}
			}

			crawlFileReader.close();

		} catch (FileNotFoundException e) {
			System.out.println(e);
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		}
	}
	
	
	// ******************************************************************************************
	// ** PAGERANK GENERATION METHODS.                                                         **
	// ******************************************************************************************

	// Call specific methods in order to create full set of reports/stats and summaries
	private void generateStatistics() {
		this.generateNumberOfOutlinks();
		this.generateNumberOfInlinks();
		this.generateSummaryFigures(this.numberOfOutlinks, "outlinks-stats.csv", "Outlinks");
		this.generateSummaryFigures(this.numberOfInlinks, "inlinks-stats.csv", "Inlinks");
		this.generateDegreeDistribution(this.numberOfOutlinks, "# outlinks,# pages", "outlinks-degree-distribution.csv");
		this.generateDegreeDistribution(this.numberOfInlinks, "# inlinks,# pages", "inlinks-degree-distribution.csv");

		this.generatePageRankFullResults(0.15, "pagerank-015.csv");
		this.generatePageRankSummary("pagerank-015.csv", "pagerank-015.txt", this.finalNumberOfIterations);

		this.generatePageRankFullResults(1.0, "pagerank-100.csv");
		this.generatePageRankSummary("pagerank-100.csv", "pagerank-100.txt", this.finalNumberOfIterations);

		this.generatePageRankFullResults(0.0, "pagerank-000.csv");
		this.generatePageRankSummary("pagerank-000.csv", "pagerank-000.txt", this.finalNumberOfIterations);

		this.generatePageRankFullResults(0.25, "pagerank-025.csv");
		this.generatePageRankSummary("pagerank-025.csv", "pagerank-025.txt", this.finalNumberOfIterations);

		this.generatePageRankFullResults(0.50, "pagerank-050.csv");
		this.generatePageRankSummary("pagerank-050.csv", "pagerank-050.txt", this.finalNumberOfIterations);

		this.generatePageRankFullResults(0.75, "pagerank-075.csv");
		this.generatePageRankSummary("pagerank-075.csv", "pagerank-075.txt", this.finalNumberOfIterations);
	}
	
	// For specified value of "T", generate the PageRank until convergence criteria are met/iteration limit exceeded, and output CSV file
	private void generatePageRankFullResults(double teleportationProb, String outputFilename) {
		ArrayList<Double> pageRanks = new ArrayList<Double>();
		double[][] pageRankOrderOrig = new double[this.urlsInCrawlFile.size()][2];
		double[][] pageRankOrderNew = new double[this.urlsInCrawlFile.size()][2];
		int positionInRankingChangedCounter = 0;
		double pageRank = 0.0;
		int numberOfPRValuesChanged = 0;
		int iterationsWithoutOrderChange = 0;
		String lineBuilder = "";

		String pageRankBuilder = "ITERATION NUMBER,NUMBER OF CHANGED VALUES";
		for ( String url : this.urlsInCrawlFile ) {
			pageRankBuilder = pageRankBuilder + ",PR(" + url + ")";
		}
		
		pageRankBuilder = pageRankBuilder + ",NUMBER OF CHANGES TO SORT ORDER" + NEWLINE + "0,0";
		int urlsCounter = 0;
		for ( String url : this.urlsInCrawlFile ) {
			pageRankBuilder = pageRankBuilder + ",1.0";
			pageRanks.add(1.0);
			pageRankOrderOrig[urlsCounter][0] = 1.0;
			pageRankOrderOrig[urlsCounter][1] = urlsCounter;
			urlsCounter++;
		}
		
		ArrayList<Double> pageRanksUpdated = new ArrayList<Double>(pageRanks);
		pageRankBuilder = pageRankBuilder + ",0" + NEWLINE;

		for (int iterationCounter = 1; (iterationCounter<=maxNumberOfIterations) && !this.convergenceAchieved; iterationCounter++) {
			pageRankBuilder = pageRankBuilder + iterationCounter;
			for (int loopCounter = 0; loopCounter<this.urlsInCrawlFile.size(); loopCounter++ ) {
				String listOfURLsLinkingToCurrent[] = this.getListOfURLsLinkingToCurrent(loopCounter);

				if (listOfURLsLinkingToCurrent.length != this.numberOfInlinks.get(loopCounter)) {
					System.out.println(iterationCounter + " : " + loopCounter + " : ERROR - Differing count for number of links. URL : " + this.urlsInCrawlFile.get(loopCounter));
				}

				if (this.numberOfInlinks.get(loopCounter) > 0) {
					for (String url : listOfURLsLinkingToCurrent) {
						int posOfInboundURL = this.urlsInCrawlFile.indexOf(url);
						double inlinkingPageCurrentPR = pageRanks.get(posOfInboundURL);
						double inlinkingPageNumOfOutlinks = this.numberOfOutlinks.get(posOfInboundURL);

						pageRank = pageRank + (inlinkingPageCurrentPR / inlinkingPageNumOfOutlinks);
					}
				}

				pageRank = (1 - teleportationProb) * pageRank;

				pageRank = (teleportationProb / this.urlsInCrawlFile.size()) + pageRank;

				if (pageRanks.get(loopCounter) != pageRank) {
					numberOfPRValuesChanged++;
				}
				pageRanksUpdated.set(loopCounter, pageRank);

				pageRankOrderNew[loopCounter][0] = pageRank;
				pageRankOrderNew[loopCounter][1] = loopCounter;

				lineBuilder = lineBuilder + "," + pageRank;

				System.out.println(iterationCounter + " : " + loopCounter + " : " + this.urlsInCrawlFile.get(loopCounter) + " PR: " + pageRank);

				pageRank = 0;
			}

			// SORT ARRAY OF PRs
			java.util.Arrays.sort(pageRankOrderNew, new java.util.Comparator<double[]>() {
				public int compare(double[] a, double[] b) {
					return Double.compare(b[0], a[0]);
				}
			});

			for (int comparisonCounter = 0; comparisonCounter < pageRankOrderNew.length; comparisonCounter++) {
				if (pageRankOrderNew[comparisonCounter][1] != pageRankOrderOrig[comparisonCounter][1]) {
					positionInRankingChangedCounter++;
				}
			}

			if (positionInRankingChangedCounter == 0) {
				iterationsWithoutOrderChange++;
			} else {
				iterationsWithoutOrderChange = 0;
			}

			if (iterationsWithoutOrderChange == 3) {
				this.convergenceAchieved = true;
				this.finalNumberOfIterations = iterationCounter - 2;
			}

			// copy new order on top of old order
			pageRankOrderOrig = new double[pageRankOrderNew.length][];
			for(int i = 0; i < pageRankOrderNew.length; i++)
			{
				double[] aMatrix = pageRankOrderNew[i];
				int aLength = aMatrix.length;
				pageRankOrderOrig[i] = new double[aLength];
				System.arraycopy(aMatrix, 0, pageRankOrderOrig[i], 0, aLength);
			}
			pageRankOrderNew = new double[pageRankOrderOrig.length][2];

			pageRankBuilder = pageRankBuilder + "," + numberOfPRValuesChanged + lineBuilder + "," + positionInRankingChangedCounter + NEWLINE;
			pageRanks = new ArrayList<Double>(pageRanksUpdated);
			numberOfPRValuesChanged = 0;
			positionInRankingChangedCounter = 0;
			lineBuilder = "";
		}

		if (this.finalNumberOfIterations == 0) {
			this.finalNumberOfIterations = maxNumberOfIterations;
		}

		this.outputTextFile(outputFilename, pageRankBuilder);
	}
	
	// For specified CSV file and iteration number, generate the PageRank summary results text file
	private void generatePageRankSummary(String inputFilename, String outputFilename, int iterationNumber) {
		String pageRankSummary = "# iterations: <" + iterationNumber + ">" + NEWLINE;
		String pageRankLine = "";
		boolean resultsFound = false;

		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFilename));

			pageRankLine = reader.readLine();
			while (!resultsFound && (pageRankLine = reader.readLine()) != null && pageRankLine.length()>0) {
				String tokens[] = pageRankLine.split(",");

				if (Integer.parseInt(tokens[0]) == iterationNumber) {
					for (int loopCounter = 0; loopCounter < this.urlsInCrawlFile.size(); loopCounter++) {
						pageRankSummary = pageRankSummary + "    " + String.format("%1$-" + 100 + "s", "<" + this.urlsInCrawlFile.get(loopCounter) + ">") + "<" + tokens[loopCounter+2] + ">" + NEWLINE;
					}
					resultsFound = true;
				}

			}

			reader.close();

		} catch (FileNotFoundException e) {
			System.out.println(e);
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		}

		this.finalNumberOfIterations = 0;
		this.convergenceAchieved = false;

		this.outputTextFile(outputFilename, pageRankSummary);
	}

	
	// ******************************************************************************************
	// ** STATS GENERATION METHODS.                                                            **
	// ******************************************************************************************

	// Build table of number of outlinks from each visited URL
	private void generateNumberOfOutlinks() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(matrixFilename));

			String matrixLine = "";
			String outLinksTable = "Visited URL,Number of Outlinks" + NEWLINE;
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
					outLinksTable = outLinksTable + outLinkTotal + NEWLINE;
				}
			}

			reader.close();

			this.outputTextFile("outlinks.csv", outLinksTable);

		} catch (FileNotFoundException e) {
			System.out.println(e);
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		}
	}
	
	// Build table of number of inlinks to each visited URL
	private void generateNumberOfInlinks() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(matrixFilename));

			for (int counter = 0; counter<this.urlsInCrawlFile.size(); counter++) {
				this.numberOfInlinks.add(0);
			}

			String matrixLine = "";
			String inLinksTable = "Visited URL,Number of Inlinks" + NEWLINE;
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
				inLinksTable = inLinksTable + this.urlsInCrawlFile.get(counter) + "," + this.numberOfInlinks.get(counter) + NEWLINE;
			}

			this.outputTextFile("inlinks.csv", inLinksTable);

		} catch (FileNotFoundException e) {
			System.out.println(e);
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		}	
	}

	// Generate degree distribution table for the number of inlinks/outlinks per visited URL, depending on parameters
	private void generateDegreeDistribution(ArrayList<Integer> listOfNumbers, String rowHeaders, String outputFilename) {
		String output = rowHeaders + NEWLINE;

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
				output = output + numberSought + "," + numberOfHits + NEWLINE;
			}

			numberOfHits = 0;
			loopCounter = 0;
			numberSought++;
		}

		this.outputTextFile(outputFilename, output);
	}
	
	// Generate summary figures for the distribution of inlinks/outlinks depending on parameters
	private void generateSummaryFigures(ArrayList<Integer> listOfNumbers, String outputFilename, String linkType) {
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

		output="Mean Number of " + linkType + ",Variance,Standard Deviation" + NEWLINE + mean + "," + variance + "," + standardDeviation;

		this.outputTextFile(outputFilename, output);
	}
	

	// ******************************************************************************************
	// ** HELPER METHODS.                                                                      **
	// ******************************************************************************************

	// Build the list of URLs visited in the crawl file
	private void buildListOfVisitedURLs() {
		Scanner crawlData = new Scanner(this.accessURL());

		adjacencyMatrix = "ORIGIN POINT,";

		int lineCounter = 1;
		while (crawlData.hasNextLine()) {
			String strCrawlFileLine = crawlData.nextLine();

			// if t x
			if (strCrawlFileLine.startsWith("Visited:")) {
				String url = strCrawlFileLine.replace("Visited: ", "");

				//--------------------------------------------------------------------------------------
				//COME BACK TO THIS ONCE THE REST OF THE ASSIGNMENT IS WORKING BETTER!
				url = calculateFullURL(url, true);
				//--------------------------------------------------------------------------------------

				if (urlsInCrawlFile.indexOf(url) == -1) {
					urlsInCrawlFile.add(url);
					adjacencyMatrix = adjacencyMatrix + url + ",";
					posOfVisitedURLInCrawlFile.add(lineCounter);
				} else {
					posOfVisitedURLInCrawlFile.set(urlsInCrawlFile.indexOf(url), lineCounter);
				}
			}

			lineCounter++;
		}		
	}
	
	// Trim a URL to its "root" version, for extenstion by relative links
	private String convertToRootURL(String urlForConversion) {

		if (urlForConversion.lastIndexOf("/") > urlForConversion.lastIndexOf(".")) {
			// do nothing, as url is already a folder without slash, so is effectively root
		} else {
			// url is full filepath, so need to trim to folder
			urlForConversion = urlForConversion.substring(0, urlForConversion.lastIndexOf("/"));
		}

		return urlForConversion;
	}

	// Generate an absolute URL from whatever input is provided - which may be relative - and perform some cleaning
	private String getFullLinkURL(String strLinkURL) {
		String finalURL = "";

		if (!strLinkURL.startsWith("http://") && !strLinkURL.startsWith("ftp://")) {
			if (strLinkURL.startsWith("/")) {
				finalURL = this.currentVisitRootURL + strLinkURL;
			} else {
				finalURL = this.currentVisitRootURL + "/" + strLinkURL;
			}
		} else {
			finalURL = strLinkURL;
		}

		if (finalURL.contains("%7e")) {
			finalURL = finalURL.replaceAll("%7e", "~");
		}

		if (finalURL.contains("/./")) {
			finalURL = finalURL.replaceAll("/./", "/");
		}

		while (finalURL.contains("../")) {
			int pointer = finalURL.indexOf("../")-1;
			String linkManipulator = finalURL.substring(0, pointer);
			linkManipulator = linkManipulator.substring(0, linkManipulator.lastIndexOf("/"));
			finalURL = linkManipulator + finalURL.substring(pointer+3);
		}

		finalURL = calculateFullURL(finalURL, false);

		return finalURL;
	}

	// Clean the URL to ensure all links are handled consistently
	private String calculateFullURL(String url, boolean isVisitedPage) {
		//String baseRelativeURL = originalURL;

		if (url.contains("#")) {
			url = url.substring(0, url.indexOf("#"));
		}

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
					// if so, then remove slash
					url = url.substring(0, url.length()-1);
					return url;
				}

				// if dot not yet encountered, then 'originalURL' was already slashless
				if (!dotEncountered)
				{
					return url;
				}
				else
				{
					// as '.' was encountered before '/' in 'originalURL', it must have been a full filepath
					// check for default endings, and remove - helps resolve duplicates
					if (url.endsWith("/index.html")) {
						url = url.replace("/index.html", "");
					} else if (url.endsWith("/index.htm")){
						url = url.replace("/index.htm", "");
					}

					return url;
				}
			}

			if (charactersInURL[loopCounter] == '.')
			{
				dotEncountered=true;
			}

			loopCounter--;
		} while (loopCounter>=0);

		// if URL is well formed, should never reach this return statement
		return url;
	}

	// For specified URL, return array listing URLs linking to it
	private String[] getListOfURLsLinkingToCurrent(int currentURLPosition) {
		String strlistOfURLs = "";

		try {
			BufferedReader reader = new BufferedReader(new FileReader(matrixFilename));

			String matrixLine = "";
			int loopCounter = 0;
			int numberOfInlinksToCurrentURL = 0;

			matrixLine = reader.readLine();
			while ((matrixLine = reader.readLine()) != null && matrixLine.length()>0) {
				loopCounter++;

				String tokens[] = matrixLine.split(",");

				numberOfInlinksToCurrentURL = Integer.parseInt(tokens[currentURLPosition+1]);
				if (numberOfInlinksToCurrentURL > 0) {
					strlistOfURLs = strlistOfURLs + tokens[0] + ",";
				}
			}

			reader.close();

		} catch (FileNotFoundException e) {
			System.out.println(e);
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		}

		if (strlistOfURLs.length() > 0) {
			strlistOfURLs = strlistOfURLs.substring(0, strlistOfURLs.length()-1);
		}

		String listOfURLs[] = strlistOfURLs.split(",");

		return listOfURLs;
	}
	

	// ******************************************************************************************
	// ** INPUT/OUTPUT METHODS.                                                                **
	// ******************************************************************************************
	
	// Take a local copy of the datafile, as the calculations are sometimes slow enough to cause a timeout error
	private void makeLocalCopyOfFileToParse() {
		InputStream isCrawlDataForCopying = null;

		System.out.println("Taking local copy of crawl data...");

		try {
			URL url = new URI(this.crawlFileURL).toURL();
			isCrawlDataForCopying = url.openStream();
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

		Scanner crawlData = new Scanner(isCrawlDataForCopying);
		String crawlDataText = "";
		while (crawlData.hasNextLine()) {
			crawlDataText = crawlDataText + crawlData.nextLine() + NEWLINE;
		}

		outputTextFile(this.localCopyOfFileToParseFilename, crawlDataText);

		System.out.println("Local copy of crawl data successfully taken...");
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