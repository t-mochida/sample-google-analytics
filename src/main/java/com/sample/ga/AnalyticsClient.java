package com.sample.ga;

import java.io.IOException;

import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.Analytics.Data.Ga.Get;
import com.google.api.services.analytics.model.GaData;
import com.sample.ga.Config.ReportSheet;

/**
 * Google Analytics 問い合わせクラス
 * @author mochida
 *
 */
public class AnalyticsClient {

	private static final String APPLICATION_NAME = "analytics-tool";

	private Config config;
	private Analytics analytics;
	private ReportPrinter printer;

	public AnalyticsClient(Config config, String dirPath, ReportPrinter printer) throws Exception, IOException {
		this.config = config;
		this.printer = printer;

        analytics = new Analytics.Builder(Authorizer.httpTransport, Authorizer.JSON_FACTORY, Authorizer.credential).setApplicationName(APPLICATION_NAME).build();
	}

	public void executeDataQuery(String startDate, String endDate) throws Exception {
		printer.begin();
		for (ReportSheet sheet : config.getReportSheets()) {
			printer.printSheetName(sheet);
			printer.printSheet( this.query(sheet, startDate, endDate), sheet, config.getReportFormat() );
		}
		printer.finish();
	}

	private GaData query(ReportSheet sheet, String startDate, String endDate) throws IOException {
		String dimensions = sheet.getDimentions();
		String sort = sheet.getSort();
		String filter = sheet.getFilter();

        Get get = analytics.data().ga().get("ga:" + config.getAnalytics().getViewId(), startDate, endDate, sheet.getMetrics());
        if ( !isEmpty(dimensions) ) {
        	get.setDimensions( dimensions );
        }
        if ( !isEmpty(sort) ) {
        	get.setSort( sort );
        }
        if ( !isEmpty(filter) ) {
        	get.setFilters( filter );
        }
        if ( sheet.getMaxResults() != 0 ) {
        	get.setMaxResults( sheet.getMaxResults() );
        }
        return get.execute();
	}

	private boolean isEmpty(String str) {
		if (str == null || "".equals(str)) {
			return true;
		}
		return false;
	}
}
