/*
 * Created on 19.04.2006
 */
package ru.myx.srv.download;

import java.io.File;
import java.io.IOException;

import ru.myx.ae3.act.Act;
import ru.myx.ae3.report.Report;

class HungDetector implements Runnable {

	private static final Runnable RESTARTER = new TaskRestarter();
	
	boolean done = false;
	
	private boolean first = true;
	
	private final File target;
	
	HungDetector(final long delay, final File target) {

		this.target = target;
		Act.later(null, this, delay);
	}
	
	@Override
	public void run() {

		if (!this.done) {
			if (this.first) {
				Report.warning("DLS/SHARE/HD", "Hung preview detected, wait another 60 seconds. Target=" + this.target.getAbsolutePath());
				this.first = false;
				Act.later(null, this, 60_000L);
			} else {
				Report.warning("DLS/SHARE/HD", "Hung preview detected, making empty preview and restart.");
				try {
					this.target.createNewFile();
				} catch (final IOException e) {
					Report.exception("DLS/SHARE/HD", "Can't make empty preview", e);
				}
				Act.later(null, HungDetector.RESTARTER, 10_000L);
			}
		}
	}
}
