/**
 *
 */
package ru.myx.srv.download.client;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/**
 * @author myx
 */
final class RequestKnownByFollowLink extends RequestAttachment<RecKnown, RunnerDatabaseRequestor> {

	private final DownloadClient parent;

	private final String key;

	private final String link;

	RequestKnownByFollowLink(final DownloadClient parent, final String link) {
		this.parent = parent;
		this.link = link;
		this.key = "kfl" + link;
	}

	@Override
	public final RecKnown apply(final RunnerDatabaseRequestor ctx) {
		
		try (final PreparedStatement ps = ctx.ctxGetConnection()
				.prepareStatement("SELECT guid,name FROM d1Known WHERE guid=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, this.link);
			try (final ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					final String guid = rs.getString(1);
					final String name = rs.getString(2);
					final RecKnown result = new RecKnown(this.parent, guid, name);
					this.setResult(result);
					return result;
				}
				this.setResult(null);
				return null;
			}
		} catch (final SQLException e) {
			throw new RuntimeException(this.getClass().getSimpleName(), e);
		}
	}

	@Override
	public final String getKey() {
		
		return this.key;
	}

}
