/**
 *
 */
package ru.myx.srv.download.client;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import ru.myx.ae3.help.Create;
import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;

/**
 * @author myx
 *
 */
final class RequestSubFolders extends RequestAttachment<Map<String, RecFolder>, RunnerDatabaseRequestor> {
	
	private final DownloadClient parent;
	
	private final int luid;
	
	private final int source;
	
	RequestSubFolders(final DownloadClient parent, final int luid, final int source) {
		this.parent = parent;
		this.luid = luid;
		this.source = source;
	}
	
	@Override
	public final Map<String, RecFolder> apply(final RunnerDatabaseRequestor ctx) {

		final Map<String, RecFolder> result = Create.tempMap();
		{
			try (final PreparedStatement ps = ctx.ctxGetConnection()
					.prepareStatement("SELECT fldLuid,fldName,fldChecked FROM d1Folders WHERE fldParentLuid=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
				ps.setInt(1, this.luid);
				try (final ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final int luid = rs.getInt(1);
						final String name = rs.getString(2);
						final long checked = rs.getTimestamp(3).getTime();
						result.put(name, new RecFolder(this.parent, luid, this.source, this.luid, name, checked));
					}
				}
			} catch (final SQLException e) {
				throw new RuntimeException(this.getClass().getSimpleName(), e);
			}
		}
		this.setResult(result);
		return result;
	}
	
	@Override
	public final String getKey() {

		return "sf-" + this.luid + '\n' + this.source;
	}
	
}
