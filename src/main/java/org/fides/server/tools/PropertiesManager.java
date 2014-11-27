package org.fides.server.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

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

	private String keystorePath;

	private char[] keystorePassword;

	/**
	 * Constructor of the properties manager
	 */
	protected PropertiesManager() {
		Properties properties = new Properties();
		InputStream in = null;

		try {
			File location = new File(FILEPATH);
			in = new FileInputStream(location.getCanonicalPath());
			properties.load(in);

		} catch (FileNotFoundException e) {
			System.err.println("Properties file is not found: " + e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			System.err.println("IOException has occured: " + e.getMessage());
			System.exit(1);
		} finally {
			IOUtils.closeQuietly(in);
		}

		port = Integer.parseInt(properties.getProperty("port"));
		userDir = properties.getProperty("userDir");
		dataDir = properties.getProperty("dataDir");
		keystorePath = properties.getProperty("keystorePath");
		keystorePassword = properties.getProperty("keystorePassword").toCharArray();

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

	public String getKeystorePath() {
		return keystorePath;
	}

	public char[] getKeystorePassword() {
		return keystorePassword;
	}
}
