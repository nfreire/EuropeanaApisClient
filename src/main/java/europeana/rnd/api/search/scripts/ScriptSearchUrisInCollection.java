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

public class ScriptSearchUrisInCollection {

	
	public static void main(String[] args) throws Exception {
		SearchApi apiClient=new SearchApi();
		SearchRequest request=new SearchRequest();
		
		request.setQuery("edm_datasetName:\"90402_M_NL_Rijksmuseum\"");
		request.setProfile(Profile.minimal);
		request.setCursor("*");
		
//		request.setMedia(true);
		
		request.setWskey("zMSWiBqsJ");
//		request.setWskey("api2demo");

		int count=0;
			
		File outFile=new File("C:\\Users\\nfrei\\Desktop\\URIs-90402_M_NL_Rijksmuseum.txt");
		
		
		SearchResponse response=apiClient.execute(request);
//				System.out.println(q+" "+response.getTotalResults());
//				count+=response.getTotalResults();

		List<Hit> items = response.getNextItems();
		HITS: while(!items.isEmpty()) {
			for(Hit h: items) {
				System.out.println(h.getUri());
				FileUtils.write(outFile, h.getUri()+"\n", StandardCharsets.UTF_8, true);
			}
			items = response.getNextItems();
		}
	}
	
	
}
