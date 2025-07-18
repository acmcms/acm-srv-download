/**
 *
 */
package ru.myx.srv.download.client;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ru.myx.ae3.help.Text;
import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/** @author myx */
final class RequestByFollowLink extends RequestAttachment<RecFile[], RunnerDatabaseRequestor> {

	private final DownloadClient parent;

	private final String key;

	private final String link;

	private final String typeFilter;

	private final String query;

	RequestByFollowLink(final DownloadClient parent, final String link, final String typeFilter, final boolean all) {

		this.parent = parent;
		this.link = link;
		this.typeFilter = typeFilter;
		this.key = "fl-" + link + '\n' + typeFilter + '\n' + all;
		final StringBuilder query = new StringBuilder(256);
		query.append( //
				"SELECT i.itmGuid,i.itmCrc,i.itmLuid,i.fldLuid,i.itmName,i.itmSize,i.itmDate,i.itmType,i.itmComment,i.itmPreview,i.itmLevel2Name,i.itmLevel3Name " + //
						"FROM d1Items i, d1ItemLinkage l, d1Folders f, d1Sources s " + //
						"WHERE i.fldLuid=f.fldLuid AND f.srcLuid=s.srcLuid AND l.itmGuid=i.itmGuid AND l.itmLink=?"//
		);
		if (typeFilter != null && typeFilter.length() > 0) {
			query.append(" AND i.itmType=?");
		}
		if (!all && !parent.available.isEmpty()) {
			if (parent.available.size() == 1) {
				query.append(" AND s.srcLuid=").append(parent.available.iterator().next());
			} else {
				query.append(" AND s.srcLuid in (").append(Text.join(parent.available, ",")).append(')');
			}
		}
		query.append(" ORDER BY i.itmName ASC, s.srcReady DESC");
		this.query = query.toString();
	}

	@Override
	public final RecFile[] apply(final RunnerDatabaseRequestor ctx) {

		final List<RecFile> pending = new ArrayList<>();

		try (final PreparedStatement ps = ctx.ctxGetConnection().prepareStatement(this.query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, this.link);
			if (this.typeFilter != null && this.typeFilter.length() > 0) {
				ps.setString(2, this.typeFilter);
			}
			try (final ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final String itmGuid = rs.getString(1);
					final String md5 = rs.getString(2);
					final int itmLuid = rs.getInt(3);
					final int parentLuid = rs.getInt(4);
					final String name = rs.getString(5);
					final long size = rs.getLong(6);
					final long date = rs.getTimestamp(7).getTime();
					final String type = rs.getString(8);
					final String comment = rs.getString(9);
					final boolean preview = "Y".equals(rs.getString(10));
					final String level2Name = rs.getString(11);
					final String level3Name = rs.getString(12);
					pending.add(new RecFile(this.parent, itmGuid, itmLuid, parentLuid, name, md5, size, date, type, comment, preview, level2Name, level3Name, null));
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException(this.getClass().getSimpleName(), e);
		}

		final RecFile[] result = pending.toArray(new RecFile[pending.size()]);
		this.setResult(result);
		return result;
	}

	@Override
	public final String getKey() {

		return this.key;
	}

}
