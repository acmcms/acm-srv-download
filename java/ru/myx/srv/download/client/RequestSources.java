/**
 *
 */
package ru.myx.srv.download.client;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/**
 * @author myx
 */
final class RequestSources extends RequestAttachment<RecSource[], RunnerDatabaseRequestor> {

	private final DownloadClient client;

	private final boolean all;

	private final String query;

	RequestSources(final DownloadClient client, final boolean all) {
		this.client = client;
		this.all = all;
		this.query = all
			? "SELECT srcLuid,srcGuid,srcHost,srcPort,idxHost,idxPort,srcCreated,srcChecked,srcIndex,srcActive FROM d1Sources ORDER BY srcChecked ASC"
			: "SELECT srcLuid,srcGuid,srcHost,srcPort,idxHost,idxPort,srcCreated,srcChecked,srcIndex,srcActive FROM d1Sources WHERE srcIndex=? OR srcActive=? ORDER BY srcChecked ASC";
	}

	@Override
	public final RecSource[] apply(final RunnerDatabaseRequestor ctx) {
		
		final List<RecSource> sourceList = new ArrayList<>();
		{
			try (final PreparedStatement ps = ctx.ctxGetConnection().prepareStatement(this.query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
				if (!this.all) {
					ps.setString(1, "Y");
					ps.setString(2, "Y");
				}
				try (final ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final int luid = rs.getInt(1);
						final String guid = rs.getString(2);
						final String srcHost = rs.getString(3);
						final int srcPort = rs.getInt(4);
						final String idxHost = rs.getString(5);
						final int idxPort = rs.getInt(6);
						final Timestamp createdStamp = rs.getTimestamp(7);
						final long created = createdStamp == null
							? 0L
							: createdStamp.getTime();
						final Timestamp checkedStamp = rs.getTimestamp(8);
						final long checked = checkedStamp == null
							? 0L
							: checkedStamp.getTime();
						final boolean index = "Y".equals(rs.getString(9));
						final boolean active = "Y".equals(rs.getString(10));
						sourceList.add(new RecSource(this.client, luid, guid, srcHost, srcPort, idxHost, idxPort, created, checked, index, active));
					}
				}
			} catch (final SQLException e) {
				throw new RuntimeException(this.getClass().getSimpleName(), e);
			}
		}
		final RecSource[] result = sourceList.toArray(new RecSource[sourceList.size()]);
		this.setResult(result);
		return result;
	}

	@Override
	public final String getKey() {
		
		return "gs-" + this.all;
	}
}
