/**
 *
 */
package ru.myx.srv.download.client;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ru.myx.ae3.help.Create;
import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/**
 * @author myx
 */
final class RequestAliasesByFollowLink extends RequestAttachment<RecAlias[], RunnerDatabaseRequestor> {

	private final DownloadClient parent;

	private final String key;

	private final String link;

	private final String query;

	RequestAliasesByFollowLink(final DownloadClient parent, final String link) {
		this.parent = parent;
		this.link = link;
		this.key = "afl" + link;
		final StringBuilder query = new StringBuilder();
		query.append("SELECT alias,guid FROM d1KnownAliases");
		if (link != null) {
			query.append(" WHERE guid=?");
		}
		this.query = query.toString();
	}

	@Override
	public final RecAlias[] apply(final RunnerDatabaseRequestor ctx) {

		final Map<String, RecAlias> lookup = Create.tempMap();
		final List<RecAlias> pending = new ArrayList<>();

		try (final PreparedStatement ps = ctx.ctxGetConnection().prepareStatement(this.query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			if (this.link != null) {
				ps.setString(1, this.link);
			}
			try (final ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final String alias = rs.getString(1);
					final String known = rs.getString(2);
					final RecAlias check = lookup.get(alias);
					if (check == null) {
						final RecAlias created = new RecAlias(this.parent, known, alias);
						pending.add(created);
						lookup.put(alias, created);
					} else {
						check.add(known);
					}
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException(this.getClass().getSimpleName(), e);
		}

		final RecAlias[] result = pending.toArray(new RecAlias[pending.size()]);
		this.setResult(result);
		return result;
	}

	@Override
	public final String getKey() {

		return this.key;
	}

}
