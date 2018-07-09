/**
 *
 */
package ru.myx.srv.download.client;

import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/**
 * @author myx
 *
 */
final class RequestByGuid extends RequestAttachment<RecFile, RunnerDatabaseRequestor> {
	
	private final DownloadClient parent;
	
	private final String key;
	
	RequestByGuid(final DownloadClient parent, final String key) {
		this.parent = parent;
		this.key = key;
	}
	
	@Override
	public final RecFile apply(final RunnerDatabaseRequestor ctx) {

		final RecFile result = this.parent.searchByGuid(this.key, ctx.ctxGetConnection());
		this.setResult(result);
		return result;
	}
	
	@Override
	public final String getKey() {

		return "gd-" + this.key;
	}
	
}
