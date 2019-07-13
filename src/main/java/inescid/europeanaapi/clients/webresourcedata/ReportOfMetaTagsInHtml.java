package inescid.europeanaapi.clients.webresourcedata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.FileWriterWithEncoding;

import inescid.europeanaapi.clients.webresourcedata.ScriptCheckForDataInHtml.Counts;
import inescid.europeanaapi.clients.webresourcedata.ScriptCheckForDataInHtml.ReportOfDataInHtml;
import inescid.europeanaapi.clients.webresourcedata.ScriptCheckForDataInHtml.Vocabulary;

public class ReportOfMetaTagsInHtml {

	public static final Set<String> HTML5_META_TAGS = new HashSet<String>() {
		private static final long serialVersionUID = 1L;
		{
			add("author");
			add("description");
			add("keywords");
		}
	};
	
	public Set<String> allMetaTagsUsed = new HashSet<>();
	// counts are stored in int[] with the indexes above
	public int[][] tagCounts = new int[Vocabulary.values().length][ScriptCheckForDataInHtml.Counts.values().length];
	// public int[] html5Tags=new int[] {0,0,0};
	// public int[] dctermsTags=new int[] {0,0,0};
	// public int[] dcTags=new int[] {0,0,0};
	// public int[] dctermsTagsLowercase=new int[] {0,0,0};
	// public int[] dcTagsLowercase=new int[] {0,0,0};
	// public int[] ogTags=new int[] {0,0,0};
	// public int[] eprintsTags=new int[] {0,0,0};
	//
	// public int[] egmsTags=new int[] {0,0,0};

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Vocabulary v : Vocabulary.values()) {
			// for(int i=0; i<tagCounts.length ; i++) {
			sb.append(v + "-[");
			int i = v.ordinal();
			for (int j = 0; j < tagCounts[i].length; j++) {
				if (j > 0)
					sb.append(", ");
				sb.append(tagCounts[i][j]);
			}
			sb.append("]\n");
		}
		return sb.toString();
	}

	public void toCsv(File csvFile) throws IOException {
		BufferedWriter csvWriter=new BufferedWriter(new FileWriterWithEncoding(csvFile, "UTF-8", true));
		CSVPrinter csvPrinter=new CSVPrinter(csvWriter, CSVFormat.EXCEL);
		csvPrinter.print("Vocabulary");
		for (Counts countTarget : ScriptCheckForDataInHtml.Counts.values()) 
			csvPrinter.print(countTarget);
		csvPrinter.println();
		
		for (Vocabulary v : Vocabulary.values()) {
			//check if any were found
			boolean hasData=false;
			for (int j = 0; j < tagCounts[v.ordinal()].length; j++) {
				if(tagCounts[v.ordinal()][j] >0) {
					hasData=true;
					break;
				}
			}
			if(!hasData)
				continue;

			csvPrinter.print(v);
			for (int j = 0; j < tagCounts[v.ordinal()].length; j++) {
				csvPrinter.print(tagCounts[v.ordinal()][j]);
			}
			csvPrinter.println();
		}
		csvPrinter.flush();
		csvPrinter.close();
	}



}