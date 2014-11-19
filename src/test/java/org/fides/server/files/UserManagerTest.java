package org.fides.server.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
 * The JUnit Test Case for the UserManager
 * 
 * @author Niels and Jesse
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesManager.class)
public class UserManagerTest {

	/** A mocked PropertiesManager which should always return the test User Directory */
	private static PropertiesManager mockedPropertiesManager = Mockito.mock(PropertiesManager.class);
	/** The test User Directory */
	private static File testUserDir;

	/**
	 * Sets up the test class by creating the test User Directory and mocking getUserDir function.
	 */
	@BeforeClass
	public static void setUp() {
		testUserDir = new File(PropertiesManager.getInstance().getUserDir(), "Test");
		if (!testUserDir.exists()) {
			testUserDir.mkdirs();
		}
		// This causes the mocked PropertiesManager to always return the test Data directory:
		Mockito.when(mockedPropertiesManager.getUserDir()).thenReturn(testUserDir.getAbsolutePath());
	}
	
	/**
	 * Mocks the PropertiesManager to always return a mocked version of the PropertiesManager.
	 * This will cause the FileManager to use a testfolder instead of the main folder.
	 * @throws IOException 
	 */
	@Before
	public void setUpMock() throws IOException {
		PowerMockito.mockStatic(PropertiesManager.class);
		Mockito.when(PropertiesManager.getInstance()).thenReturn(mockedPropertiesManager);
	}

	/**
	 * Tests whether the is created at the given path
	 */
	@Test
	public void testSaveUserFile() {
		String username = "User1";
		UserFile uf = new UserFile(username, "passwordHash");
		uf.addFile("testFile");

		try {
			assertTrue(Files.exists(Paths.get(testUserDir.getCanonicalPath(), username)));
		} catch (IOException e) {
			fail("IOException has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests whether the file can correctly be opened
	 */
	@Test
	public void testUnlockUserFile() {
		String username = "User2";
		String filename = "testFile";
		String password = "passwordHash";

		UserFile uf = new UserFile(username, password);
		uf.addFile(filename);

		UserFile loadedFile = UserManager.unlockUserFile(username, password);
		assertNotNull(loadedFile);

		assertEquals(username, loadedFile.getUsername());
		assertTrue(loadedFile.checkOwned(filename));
	}

	/**
	 * Tears down the test class by clearing the test folder.
	 */
	@AfterClass
	public static void tearDown() {
		try {
			FileUtils.deleteDirectory(testUserDir);
		} catch (Exception e) {
			fail("Unexpected error in tearDown: " + e.getMessage());
		}
	}

}
