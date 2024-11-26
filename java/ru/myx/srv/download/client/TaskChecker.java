/*
 * Created on 21.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client;

import ru.myx.ae3.act.Act;
import ru.myx.ae3.report.Report;
import ru.myx.jdbc.lock.Runner;

/** @author myx
 *
 *         Window - Preferences - Java - Code Style - Code Templates */
class TaskChecker implements Runner, Runnable {
	
	private static final int RETRY_MAX = 15;

	private final DownloadClient parent;

	private final RecSource source;

	private boolean started = false;

	private boolean first = true;

	private int retries = TaskChecker.RETRY_MAX;

	TaskChecker(final DownloadClient parent, final RecSource source) {
		
		this.parent = parent;
		this.source = source;
	}

	@Override
	public int getVersion() {
		
		return 7;
	}

	@Override
	public void run() {
		
		if (!this.started) {
			return;
		}
		try {
			Report.info("DL_CLIENT", "Check: " + this.source);
			if (this.parent.checkSource(this.source, this.first)) {
				this.retries = TaskChecker.RETRY_MAX;
				this.first = false;
			} else {
				if (--this.retries <= 0) {
					this.parent.interestCancel(this.source);
				}
			}
		} finally {
			if (this.started) {
				Act.later(null, this, 45_000L);
			}
		}
	}

	@Override
	public void start() {
		
		if (!this.started) {
			synchronized (this) {
				if (!this.started) {
					this.first = true;
					Act.later(null, this, 5_000L);
					this.started = true;
				}
			}
		}
	}

	@Override
	public void stop() {
		
		if (this.started) {
			this.started = false;
			this.parent.interestCancel(this.source);
		}
	}

	@Override
	public String toString() {
		
		return "DL_SERVER: source checker, source=" + this.source;
	}
}
