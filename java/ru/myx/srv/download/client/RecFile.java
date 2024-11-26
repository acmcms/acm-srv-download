/*
 * Created on 24.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import ru.myx.ae3.Engine;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.help.Create;

/** @author myx
 *
 *         Window - Preferences - Java - Code Style - Code Templates */
public class RecFile {

	private static final String NULL_DESCRIPTION = "no description";
	
	private static final Set<String> insertFollowIdentifier(final Connection conn, final String alias, final String hint) throws SQLException {

		try (final PreparedStatement ps = conn.prepareStatement("SELECT guid FROM d1KnownAliases WHERE alias=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, alias.toLowerCase());
			try (final ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					final Set<String> result = Create.tempSet();
					do {
						result.add(rs.getString(1));
					} while (rs.next());
					return result;
				}
			}
		}
		try (final PreparedStatement ps = conn.prepareStatement("SELECT queLuid FROM d1Queue WHERE queFormation=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, alias.toLowerCase());
			try (final ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return null;
				}
			}
		}
		try (final PreparedStatement ps = conn.prepareStatement("INSERT INTO d1Queue(queFormation,queBusy,queQueued,queText,queHint) VALUES (?,?,?,?,?)")) {
			ps.setString(1, alias.toLowerCase());
			ps.setTimestamp(2, new Timestamp(0L));
			ps.setTimestamp(3, new Timestamp(Engine.fastTime()));
			ps.setBytes(4, Transfer.EMPTY_BYTE_ARRAY);
			ps.setString(
					5,
					hint == null || hint.length() == 0
						? "*"
						: hint);
			ps.execute();
			return null;
		}
	}
	
	/** @param name
	 * @param file
	 * @return parser */
	public static final NameParser checkParser(final String name, final RecFile file) {

		final String searchName;
		{
			final int pos = name.lastIndexOf('.');
			searchName = pos == -1
				? name.trim().toLowerCase()
				: name.substring(0, pos).trim().toLowerCase();
		}
		final NameParser parser = NameParser.createNameParser(searchName, file.getFolderLuid(), file.client);
		if (parser == null) {
			return "*".equals(file.getLevel2Name()) && "*".equals(file.getLevel3Name())
				? null
				: parser;
		}
		return parser.getLevel2Name().equals(file.getLevel2Name()) && parser.getLevel3Name().equals(file.getLevel3Name())
			? null
			: parser;
	}
	
	/** @param conn
	 * @param parent
	 * @param md5
	 * @param fldLuid
	 * @param name
	 * @param size
	 * @param date
	 * @param type
	 * @param comment
	 * @param preview
	 * @throws SQLException */
	public static final void create(final Connection conn,
			final DownloadClient parent,
			final String md5,
			final int fldLuid,
			final String name,
			final int size,
			final long date,
			final String type,
			final String comment,
			final boolean preview) throws SQLException {

		final String guid = Engine.createGuid();
		final String searchName;
		{
			final int pos = name.lastIndexOf('.');
			searchName = pos == -1
				? name.trim().toLowerCase()
				: name.substring(0, pos).trim().toLowerCase();
		}
		final NameParser parser = NameParser.createNameParser(searchName, fldLuid, parent);
		{
			try (final PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO d1Items(itmGuid,itmCrc,fldLuid,itmName,itmCreated,itmSize,itmDate,itmType,itmComment,itmPreview,itmSearchName,itmLevel1Name,itmLevel2Name,itmLevel3Name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
				ps.setString(1, guid);
				ps.setString(2, md5);
				ps.setInt(3, fldLuid);
				ps.setString(4, name);
				ps.setTimestamp(5, new Timestamp(Engine.fastTime()));
				ps.setInt(6, size);
				ps.setTimestamp(7, new Timestamp(date));
				ps.setString(8, type);
				ps.setString(9, comment);
				ps.setString(
						10,
						preview
							? "Y"
							: "N");
				ps.setString(11, searchName);
				if (parser == null) {
					ps.setString(12, "*");
					ps.setString(13, "*");
					ps.setString(14, "*");
				} else {
					ps.setString(12, parser.getLevel1Name());
					ps.setString(13, parser.getLevel2Name());
					ps.setString(14, parser.getLevel3Name());
				}
				ps.execute();
			}
		}
		if (parser != null) {
			final Set<String> follow = RecFile.insertFollowIdentifier(conn, parser.getLevel1Name(), parser.getHint());
			if (follow != null && !follow.isEmpty()) {
				try (final PreparedStatement ps = conn.prepareStatement("INSERT INTO d1ItemLinkage(itmGuid,itmLink) VALUES (?,?)")) {
					for (final String link : follow) {
						ps.setString(1, guid);
						ps.setString(2, link);
						ps.execute();
						ps.clearParameters();
					}
				}
			}
		}
		parent.serializeChange(conn, 0, "create", guid, -1);
	}
	
	/** @param conn
	 * @param client
	 * @param fldLuid
	 * @param name
	 * @throws SQLException */
	public static void remove(final Connection conn, final DownloadClient client, final int fldLuid, final String name) throws SQLException {

		{
			final String query = "DELETE FROM d1ItemLinkage WHERE itmGuid IN (SELECT itmGuid FROM d1Items WHERE fldLuid=? AND itmName=?)";
			try (final PreparedStatement ps = conn.prepareStatement(query)) {
				ps.setInt(1, fldLuid);
				ps.setString(2, name);
				ps.execute();
			}
		}
		{
			final String query = "SELECT itmLuid FROM d1Items WHERE fldLuid=? AND itmName=?";
			try (final PreparedStatement ps = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
				ps.setInt(1, fldLuid);
				ps.setString(2, name);
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						client.serializeChange(conn, 0, "clean", null, rs.getInt(1));
					}
				}
			}
		}
		{
			final String query = "DELETE FROM d1Items WHERE fldLuid=? AND itmName=?";
			try (final PreparedStatement ps = conn.prepareStatement(query)) {
				ps.setInt(1, fldLuid);
				ps.setString(2, name);
				ps.execute();
			}
		}
	}
	
	private final DownloadClient client;
	
	private final String guid;
	
	private final int itmLuid;
	
	private final int fldLuid;
	
	private final String name;
	
	private final String md5;
	
	private final long size;
	
	private final long date;
	
	private final String type;
	
	private final String comment;
	
	private final String level2Name;
	
	private final String level3Name;
	
	private final boolean preview;
	
	private String path = null;
	
	private String description = null;
	
	private int hidden = -1;
	
	RecFile(
			final DownloadClient client,
			final String guid,
			final int itmLuid,
			final int fldLuid,
			final String name,
			final String md5,
			final long size,
			final long date,
			final String type,
			final String comment,
			final boolean preview,
			final String level2Name,
			final String level3Name,
			final byte[] description) {

		this.client = client;
		this.guid = guid;
		this.itmLuid = itmLuid;
		this.fldLuid = fldLuid;
		this.name = name;
		this.md5 = md5;
		this.size = size;
		this.date = date;
		this.type = type;
		this.comment = comment;
		this.preview = preview;
		this.level2Name = level2Name;
		this.level3Name = level3Name;
		if (description == null) {
			this.description = null;
		} else {
			if (description.length == 0) {
				this.description = RecFile.NULL_DESCRIPTION;
			} else {
				this.description = new String(description, StandardCharsets.UTF_8);
			}
		}
	}
	
	@Override
	public boolean equals(final Object obj) {

		return obj == this || obj instanceof RecFile && ((RecFile) obj).getGuid().equals(this.getGuid());
	}
	
	/** @return collection
	 * @throws Exception */
	public final Collection<String> getAliases() throws Exception {

		final Connection conn = this.client.nextConnection();
		try {
			final PreparedStatement ps = conn.prepareStatement(
					"SELECT a.guid FROM d1KnownAliases a, d1Items i WHERE a.alias=i.itmLevel1Name AND i.itmCrc=?",
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY);
			try {
				ps.setString(1, this.md5);
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						final Set<String> result = Create.tempSet();
						do {
							result.add(rs.getString(1));
						} while (rs.next());
						return result;
					}
					return Collections.emptySet();
				}
			} finally {
				ps.close();
			}
		} finally {
			try {
				conn.close();
			} catch (final Throwable t) {
				// ignore
			}
		}
	}
	
	/** @return string */
	public final String getComment() {

		return this.comment;
	}
	
	/** @return date */
	public final long getDate() {

		return this.date;
	}
	
	/** @return string */
	public String getDescription() {

		if (this.description == null) {
			synchronized (this) {
				if (this.description == null) {
					final RequestFileDescription request = new RequestFileDescription(this.md5);
					this.client.getLoader().add(request);
					final Map.Entry<Boolean, String> result = request.baseValue();
					if (this.hidden == -1) {
						this.hidden = result.getKey() == Boolean.TRUE
							? 1
							: 0;
					}
					if (result.getValue() == null) {
						this.description = RecFile.NULL_DESCRIPTION;
					} else {
						this.description = result.getValue();
					}
				}
			}
		}
		if (this.description == RecFile.NULL_DESCRIPTION) {
			return null;
		}
		return this.description;
	}
	
	/** @param conn
	 * @return string
	 * @throws Exception */
	public String getDescription(final Connection conn) throws Exception {

		if (conn == null) {
			return this.getDescription();
		}
		if (this.description == null) {
			synchronized (this) {
				if (this.description == null) {
					try (final PreparedStatement ps = conn
							.prepareStatement("SELECT itmDescription,itmHidden FROM d1Descriptions WHERE itmCrc=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
						ps.setString(1, this.md5);
						try (final ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								final byte[] bytes = rs.getBytes(1);
								if (bytes == null || bytes.length == 0) {
									this.description = RecFile.NULL_DESCRIPTION;
								} else {
									this.description = new String(bytes, StandardCharsets.UTF_8);
								}
								if (this.hidden == -1) {
									this.hidden = "Y".equals(rs.getString(2))
										? 1
										: 0;
								}
							} else {
								this.hidden = 1;
							}
						}
					}
				}
			}
		}
		if (this.description == RecFile.NULL_DESCRIPTION) {
			return null;
		}
		return this.description;
	}
	
	/** @return int */
	public final int getFolderLuid() {

		return this.fldLuid;
	}
	
	/** @return string */
	public final String getGuid() {

		return this.guid;
	}
	
	/** @return int */
	public final int getKeyLocal() {

		return this.itmLuid;
	}
	
	/** @return string */
	public final String getLevel2Name() {

		return this.level2Name;
	}
	
	/** @return string */
	public final String getLevel3Name() {

		return this.level3Name;
	}
	
	/** @return string */
	public final String getMd5() {

		return this.md5;
	}
	
	/** @return string */
	public final String getName() {

		return this.name;
	}
	
	/** @return string */
	public String getPath() {

		if (this.path == null) {
			synchronized (this) {
				if (this.path == null) {
					try {
						final RecFolder folder = this.client.getFolder(this.getFolderLuid());
						this.path = folder.getPath();
					} catch (final SQLException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return this.path + this.name;
	}
	
	/** @return string */
	public final String getPathLocal() {

		final String pathLocal;
		try {
			final RecFolder folder = this.client.getFolder(this.getFolderLuid());
			pathLocal = folder.getPathLocal();
		} catch (final SQLException e) {
			throw new RuntimeException(e);
		}
		return pathLocal + this.name;
	}
	
	/** @return int */
	public final long getSize() {

		return this.size;
	}
	
	/** @return string */
	public final String getType() {

		return this.type;
	}
	
	@Override
	public int hashCode() {

		return this.getGuid().hashCode();
	}
	
	/** @return boolean */
	public final boolean hasPreview() {

		return this.preview;
	}
	
	/** @return boolean
	 * @throws SQLException */
	public boolean isHidden() throws SQLException {

		if (this.hidden == -1) {
			synchronized (this) {
				if (this.hidden == -1) {
					try (final Connection conn = this.client.nextConnection()) {
						if (conn == null) {
							throw new RuntimeException("Connection is unavailable!");
						}
						try (final PreparedStatement ps = conn
								.prepareStatement("SELECT itmHidden FROM d1Descriptions WHERE itmCrc=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
							ps.setString(1, this.md5);
							try (final ResultSet rs = ps.executeQuery()) {
								if (rs.next()) {
									this.hidden = "Y".equals(rs.getString(1))
										? 1
										: 0;
								} else {
									this.hidden = 1;
								}
							}
						}
					}
				}
			}
		}
		return this.hidden != 0;
	}
	
	/** @param conn
	 * @param md5
	 * @param size
	 * @param date
	 * @param preview
	 * @param parser
	 * @return updated or not!
	 * @throws SQLException */
	public boolean update(final Connection conn, final String md5, final int size, final long date, final boolean preview, final NameParser parser) throws SQLException {

		int changes = 0;
		final StringBuilder query = new StringBuilder().append("UPDATE d1Items SET ");
		if (md5 != this.md5) {
			if (changes++ > 0) {
				query.append(", ");
			}
			query.append("itmCrc=?");
		}
		if (size != this.size) {
			if (changes++ > 0) {
				query.append(", ");
			}
			query.append("itmSize=?");
		}
		if (date / 10_000L != this.date / 10_000L) {
			if (changes++ > 0) {
				query.append(", ");
			}
			query.append("itmDate=?");
		}
		if (preview != this.preview) {
			if (changes++ > 0) {
				query.append(", ");
			}
			query.append("itmPreview=?");
		}
		if (parser != null) {
			if (changes++ > 0) {
				query.append(", ");
			}
			query.append("itmLevel1Name=?, itmLevel2Name=?, itmLevel3Name=?");
		}
		query.append(" WHERE itmGuid=?");
		if (changes == 0) {
			return false;
		}
		if (parser != null) {
			try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM d1ItemLinkage WHERE itmGuid=?")) {
				ps.setString(1, this.guid);
				ps.execute();
			}
		}
		try (final PreparedStatement ps = conn.prepareStatement(query.toString())) {
			int counter = 0;
			if (md5 != this.md5) {
				ps.setString(++counter, md5);
			}
			if (size != this.size) {
				ps.setInt(++counter, size);
			}
			if (date / 10_000L != this.date / 10_000L) {
				ps.setTimestamp(++counter, new Timestamp(date));
			}
			if (preview != this.preview) {
				ps.setString(
						++counter,
						preview
							? "Y"
							: "N");
			}
			if (parser != null) {
				ps.setString(++counter, parser.getLevel1Name());
				ps.setString(++counter, parser.getLevel2Name());
				ps.setString(++counter, parser.getLevel3Name());
			}
			ps.setString(++counter, this.guid);
			ps.execute();
		}
		if (parser != null) {
			final Set<String> follow = RecFile.insertFollowIdentifier(conn, parser.getLevel1Name(), parser.getHint());
			if (follow != null && !follow.isEmpty()) {
				try (final PreparedStatement ps = conn.prepareStatement("INSERT INTO d1ItemLinkage(itmGuid,itmLink) VALUES (?,?)")) {
					for (final String link : follow) {
						ps.setString(1, this.guid);
						ps.setString(2, link);
						ps.execute();
						ps.clearParameters();
					}
				}
			}
		}
		this.client.serializeChange(conn, 0, "update", this.guid, this.itmLuid);
		return true;
	}
}
