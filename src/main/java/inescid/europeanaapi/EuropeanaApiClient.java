package inescid.europeanaapi;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFReader;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EuropeanaApiClient {
	public static final Pattern EUROPEANA_ID_IN_URI=Pattern.compile("/([^/]+/[^/]+)$"); 
	
	public static final String BASE_URL="https://www.europeana.eu/api/v2/"; 

	public static String NS_EDM="http://www.europeana.eu/schemas/edm/";

	public static final Property EDM_IS_SHOWN_BY = ResourceFactory.createProperty(NS_EDM, "isShownBy");
	public static final Property EDM_IS_SHOWN_AT = ResourceFactory.createProperty(NS_EDM, "isShownAt");
	public static final Property EDM_LANDING_PAGE = ResourceFactory.createProperty(NS_EDM, "landingPage");

	
	String apiKey;
	ObjectMapper jsonMapper = new ObjectMapper();
	
	public EuropeanaApiClient(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public List<String> listProviderIds() throws AccessException {
		List<String> providerIds=new ArrayList<String>();
		URL url;
		String urlStr = BASE_URL+"providers.json?wskey="+apiKey;
		try {
			url = new URL(urlStr);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.connect();

			int code = con.getResponseCode();
			if (code!=200)
				throw buildAccessException(con);
			String jsonStr = IOUtils.toString(con.getInputStream(), con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding());
			JsonNode  topNode = jsonMapper.readTree(jsonStr);
			JsonNode itemsNode = topNode.get("items");
			for(int i=0; i<itemsNode.size(); i++) {
				JsonNode provNode = itemsNode.get(i);
				if(provNode.get("identifier")==null)
					System.out.println("WARNING: "+jsonMapper.writeValueAsString(provNode));
				else
					providerIds.add(provNode.get("identifier").asText());
			}
			return providerIds;
		} catch (IOException e) {
			throw new AccessException(urlStr, e);
		}
		
	}
	

	public Map<String, String> listProviderIdsAndNames() throws AccessException {
		Map<String, String> providerIds=new HashMap<>();
		URL url;
		String urlStr = BASE_URL+"providers.json?wskey="+apiKey;
		try {
			url = new URL(urlStr);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.connect();
			
			int code = con.getResponseCode();
			if (code!=200)
				throw buildAccessException(con);
			String jsonStr = IOUtils.toString(con.getInputStream(), con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding());
			JsonNode  topNode = jsonMapper.readTree(jsonStr);
			JsonNode itemsNode = topNode.get("items");
			for(int i=0; i<itemsNode.size(); i++) {
				JsonNode provNode = itemsNode.get(i);
				if(provNode.get("identifier")==null)
					System.out.println("WARNING: "+jsonMapper.writeValueAsString(provNode));
				else
					providerIds.put(provNode.get("identifier").asText(), provNode.get("name").asText());
			}
			return providerIds;
		} catch (IOException e) {
			throw new AccessException(urlStr, e);
		}
		
	}
	
	public List<String> listProviderDatasetsIds(String providerId) throws AccessException {
		List<String> datasetsIds=new ArrayList<String>();
		URL url;
		String urlStr = BASE_URL+"provider/"+providerId+"/datasets.json?wskey="+apiKey;
		try {
			System.out.println(urlStr);
			url = new URL(urlStr);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.connect();

			int code = con.getResponseCode();
			if (code!=200)
				throw buildAccessException(con);
			String jsonStr = IOUtils.toString(con.getInputStream(), con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding());
			JsonNode  topNode = jsonMapper.readTree(jsonStr);
			JsonNode itemsNode = topNode.get("items");
			for(int i=0; i<itemsNode.size(); i++) {
				JsonNode provNode = itemsNode.get(i);
				if(provNode.get("identifier")==null)
					System.out.println(jsonMapper.writeValueAsString(provNode));
				else
					datasetsIds.add(provNode.get("identifier").asText());
			}
			return datasetsIds;
		} catch (IOException e) {
			throw new AccessException(urlStr, e);
		}
		
	}
	
	public List<String> listProviderDatasetsNames(String providerId) throws AccessException {
		List<String> datasetsNames=new ArrayList<String>();
		URL url;
		String urlStr = BASE_URL+"provider/"+providerId+"/datasets.json?wskey="+apiKey;
		try {
			System.out.println(urlStr);
			url = new URL(urlStr);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.connect();
			
			int code = con.getResponseCode();
			if (code!=200) 
				throw buildAccessException(con);
			String jsonStr = IOUtils.toString(con.getInputStream(), con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding());
			JsonNode  topNode = jsonMapper.readTree(jsonStr);
			JsonNode itemsNode = topNode.get("items");
			for(int i=0; i<itemsNode.size(); i++) {
				JsonNode provNode = itemsNode.get(i);
				datasetsNames.add(provNode.get("edmDatasetName").asText());
			}
			return datasetsNames;
		} catch (IOException e) {
			throw new AccessException(urlStr, e);
		}
		
	}
	
	public List<String> listDatasetRecordsIds(String datasetName) throws AccessException {
		List<String> datasetsIds=new ArrayList<String>();
		String nextCursor="*";
		String urlStr="";
		try {
			URL url;
			while(nextCursor!=null) {
				try {
					urlStr = BASE_URL+"search.json?query=europeana_collectionName:"+URLEncoder.encode(datasetName, "UTF8")+"&cursor="+URLEncoder.encode(nextCursor, "UTF-8")+"&rows=100&profile=minimal&wskey="+apiKey;
				} catch (UnsupportedEncodingException e1) {
					throw new RuntimeException(e1.getMessage(), e1);
				}
				url = new URL(urlStr);
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				con.setRequestMethod("GET");
				con.connect();
				
				int code = con.getResponseCode();
				if (code!=200)
					throw buildAccessException(con);
				String jsonStr = IOUtils.toString(con.getInputStream(), con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding());
				JsonNode  topNode = jsonMapper.readTree(jsonStr);
				JsonNode jsonNode = topNode.get("nextCursor");
				nextCursor=jsonNode==null ? null : jsonNode.asText();
				JsonNode itemsNode = topNode.get("items");
				for(int i=0; i<itemsNode.size(); i++) {
					JsonNode provNode = itemsNode.get(i);
					if(provNode.get("id")==null)
						System.out.println(jsonMapper.writeValueAsString(provNode));
					else
						datasetsIds.add(provNode.get("id").asText());
				}
			}
			return datasetsIds;
		} catch (IOException e) {
			throw new AccessException(urlStr, e);		
		}
	}
	
	public List<Map<String, String>> listDatasetRecordsFieldValues(String datasetName, int startAt, int resultsToGet, String... fields) throws AccessException {
		List<Map<String, String>> recordsFieldValues=new ArrayList<>();
		URL url;
		String urlStr;
		try {
			urlStr = BASE_URL+"search.json?query=edm_datasetName:"+URLEncoder.encode("\""+datasetName+"\"", "UTF8")+"&start="+startAt+"&rows="+resultsToGet+"&profile=rich&wskey="+apiKey;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		try {
			System.out.println(urlStr);
//			String q = BASE_URL+"search.json?query=europeana_collectionName:"+datasetName+"&start="+startAt+"&rows="+resultsToGet+"&profile=rich&wskey="+apiKey;
			url = new URL(urlStr);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("GET");
			con.connect();
			
			int code = con.getResponseCode();
			if (code!=200) 
				throw buildAccessException(con);
			String jsonStr = IOUtils.toString(con.getInputStream(), con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding());
			JsonNode  topNode = jsonMapper.readTree(jsonStr);
			JsonNode itemsNode = topNode.get("items");
			for(int i=0; i<itemsNode.size(); i++) {
				JsonNode recNode = itemsNode.get(i);
				Map<String, String> recFields=new HashMap<>();
				for(String fldName: fields) {
					JsonNode fld = recNode.get(fldName);
					if(fld!=null) {
						if(fld.isArray()) {
							recFields.put(fldName, fld.get(0).asText());
						} else if (fld.isValueNode()){
							recFields.put(fldName, fld.asText());
						}
					}
				}
				recordsFieldValues.add(recFields);
			}
			return recordsFieldValues;
		} catch (IOException e) {
			throw new AccessException(urlStr, e);	
		}
	}
	
	public Model getRecord(String europeanaApiRecordId) throws AccessException {
		String urlStr = recordUriFromApiId(europeanaApiRecordId);
		return getRecordByUri(urlStr);
	}
	
	public String getRecordRdfXml(String europeanaApiRecordId) throws AccessException {
		String urlStr = recordUriFromApiId(europeanaApiRecordId);
		return getRecordRdfXmlByUri(urlStr);
	}
	
	public Model parseEdmRdfXml(String edmRdfXml) throws AccessException {
		StringReader mdReader = new StringReader(edmRdfXml);
		Model ldModelRdf = ModelFactory.createDefaultModel();
		RDFReader reader = ldModelRdf.getReader("RDF/XML");
		reader.setProperty("allowBadURIs", "true");
		reader.read(ldModelRdf, mdReader, null);
		mdReader.close();
		return ldModelRdf;
	}
	
	public Model getRecordByUri(String uriOfCho) throws AccessException {
		String md = getRecordRdfXmlByUri(uriOfCho);
		return parseEdmRdfXml(md);
	}
	public String getRecordRdfXmlByUri(String uriOfCho) throws AccessException {
		URL url = null;
		HttpURLConnection urlCon = null;
		int attempst=0;
		while(true) {
			try {
				url = new URL(uriOfCho);
				urlCon = (HttpURLConnection) url.openConnection();
				urlCon.setRequestMethod("GET");
				urlCon.setRequestProperty("Accept", "application/rdf+xml");
				//			System.out.println(urlCon.getResponseCode());
				//			System.out.println(urlCon.getHeaderFields());
				if(urlCon.getResponseCode()>=301 && urlCon.getResponseCode()<=308) {
					//				System.out.println("Redirect to "+ urlCon.getHeaderField("Location"));
					return getRecordRdfXmlByUri(urlCon.getHeaderField("Location"));
				}
				String md = IOUtils.toString((InputStream) urlCon.getContent(), "UTF8");
				return md;
			} catch (IOException e) {
				attempst++;
				if(attempst>3)
					throw  buildAccessException(urlCon);	
			}
		}
	}

	public String recordUriFromApiId(String europeanaApiRecordId) {
		return "http://data.europeana.eu/item" + (europeanaApiRecordId.startsWith("/") ? "" : "/") + europeanaApiRecordId;
	}
	
	private AccessException buildAccessException(HttpURLConnection con) {
		try {
			String body = null;
			try {
				if(con.getInputStream()!=null)
						body=IOUtils.toString(con.getInputStream(), con.getContentEncoding() == null ? "UTF-8" : con.getContentEncoding());
			} catch (IOException e) { /*ignore*/ }
			return new AccessException("HTTP-status:"+con.getResponseCode() +" ; "+con.getURL().toString(), String.valueOf(con.getResponseCode()), body);
		} catch (IOException e) {
			return new AccessException(con.getURL().toString());
		}
	}


}
