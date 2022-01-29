/**
 *
 */
package ru.myx.srv.download.client;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import ru.myx.jdbc.queueing.RequestAttachment;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;
import ru.myx.util.EntrySimple;

/**
 * @author myx
 *
 */
final class RequestFileDescription extends RequestAttachment<Map.Entry<Boolean, String>, RunnerDatabaseRequestor> {
	
	private final String md5;
	
	RequestFileDescription(final String md5) {
		this.md5 = md5;
	}
	
	@Override
	public final Map.Entry<Boolean, String> apply(final RunnerDatabaseRequestor ctx) {

		final Map.Entry<Boolean, String> result;
		try (final PreparedStatement ps = ctx.ctxGetConnection()
				.prepareStatement("SELECT itmDescription,itmHidden FROM d1Descriptions WHERE itmCrc=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, this.md5);
			try (final ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					final byte[] bytes = rs.getBytes(1);
					final String description;
					final Boolean hidden;
					if (bytes == null || bytes.length == 0) {
						description = null;
					} else {
						description = new String(bytes, StandardCharsets.UTF_8);
					}
					hidden = "Y".equals(rs.getString(2))
						? Boolean.TRUE
						: Boolean.FALSE;
					result = new EntrySimple<>(hidden, description);
				} else {
					result = new EntrySimple<>(Boolean.TRUE, null);
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

		return "fd-" + this.md5;
	}
	
}
