package org.fides.server.files;

import java.io.File;
import java.util.Collection;
import java.util.GregorianCalendar;

public class UserFile {

	private String username;

	private String passwordHash;

	private Collection<File> userFiles;

	private File keyFile;

	private GregorianCalendar lastRefreshed;

	public boolean checkOwned(String location) {
		return false;
	}

	public void addFile(String location) {

	}

	public void removeFile(String location) {

	}

}
