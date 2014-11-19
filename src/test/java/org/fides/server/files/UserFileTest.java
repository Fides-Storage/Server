package org.fides.server.files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 
 * @author Niels and Jesse
 * 
 */
public class UserFileTest {

	/**
	 * Checks whether the user file doesn't contain the given data
	 */
	@Test
	public void testUserFilesDontContainGivenFile() {
		UserFile uf = new UserFile("pietje", "passwordHash");
		assertFalse(uf.checkOwned("testLocation"));
	}

	/**
	 * Checks whether the user file contains the added file
	 */
	@Test
	public void testUserFilesContainsGivenFile() {
		UserFile uf = new UserFile("Pietje", "passwordHash");
		uf.addFile("testlocation");
		assertTrue(uf.checkOwned("testlocation"));

	}

	/**
	 * Checks whether the user file can be updated to remove a file
	 */
	@Test
	public void testRemoveUserFile() {
		UserFile uf = new UserFile("pietje", "passwordHash");

		uf.addFile("testlocation");
		uf.removeFile("testlocation");
		assertFalse(uf.checkOwned("testlocation"));
	}

}
