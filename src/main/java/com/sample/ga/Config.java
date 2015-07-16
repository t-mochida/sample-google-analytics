package com.sample.ga;

import java.util.HashMap;
import java.util.Map;

/**
 * 設定クラス
 * config.json ファイルとマッピングします
 * @author mochida
 *
 */
public class Config {

	public static class Analytics {
		private String viewId, clientSecrets, credential;

		public String getCredential() {
			return credential;
		}

		public void setCredential(String credential) {
			this.credential = credential;
		}

		public String getViewId() {
			return viewId;
		}

		public void setViewId(String viewId) {
			this.viewId = viewId;
		}

		public String getClientSecrets() {
			return clientSecrets;
		}

		public void setClientSecrets(String clientSecrets) {
			this.clientSecrets = clientSecrets;
		}
	}

	public static class ReportSheet {
		private String name, dimentions, metrics, sort, filter;
		private int maxResults;

		public String getFilter() {
			return filter;
		}
		public void setFilter(String filter) {
			this.filter = filter;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDimentions() {
			return dimentions;
		}
		public void setDimentions(String dimentions) {
			this.dimentions = dimentions;
		}
		public String getMetrics() {
			return metrics;
		}
		public void setMetrics(String metrics) {
			this.metrics = metrics;
		}
		public String getSort() {
			return sort;
		}
		public void setSort(String sort) {
			this.sort = sort;
		}
		public int getMaxResults() {
			return maxResults;
		}
		public void setMaxResults(int maxResults) {
			this.maxResults = maxResults;
		}

		public int getColCount() {
			String[] m = this.metrics.split(",");
			String[] d = this.dimentions.split(",");
			return m.length + d.length;
		}
		public int getRowCount() {
			return this.maxResults + 2;//ヘッダー行とサマリー行を追加
		}
	}

	public static class ReportFormat {
		private String[] headers;
		private String[] dataTypes;
		private String[] dataFormats;

		public String[] getDataTypes() {
			return dataTypes;
		}
		public void setDataTypes(String[] dataTypes) {
			this.dataTypes = dataTypes;
		}
		public String[] getHeaders() {
			return headers;
		}
		public void setHeaders(String[] headers) {
			this.headers = headers;
		}
		public String[] getDataFormats() {
			return dataFormats;
		}
		public void setDataFormats(String[] dataFormats) {
			this.dataFormats = dataFormats;
		}

		public Map<String, String> getHeaderMap() {
			Map<String, String> map = new HashMap<String, String>();
			for (String header : this.headers) {
				String[] kv = header.split("=");
				map.put(kv[0], kv[1]);
			}
			return map;
		}

		public Map<String, String> getDataFormatMap() {
			Map<String, String> map = new HashMap<String, String>();
			for (String format : this.dataFormats) {
				String[] kv = format.split("=");
				map.put(kv[0], kv[1]);
			}
			return map;
		}

		public Map<String, String> getDataTypeMap() {
			Map<String, String> map = new HashMap<String, String>();
			for (String format : this.dataTypes) {
				String[] kv = format.split("=");
				map.put(kv[0], kv[1]);
			}
			return map;
		}
	}


	private Analytics analytics;
	private String reportBookName;
	private ReportSheet[] reportSheets;
	private ReportFormat reportFormat;

	public ReportFormat getReportFormat() {
		return reportFormat;
	}

	public void setReportFormat(ReportFormat reportFormat) {
		this.reportFormat = reportFormat;
	}

	public ReportSheet[] getReportSheets() {
		return reportSheets;
	}

	public void setReportSheets(ReportSheet[] reportSheets) {
		this.reportSheets = reportSheets;
	}

	public String getReportBookName() {
		return reportBookName;
	}

	public void setReportBookName(String reportBookName) {
		this.reportBookName = reportBookName;
	}

	public Analytics getAnalytics() {
		return analytics;
	}

	public void setAnalytics(Analytics analytics) {
		this.analytics = analytics;
	}

}
