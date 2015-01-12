package org.fides.server.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

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
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ UserManager.class, FileManager.class })
public class UserFileTest {

	/**
	 * Disables the static UserManager to prevent the creation of userfiles.
	 */
	@Before
	public void disableUserManager() {
		PowerMockito.mockStatic(UserManager.class);
		Mockito.when(UserManager.saveUserFile(Mockito.any(UserFile.class))).thenReturn(true);

		PowerMockito.mockStatic(FileManager.class);
		String randomLocation = UUID.randomUUID().toString();
		Mockito.when(FileManager.createFile()).thenReturn(randomLocation);
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

	/**
	 * Tests touch file with last refreshed not this month
	 */
	@Test
	public void testRefreshedNotThisMonth() {
		Calendar calendar = Calendar.getInstance();
		GregorianCalendar thisMonth = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		GregorianCalendar olderMonth = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		olderMonth.add(GregorianCalendar.MONTH, -2);

		UserFile userFile = new UserFile("userName4", "passwordHash");

		userFile.setLastRefreshed(olderMonth);
		assertEquals(olderMonth, userFile.getLastRefreshed());

		userFile.touch();
		assertEquals(thisMonth, userFile.getLastRefreshed());

	}

	/**
	 * Tests touch file with last refreshed this month
	 */
	@Test
	public void testRefreshedThisMonth() {
		Calendar calendar = Calendar.getInstance();
		GregorianCalendar thisMonth = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

		UserFile userFile = new UserFile("userName5", "passwordHash");

		userFile.setLastRefreshed(thisMonth);
		assertEquals(thisMonth, userFile.getLastRefreshed());

		userFile.touch();
		assertEquals(thisMonth, userFile.getLastRefreshed());

	}
}
