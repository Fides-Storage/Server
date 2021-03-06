package org.fides.server.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This propertiesManager manages the application properties
 * 
 */
public class PropertiesManager {
	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(PropertiesManager.class);

	private static final String FILEPATH = "./config.properties";

	private static PropertiesManager instance = null;

	private int port;

	private String userDir;

	private String dataDir;

	private String keystorePath;

	private char[] keystorePassword;

	private long maxAmountOfBytesPerUser;

	private int expirationTimeInMonths;

	/**
	 * Constructor of the properties manager. Loads the properties file.
	 */
	protected PropertiesManager() {
		Properties properties = new Properties();
		InputStream in = null;

		try {
			File location = new File(FILEPATH);
			in = new FileInputStream(location.getCanonicalPath());
			properties.load(in);

		} catch (FileNotFoundException e) {
			LOG.error("Properties file is not found", e);
			System.exit(1);
		} catch (IOException e) {
			LOG.error("IOException has occured", e);
			System.exit(1);
		} finally {
			IOUtils.closeQuietly(in);
		}

		port = Integer.parseInt(properties.getProperty("port"));
		userDir = properties.getProperty("userDir");
		dataDir = properties.getProperty("dataDir");
		keystorePath = properties.getProperty("keystorePath");
		keystorePassword = properties.getProperty("keystorePassword").toCharArray();
		expirationTimeInMonths = Integer.parseInt(properties.getProperty("experitionTimeInMonths"));

		// Converts amount of megabytes to bytes
		maxAmountOfBytesPerUser = Long.parseLong(properties.getProperty("maxAmountOfMegabytesPerUser")) * 1048576L;

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

	/**
	 * Get the port where the application should run
	 * 
	 * @return The port as int
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Get the directory where the userfiles are located
	 * 
	 * @return The user directory as String
	 */
	public String getUserDir() {
		return userDir;
	}

	/**
	 * Get the directory where the datafiles are located
	 * 
	 * @return The data directory as String
	 */
	public String getDataDir() {
		return dataDir;
	}

	/**
	 * Get the path where the keystore is located
	 * 
	 * @return The path of the keystore as String
	 */
	public String getKeystorePath() {
		return keystorePath;
	}

	/**
	 * Get the password of the keystore
	 * 
	 * @return The password for the keystore as Char[]
	 */
	public char[] getKeystorePassword() {
		return keystorePassword;
	}

	public long getMaxAmountOfBytesPerUser() {
		return maxAmountOfBytesPerUser;
	}

	/**
	 * Get the expiration time in months
	 * 
	 * @return the expiration time in months
	 */
	public int getExpirationTimeInMonths() {
		return expirationTimeInMonths;
	}
}
