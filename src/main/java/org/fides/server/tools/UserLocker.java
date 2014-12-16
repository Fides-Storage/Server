package org.fides.server.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * A static tool that can be used to lock a user on the server. TODO: Test
 */
public final class UserLocker {

	private static final FilenameFilter LOCKFILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".lock");
		}
	};

	private UserLocker() {
	}

	/**
	 * Tries to lock the user with the given username
	 * 
	 * @param username
	 *            The user that tries to be locked
	 * @return Whether the lock was possible and successful
	 */
	public static boolean lock(String username) {
		try {
			File lockFile = new File(PropertiesManager.getInstance().getUserDir(), username + ".lock");
			return lockFile.createNewFile();
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Removes the lock of the user with the given username
	 * 
	 * @param username
	 *            The user that can be unlocked
	 */
	public static void unlock(String username) {
		File lockFile = new File(PropertiesManager.getInstance().getUserDir(), username + ".lock");
		lockFile.delete();
	}

	/**
	 * Clears all the available locks on the server. Should only be called on server startup.
	 */
	public static void clearAllLocks() {
		File directory = new File(PropertiesManager.getInstance().getUserDir());
		File[] lockFiles = directory.listFiles(LOCKFILTER);
		for (File file : lockFiles) {
			file.delete();
		}
	}
}
