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
class CreatorFolder implements CreationHandlerObject<RunnerDatabaseRequestor, RecFolder> {
	
	private final DownloadClient client;

	CreatorFolder(final DownloadClient client) {
		
		this.client = client;
	}

	@Override
	public RecFolder create(final RunnerDatabaseRequestor attachment, final String key) {
		
		final int luid = Integer.parseInt(key);
		if (luid <= 0) {
			return null;
		}
		final RequestFolder request = new RequestFolder(this.client, luid);
		attachment.add(request);
		return request.baseValue();
	}

	@Override
	public long getTTL() {
		
		return 5L * 60_000L;
	}
}
