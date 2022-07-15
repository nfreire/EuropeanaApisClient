package europeana.rnd.api.search.scripts;

import europeana.rnd.api.search.Reusability;
import europeana.rnd.api.search.SearchApi;
import europeana.rnd.api.search.SearchRequest;
import europeana.rnd.api.search.SearchResponse;
import europeana.rnd.api.search.Theme;

public class TestSearchApi {

	
	public static void main(String[] args) throws Exception {
		SearchApi apiClient=new SearchApi();
		SearchRequest request=new SearchRequest();
		
		request.setQuery("*");
//		request.setProfile(Profile.facets);
		request.setTextFulltext(true);
		request.setMedia(true);
		
		request.setWskey("zMSWiBqsJ");
		
		System.out.println("THEME,RECORD COUNT WITH FULLTEXT,RECORD COUNT WITH FULLTEXT AND OPEN REUSE");
		for(Theme t: Theme.values()) {
			request.setTheme(t);
			SearchResponse response=apiClient.execute(request);
			System.out.print(t+","+response.getTotalResults());
			request.setReusability(Reusability.open);
			response=apiClient.execute(request);
			System.out.println(","+response.getTotalResults());
			
		}
	
	}
	
	
}
