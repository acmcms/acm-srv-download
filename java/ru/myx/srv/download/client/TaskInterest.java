/*
 * Created on 20.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client;

import java.util.Arrays;
import java.util.List;

import ru.myx.ae3.act.Act;
import ru.myx.ae3.report.Report;
import ru.myx.jdbc.lock.Runner;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class TaskInterest implements Runner, Runnable {
	private final DownloadClient	parent;
	
	private final boolean			client;
	
	private boolean					started	= false;
	
	TaskInterest(final DownloadClient parent, final boolean client) {
		this.parent = parent;
		this.client = client;
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
			final List<RecSource> sourceList = Arrays.asList( this.parent.getSources( false ) );
			for (final RecSource source : sourceList) {
				final boolean check = source.check();
				if (source.isActive()) {
					if (check) {
						this.parent.avaibility( source, true );
					} else {
						this.parent.avaibility( source, false );
					}
				} else {
					this.parent.avaibility( source, false );
				}
				if (!this.client && source.isIndex()) {
					if (check) {
						this.parent.interestRegister( source );
					} else {
						this.parent.interestCancel( source );
					}
				} else {
					this.parent.interestCancel( source );
				}
			}
		} catch (final Throwable e) {
			Report.exception( "DL_CLIENT", "error while checking available hosts", e );
		} finally {
			if (this.started) {
				Act.later( null, this, 60000L );
			}
		}
	}
	
	@Override
	public void start() {
		if (!this.started) {
			synchronized (this) {
				if (!this.started) {
					Act.later( this.parent.getServer().getRootContext(), this, 1000L );
					this.started = true;
				}
			}
		}
	}
	
	@Override
	public void stop() {
		synchronized (this) {
			this.started = false;
		}
	}
	
	@Override
	public String toString() {
		return "DL_SERVER: interest checker";
	}
}
