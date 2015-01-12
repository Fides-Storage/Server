package org.fides.server.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * The tests for the Cleaner class.
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesManager.class)
public class CleanerTest {

	/**
	 * A mocked PropertiesManager which should always return the test User Directory
	 */
	private static PropertiesManager mockedPropertiesManager = Mockito.mock(PropertiesManager.class);

	/**
	 * The test User Directory
	 */
	private static File testUserDir;

	/**
	 * The test Data Directory
	 */
	private static File testDataDir;

	/**
	 * Sets up the test class by creating the test User Directory and mocking the getUserDir function.
	 * 
	 * @throws IOException
	 */
	@BeforeClass
	public static void setUp() throws IOException {
		testUserDir = new File(PropertiesManager.getInstance().getUserDir(), "Test");
		if (!testUserDir.exists()) {
			assertTrue(testUserDir.mkdirs());
		}
		// This causes the mocked PropertiesManager to always return the test Data directory:
		Mockito.when(mockedPropertiesManager.getUserDir()).thenReturn(testUserDir.getAbsolutePath());

		testDataDir = new File(PropertiesManager.getInstance().getDataDir(), "Test");
		if (!testDataDir.exists()) {
			assertTrue(testDataDir.mkdirs());
		}
		// This causes the mocked PropertiesManager to always return the test Data directory:
		Mockito.when(mockedPropertiesManager.getDataDir()).thenReturn(testDataDir.getAbsolutePath());

		// Mock the months with 2
		Mockito.when(mockedPropertiesManager.getExpirationTimeInMonths()).thenReturn(2);

	}

	/**
	 * Mocks the PropertiesManager to always return a mocked version of the PropertiesManager.
	 * 
	 * @throws IOException
	 */
	@Before
	public void setUpMock() throws IOException {
		PowerMockito.mockStatic(PropertiesManager.class);
		Mockito.when(PropertiesManager.getInstance()).thenReturn(mockedPropertiesManager);
	}

	/**
	 * Tests cleaner of outdated files
	 */
	@Test
	public void testRemoveOlderFiles() {
		try {
			Calendar calendar = Calendar.getInstance();
			GregorianCalendar olderMonth = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
			olderMonth.add(GregorianCalendar.MONTH, -4);

			File testOlderDataFile = new File(testDataDir, "older.txt");
			testOlderDataFile.createNewFile();
			testOlderDataFile.setLastModified(olderMonth.getTimeInMillis());

			File testOlderUserFile = new File(testUserDir, "older.txt");
			testOlderUserFile.createNewFile();
			testOlderUserFile.setLastModified(olderMonth.getTimeInMillis());

			Cleaner cleaner = new Cleaner();
			cleaner.cleanOutdatedFiles();

			assertFalse(testOlderDataFile.exists());
			assertFalse(testOlderUserFile.exists());

		} catch (IOException e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}

	}

	/**
	 * Tests cleaner of up to date files
	 */
	@Test
	public void testDontRemoveUpToDateFiles() {
		try {
			Calendar calendar = Calendar.getInstance();
			GregorianCalendar currentMonth = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

			File testDataFile = new File(testDataDir, "older.txt");
			testDataFile.createNewFile();
			testDataFile.setLastModified(currentMonth.getTimeInMillis());

			File testUserFile = new File(testUserDir, "older.txt");
			testUserFile.createNewFile();
			testUserFile.setLastModified(currentMonth.getTimeInMillis());

			Cleaner cleaner = new Cleaner();
			cleaner.cleanOutdatedFiles();

			assertTrue(testDataFile.exists());
			assertTrue(testUserFile.exists());

		} catch (IOException e) {
			fail("An unexpected exception has occured: " + e.getMessage());
		}

	}
}
