package com.sample.ga;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.ga.Config.ReportSheet;

/**
 * ツール起動クラス
 * @author mochida
 *
 */
public class ToolMain {

	public static void main(String[] args) throws IOException, Exception {
    	if (args == null || args.length < 3) {
    		System.out.println("[ 引数を指定してください ]");
    		System.out.println("  usage: >java -jar analytics-tool.jar 設定ファイル(相対パス) 開始年 開始月");
    		System.out.println("  e.g. : >java -jar analytics-tool.jar config/app-config.json 2015 6");
    		System.exit(0);
    		return;
    	}

    	ResourceBundle resouce = ResourceBundle.getBundle("system");
    	String currentDir = System.getProperty("user.dir") + "/";
    	if (resouce.getString("system.env").equals("product")) {
    		// jar ファイルから起動した場合のカレントディレクトリ取得処理
    		String jarPath = System.getProperty("java.class.path");
    		currentDir = jarPath.substring(0, jarPath.lastIndexOf(File.separator) + 1);
    	}

    	String configPath = args[0];
    	if ( !configPath.startsWith("/") ) {// 絶対パス・相対パス対応
    		configPath = currentDir + args[0];
    	}

		String year = args[1];
		String month = args[2];

		String[] period = null;
		try {
			period = getPeriod(year, month);
		} catch (Exception e) {
			System.out.println("[ 引数が不正です ]");
			System.exit(-1);
		}

		String startDate = period[0];
		String endDate = period[1];
		System.out.println("configPath: " + configPath + "; startDate: " + startDate + "; endDate: " + endDate);

		Config config = getConfig(configPath);

		try {
			Authorizer.init(config, currentDir);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("認証に失敗しました");
			System.exit(-1);
		}

		ReportPrinter printer = new SimplePrinter(config, startDate, endDate, year, month);
		AnalyticsClient client = new AnalyticsClient(config, currentDir, printer);
		try {
			client.executeDataQuery(startDate, endDate);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			printer.close();
		}

		System.out.println("OK.");
	}

	private static String[] getPeriod(String year, String month) {
		Calendar cal = new GregorianCalendar();
		int intYear = Integer.parseInt(year);
		int intMonth = Integer.parseInt(month) - 1;
		cal.set(intYear, intMonth, 1);

		Date startDate = new Date(cal.getTimeInMillis());
		cal.add(Calendar.MONTH, 1);
		cal.add(Calendar.DATE, -1);
		Date endDate = new Date(cal.getTimeInMillis());

		String[] period = new String[2];
		period[0] = new SimpleDateFormat("yyyy-MM-dd").format(startDate);
		period[1] = new SimpleDateFormat("yyyy-MM-dd").format(endDate);

		return period;
	}


	private static Config getConfig(String path) {
		Config config = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			config = mapper.readValue(new File(path), Config.class);

		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("[ 設定ファイルを読み込みました ]");
		System.out.println("analytics - viewId: " + config.getAnalytics().getViewId());
		System.out.println("analytics - credential: " + Authorizer.DATA_STORE_BASE_DIR + config.getAnalytics().getCredential());
		System.out.println("analytics - clientSecrets: " + config.getAnalytics().getClientSecrets());

		System.out.println("reportBookName: " + config.getReportBookName());

		ReportSheet[] sheets = config.getReportSheets();
		for (int i = 0; i < sheets.length; i++) {
			System.out.println("report[" + i + "]");
			System.out.println("reportSheet - name: " + sheets[i].getName());
			System.out.println("reportSheet - dimentions: " + sheets[i].getDimentions());
			System.out.println("reportSheet - metrics: " + sheets[i].getMetrics());
			System.out.println("reportSheet - sort: " + sheets[i].getSort());
			System.out.println("reportSheet - filter: " + sheets[i].getFilter());
			System.out.println("reportSheet - maxResults: " + sheets[i].getMaxResults());
			System.out.println();
		}

		String[] headers = config.getReportFormat().getHeaders();
		for (String header : headers) {
			System.out.println("reportFormat - header: " + header);
		}

		String[] dataTypes = config.getReportFormat().getDataTypes();
		for (String type : dataTypes) {
			System.out.println("reportFormat - dataType: " + type);
		}

		String[] dataFormats = config.getReportFormat().getDataFormats();
		for (String format : dataFormats) {
			System.out.println("reportFormat - dataFormat: " + format);
		}

		return config;
	}

}