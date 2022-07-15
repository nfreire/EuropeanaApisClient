package inescid.europeanaapi.clients.webresourcedata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import inescid.europeanaapi.EuropeanaApiClient;
import inescid.util.RdfUtil;
import inescid.util.datastruct.MapOfInts;

public class ScriptReportOnExportWebResourcesLinks {
	
	public static void main(String[] args) throws Throwable {
		File csvFile=new File("target/urls_sample_"+ScriptExportWebResourcesLinks.SAMPLED_CHOS_PER_DATASET+"urls.csv");
		MapOfInts<String> alreadyProcessedDatasets=new MapOfInts<>();
		if(csvFile.exists()) {
			CSVParser csvParser=new CSVParser(new FileReader(csvFile), CSVFormat.EXCEL);
			csvParser.forEach(record -> {
				String dasetId = record.get(1);
				alreadyProcessedDatasets.incrementTo(dasetId);
			});
			csvParser.close();
		}
		

		
		for(String ds: alreadyProcessedDatasets.keySet()) {
			Integer urls = alreadyProcessedDatasets.get(ds);
			if(urls!=ScriptExportWebResourcesLinks.SAMPLED_CHOS_PER_DATASET)
				System.out.println("Adjustement needed for "+ds+": "+urls);
		}
	}
}
