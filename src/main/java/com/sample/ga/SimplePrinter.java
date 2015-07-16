package com.sample.ga;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.GaData.ColumnHeaders;
import com.sample.ga.Config.ReportFormat;
import com.sample.ga.Config.ReportSheet;

public class SimplePrinter implements ReportPrinter {
	protected String title;

	public SimplePrinter(Config config, String startDate, String endDate, String year, String month) {
		title = config.getReportBookName().replaceFirst("\\{\\$start\\}", startDate);
		title = title.replaceFirst("\\{\\$end\\}", endDate);
		title = title.replaceFirst("\\{\\$year\\}", year);
		title = title.replaceFirst("\\{\\$month\\}", month);
	}

	@Override
	public void begin() {
		System.out.println("------------------------------------------------------------------");
		System.out.println(this.title);
		System.out.println("------------------------------------------------------------------");
	}

	@Override
	public void printSheetName(ReportSheet sheet) {
	    System.out.println();
		System.out.println("▼ " + sheet.getName());
	}

	@Override
	public void printSheet(GaData query, ReportSheet sheet, ReportFormat reportFormat) {
		System.out.println("printing results for profile: " + query.getProfileInfo().getProfileName());

		if (query.getRows() == null || query.getRows().isEmpty()) {
			System.out.println("No results Found.");
		} else {

			// Print column headers.
			for (ColumnHeaders header : query.getColumnHeaders()) {
				System.out.printf("%30s", header.getName());
			}
			System.out.println();

			// Print actual data.
			for (List<String> row : query.getRows()) {
				for (String column : row) {
					System.out.printf("%30s", column);
				}
				System.out.println();
			}
			// Total
			Map<String,String> totalMap = query.getTotalsForAllResults();
			Set<String> totalKeys = totalMap.keySet();
			for (String key : totalKeys) {
				System.out.printf("%20s - %20s", key, totalMap.get(key));
				System.out.println();
			}

			System.out.println();
			System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------");
			System.out.println();
		}
	}

	@Override
	public void finish() {
	}

	@Override
	public void close() {
	}
}
