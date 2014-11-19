package org.fides.server.files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * The JUnit Test Case for the UserFile
 * 
 * @author Niels and Jesse
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(UserManager.class)
public class UserFileTest {

	/**
	 * Disables the static UserManager to prevent the creation of userfiles.
	 */
	@Before
	public void disableUserManager() {
		PowerMockito.mockStatic(UserManager.class);
		Mockito.when(UserManager.saveUserFile(Mockito.any(UserFile.class))).thenReturn(true);
	}
	
	/**
	 * Checks whether the user file doesn't contain the given data
	 */
	@Test
	public void testUserFilesDontContainGivenFile() {
		UserFile userFile = new UserFile("userName1", "passwordHash");
		assertFalse(userFile.checkOwned("testLocation"));
	}

	/**
	 * Checks whether the user file contains the added file
	 */
	@Test
	public void testUserFilesContainsGivenFile() {
		UserFile userFile = new UserFile("userName2", "passwordHash");
		userFile.addFile("testlocation");
		assertTrue(userFile.checkOwned("testlocation"));
	}

	/**
	 * Checks whether the user file can be updated to remove a file
	 */
	@Test
	public void testRemoveUserFile() {
		UserFile userFile = new UserFile("userName3", "passwordHash");

		userFile.addFile("testlocation");
		userFile.removeFile("testlocation");
		assertFalse(userFile.checkOwned("testlocation"));
	}

}
