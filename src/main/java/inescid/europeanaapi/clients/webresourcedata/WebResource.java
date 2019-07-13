package inescid.europeanaapi.clients.webresourcedata;

import org.apache.commons.csv.CSVRecord;

public class WebResource {
	public String provId;
	public String europeanaId ;
	public String europeanaUri;
	public String shownAt;
	public String shownBy;
	public String landingPage;

	public WebResource(CSVRecord record) {
		provId = record.get(0);
		europeanaId = record.get(1);
		europeanaUri = record.get(2);
		shownAt = record.get(3);
		shownBy = record.get(4);
		landingPage = record.get(5);
	}
}
