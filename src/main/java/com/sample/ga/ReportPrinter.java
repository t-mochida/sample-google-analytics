package com.sample.ga;

import com.google.api.services.analytics.model.GaData;
import com.sample.ga.Config.ReportFormat;
import com.sample.ga.Config.ReportSheet;

public interface ReportPrinter {

	void begin() throws Exception;

	void printSheetName(ReportSheet sheet) throws Exception;

	void printSheet(GaData query, ReportSheet sheet, ReportFormat reportFormat) throws Exception;

	void finish() throws Exception;

	void close();

}
