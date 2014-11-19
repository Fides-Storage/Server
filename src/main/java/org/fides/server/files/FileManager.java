package org.fides.server.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.fides.server.Server;

public final class FileManager {
	
	/**
	 * Creates a new file with a unique name.
	 * 
	 * @return the file's location.
	 * @throws IOException
	 */
	public static String createFile() throws IOException {
		String location = UUID.randomUUID().toString();
		File newFile = new File(Server.getDataDir(), location);
		while (!newFile.createNewFile()) {
			// TODO: Prevent infinite loop by giving a max tries.
			location = UUID.randomUUID().toString();
			newFile = new File(Server.getDataDir(), location);
		}
		return location;
	}

	public static boolean updateFile(InputStream instream, String location) {
		try {
			File file = new File(Server.getDataDir(), location);
			if (!file.exists()) {
				// The FileManager can't update a non-existing file.
				return false;
			}
			OutputStream fileStream = new FileOutputStream(file);
			IOUtils.copy(instream, fileStream);
			fileStream.close();
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Fill the file

		// Return if it succeeded
		return false;
	}

	public static boolean removeFile(String location) {
		// Delete the file
		return false;
	}

	public static void updateTimestamps(Collection<File> files) {

	}

}
