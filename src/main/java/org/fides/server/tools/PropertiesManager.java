package org.fides.server.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * @author Niels and Jesse
 * 
 */
public class PropertiesManager {
	private static final String FILEPATH = "./config.properties";

	private static PropertiesManager instance = null;

	private int port;

	private String userDir;

	private String dataDir;

	/**
	 * Constructor of the properties manager
	 */
	protected PropertiesManager() {
		Properties properties = new Properties();
		try {
			InputStream in = new FileInputStream(FILEPATH);
			properties.load(in);
			in.close();
			port = Integer.parseInt(properties.getProperty("port"));
			userDir = properties.getProperty("userDir");
			dataDir = properties.getProperty("dataDir");

		} catch (FileNotFoundException e) {
			System.err.println("Properties file is not found: " + e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			System.err.println("IOException has occured: " + e.getMessage());
			System.exit(1);
		}

		// Create the userDirectory and the dataDirectory if they don't exist.
		File userFolder = new File(userDir);
		if (!userFolder.exists()) {
			userFolder.mkdirs();
		}
		File dataFolder = new File(dataDir);
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}
	}

	/**
	 * Singleton properties manager
	 * 
	 * @return instance of the properties manager
	 */
	public static PropertiesManager getInstance() {
		if (instance == null) {
			instance = new PropertiesManager();
		}
		return instance;
	}

	public int getPort() {
		return port;
	}

	public String getUserDir() {
		return userDir;
	}

	public String getDataDir() {
		return dataDir;
	}
}
