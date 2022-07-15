package inescid.europeanaapi.clients.webresourcedata;

import java.util.HashMap;
import java.util.Map;

import inescid.util.datastruct.MapOfInts;


public class DataStatsProvider {
	public class Content {
		public String contentType;
		public byte[] content;
	}
	public MapOfInts<String> contentTypes=new MapOfInts<>();
	public MapOfInts<Integer> httpStatus=new MapOfInts<>();
	public Map<String, Content> content=new HashMap<>();
	public String id;
	
	public DataStatsProvider() {
	}
}
