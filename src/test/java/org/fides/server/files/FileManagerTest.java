package org.fides.server.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.FileUtils;
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

/**
 * The JUnit Test Case for the FileManager
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesManager.class)
public class FileManagerTest {

	private static final byte[] MESSAGE = ("DEFAULT MESSAGE: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore "
		+ "et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. "
		+ "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non "
		+ "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.").getBytes();

	private static final String DEFAULTEMPTYFILELOCATION = "defaultEmptyFile.txt";

	private static final String DEFAULTFILELOCATION = "defaultFile.txt";

	private static final String DEFAULTREMOVEFILELOCATION = "defaultRemoveFile.txt";

	private static File testDataDir;

	private static PropertiesManager mockedPropertiesManager = Mockito.mock(PropertiesManager.class);

	/**
	 * Sets up the test class by adding a the necessary temporary files to the test folder.
	 */
	@BeforeClass
	public static void setUp() {
		try {
			testDataDir = new File(PropertiesManager.getInstance().getDataDir(), "Test");
			if (!testDataDir.exists()) {
				testDataDir.mkdirs();
			}
			// This causes the mocked PropertiesManager to always return the test Data directory:
			Mockito.when(mockedPropertiesManager.getDataDir()).thenReturn(testDataDir.getAbsolutePath());

			File emptyFile = new File(testDataDir, DEFAULTEMPTYFILELOCATION);
			emptyFile.createNewFile();

			File defaultFile = new File(testDataDir, DEFAULTFILELOCATION);
			FileOutputStream outputStream = new FileOutputStream(defaultFile);
			outputStream.write(MESSAGE);
			outputStream.close();

			File removeFile = new File(testDataDir, DEFAULTREMOVEFILELOCATION);
			outputStream = new FileOutputStream(removeFile);
			outputStream.write(MESSAGE);
			outputStream.close();
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
	 * Tests if the manager correctly creates a new file.
	 */
	@Test
	public void testCreateFile() {
		try {
			String fileName = FileManager.createFile();

			// Check if the file was created
			assertTrue(Files.exists(Paths.get(testDataDir.getCanonicalPath(), fileName)));
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests the removing of an existing file.
	 */
	@Test
	public void testRemoveFile() {
		try {
			assertTrue(Files.exists(Paths.get(testDataDir.getCanonicalPath(), DEFAULTREMOVEFILELOCATION)));
			assertTrue(FileManager.removeFile(DEFAULTREMOVEFILELOCATION));
			assertFalse(Files.exists(Paths.get(testDataDir.getCanonicalPath(), DEFAULTREMOVEFILELOCATION)));
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
	 * Tests if the FileManager can create a file ending with '.tmp'
	 */
	@Test
	public void testCreateTemporaryFile() {
		try {
			String fileName = FileManager.createFile(true);

			// Check if the file was created
			assertTrue(Files.exists(Paths.get(testDataDir.getCanonicalPath(), fileName)));
			assertTrue(fileName.endsWith(".tmp"));
		} catch (Exception e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests if the createFile with a non-temporary file doesn't end with '.tmp'
	 */
	@Test
	public void testCreateNonTemporaryFile() {
		try {
			String fileName = FileManager.createFile(false);

			// Check if the file was created
			assertTrue(Files.exists(Paths.get(testDataDir.getCanonicalPath(), fileName)));
			assertFalse(fileName.endsWith(".tmp"));
		} catch (Exception e) {
		}
	}

	/**
	 * Tests if touch file is correct
	 */
	@Test
	public void testTouchFile() {
		try {
			File testFile = new File(testDataDir, "touch.txt");
			testFile.createNewFile();

			FileManager.touchFile(testFile);

			Calendar currentTime = Calendar.getInstance();
			currentTime.setTime(new Date(testFile.lastModified()));

			assertEquals(Calendar.getInstance().get(Calendar.YEAR), currentTime.get(Calendar.YEAR));
			assertEquals(Calendar.getInstance().get(Calendar.MONTH), currentTime.get(Calendar.MONTH));
			assertEquals(1, currentTime.get(Calendar.DAY_OF_MONTH));
			assertEquals(0, currentTime.get(Calendar.HOUR_OF_DAY));
			assertEquals(0, currentTime.get(Calendar.MINUTE));

		} catch (IOException e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}
	}

	/**
	 * Tears down the test class by clearing the test folder.
	 */
	@AfterClass
	public static void tearDown() {
		try {
			FileUtils.deleteDirectory(testDataDir);
		} catch (Exception e) {
			fail("Unexpected error in tearDown: " + e.getMessage());
		}
	}

}
