package inescid.europeanaapi.clients.webresourcedata;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;

import inescid.europeanaapi.HttpsUtil;


public class ScriptCheckLodContentNegotiationHtmlAtWebResourcesAndDownload {
	
	private static final long CONTENT_LENGTH_LIMIT = 10*1000000;

	public static void main(String[] args) throws Throwable {
		new ScriptCheckLodContentNegotiationHtmlAtWebResourcesAndDownload().run();
	}
	
	Map<String, DataStatsProvider> stats;
	File failuresLogFile=new File("target/failures.log.txt");
	File repositoryFolder=new File("target/downloaded");
	boolean skipExisting=true;
	HashSet<String> failuresOfPrevious=new HashSet<>();
	
	public void run() throws Throwable {
		if(failuresLogFile.exists()) {
			CSVParser csvParser=new CSVParser(new FileReader(failuresLogFile), CSVFormat.EXCEL);
			csvParser.forEach(record -> {
				failuresOfPrevious.add(record.get(0));
			});
			csvParser.close();
//			failuresLogFile.delete();
		}
		if(!repositoryFolder.exists())
			repositoryFolder.mkdirs();
		
		HttpsUtil.initSslTrustingHostVerifier();
		
		File csvFile=new File("src/data/urls_sample_100urls.csv");
		if(!csvFile.exists()) {
			System.out.println("Input CSV file not found: "+csvFile.getCanonicalPath());
			return;
		}
		
		CSVParser csvParser=new CSVParser(new FileReader(csvFile), CSVFormat.EXCEL);
		final String[] lastProvId=new String[1];
		csvParser.forEach(record -> {
			WebResource wr=new WebResource(record); 
			try {
				checkLodContentNegotiation(wr.provId, wr.europeanaUri, wr.shownAt, wr.shownBy, wr.landingPage);
			} catch (IOException e) {//retry
				try {
					checkLodContentNegotiation(wr.provId, wr.europeanaUri, wr.shownAt, wr.shownBy, wr.landingPage);
				} catch (IOException e2) {
					System.err.println("Exception on: "+wr.europeanaUri);
					e2.printStackTrace();
				}
			} 
		});
		csvParser.close();
	}

	private Boolean checkLodContentNegotiation(String providerId, String europeanaUri, String... urls) throws IOException {
		Integer httpStatus=null;
		String contentType=null;
		String testedUrl=null;
		String lastError="";
		boolean lastErrorIsContentType=false;
		File repoProvFolder=new File(repositoryFolder, "html/"+providerId);
		File repoProvDataFolder=new File(repositoryFolder, "data/"+providerId);
		File contentFile = new File(repoProvFolder, URLEncoder.encode(europeanaUri, "UTF-8")+ ".html");
		File contentDataFile = new File(repoProvDataFolder, URLEncoder.encode(europeanaUri, "UTF-8")+ ".data.txt");
		if(skipExisting && ((contentFile.exists() && contentFile.length()>0) || (contentDataFile.exists() && contentDataFile.length()>0))) {
			System.out.println("Exists in repository. skipping: "+europeanaUri);
			return null;
		}
		if(skipExisting && (failuresOfPrevious.contains(europeanaUri))) {
			System.out.println("Exists in previous failures. skipping: "+europeanaUri);
			return null;
		}
		for(String urlStr: urls) {
			try {
				if(urlStr==null || urlStr.isEmpty() || urlStr.endsWith(".jpg") || urlStr.endsWith(".pdf") || urlStr.endsWith(".mp3"))
					continue;
				URL url=new URL(urlStr);
				HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
				urlCon.setRequestMethod("GET");
				urlCon.setRequestProperty("Accept", "application/rdf+xml, text/turtle, application/n-triples, application/ld+json, application/json");
				urlCon.connect();
				httpStatus = urlCon.getResponseCode();
				contentType = urlCon.getContentType();
				if (httpStatus==200) {
					if(contentType!=null) {
						String charset="UTF8";
						int charsetPos = contentType.indexOf("Charset=");
						if(charsetPos<0) 
							charsetPos = contentType.indexOf("charset=");
						if(charsetPos>=0) { 
							charset=contentType.substring(charsetPos);
							charset=charset.substring(charset.indexOf('=')+1);
							int charsetEndPos = charset.indexOf(';');
							if(charsetEndPos>=0) 
								charset=charset.substring(0, charsetEndPos).trim();
						}
						if(contentType.startsWith("text/html")) {
							if(!repoProvFolder.exists())
								repoProvFolder.mkdirs();
							String html = IOUtils.toString((InputStream) urlCon.getContent(), charset);
							FileUtils.write(contentFile, html, "UTF-8");
						} else if(!contentType.startsWith("application/octet-stream") && !contentType.startsWith("image/") &&!contentType.startsWith("video/") && !contentType.startsWith("audio/") &&!contentType.startsWith("application/pdf")) {
							if(urlCon.getContentLengthLong() >= CONTENT_LENGTH_LIMIT)
								lastError="Content too large: " +urlCon.getContentLengthLong() + " " + contentType;
							else {
								if(!repoProvDataFolder.exists())
									repoProvDataFolder.mkdirs();
								try {
									String data = IOUtils.toString((InputStream) urlCon.getContent(), charset);
									FileUtils.write(contentDataFile, contentType + "\n" + data, "UTF-8");
								} catch (OutOfMemoryError e) {
									lastError="OutOfMemory error downloading content: " +contentType;
								}
							}
						} else {
							lastError="Ignoring content-type: " +contentType;
							lastErrorIsContentType=true;
//							System.out.println(lastError);
//							StringBuilder sb=new StringBuilder();
//							CSVPrinter csvPrinter=new CSVPrinter(sb, CSVFormat.EXCEL);
//							csvPrinter.printRecord(europeanaUri, urlStr, lastError);
//							csvPrinter.flush();
//							csvPrinter.close();
//							FileUtils.write(failuresLogFile, sb.toString(), "UTF-8", true);
						}
					}else {
						lastError="Unknown content-type ";
						lastErrorIsContentType=true;
//						System.out.println(lastError);
//						StringBuilder sb=new StringBuilder();
//						CSVPrinter csvPrinter=new CSVPrinter(sb, CSVFormat.EXCEL);
//						csvPrinter.printRecord(europeanaUri, urlStr, lastError);
//						csvPrinter.flush();
//						csvPrinter.close();
//						FileUtils.write(failuresLogFile, sb.toString(), "UTF-8", true);						
					}
					testedUrl=urlStr;
				} else if (httpStatus>=300 && httpStatus<=399) {
					String loc = urlCon.getHeaderField("Location");
					if (loc!=null) {
						contentType=loc;
						testedUrl=urlStr;
						if (!checkLodContentNegotiation(providerId,europeanaUri, loc)) {
							lastError="HTTP status "+httpStatus;
						}else
							break;
					}
				} else {
					testedUrl=urlStr;
					lastError="HTTP status "+httpStatus;
				}
			} catch (Exception e) {
				testedUrl=urlStr;
				lastError=e.getMessage();
				e.printStackTrace();
			} 
		}
		System.out.print("Test result for "+europeanaUri+" ");

//		contentFile = new File(repoProvFolder, URLEncoder.encode(europeanaUri, "UTF-8")+ ".html");
//		System.out.println(contentFile.exists());
//		System.out.println(contentFile.getAbsolutePath());
		if(((contentFile.exists() && contentFile.length()>0) || (contentDataFile.exists() && contentDataFile.length()>0))) { 
			System.out.println(" OK");
			return true;
		}
		if(httpStatus!=null) 
			System.out.print(httpStatus);
		System.out.print(" ");
		System.out.print(contentType==null ? "" : contentType);
		System.out.print("\nurl: " + testedUrl+"\n ");
		if(lastError!=null) {
			System.out.println(" unsuccessful: "+lastError);
			if(!checkForHtml(providerId, europeanaUri, urls)) {
				StringBuilder sb=new StringBuilder();
				CSVPrinter csvPrinter=new CSVPrinter(sb, CSVFormat.EXCEL);
				csvPrinter.printRecord(europeanaUri, testedUrl, lastError == null ? "<No error message>" : lastError);
				csvPrinter.flush();
				csvPrinter.close();
				FileUtils.write(failuresLogFile, sb.toString(), "UTF-8", true);
				return false;
			} 
		}
		return true;
	}
	
	private boolean checkForHtml(String providerId, String europeanaUri, String... urls) throws IOException {
		Integer httpStatus=null;
		String contentType=null;
		File repoProvFolder=new File(repositoryFolder, "html/"+providerId);
		File contentFile = new File(repoProvFolder, URLEncoder.encode(europeanaUri, "UTF-8")+ ".html");
		for(String urlStr: urls) {
			try {
				if(urlStr==null || urlStr.isEmpty())
					continue;
				URL url=new URL(urlStr);
				HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
				urlCon.setRequestMethod("GET");
				urlCon.setRequestProperty("Accept", "text/html");
				urlCon.connect();
				httpStatus = urlCon.getResponseCode();
				contentType = urlCon.getContentType();
				if (httpStatus==200) {
					if(contentType!=null && contentType.startsWith("text/html")) {
						String charset="UTF8";
						int charsetPos = contentType.indexOf("Charset=");
						if(charsetPos<0) 
							charsetPos = contentType.indexOf("charset=");
						if(charsetPos>=0) {
							charset=contentType.substring(charsetPos);
							charset=charset.substring(charset.indexOf('=')+1);
							int charsetEndPos = charset.indexOf(';');
							if(charsetEndPos>=0) 
								charset=charset.substring(0, charsetEndPos).trim();
						}
						String html = IOUtils.toString((InputStream) urlCon.getContent(), charset);
						FileUtils.write(contentFile, html, "UTF-8");
						return true;
					}
				} else if (httpStatus>=300 && httpStatus<=399) {
					String loc = urlCon.getHeaderField("Location");
					if (loc!=null) {
//						contentType=loc;
						return checkForHtml(providerId,europeanaUri, loc);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		return false;
	}
	
}
