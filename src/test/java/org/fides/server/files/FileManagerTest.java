package org.fides.server.files;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.fides.server.tools.PropertiesManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The JUnit Test Case for the FileManager
 */
public class FileManagerTest {

	private static final byte[] MESSAGE = ("DEFAULT MESSAGE: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore "
		+ "et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. "
		+ "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non "
		+ "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.").getBytes();
	
	private static final String DEFAULTEMPTYFILELOCATION = "defaultEmptyFile.txt";

	private static final String DEFAULTFILELOCATION = "defaultFile.txt";

	private static final String DEFAULTREMOVEFILELOCATION = "defaultRemoveFile.txt";
	
	private static final File DATADIR = new File(PropertiesManager.getInstance().getDataDir());
	
	/**
	 * Sets up the test class by adding a the necessary temporary files to the test folder.
	 */
	@BeforeClass
	public static void setUp() {
		try {
			File emptyFile = new File(DATADIR, DEFAULTEMPTYFILELOCATION);
			emptyFile.createNewFile();

			File defaultFile = new File(DATADIR, DEFAULTFILELOCATION);
			FileOutputStream outputStream = new FileOutputStream(defaultFile);
			outputStream.write(MESSAGE);
			outputStream.close();

			File removeFile = new File(DATADIR, DEFAULTREMOVEFILELOCATION);
			outputStream = new FileOutputStream(removeFile);
			outputStream.write(MESSAGE);
			outputStream.close();
		} catch (Exception e) {
			fail("Unexpected error in setUp: " + e.getMessage());
		}
	}

	/**
	 * Tests if the manager correctly creates a new file.
	 */
	@Test
	public void testCreateFile() {
		try {
			String fileName = FileManager.createFile();

			// Check if the file was created
			assertTrue(Files.exists(Paths.get(DATADIR.getCanonicalPath(), fileName)));
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests if updating an empty file fills the file with an inputstream.
	 */
	@Test
	public void testUpdateEmptyFile() {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(MESSAGE);
			assertTrue(FileManager.updateFile(inputStream, DEFAULTEMPTYFILELOCATION));
			assertArrayEquals(MESSAGE, FileUtils.readFileToByteArray(new File(DATADIR, DEFAULTEMPTYFILELOCATION)));
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
			assertTrue(FileManager.updateFile(inputStream, DEFAULTFILELOCATION));
			assertArrayEquals(newMessage, FileUtils.readFileToByteArray(new File(DATADIR, DEFAULTFILELOCATION)));
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests if updating a non-existing file returns false.
	 */
	@Test
	public void testUpdateNonExistingFile() {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(MESSAGE);
		assertFalse(FileManager.updateFile(inputStream, "nonExistingFile.txt"));
	}

	/**
	 * Tests the removing of an existing file.
	 */
	@Test
	public void testRemoveFile() {
		try {
			assertTrue(Files.exists(Paths.get(DATADIR.getCanonicalPath(), DEFAULTREMOVEFILELOCATION)));
			assertTrue(FileManager.removeFile(DEFAULTREMOVEFILELOCATION));
			assertFalse(Files.exists(Paths.get(DATADIR.getCanonicalPath(), DEFAULTREMOVEFILELOCATION)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tests if removing a non-existing file returns false.
	 */
	@Test
	public void testRemoveNonExistingFile() {
		assertFalse(FileManager.removeFile("nonExistingFile.txt"));
	}

	/**
	 * Tears down the test class by clearing the test folder.
	 */
	@AfterClass
	public static void tearDown() {
		try {
			FileUtils.cleanDirectory(DATADIR);
		} catch (Exception e) {
			fail("Unexpected error in tearDown: " + e.getMessage());
		}
	}

}
