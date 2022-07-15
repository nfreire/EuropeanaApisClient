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

import inescid.dataaggregation.data.model.Rdf;
import inescid.dataaggregation.data.model.Edm;
import inescid.dataaggregation.data.model.Ore;
import inescid.dataaggregation.data.model.Rdf;
import inescid.europeanaapi.EuropeanaApiClient;
import inescid.util.datastruct.MapOfInts;
import inescid.util.RdfUtil;

public class ScriptExportWebResourcesLinks {
	static final int SAMPLED_CHOS_PER_DATASET=100;
	
	public static void main(String[] args) throws Throwable {
		File csvFile=new File("target/urls_sample_"+SAMPLED_CHOS_PER_DATASET+"urls.csv");
		MapOfInts<String> alreadyProcessedDatasets=new MapOfInts<>();
		if(csvFile.exists()) {
			CSVParser csvParser=new CSVParser(new FileReader(csvFile), CSVFormat.EXCEL);
			csvParser.forEach(record -> {
				String dasetId = record.get(1);
				alreadyProcessedDatasets.incrementTo(dasetId);
			});
			csvParser.close();
		}
		
		BufferedWriter csvWriter=new BufferedWriter(new FileWriterWithEncoding(csvFile, "UTF-8", true));
		CSVPrinter csvPrinter=new CSVPrinter(csvWriter, CSVFormat.EXCEL);
		EuropeanaApiClient europeanaApiClient = new EuropeanaApiClient("QdEDkmksy");
		List<String> listProviderIds = europeanaApiClient.listProviderIds();
//		System.out.println(listProviderIds);
		
		int datasetsCount=0;
		int datasetsCountEmpty=0;
		int providersCountEmpty=0;
		for(String provId: listProviderIds) {
			boolean providerIsEmpty=true;

			//			System.out.println(provId);
//			List<String> listProviderDatasetsIds = europeanaApiClient.listProviderDatasetsIds(provId);
			List<String> listProviderDatasetsNames = europeanaApiClient.listProviderDatasetsNames(provId);
			for(String dsName: listProviderDatasetsNames) {
				//remove hack
				if(dsName.equals("03922_Ag_FR_MCC_ARTS_DECORATIFS_IMAGE")) {
					System.out.println("Skipping "+dsName);
					continue;
				}
				try {
					System.out.println(dsName);
					Integer datasetSampledCount=alreadyProcessedDatasets.get(dsName);
					if(datasetSampledCount==null)
						datasetSampledCount=0;
					int datasetSampledErrorCount=0;
					if(alreadyProcessedDatasets.containsKey(dsName) && alreadyProcessedDatasets.get(dsName)>=SAMPLED_CHOS_PER_DATASET) {
						System.out.println("Skipping: "+provId +" - " + dsName);
						continue;
					}
					
					List<Map<String, String>> listDatasetRecords = europeanaApiClient.listDatasetRecordsFieldValues(dsName, 1+datasetSampledCount+datasetSampledErrorCount ,SAMPLED_CHOS_PER_DATASET , "id");
					DATASET: while(datasetSampledCount<SAMPLED_CHOS_PER_DATASET) {
//					List<Map<String, String>> listDatasetRecords = europeanaApiClient.listDatasetRecordsFieldValues(dsName, 1 ,SAMPLED_CHOS_PER_DATASET , "id", "edmIsShownAt", "edmLandingPage");
						if(listDatasetRecords.isEmpty() && datasetSampledCount==0) {
							datasetsCountEmpty++;
							break;
						} else if(listDatasetRecords.isEmpty())
							break;
						for(Map<String, String> rec: listDatasetRecords) {
							String recId = rec.get("id");
							try {
								Model edmRdf = europeanaApiClient.getRecord(recId);
		
								StmtIterator listStatements = edmRdf.listStatements(null, Rdf.type, Ore.Aggregation);
								Statement st = listStatements.next();
								Resource aggRes=st.getSubject();
								
		//						StmtIterator listStatements = edmRdf.listStatements();
		//						listStatements.forEachRemaining(stm -> {
		//							System.out.println(stm);
		//					    });
								String choUri = europeanaApiClient.recordUriFromApiId(recId);
								providerIsEmpty=false;
								Statement shownAt = aggRes.getProperty(Edm.isShownAt);
								Statement shownBy = aggRes.getProperty(Edm.isShownBy);
								Statement landingPage = aggRes.getProperty(Edm.landingPage);
		
								csvPrinter.print(provId);
								csvPrinter.print(dsName);
								csvPrinter.print(choUri);
								
								if(shownAt!=null) 
									csvPrinter.print(RdfUtil.getUriOrLiteralValue(shownAt.getObject().asResource()));
								else 
									csvPrinter.print("");
								if(shownBy!=null) 
									csvPrinter.print(RdfUtil.getUriOrLiteralValue(shownBy.getObject().asResource()));
								else 
									csvPrinter.print("");
								if(landingPage!=null) 
									csvPrinter.print(RdfUtil.getUriOrLiteralValue(landingPage.getObject().asResource()));
								else 
									csvPrinter.print("");
								csvPrinter.println();
							} catch (Exception e) {
								System.err.println("Error in record: "+recId+ " - "+datasetSampledErrorCount+" errors in ds");
								e.printStackTrace();
								datasetSampledErrorCount++;
								
								if(datasetSampledCount==0 && datasetSampledErrorCount>3*SAMPLED_CHOS_PER_DATASET) {
									System.out.println("Skipping dataset "+dsName+": only errors found");
									break DATASET;
								} else if (datasetSampledErrorCount>4*SAMPLED_CHOS_PER_DATASET) {
									System.out.println("Skipping dataset "+dsName+": many errors found: "+datasetSampledErrorCount+" good ones: "+datasetSampledCount);									
									break DATASET;
								} else if (datasetSampledErrorCount>3*SAMPLED_CHOS_PER_DATASET) {
									System.out.println("Warning dataset "+dsName+": with many errors found: "+datasetSampledErrorCount+" good ones: "+datasetSampledCount);									
								}
							}
						}
					}
					//				List<String> listDatasetRecordsIds = europeanaApiClient.listDatasetRecordsIds(dsName,1,10);
//				for(String choId: listDatasetRecordsIds) {
//					System.out.println(choId);
//				}
					csvWriter.flush();
				} catch (Exception e) {
					System.err.println("Error in dataset: "+dsName);
					e.printStackTrace();
				}
			}
			if(providerIsEmpty)
				providersCountEmpty++;
			datasetsCount+=listProviderDatasetsNames.size();
		}
		System.out.println(listProviderIds.size()+" providers exist, and of these "+providersCountEmpty+" are empty.");
		System.out.println(datasetsCount+" datasets exist, and of these "+datasetsCountEmpty+" are empty.");
		csvPrinter.close();
		csvWriter.close();
	}
}
