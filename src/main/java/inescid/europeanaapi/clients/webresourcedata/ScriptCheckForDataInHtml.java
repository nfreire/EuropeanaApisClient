package inescid.europeanaapi.clients.webresourcedata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.any23.Any23;
import org.apache.any23.configuration.Configuration;
import org.apache.any23.extractor.ExtractionContext;
import org.apache.any23.extractor.ExtractionParameters;
import org.apache.any23.extractor.Extractor;
import org.apache.any23.extractor.ExtractorFactory;
import org.apache.any23.extractor.ExtractorGroup;
import org.apache.any23.extractor.html.HTMLMetaExtractor;
import org.apache.any23.extractor.html.HTMLMetaExtractorFactory;
import org.apache.any23.extractor.microdata.MicrodataExtractorFactory;
import org.apache.any23.writer.CountingTripleHandler;
import org.apache.any23.writer.NTriplesWriter;
import org.apache.any23.writer.TripleHandlerException;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import inescid.europeanaapi.clients.webresourcedata.ScriptCheckForDataInHtml.ReportOfDataInHtml;
import inescid.opaf.www.EmbeddedJSONLDExtractorFactory;

public class ScriptCheckForDataInHtml {

	public enum Vocabulary {
		HTML5, DC, DCTERMS, DC_LOWERCASE, DCTERMS_LOWERCASE, OG, EPRINTS, EGMS;
	}

	public enum Counts {
		DATASET, RECORD, TAG;
	}

	private String charset = "UTF8";

	Pattern schemaOrgUriPattern = Pattern.compile("(https?://schema.org/[^\"]+)");

	public class ReportOfDataInHtml {
		public int recordsChecked=0;
		public int providersChecked=0;
		
		public int recordsWithContentNeg=0;
		public int providersWithContentNeg=0;

		
		public ReportOfMetaTagsInHtml reportOfMetaTags = new ReportOfMetaTagsInHtml();
		public ReportOfSchemaOrgInHtml reportOfSchemaOrg = new ReportOfSchemaOrgInHtml();
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("MetaTags:\n").append(reportOfMetaTags.toString());
			sb.append("\nSchema.org JSON/Microdata: \n").append(reportOfSchemaOrg.toString());
			return sb.toString();
		}

		public void analise() throws IOException {
			for (File provFolder : repositoryFolderWithHtml.listFiles()) {
				if (provFolder.isDirectory()) {
					providersChecked++;
					recordsChecked+=provFolder.listFiles().length;
				}
			}
			System.out.println("Analising "+providersChecked+" data providers");
			System.out.println("Analising "+recordsChecked+" records");
			
			reportOfMetaTags = analyseMetaTags();
			reportOfSchemaOrg = analyseSchemaOrg();
		}
		
		private ReportOfMetaTagsInHtml analyseMetaTags() throws IOException {

			HTMLMetaExtractorFactory metaExtractFactory = new HTMLMetaExtractorFactory();
			Any23 any23MetaTags = new Any23(new ExtractorGroup(new ArrayList<ExtractorFactory<?>>() {
				private static final long serialVersionUID = 1L;
				{
					add(metaExtractFactory);
				}
			}));

			ReportOfMetaTagsInHtml metaTagsReport = new ReportOfMetaTagsInHtml();

			for (File provFolder : repositoryFolderWithHtml.listFiles()) {
				if (provFolder.isDirectory()) {
					// totalProviders++;
					// dataReport.reportOfMetaTags=analyseMetaTags(provFolder);

					int[] datasetVocabsPresent = new int[Vocabulary.values().length];
					for (File contentFile : provFolder.listFiles()) {
						String htmlContent = FileUtils.readFileToString(contentFile, charset);
						String uriEuropeana = URLDecoder.decode(contentFile.getName(), charset);
						try {
							// ByteArrayOutputStream decodedInput = new ByteArrayOutputStream();
							// NTriplesWriter triples=new NTriplesWriter(decodedInput);
							int[] recordVocabsPresent = new int[Vocabulary.values().length];
							any23MetaTags.extract(htmlContent, uriEuropeana, "text/html", charset,
									// triples);
									new CountingTripleHandler() {
										@Override
										public void receiveTriple(Resource arg0, IRI arg1, Value arg2, IRI arg3,
												ExtractionContext arg4) throws TripleHandlerException {
											metaTagsReport.allMetaTagsUsed.add(arg1.toString());
											// System.out.println("[sub:"+ arg0.toString());
											// System.out.println("pred: "+ arg1.toString());
											// System.out.println("obj: "+ arg2.toString()+"]");

											String tagName = arg1.getLocalName();
											int endOfPrefix = tagName.indexOf('.');
											int colonIdx = tagName.indexOf(':');
											if (colonIdx > 0 && (endOfPrefix < 0 || colonIdx < endOfPrefix))
												endOfPrefix = colonIdx;

											if (endOfPrefix > 0) {
												String prefix = tagName.substring(0, endOfPrefix);
												if (prefix.equals("DC")) {
													recordVocabsPresent[Vocabulary.DC.ordinal()]++;
												} else if (prefix.equals("DCTERMS")) {
													recordVocabsPresent[Vocabulary.DCTERMS.ordinal()]++;
												} else if (prefix.equals("dc")) {
													recordVocabsPresent[Vocabulary.DC_LOWERCASE.ordinal()]++;
												} else if (prefix.equals("dcterms")) {
													recordVocabsPresent[Vocabulary.DCTERMS_LOWERCASE.ordinal()]++;
												} else if (prefix.equals("og")) {
													recordVocabsPresent[Vocabulary.OG.ordinal()]++;
												} else if (prefix.equals("eprints")) {
													recordVocabsPresent[Vocabulary.EPRINTS.ordinal()]++;
												} else if (prefix.equals("eGMS")) {
													recordVocabsPresent[Vocabulary.EGMS.ordinal()]++;
												}
											} else if (ReportOfMetaTagsInHtml.HTML5_META_TAGS.contains(tagName))
												recordVocabsPresent[Vocabulary.HTML5.ordinal()]++;
										}
									});
							for (Vocabulary v : Vocabulary.values()) {
								int i = v.ordinal();
								if (recordVocabsPresent[i] > 0) {
									metaTagsReport.tagCounts[i][Counts.RECORD.ordinal()] += 1;
									datasetVocabsPresent[i]++;
								}
							}

						} catch (Exception e) {
							System.err.println("Skiping record: " + uriEuropeana + "\nError extracting:");
							e.printStackTrace();
						}
					}
					for (Vocabulary v : Vocabulary.values()) {
						int i = v.ordinal();
						metaTagsReport.tagCounts[i][Counts.DATASET.ordinal()] += (datasetVocabsPresent[i] > 0 ? 1 : 0);
					}
				}
			}
			return metaTagsReport;
		}

		private ReportOfSchemaOrgInHtml analyseSchemaOrg() throws IOException {
			ReportOfSchemaOrgInHtml report = new ReportOfSchemaOrgInHtml();
			Any23 any23Json = new Any23(new ExtractorGroup(new ArrayList<ExtractorFactory<?>>() {
				private static final long serialVersionUID = 1L;
				{
					add(new EmbeddedJSONLDExtractorFactory());
				}
			}));
			Any23 any23Microdata = new Any23(new ExtractorGroup(new ArrayList<ExtractorFactory<?>>() {
				private static final long serialVersionUID = 1L;
				{
					add(new MicrodataExtractorFactory());
				}
			}));

			for (File provFolder : repositoryFolderWithHtml.listFiles()) {
				if (provFolder.isDirectory()) {
					boolean provHasResultsInPred = false;
					boolean provHasResultsInObj = false;
					boolean provHasResultsInObjOfRdfType = false;
					for (File contentFile : provFolder.listFiles()) {
						String htmlContent = FileUtils.readFileToString(contentFile, charset);
						String uriEuropeana = URLDecoder.decode(contentFile.getName(), charset);
						try {
							// ByteArrayOutputStream decodedInput = new ByteArrayOutputStream();
							// NTriplesWriter triples=new NTriplesWriter(decodedInput);
							int[] triplesWithSchemaOrgInPred = new int[1];
							int[] triplesWithSchemaOrgInObj = new int[1];
							int[] triplesWithSchemaOrgInObjOfRdfType = new int[1];
							any23Json.extract(htmlContent, uriEuropeana, "text/html", charset,
									// triples);
									new CountingTripleHandler() {
										@Override
										public void receiveTriple(Resource arg0, IRI arg1, Value arg2, IRI arg3,
												ExtractionContext arg4) throws TripleHandlerException {
											String predUri = arg1.toString();
											String objUri = arg2.toString();
											boolean hasSchemaInPred = schemaOrgUriPattern.matcher(predUri).find();
											boolean hasSchemaInObj = schemaOrgUriPattern.matcher(objUri).find();
											if (hasSchemaInPred || hasSchemaInObj) 
												System.out.println("Json pred: " + predUri+" obj: "+objUri);
											if (hasSchemaInPred) {
												triplesWithSchemaOrgInPred[0]++;
												report.predicateUris.incrementTo(predUri);
											} else if (hasSchemaInObj) {
												triplesWithSchemaOrgInObj[0]++;
												report.objectUris.incrementTo(objUri);
												if(predUri.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) 
													triplesWithSchemaOrgInObjOfRdfType[0]++;
											}
										}
									});
							any23Microdata.extract(htmlContent, uriEuropeana, "text/html", charset,
									// triples);
									new CountingTripleHandler() {
										@Override
										public void receiveTriple(Resource arg0, IRI arg1, Value arg2, IRI arg3,
												ExtractionContext arg4) throws TripleHandlerException {
											String predUri = arg1.toString();
											String objUri = arg2.toString();
											boolean hasSchemaInPred = schemaOrgUriPattern.matcher(predUri).find();
											boolean hasSchemaInObj = schemaOrgUriPattern.matcher(objUri).find();
											if (hasSchemaInPred || hasSchemaInObj) 
												System.out.println("micro tripple: " + predUri+" obj: "+objUri);
											if (hasSchemaInPred) {
												triplesWithSchemaOrgInPred[0]++;
												report.predicateUris.incrementTo(predUri);
											} else if (hasSchemaInObj) {
												triplesWithSchemaOrgInObj[0]++;
												report.objectUris.incrementTo(objUri);
												if(predUri.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) 
													triplesWithSchemaOrgInObjOfRdfType[0]++;
											}
										}
									});
							if (triplesWithSchemaOrgInPred[0] > 0 || triplesWithSchemaOrgInObj[0] > 0) {
								report.counts[Counts.RECORD.ordinal()]++;
								if (triplesWithSchemaOrgInPred[0] > 0) {
									report.inPredicates[Counts.RECORD.ordinal()]++;
									provHasResultsInPred = true;
								}
								if (triplesWithSchemaOrgInObj[0] > 0) {
									report.inObjects[Counts.RECORD.ordinal()]++;
									provHasResultsInObj = true;
								}
								if (triplesWithSchemaOrgInObjOfRdfType[0] > 0) {
									report.inRdfType[Counts.RECORD.ordinal()]++;
									provHasResultsInObjOfRdfType = true;
								}
							}
						} catch (Exception e) {
							System.err.println("Skiping record: " + uriEuropeana + "\nError extracting:");
							e.printStackTrace();
						}
					}
					if (provHasResultsInPred || provHasResultsInObj) {
						report.counts[Counts.DATASET.ordinal()]++;
						if (provHasResultsInPred) 
							report.inPredicates[Counts.DATASET.ordinal()]++;
						if (provHasResultsInObj) { 
							report.inObjects[Counts.DATASET.ordinal()]++;
							if (provHasResultsInObjOfRdfType) 
								report.inRdfType[Counts.DATASET.ordinal()]++;
						}
					}
				}
			}
			return report;
		}
	}

	public static void main(String[] args) throws Throwable {
		new ScriptCheckForDataInHtml().run();
	}

	File repositoryHomeFolder = new File("target/downloaded");
	File repositoryFolderWithHtml = new File(repositoryHomeFolder, "html");
	File repositoryFolderWithData = new File(repositoryHomeFolder, "html");
	Any23 any23Json;
	Any23 any23Microdata;

	public void run() throws Throwable {
//How many datasets did I tested?
//		How many records?
//				make csv's per dataset
		
		//How many successful content neg?
		//How many successful content neg to html?
		
		
		ReportOfDataInHtml dataReport = new ReportOfDataInHtml();

		dataReport.analise();

//		System.out.println("Metatags vocabs:\n" + dataReport.reportOfMetaTags.toString());
//		System.out.println("Schema.org:\n" + dataReport.toString());
		System.out.println( dataReport.toString());
		File csvFileMetaTags=new File(repositoryFolderWithHtml.getParentFile(), "EuropeanaProvidersReportOfMetaTags.csv");
		File csvFileSchemaORg=new File(repositoryFolderWithHtml.getParentFile(), "EuropeanaProvidersReportOfSchemaOrg.csv");

		dataReport.reportOfMetaTags.toCsv(csvFileMetaTags);
		dataReport.reportOfSchemaOrg.toCsv(csvFileSchemaORg);
		
		// ArrayList<String> allMetaTagsUsedList=new ArrayList<>(allMetaTagsUsed);
		// Collections.sort(allMetaTagsUsedList);
		// System.out.println(allMetaTagsUsedList.toString().replaceAll("http...vocab.sindice.net.any23.",
		// "").replaceAll(", ", "\n"));
	}


}
