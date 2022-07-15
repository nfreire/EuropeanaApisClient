package inescid.europeanaapi.clients.webresourcedata;

import java.io.File;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import inescid.util.datastruct.MapOfInts;

public class ScriptCheckForSchemaOrgInHtml {
	
	public static void main(String[] args) throws Throwable {
		new ScriptCheckForSchemaOrgInHtml().run();
	}
	
	File repositoryFolder=new File("target/downloaded/html");
	Pattern collectionUriPattern=Pattern.compile("https?://[^/]+/item/([^/]+)");
	Pattern schemaOrgUriPattern=Pattern.compile("\"(https?://schema.org/[^\"]+)\"");
	StatisticCalcMean urisPerRecord=new StatisticCalcMean();
	
	public void run() throws Throwable {
		
		int totalFiles=0;
		int totalProviders=0;
		HashSet<String> totalCollections=new HashSet<>(600);
		int totalSchemaOrg=0;
		
		MapOfInts<String> schemaOrgUrisCount=new MapOfInts<>();
		
		for(File provFolder: repositoryFolder.listFiles()) {
			if(provFolder.isDirectory()) {
				totalProviders++;
				for(File contentFile: provFolder.listFiles()) {
					{
						String uri=URLDecoder.decode(contentFile.getName());
						Matcher matcher = collectionUriPattern.matcher(uri);
						while(matcher.find()) {
							String col = matcher.group(1);
							totalCollections.add(col);
						}
					}					
					String htmlContent = FileUtils.readFileToString(contentFile, "UTF-8");
					totalFiles++;
					if(htmlContent.contains("://schema.org/")) {
						 totalSchemaOrg++;
						 int urisInRec=0;
						 Matcher matcher = schemaOrgUriPattern.matcher(htmlContent);
						 while(matcher.find()) {
							 String schemaUri = matcher.group(1);
							 schemaOrgUrisCount.incrementTo(schemaUri);
							 urisInRec++;
						 }
						 if(urisInRec==0) {
							 System.out.println("not URIs found in "+contentFile.getName());
						 }else
							 urisPerRecord.enter(urisInRec);
					}
				}	
			}
		}
		
		System.out.println("Providers: " +totalProviders);
		System.out.println("Collections: " +totalCollections.size());
		System.out.println("Files: " +totalFiles);
		System.out.println("Schema.org: " +totalSchemaOrg);
		
		System.out.println(schemaOrgUrisCount.toString());
		System.out.println(urisPerRecord.toString());
	}
}
