package org.fides.server.tools;

import java.io.File;

/**
 * File utility
 * 
 * @author jesse
 *
 */
public class FileUtil {

	/**
	 * Test if file is inside folder
	 * 
	 * @param folder
	 *            the given folder to test
	 * @param file
	 *            the given file to test
	 * @return if file inside folder
	 */
	public static boolean isInFolder(File folder, File file) {

		folder = folder.getAbsoluteFile();
		file = file.getAbsoluteFile();

		return folder.equals(file.getParentFile());

	}
}
