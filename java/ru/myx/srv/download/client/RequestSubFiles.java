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
final class RequestSubFiles extends RequestAttachment<Map<String, RecFile>, RunnerDatabaseRequestor> {
	
	private final DownloadClient parent;
	
	private final int luid;
	
	RequestSubFiles(final DownloadClient parent, final int luid) {
		this.parent = parent;
		this.luid = luid;
	}
	
	@Override
	public final Map<String, RecFile> apply(final RunnerDatabaseRequestor ctx) {

		final Map<String, RecFile> result = Create.tempMap();
		{
			try (final PreparedStatement ps = ctx.ctxGetConnection().prepareStatement(
					"SELECT itmGuid,itmCrc,itmLuid,itmName,itmSize,itmDate,itmType,itmComment,itmPreview,itmLevel2Name,itmLevel3Name FROM d1Items WHERE fldLuid=?",
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY)) {
				ps.setInt(1, this.luid);
				try (final ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final String guid = rs.getString(1);
						final String md5 = rs.getString(2);
						final int itmLuid = rs.getInt(3);
						final String name = rs.getString(4);
						final long size = rs.getLong(5);
						final long date = rs.getTimestamp(6).getTime();
						final String type = rs.getString(7);
						final String comment = rs.getString(8);
						final boolean preview = "Y".equals(rs.getString(9));
						final String level2Name = rs.getString(10);
						final String level3Name = rs.getString(11);
						result.put(name, new RecFile(this.parent, guid, itmLuid, this.luid, name, md5, size, date, type, comment, preview, level2Name, level3Name, null));
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

		return "sF-" + this.luid;
	}
	
}
