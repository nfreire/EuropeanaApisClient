package europeana.rnd.api.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SearchRequest {
	
	String query;
	Boolean textFulltext;
	Boolean media;
	Reusability reusability;
	Theme theme;
	
	Profile profile;
	String[] facets;
	
	String cursor;
	Integer start;
	Integer rows=20;

	String wskey;
	
	public String toUrlParameters() { 
		StringBuilder sb=new StringBuilder();
		appendIfNotEmpty("query", query, sb);
		appendIfNotEmpty("text_fulltext", textFulltext, sb);
		appendIfNotEmpty("media", media, sb);
		appendIfNotEmpty("reusability", reusability, sb);
		appendIfNotEmpty("theme", theme, sb);
		appendIfNotEmpty("profile", profile, sb);
		if(facets!=null)
			for(String facet: this.facets) 
				appendIfNotEmpty("facet", facet, sb);
				
		if(cursor!=null)
			appendIfNotEmpty("cursor", cursor, sb);
		else
			appendIfNotEmpty("start", start, sb);
		appendIfNotEmpty("rows", rows, sb);
		appendIfNotEmpty("wskey", wskey, sb);
		return sb.toString();
	}


	private void appendIfNotEmpty(String param, Object value, StringBuilder sb) {
		if(value!=null) {
			if(sb.length()>0)
				sb.append("&");
			else
				sb.append("?");
			try {
				sb.append(param).append("=").append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.toString()));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
	}


	public String getQuery() {
		return query;
	}


	public void setQuery(String query) {
		this.query = query;
	}


	public Boolean getTextFulltext() {
		return textFulltext;
	}


	public void setTextFulltext(Boolean textFulltext) {
		this.textFulltext = textFulltext;
	}


	public Boolean getMedia() {
		return media;
	}


	public void setMedia(Boolean media) {
		this.media = media;
	}


	public Reusability getReusability() {
		return reusability;
	}


	public void setReusability(Reusability reusability) {
		this.reusability = reusability;
	}


	public Theme getTheme() {
		return theme;
	}


	public void setTheme(Theme theme) {
		this.theme = theme;
	}


	public Profile getProfile() {
		return profile;
	}


	public void setProfile(Profile profile) {
		this.profile = profile;
	}


	public String getCursor() {
		return cursor;
	}


	public void setCursor(String cursor) {
		this.cursor = cursor;
	}


	public Integer getStart() {
		return start;
	}


	public void setStart(Integer start) {
		this.start = start;
	}


	public Integer getRows() {
		return rows;
	}


	public void setRows(Integer rows) {
		this.rows = rows;
	}


	public String getWskey() {
		return wskey;
	}


	public void setWskey(String wskey) {
		this.wskey = wskey;
	}


	public void toNextPage() {
		if(start==null)
			start=1;
		start=start+rows;;
	}


	public void setFacets(String... facets) {
		this.facets=facets;
	}
	
	
	
}
