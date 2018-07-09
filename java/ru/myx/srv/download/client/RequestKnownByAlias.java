/**
 *
 */
package ru.myx.srv.download.client;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/**
 * @author myx
 *
 */
final class RequestKnownByAlias extends RequestAttachment<RecKnown[], RunnerDatabaseRequestor> {

	private final DownloadClient parent;

	private final String key;

	private final String alias;

	RequestKnownByAlias(final DownloadClient parent, final String alias) {
		this.parent = parent;
		this.alias = alias;
		this.key = "kal" + alias;
	}

	@Override
	public final RecKnown[] apply(final RunnerDatabaseRequestor ctx) {
		
		final List<RecKnown> pending = new ArrayList<>();
		{
			final String query;
			if (this.alias == null) {
				query = "SELECT k.guid,k.name FROM d1Known k ORDER BY 2 ASC";
			} else {
				query = "SELECT DISTINCT k.guid,k.name FROM d1Known k, d1KnownAliases a WHERE k.guid=a.guid AND a.alias=? ORDER BY 2 ASC";
			}
			try (final PreparedStatement ps = ctx.ctxGetConnection().prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
				if (this.alias != null) {
					ps.setString(1, this.alias.toLowerCase());
				}
				try (final ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final String guid = rs.getString(1);
						final String name = rs.getString(2);
						final RecKnown known = new RecKnown(this.parent, guid, name);
						pending.add(known);
					}
				}
			} catch (final SQLException e) {
				throw new RuntimeException(this.getClass().getSimpleName(), e);
			}
		}
		final RecKnown[] result = pending.toArray(new RecKnown[pending.size()]);
		this.setResult(result);
		return result;
	}

	@Override
	public final String getKey() {
		
		return this.key;
	}

}
