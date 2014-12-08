package org.fides.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.components.Actions;
import org.fides.components.Responses;
import org.fides.server.files.FileManager;
import org.fides.server.files.UserFile;
import org.fides.server.tools.Errors;
import org.fides.server.tools.JsonObjectHandler;
import org.fides.server.tools.PropertiesManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * A class for handling the sending and receiving of files
 *
 * @author Thijs
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
	 * @param userFile The logged in user's userFile
	 */
	public ClientFileConnector(UserFile userFile) {
		this.userFile = userFile;
	}

	/**
	 * Downloads the keyfile of the currently logged in user.
	 *
	 * @param outputStream The stream which contains the keyfile.
	 * @return Wether the writing of the keyfile to the stream was successful
	 */
	public boolean downloadKeyFile(DataOutputStream outputStream) {
		// Gets the keyfile from the user.
		File keyFile = new File(PropertiesManager.getInstance().getDataDir(), userFile.getKeyFileLocation());
		// If the keyfile exists, return a 'successful' and copy the file to the outputstream.
		if (keyFile.exists()) {
			return FileManager.copyFileToStream(keyFile, outputStream);
		} else {
			log.error("User's keyfile doesn't exist");
			copyErrorToStream("User keyfile could not be found (Please contact a server administrator)", outputStream);
		}
		IOUtils.closeQuietly(outputStream);

		return false;
	}

	/**
	 * Downloads a file by writing it to the outputstream
	 *
	 * @param fileRequest  The Json request which contains the file's location
	 * @param outputStream The stream which the file needs to be written to
	 * @return Wether the download is successful
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
					copyErrorToStream(Errors.FILENOTFOUND, outputStream);
				}
			} else {
				copyErrorToStream(Errors.FILEWITHOUTOWNERSHIP, outputStream);
			}
		} else {
			copyErrorToStream(Errors.NOFILELOCATION, outputStream);
		}
		IOUtils.closeQuietly(outputStream);
		return false;
	}

	/**
	 * Copies an errormessage to the outputstream
	 *
	 * @param errorMessage The message to copy
	 * @param outputStream The stream to copy the message to
	 */
	private void copyErrorToStream(String errorMessage, DataOutputStream outputStream) {
		try {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty(Responses.SUCCESSFUL, false);
			returnJobj.addProperty(Responses.ERROR, errorMessage);
			outputStream.writeUTF(new Gson().toJson(returnJobj));
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * Uploads a new file with the inputStream as its content. Generates a new file on the server to fill with the
	 * stream and returns the file's server location to the client through the outputstream.
	 *
	 * @param inputStream  The content of the file
	 * @param outputStream The stream to write the response to
	 * @return Wether the upload was successful or not
	 */
	public boolean uploadFile(DataInputStream inputStream, DataOutputStream outputStream) {
		try {
			String location = FileManager.createFile();
			File file = new File(PropertiesManager.getInstance().getDataDir(), location);
			// Check if the file was created correctly (should always be true)
			if (file.exists()) {
				// Return the location on the server where the new file will be written
				JsonObject returnJobj = new JsonObject();
				returnJobj.addProperty(Responses.SUCCESSFUL, true);
				returnJobj.addProperty(Actions.Properties.LOCATION, location);
				outputStream.writeUTF(new Gson().toJson(returnJobj));

				OutputStream fileOutputStream = new FileOutputStream(file);
				IOUtils.copy(inputStream, fileOutputStream);
				fileOutputStream.flush();
				fileOutputStream.close();

				// TODO: Make sure the upload was successful
				userFile.addFile(location);
				return true;
			} else {
				log.error("A file generated with FileManager.createFile() was not generated correctly.");
				copyErrorToStream("Upload failed. Please contact your server's administrator.", outputStream);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			copyErrorToStream("Upload failed. Please contact your server's administrator.", outputStream);
		}
		return false;
	}

	/**
	 * Update a file belonging to the user with the inputStream as its new contents
	 *
	 * @param inputStream   The contents to fill the file with.
	 * @param updateRequest The request containing the location of the file that needs to be updated
	 * @param outputStream  The stream to write responses to
	 * @return Wether the update was successful or not
	 */
	public boolean updateFile(DataInputStream inputStream, JsonObject updateRequest, DataOutputStream outputStream) {
		String location = JsonObjectHandler.getProperty(updateRequest, Actions.Properties.LOCATION);
		// Check if the user sent a location
		if (!StringUtils.isBlank(location)) {
			// Check if the user owns the file on that location
			if (userFile.checkOwned(location)) {
				File file = new File(PropertiesManager.getInstance().getDataDir(), location);
				// Check if the file exists
				if (file.exists()) {
					return FileManager.copyStreamToFile(inputStream, file, outputStream);
				} else {
					copyErrorToStream(Errors.FILENOTFOUND, outputStream);
				}
			} else {
				copyErrorToStream(Errors.FILEWITHOUTOWNERSHIP, outputStream);
			}
		} else {
			copyErrorToStream(Errors.NOFILELOCATION, outputStream);
		}
		return false;
	}

	/**
	 * Remove a file belonging to the user
	 *
	 * @param removeRequest The request containing the location of the file that needs to be removed
	 * @param outputStream  The stream to write responses to
	 * @return wether the remove was successful or not
	 */
	public boolean removeFile(JsonObject removeRequest, DataOutputStream outputStream) {
		try {
			String location = JsonObjectHandler.getProperty(removeRequest, Actions.Properties.LOCATION);
			// Check if the user sent a location
			if (!StringUtils.isBlank(location)) {
				// Check if the user owns the file on that location
				if (userFile.checkOwned(location)) {
					File file = new File(PropertiesManager.getInstance().getDataDir(), location);
					// Check if the file exists
					if (file.exists()) {

						boolean result = FileManager.removeFile(location);

						// Return the location on the server where the new file will be written
						JsonObject returnJobj = new JsonObject();
						returnJobj.addProperty(Responses.SUCCESSFUL, true);
						outputStream.writeUTF(new Gson().toJson(returnJobj));

						return result;
					} else {
						copyErrorToStream(Errors.FILENOTFOUND, outputStream);
					}
				} else {
					copyErrorToStream(Errors.FILEWITHOUTOWNERSHIP, outputStream);
				}
			} else {
				copyErrorToStream(Errors.NOFILELOCATION, outputStream);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			copyErrorToStream("Upload failed. Please contact your server's administrator.", outputStream);
		}
		return false;
	}

	/**
	 * Updates the keyfile with the inputStream as its content.
	 *
	 * @param inputStream  The stream to fill the user's keyfile with
	 * @param outputStream The stream used to return feedback to the client
	 * @return Wether the update was successful or not
	 */
	public boolean updateKeyFile(DataInputStream inputStream, DataOutputStream outputStream) {
		// Gets the keyfile from the user.
		File keyFile = new File(PropertiesManager.getInstance().getDataDir(), userFile.getKeyFileLocation());
		// If the keyfile exists, return a 'successful' and copy the inputstream to the keyfile.
		if (keyFile.exists()) {
			// Create a temporary file to prevent the keyfile from becoming corrupt when the stream closes too early
			String tempFileLocation = FileManager.createFile();
			File tempFile = new File(PropertiesManager.getInstance().getDataDir(), tempFileLocation);
			// Try to fill the temporary keyfile with the inputstream.
			if (FileManager.copyStreamToFile(inputStream, tempFile, outputStream)) {
				try {
					// Try to copy the temporary file to the real keyfile.
					// TODO: Make sure the upload was successful
					Files.copy(tempFile.toPath(), keyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					return true;
				} catch (IOException e) {
					log.error(e.getMessage());
					copyErrorToStream("Upload failed. Please contact your server's administrator.", outputStream);
				} finally {
					FileManager.removeFile(tempFileLocation);
				}
			} else {
				FileManager.removeFile(tempFileLocation);
				copyErrorToStream("Upload failed. Please contact your server's administrator.", outputStream);
			}
		} else {
			log.error("User's keyfile doesn't exist");
			copyErrorToStream("User keyfile could not be found (Please contact a server administrator)", outputStream);
		}
		return false;
	}

}
