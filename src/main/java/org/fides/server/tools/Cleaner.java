package org.fides.server.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Cleaner for outdated files
 * 
 */
public class Cleaner {

	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(Cleaner.class);

	private boolean firstRun = true;

	/**
	 * Cleans the outdated files if it is the first run or first of the month
	 */
	public void cleanOutdatedFiles() {
		if (firstRun || Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 1) {

			log.trace("Start cleaner, outdatedFiles");

			long time = DateUtils.truncate(new Date(), Calendar.MONTH).getTime() - (2628000000L * (PropertiesManager.getInstance().getExpirationTimeInMonths() + 1));

			Collection<File> files = new ArrayList<File>();
			files.addAll(FileUtils.listFiles(new File(PropertiesManager.getInstance().getUserDir()), FileFilterUtils.ageFileFilter(time), FileFilterUtils.ageFileFilter(time)));
			files.addAll(FileUtils.listFiles(new File(PropertiesManager.getInstance().getDataDir()), FileFilterUtils.ageFileFilter(time), FileFilterUtils.ageFileFilter(time)));

			log.trace("Deleting " + files.size() + " files");
			for (File file : files) {
				file.delete();
			}
			log.trace("End cleaner, cleaned " + files.size() + " files");

			firstRun = false;
		} else {
			log.trace("Running cleaner is unnecessary");
		}
	}
}
