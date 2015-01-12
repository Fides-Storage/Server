package org.fides.server.tools;

import java.util.TimerTask;

/**
 * A {@link Cleaner} which periodical check for files to delete
 *
 */
public class CleanerTask extends TimerTask {

	private Cleaner cleaner = new Cleaner();

	@Override
	public void run() {
		cleaner.cleanOutdatedFiles();
	}
}
