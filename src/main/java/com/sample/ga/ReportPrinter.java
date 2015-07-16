package com.sample.ga;

import com.google.api.services.analytics.model.GaData;
import com.sample.ga.Config.ReportFormat;
import com.sample.ga.Config.ReportSheet;

public interface ReportPrinter {

	void begin();

	void printSheetName(ReportSheet sheet);

	void printSheet(GaData query, ReportSheet sheet, ReportFormat reportFormat);

	void finish();

	void close();

}
