package org.fides.server.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class UserLocker {

	private static final FilenameFilter LOCKFILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".lock");
		}
	};

	public static boolean lock(String username) {
		try {
			File lockFile = new File(PropertiesManager.getInstance().getUserDir(), username + ".lock");
			boolean locked = lockFile.createNewFile();
			if (locked) {
				System.out.println("=== USERFILE LOCKED ===");
			}
			return locked;
		} catch (IOException e) {
			return false;
		}
	}

	public static void unlock(String username) {
		File lockFile = new File(PropertiesManager.getInstance().getUserDir(), username + ".lock");
		System.out.println("=== USERFILE UNLOCKED " + lockFile.delete() + " ===");
	}

	public static void clearAllLocks() {
		File directory = new File(PropertiesManager.getInstance().getUserDir());
		File[] lockFiles = directory.listFiles(LOCKFILTER);
		for (File file : lockFiles) {
			file.delete();
		}
	}
}
