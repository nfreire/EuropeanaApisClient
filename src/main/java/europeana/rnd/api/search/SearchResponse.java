package europeana.rnd.api.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonValue;

import inescid.util.datastruct.MapOfMapsOfInts;

public class SearchResponse {
	SearchApi api;
	JsonObject json;
	SearchRequest request;
	ArrayList<Hit> items=new ArrayList<Hit>();
	MapOfMapsOfInts<String, String> facets=new MapOfMapsOfInts<String, String>();
	int start=0;
	boolean isCursorInUse;
	
	protected SearchResponse(SearchApi api,JsonObject json, SearchRequest request) {
		this.api = api;
		this.json=json;
		this.request = request;
		isCursorInUse=request.getCursor()!=null;
		for(JsonValue it:json.getJsonArray("items")) 
			items.add(new Hit(it.asJsonObject()));
		if(json.getJsonArray("facets")!=null) {
			for(JsonValue fct:json.getJsonArray("facets")) {
				JsonObject fctObj = fct.asJsonObject();
				String fctName=fctObj.getString("name");
				for(JsonValue fld:fctObj.getJsonArray("fields")) { 
					JsonObject fldObj = fld.asJsonObject();
					facets.put(fctName, fldObj.getString("label"), fldObj.getInt("count"));
				}
			}
		}
		if(json.containsKey("nextCursor"))
			request.setCursor(json.getString("nextCursor"));
		else
			request.setCursor(null);
	}
	
	public int getTotalResults() {
		return Integer.parseInt(json.get("totalResults").toString());
	}
	
	public List<Hit> getNextItems() throws IOException {
		items.clear();
		if(isCursorInUse) {
			if(request.getCursor()==null)
				return items;
			SearchResponse nextResp = api.execute(request);
			json=nextResp.json;
			for(JsonValue it:json.getJsonArray("items")) 
				items.add(new Hit(it.asJsonObject()));
			if(json.containsKey("nextCursor"))
				request.setCursor(json.getString("nextCursor"));
			else
				request.setCursor(null);
			return items;			
		} else {
			request.toNextPage();
			SearchResponse nextResp = api.execute(request);
			json=nextResp.json;
			for(JsonValue it:json.getJsonArray("items")) 
				items.add(new Hit(it.asJsonObject()));
			return items;		
		}
	}
	

	
	
	@Override
	public String toString() {
		return json.toString();
	}

	public MapOfMapsOfInts<String, String> getFacets() {
		return facets;
	}
}
