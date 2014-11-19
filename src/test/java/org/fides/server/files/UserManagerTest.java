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
import org.junit.Test;

/**
 * 
 * @author Niels and Jesse
 * 
 */
public class UserManagerTest {

	private static final File USERDIR = new File(PropertiesManager.getInstance().getUserDir());

	/**
	 * Tests whether the is created at the given path
	 */
	@Test
	public void testSaveUserFile() {
		UserFile uf = new UserFile("Pietje", "passwordHash");
		uf.addFile("testFile");

		try {
			assertTrue(Files.exists(Paths.get(USERDIR.getCanonicalPath(), "Pietje")));
		} catch (IOException e) {
			fail("IOException has occured: " + e.getMessage());
		}
	}

	/**
	 * Tests whether the file can correctly be opened
	 */
	@Test
	public void testUnlockUserFile() {
		UserFile uf = new UserFile("Henk", "passwordHash");
		uf.addFile("testFile");

		UserFile loadedFile = UserManager.unlockUserFile("Henk", "passwordHash");
		assertNotNull(loadedFile);

		assertEquals("Henk", loadedFile.getUsername());
		assertTrue(loadedFile.checkOwned("testFile"));
	}

	/**
	 * Tears down the test class by clearing the test folder.
	 */
	@AfterClass
	public static void tearDown() {
		try {
			FileUtils.cleanDirectory(USERDIR);
		} catch (Exception e) {
			fail("Unexpected error in tearDown: " + e.getMessage());
		}
	}

}
