package org.fides.server.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
	 * Copies an inputstream to fill the file.
	 * 
	 * @param inputStream
	 *            The inputstream to copy to the file
	 * @param file
	 *            The file to fill with the inputstream
	 * @param outputStream
	 *            The outputstream to respond to the client
	 * @return Wether the copy was successful or not
	 */
	public static boolean copyStreamToFile(DataInputStream inputStream, File file, DataOutputStream outputStream) {

		try {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty("successful", true);
			outputStream.writeUTF(new Gson().toJson(returnJobj));
			OutputStream fileOutputStream = new FileOutputStream(file);
			IOUtils.copy(inputStream, fileOutputStream);
			fileOutputStream.flush();
			fileOutputStream.close();
			// TODO: Check if the file was copied correctly
			return true;
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		return false;
	}

	/**
	 * Copies the content of a file to the outputstream
	 * 
	 * @param file
	 *            The file to use
	 * @param outputStream
	 *            The stream to copy the file to
	 * @return Wether the copy was successful.
	 */
	public static boolean copyFileToStream(File file, DataOutputStream outputStream) {
		try (InputStream inStream = new FileInputStream(file)) {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty("successful", true);
			outputStream.writeUTF(new Gson().toJson(returnJobj));

			IOUtils.copy(inStream, outputStream);
			outputStream.flush();
			outputStream.close();
			return true;
		} catch (IOException e) {
			log.error(e.getMessage());
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
	 * Updates the timestamp in a list of files.
	 * 
	 * @param fileLocations
	 *            The list with the locations of the files to update.
	 */
	public static void updateTimestamps(Collection<String> fileLocations) {

	}

}
