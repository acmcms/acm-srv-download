/*
 * Created on 30.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.util.Map;
import java.util.function.Function;

import ru.myx.ae3.act.Act;
import ru.myx.ae3.report.Report;

/** @author myx
 *
 *         Window - Preferences - Java - Code Style - Code Templates */
class TaskMaintenance implements Function<DownloadServer, Object> {

	@Override
	public Object apply(final DownloadServer server) {

		try {
			final Map<String, Share> roots = server.getServerRoots();
			for (final Share share : roots.values()) {
				share.check();
			}
			Act.later(null, this, server, 30_000L);
		} catch (final Throwable t) {
			Report.exception("DLSMTN", "Error while doing maintenance", t);
			Act.later(null, this, server, 10_000L);
		}
		return null;
	}
}
