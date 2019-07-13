package inescid.europeanaapi.clients.webresourcedata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.FileWriterWithEncoding;

import inescid.europeanaapi.clients.webresourcedata.ScriptCheckForDataInHtml.Counts;
import inescid.europeanaapi.clients.webresourcedata.ScriptCheckForDataInHtml.Vocabulary;
import inescid.opaf.data.profile.MapOfInts;

public class ReportOfSchemaOrgInHtml {
		public int[] counts = new int[ScriptCheckForDataInHtml.Counts.values().length];
		public int[] inPredicates = new int[ScriptCheckForDataInHtml.Counts.values().length];
		public int[] inObjects = new int[ScriptCheckForDataInHtml.Counts.values().length];
		public int[] inRdfType = new int[ScriptCheckForDataInHtml.Counts.values().length];
		public MapOfInts<String> predicateUris=new MapOfInts<>();
		public MapOfInts<String> objectUris=new MapOfInts<>();

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
//			sb.append("\nSchema.org JSON/Microdata: \n - Total counts[");
			sb.append(" - Total counts[");
			for (int j = 0; j < counts.length; j++) {
				if (j > 0)
					sb.append(", ");
				sb.append(counts[j]);
			}
			sb.append("], of which:\n");
			sb.append("   - In predicate[");
			for (int j = 0; j < inPredicates.length; j++) {
				if (j > 0)
					sb.append(", ");
				sb.append(inPredicates[j]);
			}
			sb.append(", ");
			sb.append(predicateUris.total());
			sb.append("]\n");
			sb.append("   - In object[");
			for (int j = 0; j < inObjects.length; j++) {
				if (j > 0)
					sb.append(", ");
				sb.append(inObjects[j]);
			}
			sb.append(", ");
			sb.append(objectUris.total());
			sb.append("], of which:\n");
			sb.append("     - In object of rdf:type[");
			for (int j = 0; j < inRdfType.length; j++) {
				if (j > 0)
					sb.append(", ");
				sb.append(inRdfType[j]);
			}
			sb.append("]\n");
			sb.append(" - In predicate of URIs:\n");
			for(String uri: predicateUris.keySet()) {
				sb.append("   - "+predicateUris.get(uri)+" - " +uri+"\n");				
			}
			sb.append(" - In object of URIs:\n");
			for(String uri: objectUris.keySet()) {
				sb.append("   - "+objectUris.get(uri)+" - " +uri+"\n");				
			}
			
			return sb.toString();
		}

		public void toCsv(File csvFile) throws IOException {
			BufferedWriter csvWriter=new BufferedWriter(new FileWriterWithEncoding(csvFile, "UTF-8", true));
			CSVPrinter csvPrinter=new CSVPrinter(csvWriter, CSVFormat.EXCEL);
			csvPrinter.print("Occurrence at");
			for (Counts countTarget : ScriptCheckForDataInHtml.Counts.values()) 
				csvPrinter.print(countTarget);
			csvPrinter.print("Total statements");
			csvPrinter.println();
			
			csvPrinter.print("Total occurrences");
			for (int j = 0; j < counts.length; j++) {
				csvPrinter.print(counts[j]);
			}
			csvPrinter.println();
			csvPrinter.print("Predicate");
			for (int j = 0; j < inPredicates.length; j++) {
				csvPrinter.print(inPredicates[j]);
			}
			csvPrinter.print(predicateUris.total());
			csvPrinter.println();
			csvPrinter.print("object");
			for (int j = 0; j < inObjects.length; j++) {
				csvPrinter.print(inObjects[j]);
			}
			csvPrinter.print(objectUris.total());
			csvPrinter.println();
			csvPrinter.print("object  of rdf:type");
			for (int j = 0; j < inRdfType.length; j++) {
				csvPrinter.print(inRdfType[j]);
			}
			csvPrinter.println();

			csvPrinter.println();
			csvPrinter.print("Schema.org URIs found in predicate of statements");
			csvPrinter.println();
			csvPrinter.print("URI");
			csvPrinter.print("Occurrences");
			csvPrinter.println();
			for(String uri: predicateUris.keySet()) {
				csvPrinter.print(uri);
				csvPrinter.print(predicateUris.get(uri));
				csvPrinter.println();
			}
			
			csvPrinter.println();
			csvPrinter.print("Schema.org URIs found in object of statements");
			csvPrinter.println();
			csvPrinter.print("URI");
			csvPrinter.print("Occurrences");
			csvPrinter.println();
			for(String uri: objectUris.keySet()) {
				csvPrinter.print(uri);
				csvPrinter.print(objectUris.get(uri));
				csvPrinter.println();
			}
			
			csvPrinter.flush();
			csvPrinter.close();
		}
	}