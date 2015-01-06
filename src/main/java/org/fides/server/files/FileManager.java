package org.fides.server.files;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.components.virtualstream.VirtualInputStream;
import org.fides.components.virtualstream.VirtualOutputStream;
import org.fides.server.tools.CommunicationUtil;
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
	private static final int MAX_UNIQUE_NAME_ATTEMPTS = 10;

	/**
	 * The default buffer size ({@value} ) to use for {@link #copyLarge(InputStream, OutputStream)}
	 */
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	private static final int EOF = -1;

	/**
	 * Creates a new file with a unique name.
	 * 
	 * @return The file's location.
	 */
	public static String createFile() {
		return createFile(false);
	}

	/**
	 * Creates a new file with a unique name. If the file is temporary, it ends with .tmp
	 * 
	 * @param temporary
	 *            Whether the file is temporary or not
	 * @return The file's location.
	 */
	public static String createFile(boolean temporary) {
		// Generate a random name for the createfile
		PropertiesManager properties = PropertiesManager.getInstance();
		String location = UUID.randomUUID().toString();
		// If the user wants a temporary file, make the name end with .tmp
		if (temporary) {
			location += ".tmp";
		}
		String dataDir = PropertiesManager.getInstance().getDataDir();
		if (StringUtils.isNotEmpty(dataDir)) {
			File newFile = new File(dataDir, location);

			// Check if the filename is unique, there's a maximum number of attempts to prevent an overflow
			try {
				int uniqueAttempts = 0;
				while (!newFile.createNewFile() && ++uniqueAttempts <= MAX_UNIQUE_NAME_ATTEMPTS) {
					location = UUID.randomUUID().toString();
					if (temporary) {
						location += ".tmp";
					}
					newFile = new File(properties.getDataDir(), location);
				}
			} catch (IOException e) {
				log.error(e);
				location = null;
			}
		} else {
			location = null;
		}
		// Return the location of the generated file
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
	 * @param userFile
	 *            used to get amount of free space
	 * @param isDataFile
	 *            used to exclude the key file
	 * @return Whether the copy was successful or not
	 */
	public static boolean copyStreamToFile(InputStream inputStream, File file, DataOutputStream outputStream, UserFile userFile, boolean isDataFile) {
		String dataDir = PropertiesManager.getInstance().getDataDir();
		String tempFileName = createFile(true);
		if (StringUtils.isNotEmpty(dataDir) && StringUtils.isNotEmpty(tempFileName)) {
			// Create a temporary file to prevent the keyfile from becoming corrupt when the stream closes too early
			File tempFile = new File(dataDir, tempFileName);
			try (InputStream virtualIn = new VirtualInputStream(inputStream);
				OutputStream fileOutputStream = new FileOutputStream(tempFile)) {
				// Tell the cli�nt he can start sending the file.
				CommunicationUtil.returnSuccessful(outputStream);

				// Put the stream into a temporary file
				long bytesCoppied = FileManager.copyLarge(virtualIn, fileOutputStream, userFile, isDataFile);
				fileOutputStream.flush();
				fileOutputStream.close();
				virtualIn.close();

				if (bytesCoppied != -1) {

					// don't use key file
					if (isDataFile) {
						userFile.removeAmountOfBytes(file.length());
						userFile.addAmountOfBytes(bytesCoppied);
					}

					// Copy the temporary file into the official file
					Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
					CommunicationUtil.returnSuccessful(outputStream);
					return true;
				} else {
					CommunicationUtil.returnError(outputStream, "Upload file size to big.");
					return false;
				}

			} catch (IOException e) {
				log.error(e.getMessage());
			} finally {
				tempFile.delete();
			}
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
	 * @return Whether the copy was successful.
	 */
	public static boolean copyFileToStream(File file, DataOutputStream outputStream) {
		// Open an inputstream to the file and a virtualoutputstream of the output
		try (InputStream inStream = new FileInputStream(file);
			VirtualOutputStream virtualOutStream = new VirtualOutputStream(outputStream)) {
			// Tell the client he can start downloading
			CommunicationUtil.returnSuccessful(outputStream);

			// Copy the content of the file to the stream
			IOUtils.copy(inStream, virtualOutStream);
			virtualOutStream.flush();
			virtualOutStream.close();

			return true;
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		return false;
	}

	/**
	 * Removes a file, and clears the space in the user file
	 * 
	 * @param location
	 *            The location of the file
	 * @param userFile
	 *            user file used to save removed amount of bytes if file is deleted
	 * @return If the file was successfully deleted. Returns false if the file doesn't exist.
	 */
	public static boolean removeFile(String location, UserFile userFile) {
		String dataDir = PropertiesManager.getInstance().getDataDir();
		if (StringUtils.isNotEmpty(dataDir) && StringUtils.isNotEmpty(location)) {
			File file = new File(dataDir, location);
			userFile.removeAmountOfBytes(file.length());
			return file.delete();
		}
		return false;
	}

	/**
	 * Updates the timestamp in a list of files.
	 * 
	 * @param fileLocations
	 *            The list with the locations of the files to update.
	 */
	public static void updateTimestamps(Collection<String> fileLocations) {

	}

	/**
	 * Copy bytes from a large (over 2GB) <code>InputStream</code> to an <code>OutputStream</code>.
	 * <p>
	 * This method uses the provided buffer, so there is no need to use a <code>BufferedInputStream</code>.
	 * <p>
	 * 
	 * Modified version of IOUtils of apache lib
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @param userFile
	 *            used to get amount of free space
	 * @param isDataFile
	 *            used to exclude the key file
	 * @return the number of bytes copied, -1 of not succeed
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static long copyLarge(InputStream input, OutputStream output, UserFile userFile, boolean isDataFile)
		throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

		if (isDataFile) {
			log.trace("Maximum amount of bytes allowed to copy: " + userFile.getAmountOfFreeBytes());
		}

		long count = 0;
		int n = 0;
		while (EOF != (n = input.read(buffer)) && (count <= userFile.getAmountOfFreeBytes() || !isDataFile)) {
			output.write(buffer, 0, n);
			count += n;
		}

		if (count <= userFile.getAmountOfFreeBytes() || !isDataFile) {
			log.trace("Copy amount of bytes: " + count + ", isDataFile: " + isDataFile);
			return count;
		} else {
			log.trace("Copy amount of bytes: -1 , isDataFile: " + isDataFile);
			return -1;
		}

	}
}
