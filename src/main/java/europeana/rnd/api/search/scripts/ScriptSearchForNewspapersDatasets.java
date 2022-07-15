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
import europeana.rnd.api.search.Theme;
import inescid.util.datastruct.MapOfMapsOfInts;
import apiclient.google.GoogleApi;
import apiclient.google.sheets.SheetsPrinter;

public class ScriptSearchForNewspapersDatasets {

	public static void main(String[] args) throws Exception {
		SearchApi apiClient = new SearchApi();
		SearchRequest request = new SearchRequest();
		request.setWskey("zMSWiBqsJ");
//		request.setWskey("api2demo");
		request.setQuery("*");
		request.setProfile(Profile.facets);
//		request.setTheme(Theme.newspaper);
		request.setTheme(Theme.ww1);
//		request.setTheme(Theme.art);
		request.setFacets("edm_datasetName");

		SearchResponse response = apiClient.execute(request);
//				System.out.println(q+" "+response.getTotalResults());
//				count+=response.getTotalResults();

		MapOfMapsOfInts<String, String> items = response.getFacets();
		for (String dataset : items.get("edm_datasetName").keySet()) {
			System.out.println("add(\""+dataset.substring(0, dataset.indexOf('_'))+"\");");
		}
	}

}
