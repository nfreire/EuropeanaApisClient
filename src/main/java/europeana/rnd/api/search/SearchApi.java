package europeana.rnd.api.search;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class SearchApi {
	public final String BASE_URL="https://api.europeana.eu/record/v2/search.json";

	public SearchResponse execute(SearchRequest request) throws IOException {
//		System.out.println(BASE_URL+request.toUrlParameters());
		
		URL url=new URL(BASE_URL+request.toUrlParameters());
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		if(con.getResponseCode()!=200) {
			throw new RuntimeException("Error. response code:"+ con.getResponseCode());
		}
		InputStream inputStream = con.getInputStream();
		
		JsonReader parser = Json.createReader(inputStream);
		JsonObject json=parser.readObject();
		
		return new SearchResponse(this, json, request);
	}

}
