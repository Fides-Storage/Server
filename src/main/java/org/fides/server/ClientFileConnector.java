package org.fides.server;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.components.Actions;
import org.fides.components.virtualstream.VirtualInputStream;
import org.fides.server.files.FileManager;
import org.fides.server.files.UserFile;
import org.fides.server.tools.CommunicationUtil;
import org.fides.server.tools.Errors;
import org.fides.server.tools.JsonObjectHandler;
import org.fides.server.tools.PropertiesManager;

import com.google.gson.JsonObject;

/**
 * A class for handling the sending and receiving of files
 * 
 */
public class ClientFileConnector {

	/**
	 * Logger for this class
	 */
	private static Logger log = LogManager.getLogger(ClientFileConnector.class);

	private UserFile userFile;

	/**
	 * Constructor for ClientFileConnector
	 * 
	 * @param userFile
	 *            The logged in user's userFile
	 */
	public ClientFileConnector(UserFile userFile) {
		this.userFile = userFile;
	}

	/**
	 * Downloads the keyfile of the currently logged in user.
	 * 
	 * @param outputStream
	 *            The stream which contains the keyfile.
	 * @return Whether the writing of the keyfile to the stream was successful
	 */
	public boolean downloadKeyFile(DataOutputStream outputStream) {
		String dataDir = PropertiesManager.getInstance().getDataDir();
		String keyFileLocation = userFile.getKeyFileLocation();

		// Gets the keyfile from the user.
		if (StringUtils.isNotEmpty(dataDir) && StringUtils.isNotEmpty(keyFileLocation)) {
			File keyFile = new File(dataDir, keyFileLocation);
			// If the keyfile exists, return a 'successful' and copy the file to the outputstream.
			if (keyFile.exists()) {
				return FileManager.copyFileToStream(keyFile, outputStream);
			} else {
				log.error("User's keyfile doesn't exist");
				CommunicationUtil.returnError(outputStream, "User keyfile could not be found (Please contact a server administrator)");
			}
			IOUtils.closeQuietly(outputStream);
		}

		return false;
	}

	/**
	 * Downloads a file by writing it to the outputstream
	 * 
	 * @param fileRequest
	 *            The Json request which contains the file's location
	 * @param outputStream
	 *            The stream which the file needs to be written to
	 * @return Whether the download is successful
	 */
	public boolean downloadFile(JsonObject fileRequest, DataOutputStream outputStream) {
		String fileLocation = JsonObjectHandler.getProperty(fileRequest, Actions.Properties.LOCATION);
		// Check if the user sent a location
		if (!StringUtils.isBlank(fileLocation)) {
			// Check if the file is owned by the user
			if (userFile.checkOwned(fileLocation)) {
				File file = new File(PropertiesManager.getInstance().getDataDir(), fileLocation);
				// Check if the requested file exists
				if (file.exists()) {
					return FileManager.copyFileToStream(file, outputStream);
				} else {
					CommunicationUtil.returnError(outputStream, Errors.FILE_NOT_FOUND);
				}
			} else {
				CommunicationUtil.returnError(outputStream, Errors.FILE_WITHOUT_OWNERSHIP);
			}
		} else {
			CommunicationUtil.returnError(outputStream, Errors.NO_FILE_LOCATION);
		}
		IOUtils.closeQuietly(outputStream);
		return false;
	}

	/**
	 * Uploads a new file with the inputStream as its content. Generates a new file on the server to fill with the
	 * stream and returns the file's server location to the client through the outputstream.
	 * 
	 * @param inputStream
	 *            The content of the file
	 * @param outputStream
	 *            The stream to write the response to
	 * @return Whether the upload was successful or not
	 */
	public boolean uploadFile(InputStream inputStream, DataOutputStream outputStream) {
		File tempFile = new File(PropertiesManager.getInstance().getDataDir(), FileManager.createFile(true));
		String location = FileManager.createFile();
		File file = new File(PropertiesManager.getInstance().getDataDir(), location);
		boolean uploadSuccessful = false;
		// Check if the file was created correctly (should always be true)
		if (file.exists() && tempFile.exists()) {
			try (InputStream virtualInputStream = new VirtualInputStream(inputStream);
				OutputStream fileOutputStream = new FileOutputStream(tempFile)) {
				// Return the location on the server where the new file will be written
				Map<String, Object> properties = new HashMap<>();
				properties.put(Actions.Properties.LOCATION, location);
				CommunicationUtil.returnSuccessfulWithProperties(outputStream, properties);

				// Put the inputstream received from the user into a temporary file
				long bytesCopied = FileManager.copyLarge(virtualInputStream, fileOutputStream, userFile, true);
				fileOutputStream.flush();
				fileOutputStream.close();
				virtualInputStream.close();

				if (bytesCopied != -1) {
					CommunicationUtil.returnSuccessful(outputStream);

					// Add the file to the user
					Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
					userFile.addFile(location);

					userFile.addAmountOfBytes(bytesCopied);
					log.trace("Amount of free bytes: " + userFile.getAmountOfFreeBytes());
					uploadSuccessful = true;
				} else {
					CommunicationUtil.returnError(outputStream, "Upload file size too big.");
				}
			} catch (IOException e) {
				log.error(e.getMessage());
				CommunicationUtil.returnError(outputStream, "Upload failed. Please contact your server's administrator.");
			} finally {
				if (!uploadSuccessful) {
					file.delete();
				}
				tempFile.delete();
			}
		} else {
			log.error("A file generated with FileManager.createFile() was not generated correctly.");
			CommunicationUtil.returnError(outputStream, "Upload failed. Please contact your server's administrator.");
		}
		return uploadSuccessful;
	}

	/**
	 * Update a file belonging to the user with the inputStream as its new contents
	 * 
	 * @param inputStream
	 *            The contents to fill the file with.
	 * @param updateRequest
	 *            The request containing the location of the file that needs to be updated
	 * @param outputStream
	 *            The stream to write responses to
	 * @return Whether the update was successful or not
	 */
	public boolean updateFile(InputStream inputStream, JsonObject updateRequest, DataOutputStream outputStream) {
		String location = JsonObjectHandler.getProperty(updateRequest, Actions.Properties.LOCATION);
		// Check if the user sent a location
		if (!StringUtils.isBlank(location)) {
			// Check if the user owns the file on that location
			if (userFile.checkOwned(location)) {
				File file = new File(PropertiesManager.getInstance().getDataDir(), location);
				// Check if the file exists
				if (file.exists()) {
					return FileManager.copyStreamToFile(inputStream, file, outputStream, userFile, true);
				} else {
					CommunicationUtil.returnError(outputStream, Errors.FILE_NOT_FOUND);
				}
			} else {
				CommunicationUtil.returnError(outputStream, Errors.FILE_WITHOUT_OWNERSHIP);
			}
		} else {
			CommunicationUtil.returnError(outputStream, Errors.NO_FILE_LOCATION);
		}
		return false;
	}

	/**
	 * Remove a file belonging to the user
	 * 
	 * @param removeRequest
	 *            The request containing the location of the file that needs to be removed
	 * @param outputStream
	 *            The stream to write responses to
	 * @return whether the remove was successful or not
	 */
	public boolean removeFile(JsonObject removeRequest, DataOutputStream outputStream) {
		String location = JsonObjectHandler.getProperty(removeRequest, Actions.Properties.LOCATION);
		// Check if the user sent a location
		if (!StringUtils.isBlank(location)) {
			// Check if the user owns the file on that location
			if (userFile.checkOwned(location)) {
				File file = new File(PropertiesManager.getInstance().getDataDir(), location);
				// Check if the file exists
				if (file.exists()) {
					// Remove the file
					boolean result = FileManager.removeFile(location, userFile);

					if (result) {
						try {
							log.trace("Amount of free bytes: " + userFile.getAmountOfFreeBytes());
							CommunicationUtil.returnSuccessful(outputStream);
						} catch (IOException e) {
							log.debug("IOException when returning the successful delete: ", e);
						}

						// Remove file in UserFile
						userFile.removeFile(location);
					} else {
						CommunicationUtil.returnError(outputStream, Errors.FILE_NOT_REMOVED);
					}
					return result;
				} else {
					CommunicationUtil.returnError(outputStream, Errors.FILE_NOT_FOUND);
				}
			} else {
				CommunicationUtil.returnError(outputStream, Errors.FILE_WITHOUT_OWNERSHIP);
			}
		} else {
			CommunicationUtil.returnError(outputStream, Errors.NO_FILE_LOCATION);
		}
		return false;
	}

	/**
	 * Updates the keyfile with the inputStream as its content.
	 * 
	 * @param inputStream
	 *            The stream to fill the user's keyfile with
	 * @param outputStream
	 *            The stream used to return feedback to the client
	 * @return Whether the update was successful or not
	 */

	public boolean updateKeyFile(InputStream inputStream, DataOutputStream outputStream) {
		String dataDir = PropertiesManager.getInstance().getDataDir();
		String keyFileLocation = userFile.getKeyFileLocation();

		if (StringUtils.isNotEmpty(dataDir) && StringUtils.isNotEmpty(keyFileLocation)) {
			// Gets the keyfile from the user.
			File keyFile = new File(PropertiesManager.getInstance().getDataDir(), userFile.getKeyFileLocation());
			// If the keyfile exists, copy the stream to the keyfile (via a temporary file)
			if (keyFile.exists()) {
				return FileManager.copyStreamToFile(inputStream, keyFile, outputStream, userFile, false);

			} else {
				log.error("User's keyfile doesn't exist");
				CommunicationUtil.returnError(outputStream, "User keyfile could not be found (Please contact a server administrator)");
			}
		}
		return false;
	}
}
