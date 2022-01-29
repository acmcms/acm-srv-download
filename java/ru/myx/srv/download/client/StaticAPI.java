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
import java.util.ArrayList;
import java.util.List;

import ru.myx.ae3.Engine;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.report.Report;

/**
 * @author myx
 * 
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class StaticAPI {
	private final DownloadClient	client;
	
	StaticAPI(final DownloadClient client) {
		this.client = client;
	}
	
	/**
	 * @param name
	 * @throws Exception
	 */
	public void ensureKnown(final String name) throws Exception {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			conn.setAutoCommit( false );
			{
				final boolean knownFound;
				{
					try (final PreparedStatement ps = conn
							.prepareStatement( "SELECT count(*) FROM d1Known WHERE name=?",
									ResultSet.TYPE_FORWARD_ONLY,
									ResultSet.CONCUR_READ_ONLY )) {
						ps.setString( 1, name );
						try (final ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								knownFound = rs.getInt( 1 ) > 0;
							} else {
								knownFound = false;
							}
						}
					}
				}
				final boolean aliasFound;
				{
					try (final PreparedStatement ps = conn
							.prepareStatement( "SELECT count(*) FROM d1KnownAliases WHERE alias=?",
									ResultSet.TYPE_FORWARD_ONLY,
									ResultSet.CONCUR_READ_ONLY )) {
						ps.setString( 1, name.toLowerCase() );
						try (final ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								aliasFound = rs.getInt( 1 ) > 0;
							} else {
								aliasFound = false;
							}
						}
					}
				}
				if (!knownFound && !aliasFound) {
					try (final PreparedStatement ps = conn
							.prepareStatement( "INSERT INTO d1Known(guid,name) VALUES (?,?)" )) {
						ps.setString( 1, Engine.createGuid() );
						ps.setString( 2, name );
						ps.execute();
					}
					{
						final int count;
						try (final PreparedStatement ps = conn
								.prepareStatement( "SELECT count(*) FROM d1Queue WHERE queFormation=?",
										ResultSet.TYPE_FORWARD_ONLY,
										ResultSet.CONCUR_READ_ONLY )) {
							ps.setString( 1, name.toLowerCase() );
							try (final ResultSet rs = ps.executeQuery()) {
								if (rs.next()) {
									count = rs.getInt( 1 );
								} else {
									count = 0;
								}
							}
						}
						if (count == 0) {
							try (final PreparedStatement ps = conn
									.prepareStatement( "INSERT INTO d1Queue(queFormation,queBusy,queQueued,queText,queHint) VALUES (?,?,?,?,?)" )) {
								ps.setString( 1, name.toLowerCase() );
								ps.setTimestamp( 2, new Timestamp( 0L ) );
								ps.setTimestamp( 3, new Timestamp( Engine.fastTime()
										- 1000L
										* 60L
										* 60L
										* 24L
										* 7L
										* 58 ) );
								ps.setBytes( 4, "".getBytes( StandardCharsets.UTF_8 ) );
								ps.setString( 5, "divided" );
								ps.execute();
							}
						} else {
							try (final PreparedStatement ps = conn
									.prepareStatement( "UPDATE d1Queue SET queText=?, queQueued=? WHERE queFormation=?" )) {
								ps.setBytes( 1, "".getBytes( StandardCharsets.UTF_8 ) );
								ps.setTimestamp( 2, new Timestamp( Engine.fastTime()
										- 1000L
										* 60L
										* 60L
										* 24L
										* 7L
										* 58 ) );
								ps.setString( 3, name.toLowerCase() );
								ps.execute();
							}
						}
					}
				}
			}
			conn.commit();
		}
	}
	
	/**
	 * @param alias
	 * @return alias
	 * @throws Exception
	 */
	public RecAlias getAlias(final String alias) throws Exception {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "Connection unavailable!" );
			}
			{
				try (final PreparedStatement ps = conn
						.prepareStatement( "SELECT guid FROM d1KnownAliases WHERE alias=?",
								ResultSet.TYPE_FORWARD_ONLY,
								ResultSet.CONCUR_READ_ONLY )) {
					ps.setString( 1, alias.toLowerCase() );
					try (final ResultSet rs = ps.executeQuery()) {
						RecAlias result = null;
						while (rs.next()) {
							final String known = rs.getString( 1 );
							if (result == null) {
								result = new RecAlias( this.client, known, alias.toLowerCase() );
							} else {
								result.add( known );
							}
						}
						return result;
					}
				}
			}
		} catch (final SQLException e) {
			Report.exception( "DL_CLIENT", "Error while getting alias", e );
			return null;
		}
	}
	
	/**
	 * @return alias array
	 * @throws Exception
	 */
	public RecAlias[] getAliases() throws Exception {
		return this.client.searchAliasesByFollowLink( null );
	}
	
	/**
	 * @param guid
	 * @return file
	 */
	public RecFile getItemByGuid(final String guid) {
		return this.client.searchByGuid( guid );
	}
	
	/**
	 * @return known array
	 * @throws Exception
	 */
	public RecKnown[] getKnown() throws Exception {
		return this.client.getKnownByAlias( null );
	}
	
	/**
	 * @param name
	 * @return string
	 * @throws SQLException
	 */
	public String getKnown(final String name) throws SQLException {
		final Connection conn = this.client.nextConnection();
		if (conn == null) {
			throw new RuntimeException( "No connection!" );
		}
		try {
			try (final PreparedStatement ps = conn.prepareStatement( "SELECT guid FROM d1KnownAliases WHERE alias=?",
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY )) {
				ps.setString( 1, name.toLowerCase() );
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getString( 1 );
					}
					return null;
				}
			}
		} finally {
			try {
				conn.close();
			} catch (final Throwable t) {
				// ignore
			}
		}
	}
	
	/**
	 * @param followLink
	 * @return known
	 * @throws Exception
	 */
	public RecKnown getKnownByFollowLink(final String followLink) throws Exception {
		return this.client.getKnownByFollowLink( followLink );
	}
	
	/**
	 * @return int
	 * @throws SQLException
	 */
	public int getL1KnownAliasCount() throws SQLException {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			try (final PreparedStatement ps = conn.prepareStatement( "SELECT count(*) FROM d1KnownAliases",
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY )) {
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt( 1 );
					}
					return -1;
				}
			}
		}
	}
	
	/**
	 * @return int
	 * @throws SQLException
	 */
	public int getL1KnownCount() throws SQLException {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			try (final PreparedStatement ps = conn.prepareStatement( "SELECT count(*) FROM d1Known",
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY )) {
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt( 1 );
					}
					return -1;
				}
			}
		}
	}
	
	/**
	 * @return int
	 * @throws SQLException
	 */
	public int getL1UnknownCount() throws SQLException {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			try (final PreparedStatement ps = conn.prepareStatement( "SELECT count(*) FROM d1Queue",
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY )) {
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt( 1 );
					}
					return -1;
				}
			}
		}
	}
	
	/**
	 * @return int
	 * @throws SQLException
	 */
	public int getNextQueued() throws SQLException {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "SELECT MIN(q.queLuid),count(*) FROM d1Queue q, d1Items i WHERE q.queBusy<? AND q.queFormation=i.itmLevel1Name GROUP BY q.queFormation, q.queDelay ORDER BY q.queDelay, 2 DESC",
							ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY )) {
				ps.setMaxRows( 1 );
				ps.setTimestamp( 1, new Timestamp( Engine.fastTime() - 5L * 1000L * 60L ) );
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt( 1 );
					}
					return -1;
				}
			}
		}
	}
	
	/**
	 * @return queues array
	 * @throws Exception
	 */
	public RecQueued[] getQueue() throws Exception {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "SELECT queLuid,queFormation,queText,queHint FROM d1Queue WHERE queBusy<? ORDER BY 2 ASC",
							ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY )) {
				ps.setTimestamp( 1, new Timestamp( Engine.fastTime() - 5L * 1000L * 60L ) );
				try (final ResultSet rs = ps.executeQuery()) {
					final List<RecQueued> result = new ArrayList<>();
					while (rs.next()) {
						result.add( new RecQueued( this.client, rs.getInt( 1 ), rs.getString( 2 ).toLowerCase(), rs
								.getBytes( 3 ), rs.getString( 4 ) ) );
					}
					return result.toArray( new RecQueued[result.size()] );
				}
			}
		}
	}
	
	/**
	 * @param luid
	 * @return queued
	 * @throws Exception
	 */
	public RecQueued getQueued(final int luid) throws Exception {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "SELECT queFormation,queText,queHint FROM d1Queue WHERE queLuid=?",
							ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY )) {
				ps.setMaxRows( 1 );
				ps.setInt( 1, luid );
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return new RecQueued( this.client,
								luid,
								rs.getString( 1 ).toLowerCase(),
								rs.getBytes( 2 ),
								rs.getString( 3 ) );
					}
					return null;
				}
			}
		}
	}
	
	/**
	 * @param all
	 * @return sources
	 */
	public RecSource[] getSources(final boolean all) {
		return this.client.getSources( all );
	}
	
	/**
	 * @return file array
	 * @throws Exception
	 */
	public RecFile[] getUndescribedItems() throws Exception {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "Connection unavailable!" );
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "SELECT itmGuid,itmCrc,itmLuid,fldLuid,itmName,itmSize,itmDate,itmType,itmComment,itmPreview,itmLevel2Name,itmLevel3Name FROM d1Items WHERE itmGuid IN (SELECT MIN(i.itmGuid) FROM d1Items i LEFT OUTER JOIN d1Descriptions d ON i.itmCrc=d.itmCrc WHERE d.itmCrc is NULL AND i.itmCrc!=? AND i.itmLevel2Name!=? GROUP BY i.itmCrc)",
							ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY )) {
				ps.setString( 1, "*" );
				ps.setString( 2, "*" );
				try (final ResultSet rs = ps.executeQuery()) {
					final List<RecFile> pending = new ArrayList<>();
					while (rs.next()) {
						final String itmGuid = rs.getString( 1 );
						final String md5 = rs.getString( 2 );
						final int itmLuid = rs.getInt( 3 );
						final int parentLuid = rs.getInt( 4 );
						final String name = rs.getString( 5 );
						final long size = rs.getLong( 6 );
						final long date = rs.getTimestamp( 7 ).getTime();
						final String type = rs.getString( 8 );
						final String comment = rs.getString( 9 );
						final boolean preview = "Y".equals( rs.getString( 10 ) );
						final String level2Name = rs.getString( 11 );
						final String level3Name = rs.getString( 12 );
						pending.add( new RecFile( this.client,
								itmGuid,
								itmLuid,
								parentLuid,
								name,
								md5,
								size,
								date,
								type,
								comment,
								preview,
								level2Name,
								level3Name,
								Transfer.EMPTY_BYTE_ARRAY ) );
					}
					return pending.toArray( new RecFile[pending.size()] );
				}
			}
		}
	}
	
	/**
	 * @param limit
	 * @param all
	 * @param timeout
	 * @param sort
	 * @param dateStart
	 * @param dateEnd
	 * @param filter
	 * @return file array
	 */
	public List<RecFile> search(
			final int limit,
			final boolean all,
			final long timeout,
			final String sort,
			final long dateStart,
			final long dateEnd,
			final String filter) {
		if (filter == null || filter.trim().length() == 0) {
			return null;
		}
		return this.client.search( limit, all, timeout, sort, dateStart, dateEnd, filter );
	}
	
	/**
	 * @param alias
	 * @return file array
	 */
	public RecFile[] searchByAlias(final String alias) {
		if (alias == null) {
			return null;
		}
		return this.client.searchFilesByAlias( alias, null, false );
	}
	
	/**
	 * @param guid
	 * @return file array
	 */
	public RecFile[] searchByFollowLink(final String guid) {
		if (guid == null) {
			return null;
		}
		return this.client.searchFilesByFollowLink( guid, null, false );
	}
	
	/**
	 * @param guid
	 * @param type
	 * @return file array
	 */
	public RecFile[] searchByFollowLinkAndType(final String guid, final String type) {
		if (guid == null) {
			return null;
		}
		return this.client.searchFilesByFollowLink( guid, type, false );
	}
	
	/**
	 * @param md5
	 * @return file array
	 */
	public RecFile[] searchByMd5(final String md5) {
		if (md5 == null) {
			return null;
		}
		return this.client.searchByMd5( md5, null, false );
	}
	
	/**
	 * @param name
	 * @return file array
	 */
	public RecFile[] searchByName(final String name) {
		if (name == null) {
			return null;
		}
		final int pos = name.lastIndexOf( '/' );
		if (pos == -1) {
			return this.client.searchByName( name, null, false );
		}
		return this.client.searchByName( name.substring( pos + 1 ), null, false );
	}
	
	/**
	 * @param name
	 * @param type
	 * @return file array
	 */
	public RecFile[] searchByNameAndType(final String name, final String type) {
		if (name == null) {
			return null;
		}
		final int pos = name.lastIndexOf( '/' );
		if (pos == -1) {
			return this.client.searchByName( name, type, false );
		}
		return this.client.searchByName( name.substring( pos + 1 ), type, false );
	}
	
	/**
	 * @param alias
	 * @return group array
	 */
	public RecFileGroup[] searchFileGroupsByAlias(final String alias) {
		if (alias == null) {
			return null;
		}
		return this.client.searchFileGroupsByAlias( alias, null, false );
	}
	
	/**
	 * @param guid
	 * @param type
	 * @return group array
	 */
	public RecFileGroup[] searchFileGroupsByAliasAndType(final String guid, final String type) {
		if (guid == null) {
			return null;
		}
		return this.client.searchFileGroupsByAlias( guid, type, false );
	}
	
	/**
	 * @param guid
	 * @return group array
	 */
	public RecFileGroup[] searchFileGroupsByFollowLink(final String guid) {
		if (guid == null) {
			return null;
		}
		return this.client.searchFileGroupsByFollowLink( guid, null, false, false );
	}
	
	/**
	 * @param guid
	 * @param listable
	 * @return group array
	 */
	public RecFileGroup[] searchFileGroupsByFollowLink(final String guid, final boolean listable) {
		if (guid == null) {
			return null;
		}
		return this.client.searchFileGroupsByFollowLink( guid, null, false, listable );
	}
	
	/**
	 * @param guid
	 * @param type
	 * @return group array
	 */
	public RecFileGroup[] searchFileGroupsByFollowLinkAndType(final String guid, final String type) {
		if (guid == null) {
			return null;
		}
		return this.client.searchFileGroupsByFollowLink( guid, type, false, false );
	}
	
	/**
	 * @param guid
	 * @param type
	 * @param listable
	 * @return group array
	 */
	public RecFileGroup[] searchFileGroupsByFollowLinkAndType(
			final String guid,
			final String type,
			final boolean listable) {
		if (guid == null) {
			return null;
		}
		return this.client.searchFileGroupsByFollowLink( guid, type, false, listable );
	}
	
	/**
	 * @param alias
	 * @return group array
	 */
	public RecFileGroup[] searchFileVariantsByAlias(final String alias) {
		if (alias == null) {
			return null;
		}
		return this.client.searchFileVariantsByAlias( alias, null, false );
	}
	
	/**
	 * @param guid
	 * @param type
	 * @return group array
	 */
	public RecFileGroup[] searchFileVariantsByAliasAndType(final String guid, final String type) {
		if (guid == null) {
			return null;
		}
		return this.client.searchFileVariantsByAlias( guid, type, false );
	}
	
	/**
	 * @param guid
	 * @return group array
	 */
	public RecFileGroup[] searchFileVariantsByFollowLink(final String guid) {
		if (guid == null) {
			return null;
		}
		return this.client.searchFileVariantsByFollowLink( guid, null, false );
	}
	
	/**
	 * @param guid
	 * @param type
	 * @return group array
	 */
	public RecFileGroup[] searchFileVariantsByFollowLinkAndType(final String guid, final String type) {
		if (guid == null) {
			return null;
		}
		return this.client.searchFileVariantsByFollowLink( guid, type, false );
	}
	
	/**
	 * @param names
	 * @return known array
	 */
	public RecKnown[] searchKnown(final String names) {
		if (names == null) {
			return null;
		}
		return this.client.searchKnown( names.trim() );
	}
	
	/**
	 * @param md5
	 * @param description
	 * @throws Exception
	 */
	public void setItemDescription(final String md5, final String description) throws Exception {
		this.setItemDescription( md5, description, false );
	}
	
	/**
	 * @param md5
	 * @param description
	 * @param hidden
	 * @throws Exception
	 */
	public void setItemDescription(final String md5, final String description, final boolean hidden) throws Exception {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "Connection unavailable!" );
			}
			conn.setAutoCommit( false );
			try (final PreparedStatement ps = conn.prepareStatement( "DELETE FROM d1Descriptions WHERE itmCrc=?" )) {
				ps.setString( 1, md5 );
				ps.execute();
			}
			final String realDescription = description == null
					? ""
					: description;
			if (realDescription.length() > 0 || hidden) {
				try (final PreparedStatement ps = conn
						.prepareStatement( "INSERT INTO d1Descriptions(itmCrc,itmDescription,itmHidden) VALUES (?,?,?)" )) {
					ps.setString( 1, md5 );
					ps.setBytes( 2, realDescription.getBytes( StandardCharsets.UTF_8 ) );
					ps.setString( 3, hidden
							? "Y"
							: "N" );
					ps.execute();
				}
			}
			this.client.serializeChange( conn, 0, "update-all", md5, -1 );
			conn.commit();
		}
	}
	
	/**
	 * @param name
	 * @param text
	 * @throws Exception
	 */
	public void setQueued(final String name, final String text) throws Exception {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			final int count;
			try (final PreparedStatement ps = conn
					.prepareStatement( "SELECT count(*) FROM d1Queue WHERE queFormation=?",
							ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY )) {
				ps.setString( 1, name.toLowerCase() );
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						count = rs.getInt( 1 );
					} else {
						count = 0;
					}
				}
			}
			if (count == 0) {
				try (final PreparedStatement ps = conn
						.prepareStatement( "INSERT INTO d1Queue(queFormation,queBusy,queQueued,queText,queHint) VALUES (?,?,?,?,?)" )) {
					ps.setString( 1, name.toLowerCase() );
					ps.setTimestamp( 2, new Timestamp( 0L ) );
					ps.setTimestamp( 3, new Timestamp( Engine.fastTime() - 1000L * 60L * 60L * 24L * 7L * 58 ) );
					ps.setBytes( 4, text.getBytes( StandardCharsets.UTF_8 ) );
					ps.setString( 5, "setQueue( text )" );
					ps.execute();
				}
			} else {
				try (final PreparedStatement ps = conn
						.prepareStatement( "UPDATE d1Queue SET queText=?, queQueued=? WHERE queFormation=?" )) {
					ps.setBytes( 1, text.getBytes( StandardCharsets.UTF_8 ) );
					ps.setTimestamp( 2, new Timestamp( Engine.fastTime() - 1000L * 60L * 60L * 24L * 7L * 58 ) );
					ps.setString( 3, name.toLowerCase() );
					ps.execute();
				}
			}
		}
	}
	
	/**
	 * @throws Exception
	 */
	public final void shiftQueue() throws Exception {
		try (final Connection conn = this.client.nextConnection()) {
			try (final PreparedStatement ps = conn.prepareStatement( "UPDATE d1Queue SET queQueued=queQueued+?" )) {
				ps.setTimestamp( 1, new Timestamp( 1000L * 60L * 60L * 24L * 365 ) );
				ps.execute();
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "UPDATE d1Queue SET queQueued=? WHERE queQueued<?" )) {
				final Timestamp shiftPeriod = new Timestamp( Engine.fastTime() + 1000L * 60L * 60L * 24L * 7L * 2 );
				ps.setTimestamp( 1, shiftPeriod );
				ps.setTimestamp( 2, shiftPeriod );
				ps.execute();
			}
		}
	}
}
