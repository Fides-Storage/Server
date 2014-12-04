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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fides.server.files.FileManager;
import org.fides.server.files.UserFile;
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

	private static final String KEYFILELOCATION = "KeyFile";

	private static final String KEYFILECONTENT = "This is the default keyfile content";

	private static final byte[] FILECONTENT = "This is the default normal file content".getBytes();

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

			File keyfile = new File(testDataDir, KEYFILELOCATION);
			FileUtils.writeStringToFile(keyfile, KEYFILECONTENT);
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
			in = new DataInputStream(new ByteArrayInputStream(FILECONTENT));

			// Upload the file
			assertTrue(connector.uploadFile(in, out));
			in.close();

			// Make sure the connector tried adding the new file to the userfile.
			Mockito.verify(mockedUserFile, Mockito.times(1)).addFile(newFileLocation);

			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertArrayEquals(FILECONTENT, Files.readAllBytes(newFile.toPath()));
			assertTrue(response.contains("\"successful\":true"));
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
			in = new DataInputStream(new ByteArrayInputStream(updatedFileContent));

			// The update
			JsonObject updateRequest = new JsonObject();
			updateRequest.addProperty("location", existingFileLocation);
			assertTrue(connector.updateFile(in, updateRequest, out));
			in.close();

			// Test if file contents changed
			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertArrayEquals(updatedFileContent, Files.readAllBytes(existingFile.toPath()));
			assertTrue(response.contains("\"successful\":true"));
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
			updateRequest.addProperty("location", "");
			assertFalse(connector.updateFile(in, updateRequest, out));
			in.close();

			// TODO: Use variables for errormessage
			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertTrue(response.contains("\"successful\":false"));
			assertTrue(response.contains("User didn't include a filelocation to upload to"));
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

			// TODO: Use variables for errormessage
			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertTrue(response.contains("\"successful\":false"));
			assertTrue(response.contains("User didn't include a filelocation to upload to"));
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
			updateRequest.addProperty("location", notOwnedFileLocation);
			assertFalse(connector.updateFile(in, updateRequest, out));
			in.close();

			// TODO: Use variables for errormessage
			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertTrue(response.contains("\"successful\":false"));
			assertTrue(response.contains("User doesn't own a file on that location"));

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
			updateRequest.addProperty("location", notExistingFileLocation);
			assertFalse(connector.updateFile(in, updateRequest, out));
			in.close();

			// Check if the correct error was returned
			// TODO: Use variables for errormessage
			String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8).replace("\\u0027", "'");
			assertTrue(response.contains("\"successful\":false"));
			assertTrue(response.contains("File could not be found on the server"));

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
			fileRequest.addProperty("location", downloadFileLocation);
			assertTrue(connector.downloadFile(fileRequest, out));

			// First read the JsonResponse to see if the request is successful
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"successful\":true"));

			// Read the rest of the stream to check if the file was correctly downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertArrayEquals(FILECONTENT, fileResponseStream.toByteArray());
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
			fileRequest.addProperty("location", "");
			assertFalse(connector.downloadFile(fileRequest, out));

			// First read the JsonResponse to see if the request was unsuccessful
			// TODO: Use variables for errormessage
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"successful\":false"));
			assertTrue(downloadResponse.toString().contains("User didn't include a file to download"));

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
			// TODO: Use variables for errormessage
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"successful\":false"));
			assertTrue(downloadResponse.toString().contains("User didn't include a file to download"));

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
			fileRequest.addProperty("location", notOwnedFileLocation);
			assertFalse(connector.downloadFile(fileRequest, out));

			// First read the JsonResponse to see if the request was unsuccessful
			// TODO: Use variables for errormessage
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"successful\":false"));
			assertTrue(downloadResponse.toString().contains("Requested file does not belong to the user"));

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
			fileRequest.addProperty("location", notExistingFileLocation);
			assertFalse(connector.downloadFile(fileRequest, out));

			// First read the JsonResponse to see if the request was unsuccessful
			// TODO: Use variables for errormessage
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"successful\":false"));
			assertTrue(downloadResponse.toString().contains("Requested file could not be found"));

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
			in = new DataInputStream(new ByteArrayInputStream(FILECONTENT));

			// Upload the file
			connector.uploadFile(in, out);
			in.close();

			// Make sure the connector tried adding the new file to the userfile.
			Mockito.verify(mockedUserFile, Mockito.times(1)).addFile(newFileLocation);
			Mockito.when(mockedUserFile.checkOwned(newFileLocation)).thenReturn(true);

			// Create the stream to use for the download's response.
			outputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(outputStream);

			// The download request
			JsonObject fileRequest = new JsonObject();
			fileRequest.addProperty("location", newFileLocation);
			connector.downloadFile(fileRequest, out);

			// First read the JsonResponse to see if the request is successful
			in = new DataInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"successful\":true"));

			// Read the rest of the stream to check if the file was correctly downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertArrayEquals(FILECONTENT, fileResponseStream.toByteArray());
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
			in = new DataInputStream(new ByteArrayInputStream(FILECONTENT));

			// Upload the file
			connector.uploadFile(in, out);
			in.close();

			// Make sure the connector tried adding the new file to the userfile.
			Mockito.verify(mockedUserFile, Mockito.times(1)).addFile(newFileLocation);
			Mockito.when(mockedUserFile.checkOwned(newFileLocation)).thenReturn(true);

			// Create the streams to use for the update and the update's response.
			byte[] updatedFileContent = "This is an updated file content".getBytes();
			ByteArrayOutputStream updateOutputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(updateOutputStream);
			in = new DataInputStream(new ByteArrayInputStream(updatedFileContent));

			// The update
			JsonObject updateRequest = new JsonObject();
			updateRequest.addProperty("location", newFileLocation);
			assertTrue(connector.updateFile(in, updateRequest, out));
			in.close();

			// Create the stream to use for the download's response.
			ByteArrayOutputStream downloadOutputStream = new ByteArrayOutputStream();
			out = new DataOutputStream(downloadOutputStream);

			// The download request
			JsonObject fileRequest = new JsonObject();
			fileRequest.addProperty("location", newFileLocation);
			connector.downloadFile(fileRequest, out);

			// First read the JsonResponse to see if the request is successful
			in = new DataInputStream(new ByteArrayInputStream(downloadOutputStream.toByteArray()));
			JsonObject downloadResponse = new Gson().fromJson(in.readUTF(), JsonObject.class);
			assertTrue(downloadResponse.toString().contains("\"successful\":true"));

			// Read the rest of the stream to check if the file was correctly downloaded
			ByteArrayOutputStream fileResponseStream = new ByteArrayOutputStream();
			IOUtils.copy(in, fileResponseStream);
			assertArrayEquals(updatedFileContent, fileResponseStream.toByteArray());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests the keyfile upload
	 */
	@Test
	public void testKeyFileUpload() {
		// Test for temporary file:
		// If it's created
		// If it's filled
		// If it's copied
		// If it's deleted
	}

	/**
	 * Tests if the keyfile upload fails correctly if the keyfile doesn't exist.
	 */
	@Test
	public void testKeyFileUploadNotExisting() {

	}

	/**
	 * Tests the keyfile download
	 */
	@Test
	public void testKeyFileDownload() {

	}

	/**
	 * Tests if the keyfile download fails correctly if the keyfile doesn't exist.
	 */
	@Test
	public void testKeyFileDownloadNotExisting() {

	}

	/**
	 * Test if the Keyfile Upload and Download work well together.
	 */
	@Test
	public void testKeyFilUploadDownload() {

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
