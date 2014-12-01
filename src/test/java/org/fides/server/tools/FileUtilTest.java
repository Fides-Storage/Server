package org.fides.server.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

/**
 * Test the FileUtil class
 * 
 * @author jesse
 *
 */
public class FileUtilTest {

	/**
	 * Test valid file location
	 */
	@Test
	public void testValidFile() {
		File folder = new File("./test/");
		File file = new File("./test/file");

		assertTrue(FileUtil.isInFolder(folder, file));

	}

	/**
	 * Test invalid file location
	 */
	@Test
	public void testInvalidFile() {
		File folder = new File("./test/");
		File file = new File("./test/");

		assertFalse(FileUtil.isInFolder(folder, file));

	}

	/**
	 * Test invalid file location with upper path
	 */
	@Test
	public void testInvalidFile2() {
		File folder = new File("./test/");
		File file = new File("./test/../file");

		assertFalse(FileUtil.isInFolder(folder, file));

	}
}
