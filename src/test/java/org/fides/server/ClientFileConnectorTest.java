package org.fides.server;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fides.components.Actions;
import org.fides.components.Responses;
import org.fides.components.virtualstream.VirtualInputStream;
import org.fides.components.virtualstream.VirtualOutputStream;
import org.fides.server.files.FileManager;
import org.fides.server.files.UserFile;
import org.fides.server.tools.Errors;
import org.fides.server.tools.PropertiesManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The tests for the ClientFileConnector class
 * 
 * @author Thijs
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PropertiesManager.class, FileManager.class })
public class ClientFileConnectorTest {

	/** A mocked PropertiesManager which should always return the test Data Directory */
	private static PropertiesManager mockedPropertiesManager = Mockito.mock(PropertiesManager.class);

	/** The test User Directory */
	private static File testDataDir;

	private static final byte[] KEYFILECONTENT = "This is the default keyfile content".getBytes();

	private static final byte[] FILECONTENT = "This is the default normal file content".getBytes();

	private static final FilenameFilter TEMPFILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".tmp");
		}
	};

	private DataInputStream in;

	private DataOutputStream out;

	private UserFile mockedUserFile = Mockito.mock(UserFile.class);

	private ClientFileConnector connector = new ClientFileConnector(mockedUserFile);

	/**
	 * Sets up the test class by mocking the ClientFileConnector to return a testdatadir.
	 */
	@BeforeClass
	public static void setUp() {
		try {
			testDataDir = new File(PropertiesManager.getInstance().getDataDir(), "Test");
			if (!testDataDir.exists()) {
				assertTrue(testDataDir.mkdirs());
			}
			// This causes the mocked PropertiesManager to always return the test Data directory:
			Mockito.when(mockedPropertiesManager.getDataDir()).thenReturn(testDataDir.getAbsolutePath());
		} catch (Exception e) {
			fail("Unexpected error in setUp: " + e.getMessage());
		}
	}

	/**
	 * Mocks the PropertiesManager to always return a mocked version of the PropertiesManager. This will cause the
	 * FileManager to use a testfolder instead of the main folder.
	 */
	@Before
	public void setUpMock() {
		PowerMockito.mockStatic(PropertiesManager.class);
		Mockito.when(PropertiesManager.getInstance()).thenReturn(mockedPropertiesManager);
	}

	/**
	 * This emulates the client side of changing data to a virtualoutputstream which then gets send to the server as an
	 * inputstream.
	 * 
	 * @param data
	 *            The bytearray to send to the inputstream
	 * @return The inputstream with the data send through a virtualstream
	 * @throws IOException
	 */
	private InputStream byteArrayToDataStream(byte[] data) throws IOException {
		ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		VirtualOutputStream virtualOut = new VirtualOutputStream(byteOut);
		IOUtils.copy(byteIn, virtualOut);
		virtualOut.flush();
		virtualOut.close();

		return new ByteArrayInputStream(byteOut.toByteArray());
	}

	/**
	 * This emulates the client side of receiving the outputstream as inputstream, sending it through a
	 * virtualinputstream and then reading the bytes.
	 * 
	 * @param outputStream
	 *            The outputstream to read the bytes from
	 * @return The bytes in the outputstream
	 * @throws IOException
	 */
	private byte[] byteDataStreamToByteArray(ByteArrayOutputStream outputStream) throws IOException {
		ByteArrayInputStream byteIn = new ByteArrayInputStream(outputStream.toByteArray());
		VirtualInputStream virtualIn = new VirtualInputStream(byteIn);
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		IOUtils.copy(virtualIn, byteOut);
		byteOut.flush();
		byteOut.close();
		return byteOut.toByteArray();
	}

	/**
	 * Tests the file upload
	 */
	@Test
	public void testFileUpload() {
		try {
			// Create an empty file and make the 'createFile' return that file.
			String newFileLocation = "UploadTestFile";
			File newFile = new File(testDataDir, newFileLocation);
			assertFalse(newFile.exists());
			newFile.createNewFile();
			PowerMockito.stub(PowerMockito.method(FileManager.class, "createFile")).toReturn(newFileLocation);

			// Create the streams to use for the upload and the upload's response.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			InputStream inStream = byteArrayToDataStream(FILECONTENT);

			// Upload the file
			assertTrue(connector.uploadFile(inStream, out));
			inStream.close();

			// Make sure the connector tried adding the new file to the userfile.
			Mockito.verify(mockedUserFile, Mockito.times(1)).addFile(newFileLocation);

			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertArrayEquals(FILECONTENT, Files.readAllBytes(newFile.toPath()));
			assertTrue(response.contains("\"" + Responses.SUCCESSFUL + "\":true"));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests the file update on an 'existing' file
	 */
	@Test
	public void testFileUpdate() {
		try {
			// Create an existing file for the user to update.
			String existingFileLocation = "UpdateTestFile";
			File existingFile = new File(testDataDir, existingFileLocation);
			Mockito.when(mockedUserFile.checkOwned(existingFileLocation)).thenReturn(true);
			assertFalse(existingFile.exists());

			// Fill the existing file with some default content
			OutputStream existingFileOut = new FileOutputStream(existingFile);
			existingFileOut.write(FILECONTENT);
			existingFileOut.flush();
			existingFileOut.close();

			// Create the streams to use for the update and the update's response.
			byte[] updatedFileContent = "This is an updated file content".getBytes();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			InputStream inStream = byteArrayToDataStream(updatedFileContent);

			// The update
			JsonObject updateRequest = new JsonObject();
			updateRequest.addProperty(Actions.Properties.LOCATION, existingFileLocation);
			assertTrue(connector.updateFile(inStream, updateRequest, out));
			inStream.close();

			// Test if file contents changed
			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertArrayEquals(updatedFileContent, Files.readAllBytes(existingFile.toPath()));
			assertTrue(response.contains("\"" + Responses.SUCCESSFUL + "\":true"));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests removing an existing file
	 */
	@Test
	public void testFileRemove() {
		try {
			// Create an existing file for the user to update.
			String existingFileLocation = "RemoveTestFile";
			File existingFile = new File(testDataDir, existingFileLocation);
			Mockito.when(mockedUserFile.checkOwned(existingFileLocation)).thenReturn(true);
			assertFalse(existingFile.exists());

			// Fill the existing file with some default content
			OutputStream existingFileOut = new FileOutputStream(existingFile);
			existingFileOut.write(FILECONTENT);
			existingFileOut.flush();
			existingFileOut.close();

			// create stream for the response
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);

			// Remove the file
			JsonObject updateRequest = new JsonObject();
			updateRequest.addProperty(Actions.Properties.LOCATION, existingFileLocation);
			assertTrue(connector.removeFile(updateRequest, out));

			// Assert if the file was removed
			assertFalse(existingFile.exists());
			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");

			// Assert if the response contains successful:true
			assertTrue(response.contains("\"" + Responses.SUCCESSFUL + "\":true"));

			// Tests if the removal of a non existing file won't return a true
			JsonObject removeIncorrectRequest = new JsonObject();
			removeIncorrectRequest.addProperty(Actions.Properties.LOCATION, "incorrectLocaion");
			assertFalse(connector.removeFile(removeIncorrectRequest, out));

			// Assert if the response contains successful:false
			response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertTrue(response.contains("\"" + Responses.SUCCESSFUL + "\":false"));

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests if the file update fails correctly if no location is given.
	 */
	@Test
	public void testFileUpdateEmptyLocation() {
		try {
			// Create the streams to use for the update and the update's response.
			byte[] updatedFileContent = "This is an updated file content".getBytes();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			in = new DataInputStream(new ByteArrayInputStream(updatedFileContent));

			// The update
			JsonObject updateRequest = new JsonObject();
			updateRequest.addProperty(Actions.Properties.LOCATION, "");
			assertFalse(connector.updateFile(in, updateRequest, out));
			in.close();

			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertTrue(response.contains("\"" + Responses.SUCCESSFUL + "\":false"));
			assertTrue(response.contains(Errors.NO_FILE_LOCATION));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests if the file update fails correctly if no location is given.
	 */
	@Test
	public void testFileUpdateNoLocation() {
		try {
			// Create the streams to use for the update and the update's response.
			byte[] updatedFileContent = "This is an updated file content".getBytes();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			in = new DataInputStream(new ByteArrayInputStream(updatedFileContent));

			// The update
			JsonObject updateRequest = new JsonObject();
			assertFalse(connector.updateFile(in, updateRequest, out));
			in.close();

			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertTrue(response.contains("\"" + Responses.SUCCESSFUL + "\":false"));
			assertTrue(response.contains(Errors.NO_FILE_LOCATION));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests if the file update fails correctly if the given location is not owned by the user.
	 */
	@Test
	public void testFileUpdateNotOwned() {
		try {
			// Create an existing file for the user to update.
			String notOwnedFileLocation = "TestFileNotOwned";
			File existingFile = new File(testDataDir, notOwnedFileLocation);
			Mockito.when(mockedUserFile.checkOwned(notOwnedFileLocation)).thenReturn(false);
			assertFalse(existingFile.exists());

			// Fill the existing file with some default content
			OutputStream existingFileOut = new FileOutputStream(existingFile);
			existingFileOut.write(FILECONTENT);
			existingFileOut.flush();
			existingFileOut.close();

			// Create the streams to use for the update and the update's response.
			byte[] updatedFileContent = "This is an updated file content".getBytes();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			in = new DataInputStream(new ByteArrayInputStream(updatedFileContent));

			// The update
			JsonObject updateRequest = new JsonObject();
			updateRequest.addProperty(Actions.Properties.LOCATION, notOwnedFileLocation);
			assertFalse(connector.updateFile(in, updateRequest, out));
			in.close();

			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertTrue(response.contains("\"" + Responses.SUCCESSFUL + "\":false"));
			assertTrue(response.contains(Errors.FILE_WITHOUT_OWNERSHIP));

			// Make sure the file didn't get updated.
			assertFalse(Arrays.equals(updatedFileContent, Files.readAllBytes(existingFile.toPath())));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests if the file update fails correctly if the given location doesn't exist.
	 */
	@Test
	public void testFileUpdateNotExisting() {
		try {
			// Create a non-existing file for the user to update.
			String notExistingFileLocation = "TestFileNotExisting";
			File notExistingFile = new File(testDataDir, notExistingFileLocation);
			Mockito.when(mockedUserFile.checkOwned(notExistingFileLocation)).thenReturn(true);
			assertFalse(notExistingFile.exists());

			// Create the streams to use for the update and the update's response.
			byte[] updatedFileContent = "This is an updated file content".getBytes();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			in = new DataInputStream(new ByteArrayInputStream(updatedFileContent));

			// The update of the non-existing file.
			JsonObject updateRequest = new JsonObject();
			updateRequest.addProperty(Actions.Properties.LOCATION, notExistingFileLocation);
			assertFalse(connector.updateFile(in, updateRequest, out));
			in.close();

			// Check if the correct error was returned
			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertTrue(response.contains("\"" + Responses.SUCCESSFUL + "\":false"));
			assertTrue(response.contains(Errors.FILE_NOT_FOUND));

			// Make sure the file didn't get created.
			assertFalse(notExistingFile.exists());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests the file download
	 */
	@Test
	public void testFileDownload() {
		try {
			// Create an existing file for the user to update.
			String downloadFileLocation = "DownloadTestFile";
			File downloadFile = new File(testDataDir, downloadFileLocation);
			Mockito.when(mockedUserFile.checkOwned(downloadFileLocation)).thenReturn(true);
			assertFalse(downloadFile.exists());

			// Fill the existing file with some default content
			OutputStream existingFileOut = new FileOutputStream(downloadFile);
			existingFileOut.write(FILECONTENT);
			existingFileOut.flush();
			existingFileOut.close();

			// Create the stream to use for the download's response.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);

			// The actual download request
			JsonObject fileRequest = new JsonObject();
			fileRequest.addProperty(Actions.Properties.LOCATION, downloadFileLocation);
			assertTrue(connector.downloadFile(fileRequest, out));

			// First read the JsonResponse to see if the request is successful
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"" + Responses.SUCCESSFUL + "\":true"));

			// Read the rest of the stream to check if the file was correctly downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertArrayEquals(FILECONTENT, byteDataStreamToByteArray(fileResponseStream));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests if the file download fails correctly if no location is given.
	 */
	@Test
	public void testFileDownloadEmptyLocation() {
		try {
			// Create the stream to use for the download's response.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);

			// The actual download request
			JsonObject fileRequest = new JsonObject();
			fileRequest.addProperty(Actions.Properties.LOCATION, "");
			assertFalse(connector.downloadFile(fileRequest, out));

			// First read the JsonResponse to see if the request was unsuccessful
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"" + Responses.SUCCESSFUL + "\":false"));
			assertTrue(downloadResponse.toString().contains(Errors.NO_FILE_LOCATION));

			// Read the rest of the stream to check if indeed no file was downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertEquals(0, fileResponseStream.toByteArray().length);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests if the file download fails correctly if no location is given.
	 */
	@Test
	public void testFileDownloadNoLocation() {
		try {
			// Create the stream to use for the download's response.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);

			// The actual download request
			JsonObject fileRequest = new JsonObject();
			assertFalse(connector.downloadFile(fileRequest, out));

			// First read the JsonResponse to see if the request was unsuccessful
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"" + Responses.SUCCESSFUL + "\":false"));
			assertTrue(downloadResponse.toString().contains(Errors.NO_FILE_LOCATION));

			// Read the rest of the stream to check if indeed no file was downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertEquals(0, fileResponseStream.toByteArray().length);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests if the file download fails correctly if the given location is not owned by the user.
	 */
	@Test
	public void testFileDownloadNotOwned() {
		try {
			// Create an existing file that the user doesn't own.
			String notOwnedFileLocation = "DownloadFileNotOwned";
			File existingFile = new File(testDataDir, notOwnedFileLocation);
			Mockito.when(mockedUserFile.checkOwned(notOwnedFileLocation)).thenReturn(false);
			assertFalse(existingFile.exists());

			// Fill the existing file with some default content
			OutputStream existingFileOut = new FileOutputStream(existingFile);
			existingFileOut.write(FILECONTENT);
			existingFileOut.flush();
			existingFileOut.close();

			// Create the stream to use for the download's response.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);

			// The actual download request
			JsonObject fileRequest = new JsonObject();
			fileRequest.addProperty(Actions.Properties.LOCATION, notOwnedFileLocation);
			assertFalse(connector.downloadFile(fileRequest, out));

			// First read the JsonResponse to see if the request was unsuccessful
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"" + Responses.SUCCESSFUL + "\":false"));
			assertTrue(downloadResponse.toString().contains(Errors.FILE_WITHOUT_OWNERSHIP));

			// Read the rest of the stream to check if indeed no file was downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertEquals(0, fileResponseStream.toByteArray().length);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests if the file download fails correctly if the given location doesn't exist.
	 */
	@Test
	public void testFileDownloadNotExisting() {
		try {
			// Create a non-existing file for the user to update.
			String notExistingFileLocation = "TestFileNotExisting";
			File notExistingFile = new File(testDataDir, notExistingFileLocation);
			Mockito.when(mockedUserFile.checkOwned(notExistingFileLocation)).thenReturn(true);
			assertFalse(notExistingFile.exists());

			// Create the stream to use for the download's response.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);

			// The actual download request
			JsonObject fileRequest = new JsonObject();
			fileRequest.addProperty(Actions.Properties.LOCATION, notExistingFileLocation);
			assertFalse(connector.downloadFile(fileRequest, out));

			// First read the JsonResponse to see if the request was unsuccessful
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"" + Responses.SUCCESSFUL + "\":false"));
			assertTrue(downloadResponse.toString().contains(Errors.FILE_NOT_FOUND));

			// Read the rest of the stream to check if indeed no file was downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertEquals(0, fileResponseStream.toByteArray().length);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test if the Upload and Download work well together
	 */
	@Test
	public void testFileUploadDownload() {
		try {
			// Create an empty file and make the 'createFile' return that file.
			String newFileLocation = "UploadDownloadTestFile";
			File newFile = new File(testDataDir, newFileLocation);
			assertFalse(newFile.exists());
			newFile.createNewFile();
			PowerMockito.stub(PowerMockito.method(FileManager.class, "createFile")).toReturn(newFileLocation);

			// Create the streams to use for the upload and the upload's response.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			InputStream inStream = byteArrayToDataStream(FILECONTENT);

			// Upload the file
			connector.uploadFile(inStream, out);
			inStream.close();

			// Make sure the connector tried adding the new file to the userfile.
			Mockito.verify(mockedUserFile, Mockito.times(1)).addFile(newFileLocation);
			Mockito.when(mockedUserFile.checkOwned(newFileLocation)).thenReturn(true);

			// Create the stream to use for the download's response.
			outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);

			// The download request
			JsonObject fileRequest = new JsonObject();
			fileRequest.addProperty(Actions.Properties.LOCATION, newFileLocation);
			connector.downloadFile(fileRequest, out);

			// First read the JsonResponse to see if the request is successful
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"" + Responses.SUCCESSFUL + "\":true"));

			// Read the rest of the stream to check if the file was correctly downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertArrayEquals(FILECONTENT, byteDataStreamToByteArray(fileResponseStream));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test if the Upload, Update and Download work well together
	 */
	@Test
	public void testFileUploadUpdateDownload() {
		try {
			// Create an empty file and make the 'createFile' return that file.
			String newFileLocation = "UploadUpdateDownloadTestFile";
			File newFile = new File(testDataDir, newFileLocation);
			assertFalse(newFile.exists());
			newFile.createNewFile();
			PowerMockito.stub(PowerMockito.method(FileManager.class, "createFile")).toReturn(newFileLocation);

			// Create the streams to use for the upload and the upload's response.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			InputStream inStream = byteArrayToDataStream(FILECONTENT);

			// Upload the file
			connector.uploadFile(inStream, out);
			inStream.close();

			// Make sure the connector tried adding the new file to the userfile.
			Mockito.verify(mockedUserFile, Mockito.times(1)).addFile(newFileLocation);
			Mockito.when(mockedUserFile.checkOwned(newFileLocation)).thenReturn(true);

			// Create the streams to use for the update and the update's response.
			byte[] updatedFileContent = "This is an updated file content".getBytes();
			ByteArrayOutputStream updateOutputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(updateOutputStream);
			inStream = byteArrayToDataStream(updatedFileContent);

			// The update
			JsonObject updateRequest = new JsonObject();
			updateRequest.addProperty(Actions.Properties.LOCATION, newFileLocation);
			assertTrue(connector.updateFile(inStream, updateRequest, out));
			inStream.close();

			// Create the stream to use for the download's response.
			ByteArrayOutputStream downloadOutputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(downloadOutputStream);

			// The download request
			JsonObject fileRequest = new JsonObject();
			fileRequest.addProperty(Actions.Properties.LOCATION, newFileLocation);
			connector.downloadFile(fileRequest, out);

			// First read the JsonResponse to see if the request is successful
			in = new DataInputStream(new ByteArrayInputStream(downloadOutputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"" + Responses.SUCCESSFUL + "\":true"));

			// Read the rest of the stream to check if the file was correctly downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertArrayEquals(updatedFileContent, byteDataStreamToByteArray(fileResponseStream));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests the keyfile upload
	 */
	@Test
	public void testKeyFileUpdate() {
		try {
			// Create a keyfile that belongs to the user.
			String keyFileLocation = "UploadKeyFile";
			File keyFile = new File(testDataDir, keyFileLocation);
			assertFalse(keyFile.exists());
			keyFile.createNewFile();
			Mockito.when(mockedUserFile.getKeyFileLocation()).thenReturn(keyFileLocation);

			// Fill the keyfile with some default content
			OutputStream existingKeyFileOut = new FileOutputStream(keyFile);
			existingKeyFileOut.write(KEYFILECONTENT);
			existingKeyFileOut.flush();
			existingKeyFileOut.close();

			// Create an empty file to use for the temporary keyfile and make the 'createFile' return that file.
			String tempFileLocation = "TemporaryKeyFile";
			File tempKeyFile = new File(testDataDir, tempFileLocation);
			assertFalse(tempKeyFile.exists());
			tempKeyFile.createNewFile();
			PowerMockito.stub(PowerMockito.method(FileManager.class, "createFile")).toReturn(tempFileLocation);

			// Create the streams to use for the update and the update's response.
			byte[] updatedKeyFileContent = "This is an updated keyfile content".getBytes();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			InputStream inStream = byteArrayToDataStream(updatedKeyFileContent);

			// Update the keyfilefile
			assertTrue(connector.updateKeyFile(inStream, out));
			inStream.close();

			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertArrayEquals(updatedKeyFileContent, Files.readAllBytes(keyFile.toPath()));
			assertTrue(response.contains("\"" + Responses.SUCCESSFUL + "\":true"));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests if the temporary file gets deleted when uploading the keyfile
	 */
	@Test
	public void testKeyFileUpdateTempDeleted() {
		try {
			// Create a keyfile that belongs to the user.
			String keyFileLocation = "UploadTempKeyFile";
			File keyFile = new File(testDataDir, keyFileLocation);
			assertFalse(keyFile.exists());
			keyFile.createNewFile();
			Mockito.when(mockedUserFile.getKeyFileLocation()).thenReturn(keyFileLocation);

			// Fill the keyfile with some default content
			OutputStream existingKeyFileOut = new FileOutputStream(keyFile);
			existingKeyFileOut.write(KEYFILECONTENT);
			existingKeyFileOut.flush();
			existingKeyFileOut.close();

			// Create the streams to use for the update and the update's response.
			byte[] updatedKeyFileContent = "This is an updated keyfile content".getBytes();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			InputStream inStream = byteArrayToDataStream(updatedKeyFileContent);

			// Update the keyfilefile
			assertTrue(connector.updateKeyFile(inStream, out));
			inStream.close();

			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertArrayEquals(updatedKeyFileContent, Files.readAllBytes(keyFile.toPath()));
			assertTrue(response.contains("\"" + Responses.SUCCESSFUL + "\":true"));

			File[] lockFiles = testDataDir.listFiles(TEMPFILTER);
			for (File file : lockFiles) {
				fail("The temporary file " + file.getName() + " didn't get deleted after the update.");
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests the keyfile download
	 */
	@Test
	public void testKeyFileDownload() {
		try {
			// Create a keyfile that belongs to the user.
			String keyFileLocation = "DownloadKeyFile";
			File keyFile = new File(testDataDir, keyFileLocation);
			assertFalse(keyFile.exists());
			keyFile.createNewFile();
			Mockito.when(mockedUserFile.getKeyFileLocation()).thenReturn(keyFileLocation);

			// Fill the keyfile with some default content
			OutputStream existingKeyFileOut = new FileOutputStream(keyFile);
			existingKeyFileOut.write(KEYFILECONTENT);
			existingKeyFileOut.flush();
			existingKeyFileOut.close();

			// Create the stream to use for the download's response.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);
			assertTrue(connector.downloadKeyFile(out));

			// First read the JsonResponse to see if the request is successful
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"" + Responses.SUCCESSFUL + "\":true"));

			// Read the rest of the stream to check if the file was correctly downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertArrayEquals(KEYFILECONTENT, byteDataStreamToByteArray(fileResponseStream));

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tears down the test class by clearing the test folder.
	 */
	@AfterClass
	public static void tearDown() {
		try {
			Thread.sleep(1000);
			FileUtils.deleteDirectory(testDataDir);
		} catch (Exception e) {
			fail("Unexpected error in tearDown: " + e.getMessage());
		}
	}
}
