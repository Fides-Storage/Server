package org.fides.server.tools;

import java.io.File;
import java.io.IOException;

public class FileUtil {

	public static boolean isInFolder(File folder, File file) {
		try {
			File fileParent = file.getCanonicalFile();

			while (fileParent != null) {

				if (folder.equals(fileParent)) {
					return true;
				}

				fileParent = fileParent.getCanonicalFile();
			}

		} catch (IOException e) {
			return false;
		}

		return false;

	}
}
