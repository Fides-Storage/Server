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

/**
 * This class is responsible for creating a file, removing a file and filling a file with an inputstream.
 */
public final class FileManager {

	// TODO: replace with singleton's basepath
	private static final String BASEPATH = "C:/Temp/Fides/";

	private static final int MAXUNIQUEATTEMPTS = 10;

	/**
	 * Creates a new file with a unique name.
	 * 
	 * @return The file's location.
	 * @throws IOException
	 */
	public static String createFile() {
		String location = UUID.randomUUID().toString();
		File newFile = new File(BASEPATH, location);
		
		try {
			int uniqueAttempts = 0;
			while (!newFile.createNewFile() && ++uniqueAttempts <= MAXUNIQUEATTEMPTS) {
				location = UUID.randomUUID().toString();
				newFile = new File(BASEPATH, location);
			}
		} catch (IOException e) {
			e.printStackTrace();
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
	 * @return Wether the update was succesfull
	 */
	public static boolean updateFile(InputStream instream, String location) {
		try {
			File file = new File(BASEPATH, location);
			if (!file.exists()) {
				// The FileManager can't update a non-existing file.
				return false;
			}
			OutputStream fileStream = new FileOutputStream(file);
			IOUtils.copy(instream, fileStream);
			fileStream.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Removes a file
	 * 
	 * @param location The location of the file
	 * @return If the file was succesfully deleted. Returns false if the file doesn't exist.
	 */
	public static boolean removeFile(String location) {
		File file = new File(BASEPATH, location);
		if (!file.exists()) {
			// The FileManager can't remove a non-existing file.
			return false;
		}
		return file.delete();
	}

	/**
	 * Updates the timestamp in a list of files.
	 * 
	 * @param fileLocations The list with the locations of the files to update.
	 */
	public static void updateTimestamps(Collection<String> fileLocations) {

	}

}
