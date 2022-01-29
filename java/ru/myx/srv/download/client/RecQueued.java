/*
 * Created on 29.12.2004
 */
package ru.myx.srv.download.client;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;

import ru.myx.ae3.Engine;
import ru.myx.ae3.binary.Transfer;

/**
 * @author myx
 * 
 */
public final class RecQueued {
	final DownloadClient	client;
	
	final int				luid;
	
	final String			formation;
	
	final String			text;
	
	final String			hint;
	
	RecQueued(final DownloadClient client, final int luid, final String formation, final byte[] text, final String hint)
			throws Exception {
		this.client = client;
		this.luid = luid;
		this.formation = formation;
		this.text = text == null || text.length == 0
				? ""
				: Transfer.createBuffer( text ).toString( StandardCharsets.UTF_8 );
		this.hint = hint == null || "*".equals( hint )
				? null
				: hint;
	}
	
	/**
	 * @param followLink
	 * @param names
	 * @throws SQLException
	 */
	public void doAcknowledge(final String followLink, final Collection<String> names) throws SQLException {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			conn.setAutoCommit( false );
			try (final PreparedStatement ps = conn.prepareStatement( "DELETE FROM d1Queue WHERE queFormation=?" )) {
				ps.setString( 1, this.formation.toLowerCase() );
				ps.execute();
			}
			try (final PreparedStatement ps = conn.prepareStatement( "INSERT INTO d1Known(guid,name) VALUES (?,?)" )) {
				ps.setString( 1, followLink );
				ps.setString( 2, names.iterator().next() );
				ps.execute();
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "INSERT INTO d1KnownAliases(guid,alias) VALUES (?,?)" )) {
				ps.setString( 1, followLink );
				ps.setString( 2, this.formation.toLowerCase() );
				ps.execute();
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "INSERT INTO d1ItemLinkage(itmGuid,itmLink) SELECT i.itmGuid,? FROM d1Items i LEFT OUTER JOIN d1ItemLinkage l ON i.itmGuid=l.itmGuid AND l.itmLink=? WHERE i.itmLevel1Name=? AND l.itmGuid is NULL" )) {
				ps.setString( 1, followLink );
				ps.setString( 2, followLink );
				ps.setString( 3, this.formation );
				ps.execute();
			}
			for (final String nextName : names) {
				final String name = nextName.toLowerCase().trim();
				if (name.length() > 0 && !name.equals( this.formation.toLowerCase().trim() )) {
					try (final PreparedStatement ps = conn
							.prepareStatement( "DELETE FROM d1Queue WHERE queFormation=?" )) {
						ps.setString( 1, name );
						ps.execute();
					}
					try (final PreparedStatement ps = conn
							.prepareStatement( "INSERT INTO d1KnownAliases(guid,alias) VALUES (?,?)" )) {
						ps.setString( 1, followLink );
						ps.setString( 2, name.toLowerCase() );
						ps.execute();
					}
					try (final PreparedStatement ps = conn
							.prepareStatement( "INSERT INTO d1ItemLinkage(itmGuid,itmLink) SELECT i.itmGuid,? FROM d1Items i LEFT OUTER JOIN d1ItemLinkage l ON i.itmGuid=l.itmGuid AND l.itmLink=? WHERE i.itmLevel1Name=? AND l.itmGuid is NULL" )) {
						ps.setString( 1, followLink );
						ps.setString( 2, followLink );
						ps.setString( 3, name );
						ps.execute();
					}
				}
			}
			conn.commit();
		}
	}
	
	/**
	 * @param followLinks
	 * @throws SQLException
	 */
	public void doConnect(final Collection<String> followLinks) throws SQLException {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			conn.setAutoCommit( false );
			try (final PreparedStatement ps = conn.prepareStatement( "DELETE FROM d1Queue WHERE queFormation=?" )) {
				ps.setString( 1, this.formation.toLowerCase() );
				ps.execute();
			}
			for (final String followLink : followLinks) {
				{
					final boolean knownFound;
					try (final PreparedStatement ps = conn
							.prepareStatement( "SELECT count(*) FROM d1Known WHERE guid=?",
									ResultSet.TYPE_FORWARD_ONLY,
									ResultSet.CONCUR_READ_ONLY )) {
						ps.setString( 1, followLink );
						try (final ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								knownFound = rs.getInt( 1 ) > 0;
							} else {
								knownFound = false;
							}
						}
					}
					if (!knownFound) {
						try (final PreparedStatement ps = conn
								.prepareStatement( "INSERT INTO d1Known(guid,name) VALUES (?,?)" )) {
							ps.setString( 1, followLink );
							ps.setString( 2, followLink );
							ps.execute();
						}
					}
				}
				{
					final boolean aliasFound;
					try (final PreparedStatement ps = conn
							.prepareStatement( "SELECT count(*) FROM d1KnownAliases WHERE guid=? AND alias=?",
									ResultSet.TYPE_FORWARD_ONLY,
									ResultSet.CONCUR_READ_ONLY )) {
						ps.setString( 1, followLink );
						ps.setString( 2, this.formation );
						try (final ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								aliasFound = rs.getInt( 1 ) > 0;
							} else {
								aliasFound = false;
							}
						}
					}
					if (!aliasFound) {
						try (final PreparedStatement ps = conn
								.prepareStatement( "INSERT INTO d1KnownAliases(guid,alias) VALUES (?,?)" )) {
							ps.setString( 1, followLink );
							ps.setString( 2, this.formation );
							ps.execute();
						}
					}
				}
				try (final PreparedStatement ps = conn
						.prepareStatement( "INSERT INTO d1ItemLinkage(itmGuid,itmLink) SELECT i.itmGuid,? FROM d1Items i LEFT OUTER JOIN d1ItemLinkage l ON i.itmGuid=l.itmGuid AND l.itmLink=? WHERE i.itmLevel1Name=? AND l.itmGuid is NULL" )) {
					ps.setString( 1, followLink );
					ps.setString( 2, followLink );
					ps.setString( 3, this.formation );
					ps.execute();
				}
			}
			conn.commit();
		}
	}
	
	/**
	 * @param followLink
	 * @throws SQLException
	 */
	public void doCreateAlias(final String followLink) throws SQLException {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			conn.setAutoCommit( false );
			try (final PreparedStatement ps = conn.prepareStatement( "DELETE FROM d1Queue WHERE queFormation=?" )) {
				ps.setString( 1, this.formation.toLowerCase() );
				ps.execute();
			}
			{
				final boolean aliasFound;
				try (final PreparedStatement ps = conn
						.prepareStatement( "SELECT count(*) FROM d1KnownAliases WHERE guid=? AND alias=?",
								ResultSet.TYPE_FORWARD_ONLY,
								ResultSet.CONCUR_READ_ONLY )) {
					ps.setString( 1, followLink );
					ps.setString( 2, this.formation.toLowerCase() );
					try (final ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							aliasFound = rs.getInt( 1 ) > 0;
						} else {
							aliasFound = false;
						}
					}
				}
				if (!aliasFound) {
					try (final PreparedStatement ps = conn
							.prepareStatement( "INSERT INTO d1KnownAliases(guid,alias) VALUES (?,?)" )) {
						ps.setString( 1, followLink );
						ps.setString( 2, this.formation.toLowerCase() );
						ps.execute();
					}
				}
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "INSERT INTO d1ItemLinkage(itmGuid,itmLink) SELECT i.itmGuid,? FROM d1Items i LEFT OUTER JOIN d1ItemLinkage l ON i.itmGuid=l.itmGuid WHERE l.itmGuid is NULL AND i.itmLevel1Name=?" )) {
				ps.setString( 1, followLink );
				ps.setString( 2, this.formation );
				ps.execute();
			}
			conn.commit();
		}
	}
	
	/**
	 * @param names
	 * @throws SQLException
	 */
	public void doDivide(final Collection<String> names) throws SQLException {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			conn.setAutoCommit( false );
			try (final PreparedStatement ps = conn.prepareStatement( "DELETE FROM d1Queue WHERE queFormation=?" )) {
				ps.setString( 1, this.formation.toLowerCase() );
				ps.execute();
			}
			for (final String name : names) {
				final String followLink;
				{
					try (final PreparedStatement ps = conn
							.prepareStatement( "SELECT guid FROM d1Known WHERE name=? UNION SELECT guid FROM d1KnownAliases WHERE alias=?",
									ResultSet.TYPE_FORWARD_ONLY,
									ResultSet.CONCUR_READ_ONLY )) {
						ps.setString( 1, name );
						ps.setString( 2, name.toLowerCase() );
						try (final ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								followLink = rs.getString( 1 );
							} else {
								followLink = null;
							}
						}
					}
					if (followLink == null) {
						try (final PreparedStatement ps = conn
								.prepareStatement( "INSERT INTO d1Known(guid,name) VALUES (?,?)" )) {
							ps.setString( 1, Engine.createGuid() );
							ps.setString( 2, name );
							ps.execute();
						}
					} else {
						{
							final boolean aliasFound;
							try (final PreparedStatement ps = conn
									.prepareStatement( "SELECT count(*) FROM d1KnownAliases WHERE guid=? AND alias=?",
											ResultSet.TYPE_FORWARD_ONLY,
											ResultSet.CONCUR_READ_ONLY )) {
								ps.setString( 1, followLink );
								ps.setString( 2, this.formation.toLowerCase() );
								try (final ResultSet rs = ps.executeQuery()) {
									if (rs.next()) {
										aliasFound = rs.getInt( 1 ) > 0;
									} else {
										aliasFound = false;
									}
								}
							}
							if (!aliasFound) {
								try (final PreparedStatement ps = conn
										.prepareStatement( "INSERT INTO d1KnownAliases(guid,alias) VALUES (?,?)" )) {
									ps.setString( 1, followLink );
									ps.setString( 2, this.formation.toLowerCase() );
									ps.execute();
								}
							}
						}
						try (final PreparedStatement ps = conn
								.prepareStatement( "INSERT INTO d1ItemLinkage(itmGuid,itmLink) SELECT i.itmGuid,? FROM d1Items i LEFT OUTER JOIN d1ItemLinkage l ON i.itmGuid=l.itmGuid AND l.itmLink=? WHERE i.itmLevel1Name=? AND l.itmGuid is NULL" )) {
							ps.setString( 1, followLink );
							ps.setString( 2, followLink );
							ps.setString( 3, this.formation );
							ps.execute();
						}
					}
				}
			}
			conn.commit();
		}
	}
	
	/**
	 * @throws SQLException
	 */
	public void doLock() throws SQLException {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "UPDATE d1Queue SET queBusy=? WHERE queLuid=? AND queFormation=?" )) {
				ps.setMaxRows( 1 );
				ps.setTimestamp( 1, new Timestamp( Engine.fastTime() ) );
				ps.setInt( 2, this.luid );
				ps.setString( 3, this.formation.toLowerCase() );
				ps.execute();
			}
		}
	}
	
	/**
	 * @throws SQLException
	 */
	public void doRequeue() throws SQLException {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			final long last;
			try (final PreparedStatement ps = conn.prepareStatement( "SELECT MAX(queQueued) FROM d1Queue",
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY )) {
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						final Timestamp timestamp = rs.getTimestamp( 1 );
						if (timestamp == null) {
							last = Engine.fastTime();
						} else {
							last = timestamp.getTime() < Engine.fastTime()
									? Engine.fastTime()
									: timestamp.getTime();
						}
					} else {
						last = Engine.fastTime();
					}
				}
			}
			try (final PreparedStatement ps = conn
					.prepareStatement( "UPDATE d1Queue SET queQueued=?, queDelay=queDelay+1 WHERE queLuid=? AND queFormation=?" )) {
				ps.setTimestamp( 1, new Timestamp( last + 1000L * 60L ) );
				ps.setInt( 2, this.luid );
				ps.setString( 3, this.formation.toLowerCase() );
				ps.execute();
			}
		}
	}
	
	/**
	 * @return string
	 */
	public String getFormation() {
		return this.formation;
	}
	
	/**
	 * @return string
	 */
	public String getHint() {
		return this.hint;
	}
	
	/**
	 * @return int
	 */
	public int getLuid() {
		return this.luid;
	}
	
	/**
	 * @return string
	 */
	public String getName() {
		return this.formation;
	}
	
	/**
	 * @return string
	 */
	public String getText() {
		return this.text;
	}
}
