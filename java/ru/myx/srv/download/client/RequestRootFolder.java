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
 *
 */
final class RequestRootFolder extends RequestAttachment<RecFolder, RunnerDatabaseRequestor> {
	
	private final DownloadClient parent;
	
	private final int luid;
	
	RequestRootFolder(final DownloadClient parent, final int luid) {
		this.parent = parent;
		this.luid = luid;
	}
	
	@Override
	public final RecFolder apply(final RunnerDatabaseRequestor ctx) {

		final RecFolder result;
		try (final PreparedStatement ps = ctx.ctxGetConnection().prepareStatement(
				"SELECT fldLuid,fldParentLuid,fldName,fldChecked FROM d1Folders WHERE srcLuid=? AND fldParentLuid=0",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY)) {
			ps.setInt(1, this.luid);
			try (final ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					final int fldLuid = rs.getInt(1);
					final int fldParentLuid = rs.getInt(2);
					final String name = rs.getString(3);
					final long checked = rs.getTimestamp(4).getTime();
					result = new RecFolder(this.parent, fldLuid, this.luid, fldParentLuid, name, checked);
				} else {
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

		return "rf-" + this.luid;
	}
	
}
