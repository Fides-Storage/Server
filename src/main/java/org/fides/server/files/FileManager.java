package org.fides.server.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.UUID;

public class FileManager {

	private String basePath;

	/**
	 * Creates a new {@link FileManager} which manages the files in the folder specified by basepath. If basepath points
	 * to a non-existing directory, it gets created.
	 * 
	 * @param basePath
	 *            the path used by the {@link FileManager}
	 */
	public FileManager(String basePath)
	{
		this.basePath = basePath;
		File baseFolder = new File(basePath);
		if (!baseFolder.exists()) {
			baseFolder.mkdirs();
		}

		// If basepath doesn't exist, create the basepath.
	}

	/**
	 * Creates a new file with a unique name.
	 * 
	 * @return the file's location.
	 * @throws IOException
	 */
	public String createFile() throws IOException {
		String location = UUID.randomUUID().toString();
		File newFile = new File(basePath, location);
		while (!newFile.createNewFile()) {
			// TODO: Prevent infinite loop by giving a max tries.
			location = UUID.randomUUID().toString();
			newFile = new File(basePath + location);
		}
		return location;
	}

	public boolean updateFile(InputStream instream, String location) {
		try {
			File file = new File(basePath, location);
			OutputStream fileStream = new FileOutputStream(file);

			fileStream.close();
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

	public boolean removeFile(String location) {
		// Delete the file
		return false;
	}

	public void updateTimestamps(Collection<File> files) {

	}

}
