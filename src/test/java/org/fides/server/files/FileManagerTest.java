package org.fides.server.files;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The JUnit Test Case for the FileManager
 */
public class FileManagerTest {

	private static final String BASEPATH = "C:/Temp/Fides/";

	private static final String BASEPATH2 = "C:/Temp/Fides";
	
	private static final byte[] MESSAGE = ("DEFAULT MESSAGE: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore "
		+ "et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. "
		+ "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non "
		+ "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.").getBytes();

	private static final String DEFAULTEMPTYFILELOCATION = "defaultEmptyFile.txt";
	
	private static final String DEFAULTFILELOCATION = "defaultFile.txt";

	/**
	 * Sets up the test class by adding a temporary file.
	 */
	@BeforeClass
	public static void setUp() {
		try {
			File emptyFile = new File(BASEPATH, DEFAULTEMPTYFILELOCATION);
			emptyFile.createNewFile();
			
			File defaultFile = new File(BASEPATH, DEFAULTFILELOCATION);
			FileOutputStream outputStream = new FileOutputStream(defaultFile);
			outputStream.write(MESSAGE);
			outputStream.close();
		} catch (Exception e) {
			fail("Unexpected error in setUp: " + e.getMessage());
		}
	}

	/**
	 * Tests if the constructor of FileManager correctly creates a new directory
	 * if it doesn't exist already.
	 */
	@Test
	public void testConstructor() {
		String testPath = BASEPATH + "TestFolderCreate";

		// Test if the fileManager created the testfolder
		Path path = Paths.get(testPath);
		assertTrue(Files.exists(path));
	}

	/**
	 * Tests if the manager correctly creates a new file.
	 */
	@Test
	public void testCreateFile() {
		try {
			String fileName = FileManager.createFile();

			// Check if the file was created
			assertTrue(Files.exists(Paths.get(BASEPATH, fileName)));
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests if a file is still correctly created if the basepath has no slash at the end.
	 */
	/* Not sure if this test has to exist
	@Test
	public void testCreateFileWithoutSlash() {
		try {
			FileManager fileManager = new FileManager(BASEPATH2);
			String fileName = fileManager.createFile();

			// Check if the file was created
			assertTrue(Files.exists(Paths.get(BASEPATH, fileName)));
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}
	*/

	/**
	 * Tests if updating an empty file fills the file with an inputstream.
	 */
	@Test
	public void testUpdateEmptyFile() {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(MESSAGE);
			FileManager.updateFile(inputStream, DEFAULTEMPTYFILELOCATION);
			assertArrayEquals(MESSAGE, FileUtils.readFileToByteArray(new File(BASEPATH, DEFAULTEMPTYFILELOCATION)));
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests if updating a file which already has content overwrites the original content with the new inputstream.
	 */
	@Test
	public void testUpdateFile() {
		try {
			byte[] newMessage = "This file has been updated correctly.".getBytes();
			ByteArrayInputStream inputStream = new ByteArrayInputStream(newMessage);
			FileManager.updateFile(inputStream, DEFAULTFILELOCATION);
			assertArrayEquals(newMessage, FileUtils.readFileToByteArray(new File(BASEPATH, DEFAULTFILELOCATION)));
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tears down the test class by removing the temporary folder.
	 */
	@AfterClass
	public static void tearDown() {
		File dir = new File(BASEPATH);
		try {
			FileUtils.deleteDirectory(dir);
		} catch (Exception e) {
			fail("Unexpected error in tearDown: " + e.getMessage());
		}
	}

}
