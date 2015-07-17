package com.sample.ga;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.GaData.ColumnHeaders;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.docs.DocsService;
import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.batch.BatchOperationType;
import com.google.gdata.data.batch.BatchStatus;
import com.google.gdata.data.batch.BatchUtils;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import com.sample.ga.Config.ReportFormat;
import com.sample.ga.Config.ReportSheet;

/**
 * スプレッドシート出力クラス
 * @author mochida
 *
 */
public class SpreadSheetPrinter extends SimplePrinter {

	private static final String APPLICATION_NAME = "analytics-tool";

	private static final String DATA_TYPE_DATE = "Date";
	private static final String DATA_TYPE_DOUBLE = "Double";

	private SpreadsheetService service = null;
	private Drive driveService = null;
	private SpreadsheetEntry spreadsheetEntry;
	private WorksheetEntry currentEntry;

	private int maxRows = 5;
	private int maxCols = 5;

	private List<String> headerList;

	private Logger logger;

	/**
	 * コンストラクタ
	 * @param config
	 * @param startDate
	 * @param endDate
	 * @param year
	 * @param month
	 * @throws AuthenticationException
	 * @throws OAuthException
	 */
	public SpreadSheetPrinter(Config config, String startDate, String endDate, String year, String month, String currentDir) throws AuthenticationException, OAuthException {
		super(config, startDate, endDate, year, month);
		this.createOauthedService();
		this.logger = new Logger(currentDir, year, month);
	}

	private void createOauthedService() throws OAuthException, AuthenticationException {
		driveService = new Drive.Builder(Authorizer.HTTP_TRANSPORT, Authorizer.JSON_FACTORY, Authorizer.credential)
		.setApplicationName(APPLICATION_NAME).build();

		service = new SpreadsheetService(APPLICATION_NAME);
		service.setProtocolVersion(SpreadsheetService.Versions.V3);
		service.setOAuth2Credentials(Authorizer.credential);
	}


	@Override
	public void begin() throws Exception {
		String key = null;
		if ( this.logger.createLog() ) {//リトライ対応
			key = this.logger.get("ssc");
		}
		if ( key == null ) {
			key = createSpreadSheet();
			this.logger.save("ssc", key);
		}
		System.out.println("spreadsheet - key: " + key);
		spreadsheetEntry = getSpreadSheet(key);
	}

    public static Drive getDriveService() throws IOException {
        return new Drive.Builder(
        		Authorizer.HTTP_TRANSPORT, Authorizer.JSON_FACTORY, Authorizer.credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

	/**
	 * スプレッドシートを作成します
	 * @return
	 * @throws Exception
	 * @throws IOException
	 * @throws ServiceException
	 */
	private String createSpreadSheet() throws Exception, IOException, ServiceException {
		System.out.println();
		System.out.println("[ スプレッドシート: " + super.title + " を作成します ]");

		File body = new File();
	    body.setTitle(super.title);
	    body.setMimeType("text/csv");

	    ImportFormats formats = new ImportFormats();
	    String[] targets = {"Google Sheets"};
	    formats.source = "CSV";
	    formats.targets = targets;
	    body.set("importFormats", formats);

	    InputStream is = new ByteArrayInputStream("dummy,dummy".getBytes("utf-8"));//スプレッドシートを生成するためにダミーのCSVをアップロードして変換
	    InputStreamContent isContent = new InputStreamContent("text/csv", is);
		File file = driveService.files().insert(body, isContent ).setConvert(true).execute();

	    return file.getId();
	}

	/**
	 * キーを元に、SpreadsheetEntry クラスを取得します。
	 * @param key
	 * @return
	 * @throws Exception
	 */
	private SpreadsheetEntry getSpreadSheet(String key) throws Exception {
		// keyを利用してスプレッドシートを取得
		FeedURLFactory factory = FeedURLFactory.getDefault();
		SpreadsheetFeed	feed = service.getFeed(factory.getSpreadsheetsFeedUrl(), SpreadsheetFeed.class);
		List<SpreadsheetEntry> entrys = feed.getEntries();
		for (SpreadsheetEntry entry : entrys) {
			if (key.equals(entry.getKey())) {
				return entry;
			}
		}
		throw new Exception("該当するスプレッドシートがありません");
	}

	/**
	 * ワークシートを追加します。
	 */
	@Override
	public void printSheetName(ReportSheet sheet) throws Exception {
		if ( this.logger.get("wsc:" + sheet.getName()) == null) {
			currentEntry = this.createWorkSheet(sheet);
	        this.logger.save("wsc:" + sheet.getName(), "ok");
		} else {//リトライ対応
			System.out.println("[ ワークシート: "+ sheet.getName() + " は追加済みです ]");
			currentEntry = this.getWorkSheet(sheet.getName());
		}
	}

	/**
	 * ワークシートを作成します
	 * @param sheet
	 * @return
	 * @throws Exception
	 * @throws ServiceException
	 */
	private WorksheetEntry createWorkSheet(ReportSheet sheet) throws Exception, ServiceException {
        System.out.println("[ ワークシート: "+ sheet.getName() + " を追加します ]");

        WorksheetEntry worksheetEntry = new WorksheetEntry(sheet.getRowCount(), sheet.getColCount());
        worksheetEntry.setTitle(new PlainTextConstruct(sheet.getName()));
        URL worksheetFeedUrl = spreadsheetEntry.getWorksheetFeedUrl();
        return service.insert(worksheetFeedUrl, worksheetEntry);
	}

	/**
	 * ワークシート名をキーに、WorkSheetEntry クラスを取得します。
	 * @param name
	 * @return
	 * @throws Exception
	 */
	private WorksheetEntry getWorkSheet(String name) throws Exception {
		 URL worksheetFeedURL = spreadsheetEntry.getWorksheetFeedUrl();
         WorksheetFeed worksheetFeed = spreadsheetEntry.getService().getFeed(worksheetFeedURL, WorksheetFeed.class);
         List<WorksheetEntry> worksheetEntries = worksheetFeed.getEntries();
		for (WorksheetEntry entry : worksheetEntries) {
			if (entry.getTitle().getPlainText().equals(name) ) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public void printSheet(GaData results, ReportSheet sheet, ReportFormat format) throws Exception {
		if (this.logger.get("wsu:" + sheet.getName()) != null) {
			System.out.println("[ ワークシート: " + sheet.getName() + " はデータ反映済みです" );
			return;
		}

		super.printSheet(results, sheet, format);

		if (results.getRows() == null || results.getRows().isEmpty()) {
			return;
		}
		System.out.println("[ バッチ処理でワークシート: " + sheet.getName() + " にデータを反映します ]");
		maxCols = results.getColumnHeaders().size();
		maxRows = results.getRows().size() + 1;

		URL cellFeedUrl = currentEntry.getCellFeedUrl();
	    CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

	    //セルアドレスの生成
	    List<CellAddress> cellAddrs = new ArrayList<CellAddress>();
	    this.prepareHeader(cellAddrs, results, format);//ヘッダー
	    this.prepareData(cellAddrs, results, format);//データ
	    this.prepareDataSummary(cellAddrs, results, sheet, format);//サマリー

	    Map<String, CellEntry> cellEntries = getCellEntryMap(service, cellFeedUrl, cellAddrs);

	    CellFeed batchRequest = new CellFeed();
	    for (CellAddress cellAddr : cellAddrs) {
	    	CellEntry batchEntry = new CellEntry(cellEntries.get(cellAddr.idString));
	    	batchEntry.changeInputValueLocal( cellAddr.getValue() );
	    	BatchUtils.setBatchId(batchEntry, cellAddr.idString);
	    	BatchUtils.setBatchOperationType(batchEntry, BatchOperationType.UPDATE);
	    	batchRequest.getEntries().add(batchEntry);
	    }

	    //更新処理
	    Link batchLink = cellFeed.getLink(Link.Rel.FEED_BATCH, Link.Type.ATOM);
	    CellFeed batchResponse = service.batch(new URL(batchLink.getHref()), batchRequest);

	    //結果判定
	    checkResponse(batchResponse, sheet.getName());
	}

	private void prepareHeader(List<CellAddress> cellAddrs, GaData results, ReportFormat format) {
	    List<ColumnHeaders> headers = results.getColumnHeaders();
	    Map<String, String> map = format.getHeaderMap();
	    headerList = new ArrayList<String>();

	    for (int col = 1; col <= maxCols; ++col) {
	    	String name = ( (ColumnHeaders) headers.get(col-1) ).getName();
	    	headerList.add(name);//データフォーマットとのマッピングに使用

	    	String value = map.get(name);
	    	if (value == null) {
	    		value = name;
	    	}
	    	cellAddrs.add(new CellAddress(1, col, value));
	    }
	}

	private void prepareData(List<CellAddress> cellAddrs, GaData results, ReportFormat format) throws Exception {
		List<List<String>> dataRows = results.getRows();
		Map<String, String> typeMap = format.getDataTypeMap();
		Map<String, String> formatMap = format.getDataFormatMap();

		for (int row = 2; row <= maxRows; ++row) {
			List<String> dataRow = dataRows.get(row-2);
			for (int col = 1; col <= maxCols; ++col) {
				int idx = col - 1;
				String value = dataRow.get(idx);
				String header = headerList.get(idx);

				cellAddrs.add(new CellAddress(row, col, this.format(value, header, typeMap, formatMap)));
			}
		}
	}

	private void prepareDataSummary(List<CellAddress> cellAddrs, GaData results, ReportSheet sheet, ReportFormat format) throws Exception {
		List<ColumnHeaders> headers = results.getColumnHeaders();
		Map<String,String> totalMap = results.getTotalsForAllResults();
		Map<String, String> typeMap = format.getDataTypeMap();
		Map<String, String> formatMap = format.getDataFormatMap();

		for (int col = 2; col <= maxCols; ++col) {
			String header = ( (ColumnHeaders) headers.get(col-1) ).getName();
			String value = totalMap.get(header);

			cellAddrs.add(new CellAddress(maxRows+1, col, this.format(value, header, typeMap, formatMap)));
		}
	}

	/**
	 * データの表示書式を変換します
	 * @param value
	 * @param header
	 * @param typeMap
	 * @param formatMap
	 * @return
	 * @throws Exception
	 */
	private String format(String value, String header, Map<String, String> typeMap, Map<String, String> formatMap) throws Exception {
		String dataType = typeMap.get(header);
		if (DATA_TYPE_DATE.equals(dataType)) {
			//System.out.println("Date型");
			value = this.toDateText(value, formatMap.get(dataType));
		}
		else if ( DATA_TYPE_DOUBLE.equals(dataType) ) {
			//System.out.println("小数点型");
			value = this.toDoubleText(value, formatMap.get(dataType));
		}
		else if ( dataType != null ) {
			value = String.format(formatMap.get(dataType), value);
		}
		return value;
	}

	private String toDateText(String value, String format) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date date = sdf.parse(value);
		sdf.applyPattern(format);
		return sdf.format(date);
	}

	private String toDoubleText(String value, String format) throws Exception {
		double dbl = Double.parseDouble(value);
		return String.format(format, dbl);
	}

	/**
	 * バッチ処理の結果を判定します。
	 * @param batchResponse
	 * @param name
	 */
	private void checkResponse(CellFeed batchResponse, String name) {
	    boolean isSuccess = true;
	    for (CellEntry entry : batchResponse.getEntries()) {
	      String batchId = BatchUtils.getBatchId(entry);
	      if (!BatchUtils.isSuccess(entry)) {
	        isSuccess = false;
	        BatchStatus status = BatchUtils.getBatchStatus(entry);
	        System.out.printf("%s failed (%s) %s", batchId, status.getReason(), status.getContent());
	      }
	    }

	    System.out.println(isSuccess ? "[ バッチ処理が完了しました] " : "[ バッチ処理が失敗しました ]");
	    System.out.println();
	    this.logger.save("wsu:" + name, "ok");
	}


	/**
	 * セルアドレスクラス
	 * @author mochida
	 *
	 */
	private static class CellAddress {
		public final int row;
		public final int col;
		public final String idString;
		private String value;

		public String getValue() {
			return value;
		}

		/**
		 * Constructs a CellAddress representing the specified {@code row} and
		 * {@code col}.  The idString will be set in 'RnCn' notation.
		 */
		public CellAddress(int row, int col, String value) {
			this.row = row;
			this.col = col;
			this.idString = String.format("R%sC%s", row, col);
			this.value = value;
		}
	}

	public static Map<String, CellEntry> getCellEntryMap(SpreadsheetService service, URL cellFeedUrl, List<CellAddress> cellAddrs) throws Exception {

		CellFeed batchRequest = new CellFeed();

		for (CellAddress cellId : cellAddrs) {
			CellEntry batchEntry = new CellEntry(cellId.row, cellId.col, cellId.idString);
			batchEntry.setId(String.format("%s/%s", cellFeedUrl.toString(), cellId.idString));
			BatchUtils.setBatchId(batchEntry, cellId.idString);
			BatchUtils.setBatchOperationType(batchEntry, BatchOperationType.QUERY);
			batchRequest.getEntries().add(batchEntry);
		}

		CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);
		CellFeed queryBatchResponse = service.batch(new URL(cellFeed.getLink(Link.Rel.FEED_BATCH, Link.Type.ATOM).getHref()), batchRequest);

		Map<String, CellEntry> cellEntryMap = new HashMap<String, CellEntry>(cellAddrs.size());
		for (CellEntry entry : queryBatchResponse.getEntries()) {
			cellEntryMap.put(BatchUtils.getBatchId(entry), entry);
/*
			System.out.printf("batch %s {CellEntry: id=%s editLink=%s inputValue=%s\n",
					BatchUtils.getBatchId(entry), entry.getId(), entry.getEditLink().getHref(),
					entry.getCell().getInputValue());
*/
		}

		return cellEntryMap;
	}

	@Override
	public void finish() throws Exception {
		//デフォルトのシートの削除
		WorksheetEntry worksheetEntry = spreadsheetEntry.getDefaultWorksheet();
		worksheetEntry.delete();
		System.out.println("[ デフォルトのワークシートを削除しました ]");
		this.logger.deleteLog();
	}

	@Override
	public void close() {
		this.logger.close();
	}

	/*
	 * CSVファイルをアップロード時にスプレッドシートに変換するためのフォーマット
	 */
	static public class ImportFormats {
		public String source;
		public String[] targets;
	}
}
