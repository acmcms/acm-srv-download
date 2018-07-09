/**
 *
 */
package ru.myx.srv.download.client;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import ru.myx.ae3.report.Report;
import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/**
 * @author myx
 *
 */
final class RequestSource extends RequestAttachment<RecSource, RunnerDatabaseRequestor> {

	private final DownloadClient client;

	private final int luid;

	RequestSource(final DownloadClient client, final int luid) {
		this.client = client;
		this.luid = luid;
	}

	@Override
	public final RecSource apply(final RunnerDatabaseRequestor ctx) {

		final RecSource result;
		try (final PreparedStatement ps = ctx.ctxGetConnection().prepareStatement(
				"SELECT srcGuid,srcHost,srcPort,idxHost,idxPort,srcCreated,srcChecked,srcIndex,srcActive FROM d1Sources WHERE srcLuid=?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY)) {
			ps.setInt(1, this.luid);
			try (final ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					final String guid = rs.getString(1);
					final String srcHost = rs.getString(2);
					final int srcPort = rs.getInt(3);
					final String idxHost = rs.getString(4);
					final int idxPort = rs.getInt(5);
					final Timestamp createdStamp = rs.getTimestamp(6);
					final long created = createdStamp == null
						? 0L
						: createdStamp.getTime();
					final Timestamp checkedStamp = rs.getTimestamp(7);
					final long checked = checkedStamp == null
						? 0L
						: checkedStamp.getTime();
					final boolean index = "Y".equals(rs.getString(8));
					final boolean active = "Y".equals(rs.getString(9));
					result = new RecSource(this.client, this.luid, guid, srcHost, srcPort, idxHost, idxPort, created, checked, index, active);
				} else {
					Report.warning("DL/SOURCE/CREATOR", "Source was not found: luid=" + this.luid);
					result = null;
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException(this.getClass().getSimpleName(), e);
		}
		this.setResult(result);
		return result;
	}

	@Override
	public final String getKey() {

		return "gf-" + this.luid;
	}

}
