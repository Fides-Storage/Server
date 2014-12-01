package org.fides.server.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.server.tools.PropertiesManager;

/**
 * This class is responsible for creating a file, removing a file and filling a file with an inputstream.
 */
public final class FileManager {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(FileManager.class);

	/** The maximum number of attempts when trying to create a unique filename */
	private static final int MAXUNIQUENAMEATTEMPTS = 10;

	/**
	 * Creates a new file with a unique name.
	 * 
	 * @return The file's location.
	 */
	public static String createFile() {
		PropertiesManager properties = PropertiesManager.getInstance();
		String location = UUID.randomUUID().toString();
		File newFile = new File(properties.getDataDir(), location);

		try {
			int uniqueAttempts = 0;
			while (!newFile.createNewFile() && ++uniqueAttempts <= MAXUNIQUENAMEATTEMPTS) {
				location = UUID.randomUUID().toString();
				newFile = new File(properties.getDataDir(), location);
			}
		} catch (IOException e) {
			log.error(e);
			location = null;
		}
		return location;
	}

	/**
	 * Updates a file by filling it with the given inputstream. If a file already had content, it will be erased.
	 * 
	 * @param instream
	 *            The new content
	 * @param location
	 *            The location of the file
	 * @return Whether the update was successful
	 */
	public static boolean updateFile(InputStream instream, String location) {
		OutputStream fileStream = null;
		try {
			File file = new File(PropertiesManager.getInstance().getDataDir(), location);
			if (!file.exists()) {
				// The FileManager can't update a non-existing file.
				return false;
			}
			fileStream = new FileOutputStream(file);
			IOUtils.copy(instream, fileStream);

			return true;
		} catch (FileNotFoundException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		} finally {
			IOUtils.closeQuietly(fileStream);
		}
		return false;
	}

	/**
	 * Removes a file
	 * 
	 * @param location
	 *            The location of the file
	 * @return If the file was successfully deleted. Returns false if the file doesn't exist.
	 */
	public static boolean removeFile(String location) {
		File file = new File(PropertiesManager.getInstance().getDataDir(), location);
		if (!file.exists()) {
			// The FileManager can't remove a non-existing file.
			return false;
		}
		return file.delete();
	}
	
	/**
	 * Requests the data in a user's file
	 * 
	 * @param location
	 * 			The location of the file
	 * @return The inputstream with the content of the file.
	 */
	public static InputStream requestFile(String location) {
		File file = new File(PropertiesManager.getInstance().getDataDir(), location);
		if (!file.exists()) {
			// The FileManager can't request a non-existing file.
			return null;
		}
		
		return null;
	}

	/**
	 * Updates the timestamp in a list of files.
	 * 
	 * @param fileLocations
	 *            The list with the locations of the files to update.
	 */
	public static void updateTimestamps(Collection<String> fileLocations) {

	}

}
