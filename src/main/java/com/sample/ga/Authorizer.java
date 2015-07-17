package com.sample.ga;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.drive.DriveScopes;

/**
 * 認証クラス
 * @author mochida
 *
 */
public class Authorizer {

	private static final List<String> SCOPES = Arrays.asList(
			AnalyticsScopes.ANALYTICS_READONLY,
			"http://spreadsheets.google.com/feeds",
			"https://spreadsheets.google.com/feeds",
			"http://docs.google.com/feeds",
			DriveScopes.DRIVE_FILE);

	public static final String DATA_STORE_BASE_DIR = ".store/analytics_tool/";

	public static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	public static HttpTransport HTTP_TRANSPORT;

	public static FileDataStoreFactory dataStoreFactory;

	public static Credential credential;

	/**
	 * 初期化
	 * @param config
	 * @param dirPath
	 * @throws Exception
	 */
	public static void init(Config config, String dirPath) throws Exception {

		HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        dataStoreFactory = new FileDataStoreFactory( getDataStoreDir(config.getAnalytics().getCredential()) );

        String filePath = config.getAnalytics().getClientSecrets();
        if ( !filePath.startsWith("/")) {
        	filePath = dirPath + filePath;
        }
        System.out.println("analytics - client_secrets_path: " + filePath);

		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader( new FileInputStream(new File(filePath)) ));

		if (clientSecrets.getDetails().getClientId().startsWith("Enter") || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {

			System.out.println("Enter Client ID and Secret from https://code.google.com/apis/console/?api=analytics ");
			System.exit(1);
		}

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(dataStoreFactory).setAccessType("offline").build();

		credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	}

	/**
	 *
	 * @param credentialDir
	 * クレデンシャルファイルを保存するディレクトリを取得します
	 * @return
	 */
	private static File getDataStoreDir(String credentialDir) {
		return new File(System.getProperty("user.home"), DATA_STORE_BASE_DIR + credentialDir);
	}

}
