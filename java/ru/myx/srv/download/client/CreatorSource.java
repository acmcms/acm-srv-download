/*
 * Created on 24.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client;

import ru.myx.ae3.cache.CreationHandlerObject;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/** @author myx
 *
 *         Window - Preferences - Java - Code Style - Code Templates */
final class CreatorSource implements CreationHandlerObject<RunnerDatabaseRequestor, RecSource> {
	
	private final DownloadClient client;

	CreatorSource(final DownloadClient client) {
		
		this.client = client;
	}

	@Override
	public final RecSource create(final RunnerDatabaseRequestor attachment, final String key) {
		
		final int luid = Integer.parseInt(key);
		if (luid <= 0) {
			return null;
		}
		final RequestSource request = new RequestSource(this.client, luid);
		attachment.add(request);
		return request.baseValue();
	}

	@Override
	public final long getTTL() {
		
		return 15L * 60_000L;
	}
}
