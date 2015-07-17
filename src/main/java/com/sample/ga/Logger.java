package com.sample.ga;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * リトライ用ロガー
 * @author mochida
 *
 */
public class Logger {

	private static final String LOG_NAME = ".analytics_%s%2s.log";
	private static final String SP = System.getProperty("line.separator");
	
	private File logFile;
	private PrintWriter printWriter;
	private Map<String, String> historyMap;
	
	public Logger(String currentDir, String year, String month) {
		super();
		String filename = String.format(LOG_NAME, year, month).replace(" ", "0");
		this.logFile = new File(currentDir + filename);
	}
	
	public boolean createLog() throws Exception {
		boolean isExist = this.logFile.exists();
		if (isExist) {
			this.load();
		} else {
			this.logFile.createNewFile();						
		}
		this.printWriter = new PrintWriter(new BufferedWriter(new FileWriter(this.logFile, true)));
		return isExist;
	}

	private void load() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(this.logFile));
		historyMap = new HashMap<String, String>();
		try {
			String line = br.readLine();
			while(line != null){
				System.out.println("log: " + line);
				String[] kv = line.split("=");
				this.historyMap.put(kv[0], kv[1]);
				line = br.readLine();
			}

		} catch (Exception e) {
			throw e;
		} finally {
			br.close();			
		}

	}
	
	public String get(String key){
		if (this.historyMap == null) {
			return null;
		}
		return this.historyMap.get(key);
	}
	
	public void save(String key, String value) {
		this.printWriter.write(key + "=" + value + SP);
	}
	
	public void close() {
		if (this.printWriter != null) {
			this.printWriter.close();			
		}
	}

	public void deleteLog() {
		this.close();
		this.logFile.delete();
	}
	
}
