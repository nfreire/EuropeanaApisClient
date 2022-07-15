package europeana.rnd.api.search.scripts;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;

import europeana.rnd.api.search.Hit;
import europeana.rnd.api.search.Profile;
import europeana.rnd.api.search.SearchApi;
import europeana.rnd.api.search.SearchRequest;
import europeana.rnd.api.search.SearchResponse;
import apiclient.google.GoogleApi;
import apiclient.google.sheets.SheetsPrinter;

public class ScriptSearchIconClass {

	
	public static void main(String[] args) throws Exception {
		SearchApi apiClient=new SearchApi();
		SearchRequest request=new SearchRequest();
		
		request.setQuery("\"http://iconclass.org/11\"");
//		request.setProfile(Profile.minimal);
//		request.setMedia(true);
		
		request.setWskey("zMSWiBqsJ");
//		request.setWskey("api2demo");

		GoogleApi.init("c:/users/nfrei/.credentials/Data Aggregation Lab-b1ec5c3705fc.json");
//		String gSheetId = GoogleSheetsApi.create("SGoaB - ICONCLASS at Europeana", "Counts");
//		System.out.println("https://docs.google.com/spreadsheets/d/"+gSheetId);

		
		String gSheetId = "1w4maZ7pnhE-hiWFZj_a9yr9zC3MCFXJrFZCH9FN61XE";
		
		
		SheetsPrinter sheetCounts=new SheetsPrinter(gSheetId, "Counts"); 
		
		for(String icCode : FileUtils.readLines(new File("C:\\Users\\nfrei\\Desktop\\data\\sgoab-iconclass.txt"), StandardCharsets.UTF_8)) {
			int count=0;
			SheetsPrinter sheetIds=new SheetsPrinter(gSheetId, icCode); 
			
//			for(String q: new String[] {"\"http://iconclass.org/"+icCode+"\"", "\""+icCode+"\""}) {
//			for(String q: new String[] {"\"http://iconclass.org/"+icCode+"\""}) {
			for(String q: new String[] {"proxy_dc_subject:\""+icCode+"\""}) {
				request.setCursor("*");
				request.setQuery(q);
				request.setProfile(Profile.rich);			
				SearchResponse response=apiClient.execute(request);
//				System.out.println(q+" "+response.getTotalResults());
//				count+=response.getTotalResults();

				List<Hit> items = response.getNextItems();
				HITS: while(!items.isEmpty()) {
					for(Hit h: items) {
						boolean hasCode=false;
						for(String concept: h.getEdmConcepts()) {
							concept=concept.trim();
							if(concept.equals(icCode) || concept.endsWith("http://iconclass.org/"+icCode)) {
								hasCode=true;
								break;
							}
						}
						if(!hasCode)
							for(String concept: h.getDcSubjectLangAware()) {
								concept=concept.trim();
								if(concept.equals(icCode) || concept.endsWith("http://iconclass.org/"+icCode)) {
									hasCode=true;
									break;
								}							
							}
						if(hasCode) {
							count++;
							sheetIds.printRecord(h.getId());
//							if(count>=1000)
//								break HITS;
						}
					}
					items = response.getNextItems();
				}
			}
			sheetIds.close();
			sheetCounts.printRecord(icCode, count);
			System.out.println(icCode+" - "+count + " results");
		}
		sheetCounts.close();
	}
	
	
}
