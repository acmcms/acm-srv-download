/*
 * Created on 19.04.2006
 */
package ru.myx.srv.download;

import java.util.function.Function;

final class ServerSettngsRebuilderTask implements Function<DownloadServer, Object> {
	
	@Override
	public final Object apply(final DownloadServer server) {
		
		server.rebuildServerSettings();
		return null;
	}
}
