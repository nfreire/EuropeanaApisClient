package europeana.rnd.api.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class Hit {
	JsonObject json;

	public Hit(JsonObject json) {
		super();
		this.json = json;
	}

	public String getId() {
		return json.getString("id");
	}

	public String getUri() {
		return recordUriFromApiId(json.getString("id"));
	}
		
	
	public static String recordUriFromApiId(String europeanaApiRecordId) {
		return "http://data.europeana.eu/item" + (europeanaApiRecordId.startsWith("/") ? "" : "/") + europeanaApiRecordId;
	}

	public List<String> getEdmConcepts() {
		ArrayList<String> res=new ArrayList<String>();
		JsonArray array = json.getJsonArray("edmConcept");
		if(array!=null)
			for(int i=0; i<array.size() ; i++ ) 
				res.add(array.getString(i));
		return res;
	}

	public List<String> getDcSubjectLangAware() {
		ArrayList<String> res=new ArrayList<String>();
		JsonObject langsObj = json.getJsonObject("dcSubjectLangAware");
		for(String lang: langsObj.keySet()) {
			JsonArray array = langsObj.getJsonArray(lang);
			for(int i=0; i<array.size() ; i++ ) 
				res.add(array.getString(i));			
		}
		return res;
	}
}
