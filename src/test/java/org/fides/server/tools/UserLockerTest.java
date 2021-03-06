package org.fides.server.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * The tests for the UserLocker class.
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesManager.class)
@PowerMockIgnore("javax.management.*")
public class UserLockerTest {

	/**
	 * A mocked PropertiesManager which should always return the test User Directory
	 */
	private static final PropertiesManager MOCKED_PROPERTIES_MANAGER = Mockito.mock(PropertiesManager.class);

	/**
	 * The test User Directory
	 */
	private static File testUserDir;

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
		Mockito.when(MOCKED_PROPERTIES_MANAGER.getUserDir()).thenReturn(testUserDir.getAbsolutePath());
	}

	/**
	 * Mocks the PropertiesManager to always return a mocked version of the PropertiesManager.
	 * 
	 * @throws IOException
	 */
	@Before
	public void setUpMock() throws IOException {
		PowerMockito.mockStatic(PropertiesManager.class);
		Mockito.when(PropertiesManager.getInstance()).thenReturn(MOCKED_PROPERTIES_MANAGER);
	}

	/**
	 * Tests if a lockfile is created correctly
	 */
	@Test
	public void testLockCreateLockfile() {
		String username = "testLockCreateLockfile";
		assertTrue(UserLocker.lock(username));
		File testLockFile = new File(testUserDir, username.concat(".lock"));
		assertTrue(testLockFile.exists());
	}

	/**
	 * Tests if a lockfile is removed correctly
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRemoveLockFile() throws IOException {
		String username = "testRemoveLockFile";
		File testLockFile = new File(testUserDir, username.concat(".lock"));
		assertTrue(testLockFile.createNewFile());
		assertTrue(testLockFile.exists());
		UserLocker.unlock(username);
		assertFalse(testLockFile.exists());
	}

	/**
	 * Tests if a lockfile blocks another lockfile request correctly
	 * 
	 * @throws IOException
	 */
	@Test
	public void testBlockingLockFile() throws IOException {
		String username = "testBlockingLockFile";
		File testLockFile = new File(testUserDir, username.concat(".lock"));
		assertTrue(testLockFile.createNewFile());
		assertTrue(testLockFile.exists());
		assertFalse(UserLocker.lock(username));
	}

	/**
	 * Tests if adding and then removing a lockfile works correctly
	 */
	@Test
	public void testAddAndRemoveLockFile() {
		String username = "testAddAndRemoveLockFile";
		assertTrue(UserLocker.lock(username));
		File testLockFile = new File(testUserDir, username.concat(".lock"));
		assertTrue(testLockFile.exists());
		UserLocker.unlock(username);
		assertFalse(testLockFile.exists());
	}

	/**
	 * Tests if trying to add the same lockfile twice and then removing it works correctly
	 */
	@Test
	public void testAddBlockAndRemoveLockFile() {
		String username = "testAddBlockAndRemoveLockFile";
		assertTrue(UserLocker.lock(username));
		assertFalse(UserLocker.lock(username));
		File testLockFile = new File(testUserDir, username.concat(".lock"));
		assertTrue(testLockFile.exists());
		UserLocker.unlock(username);
		assertFalse(testLockFile.exists());
	}

	/**
	 * Tests if all the locks get removed with clearAllLocks
	 * 
	 * @throws IOException
	 */
	@Test
	public void testClearAllLocks() throws IOException {
		String username = "testClearAllLocks";
		String username2 = "testClearAllLocks2";
		File testLockFile = new File(testUserDir, username.concat(".lock"));
		File testLockFile2 = new File(testUserDir, username2.concat(".lock"));
		assertTrue(testLockFile.createNewFile());
		assertTrue(testLockFile2.createNewFile());
		assertTrue(testLockFile.exists());
		assertTrue(testLockFile2.exists());
		UserLocker.clearAllLocks();
		assertFalse(testLockFile.exists());
		assertFalse(testLockFile2.exists());
	}

	/**
	 * Tests if only lockfiles get removed with clearAllLocks
	 * 
	 * @throws IOException
	 */
	@Test
	public void testClearAllLocksKeepsUsers() throws IOException {
		String username = "testClearAllLocks";
		String username2 = "testClearAllLocks2";
		File testLockFile = new File(testUserDir, username.concat(".lock"));
		File testUserFile = new File(testUserDir, username);
		File testUserFile2 = new File(testUserDir, username2);
		assertTrue(testLockFile.createNewFile());
		assertTrue(testUserFile.createNewFile());
		assertTrue(testUserFile2.createNewFile());
		assertTrue(testLockFile.exists());
		assertTrue(testUserFile.exists());
		assertTrue(testUserFile2.exists());
		UserLocker.clearAllLocks();
		assertFalse(testLockFile.exists());
		assertTrue(testUserFile.exists());
		assertTrue(testUserFile2.exists());
	}

	/**
	 * Tests if the user is locked
	 */
	@Test
	public void testIsLocked() {
		String usernameLocked = "testUsernameLocked";
		UserLocker.lock(usernameLocked);
		assertTrue(UserLocker.isLocked(usernameLocked));
	}

	/**
	 * Tests if the user is not locked
	 */
	@Test
	public void testIsNotLocked() {
		String usernameNotLocked = "testUsernameNotLocked";
		assertFalse(UserLocker.isLocked(usernameNotLocked));
	}

	/**
	 * Tests if the user is unlocked
	 */
	@Test
	public void testIsUnlocked() {
		String usernameUnLocked = "testUsernameUnLocked";
		UserLocker.lock(usernameUnLocked);
		UserLocker.unlock(usernameUnLocked);
		assertFalse(UserLocker.isLocked(usernameUnLocked));
	}

	/**
	 * Empties the test folder to clear all locks
	 */
	@After
	public void cleanTestFolder() {
		try {
			FileUtils.cleanDirectory(testUserDir);
		} catch (Exception e) {
			fail("Unexpected error in tearDown: " + e.getMessage());
		}
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
