package inescid.europeanaapi.clients.sitemap;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import inescid.europeanaapi.AccessException;
import inescid.europeanaapi.EuropeanaApiClient;
import inescid.opaf.data.RdfReg;
import inescid.opaf.data.profile.MapOfInts;
import inescid.util.RdfUtil;

public class ScriptExportSitemap {
	static  int SAMPLED_CHOS_PER_DATASET=10;
	static final int CHOS_PER_SITEMAP_FILE=10000;
//	static final int SAMPLED_CHOS_PER_DATASET=-1;
	
	static final DatatypeFactory xmlDateFactory;
	static {
		try {
			xmlDateFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public static void main(String[] args) throws Throwable {
		if(args.length==0) {
			System.out.println("parameters missing: folder to sitemaps, and sample number per dataset");
			return;
		}
		
		File sitemapsFolder=new File(args[0]);
		if(args.length>1)
			SAMPLED_CHOS_PER_DATASET=Integer.parseInt(args[1]);
//		File sitemapsFolder=new File("src/data/europeana-dataset-sitemap");
		if(!sitemapsFolder.exists())
			sitemapsFolder.mkdirs();
		SitemapWriter writer=new SitemapWriter(sitemapsFolder);
		String apiKey = "api2demo";
		EuropeanaApiClient europeanaApiClient = new EuropeanaApiClient(apiKey);
		List<String> listProviderIds = europeanaApiClient.listProviderIds();

		List<AccessException> accessExceptions = new ArrayList<>();
//		System.out.println(listProviderIds);
		int datasetsCount=0;
		List<String> datasetsEmpty=new ArrayList<>();
		List<String> providersEmpty=new ArrayList<>();
		for(String provId: listProviderIds) {
			boolean providerIsEmpty=true;
			//			System.out.println(provId);
//			List<String> listProviderDatasetsIds = europeanaApiClient.listProviderDatasetsIds(provId);
			List<String> listProviderDatasetsNames = europeanaApiClient.listProviderDatasetsNames(provId);
			for(String dsName: listProviderDatasetsNames) {
				try {
					System.out.println(dsName);
					writer.resetDatasetCounter();
					
					int startAt=1;
					List<Map<String, String>> listDatasetRecords = europeanaApiClient.listDatasetRecordsFieldValues(dsName, 1 ,
							(SAMPLED_CHOS_PER_DATASET>0 ? SAMPLED_CHOS_PER_DATASET : 1000), "id");
					while(SAMPLED_CHOS_PER_DATASET<0 || writer.getWrittenCountDataset()<SAMPLED_CHOS_PER_DATASET) {
//					List<Map<String, String>> listDatasetRecords = europeanaApiClient.listDatasetRecordsFieldValues(dsName, 1 ,SAMPLED_CHOS_PER_DATASET , "id", "edmIsShownAt", "edmLandingPage");
						if(listDatasetRecords.isEmpty() && writer.getWrittenCountDataset()==0) {
							datasetsEmpty.add(dsName);
							break;
						} else if(listDatasetRecords.isEmpty())
							break;
						for(Map<String, String> rec: listDatasetRecords) {
							String europeanaApiRecordId = rec.get("id");
							String apiUrl="https://search-api-test.eanadev.org/api/v2/record" + (europeanaApiRecordId.startsWith("/") ? "" : "/") + europeanaApiRecordId
									+".schema.jsonld?wskey="+apiKey;
							writer.writeUri(apiUrl);
						}
						if(SAMPLED_CHOS_PER_DATASET>0)
							listDatasetRecords = europeanaApiClient.listDatasetRecordsFieldValues(dsName, startAt+SAMPLED_CHOS_PER_DATASET, SAMPLED_CHOS_PER_DATASET , "id");
						else
							listDatasetRecords = europeanaApiClient.listDatasetRecordsFieldValues(dsName, startAt+1000, 1000 , "id");
					}
					if(writer.getWrittenCountDataset()>0)
						providerIsEmpty=false;
					//				List<String> listDatasetRecordsIds = europeanaApiClient.listDatasetRecordsIds(dsName,1,10);
//				for(String choId: listDatasetRecordsIds) {
//					System.out.println(choId);
//				}
				} catch (AccessException e) {
					System.err.println("Error in dataset: "+dsName);
					accessExceptions.add(e);
					providerIsEmpty=false;
				}
			}
			if(providerIsEmpty)
				providersEmpty.add(provId);
			datasetsCount+=listProviderDatasetsNames.size();
		}
		
		if(!accessExceptions.isEmpty()) {
			System.out.println("Failures in the following URLs:");
			for(AccessException e: accessExceptions) {
				System.out.println(e.getExceptionSummary());
			}
			System.out.println();
		}
		
		System.out.println(writer.getWrittenCountTotal()+" URLs written.");
		
		System.out.println(listProviderIds.size()+" providers exist, and of these "+providersEmpty.size()+" are empty.");
		if (providersEmpty.size()>0) {
			StringBuilder sb=new StringBuilder();
			for(String provId: providersEmpty)
				sb.append(provId).append("\n");
			String path = "target/EuropeanaAPI-emptyProviders.txt";
			FileUtils.write(new File(path), sb.toString(), "UTF8");
			System.out.println("Empty providers exported to "+path);
		}
		System.out.println(datasetsCount+" datasets exist, and of these "+datasetsEmpty.size()+" are empty.");
		if (datasetsEmpty.size()>0) {
			StringBuilder sb=new StringBuilder();
			for(String dsId: datasetsEmpty)
				sb.append(dsId).append("\n");
			String path = "target/EuropeanaAPI-emptyDatasets.txt";
			FileUtils.write(new File(path), sb.toString(), "UTF8");
			System.out.println("Empty providers exported to "+path);
		}
		
		writer.end();
	}
}
