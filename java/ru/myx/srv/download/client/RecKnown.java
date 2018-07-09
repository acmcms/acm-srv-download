/*
 * Created on 03.01.2005
 */
package ru.myx.srv.download.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author myx Window - Preferences - Java - Code Style - Code Templates
 */
public class RecKnown implements Comparable<RecKnown> {
	private final DownloadClient	client;
	
	private final String			followLink;
	
	private final String			name;
	
	RecKnown(final DownloadClient client, final String followLink, final String name) {
		this.client = client;
		this.followLink = followLink;
		this.name = name;
	}
	
	@Override
	public int compareTo(final RecKnown o) {
		return this.followLink.compareTo( o.getFollowLink() );
	}
	
	/**
	 * @param name
	 * @throws Exception
	 */
	public void doRename(final String name) throws Exception {
		try (final Connection conn = this.client.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException( "No connection!" );
			}
			conn.setAutoCommit( false );
			final String replaceTo;
			{
				final String knownFound;
				try (final PreparedStatement ps = conn.prepareStatement( "SELECT guid FROM d1Known WHERE name=?",
						ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_READ_ONLY )) {
					ps.setString( 1, name );
					try (final ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							knownFound = rs.getString( 1 );
						} else {
							knownFound = null;
						}
					}
				}
				if (knownFound == null) {
					final String aliasFound;
					try (final PreparedStatement ps = conn
							.prepareStatement( "SELECT guid FROM d1KnownAliases WHERE alias=?",
									ResultSet.TYPE_FORWARD_ONLY,
									ResultSet.CONCUR_READ_ONLY )) {
						ps.setString( 1, name.toLowerCase() );
						try (final ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								aliasFound = rs.getString( 1 );
							} else {
								aliasFound = null;
							}
						}
					}
					if (aliasFound == null) {
						replaceTo = null;
					} else {
						replaceTo = aliasFound;
					}
				} else {
					replaceTo = knownFound;
				}
			}
			if (replaceTo == null || replaceTo.equals( this.followLink )) {
				try (final PreparedStatement ps = conn
						.prepareStatement( "UPDATE d1Queue SET queFormation=? WHERE queFormation=?" )) {
					ps.setString( 1, name.toLowerCase() );
					ps.setString( 2, this.name.toLowerCase() );
					ps.execute();
				}
				{
					final boolean aliasFound;
					try (final PreparedStatement ps = conn
							.prepareStatement( "SELECT count(*) FROM d1KnownAliases WHERE guid=? AND alias=?",
									ResultSet.TYPE_FORWARD_ONLY,
									ResultSet.CONCUR_READ_ONLY )) {
						ps.setString( 1, this.followLink );
						ps.setString( 2, name.toLowerCase() );
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
							ps.setString( 1, this.followLink );
							ps.setString( 2, name.toLowerCase() );
							ps.execute();
						}
					}
				}
				try (final PreparedStatement ps = conn.prepareStatement( "UPDATE d1Known SET name=? WHERE name=?" )) {
					ps.setString( 1, name );
					ps.setString( 2, this.name );
					ps.execute();
				}
			} else {
				try (final PreparedStatement ps = conn.prepareStatement( "DELETE FROM d1Queue WHERE queFormation=?" )) {
					ps.setString( 1, this.name.toLowerCase() );
					ps.execute();
				}
				try (final PreparedStatement ps = conn
						.prepareStatement( "UPDATE d1KnownAliases SET guid=? WHERE guid=?" )) {
					ps.setString( 1, replaceTo );
					ps.setString( 2, this.followLink );
					ps.execute();
				}
				try (final PreparedStatement ps = conn
						.prepareStatement( "INSERT INTO d1ItemLinkage(itmGuid,itmLink) SELECT i.itmGuid,? FROM d1ItemLinkage lt, d1Items i LEFT OUTER JOIN d1ItemLinkage l ON i.itmGuid=l.itmGuid AND l.itmLink=? WHERE lt.itmGuid=i.itmGuid AND lt.itmLink=? AND l.itmGuid is NULL" )) {
					ps.setString( 1, replaceTo );
					ps.setString( 2, replaceTo );
					ps.setString( 3, this.followLink );
					ps.execute();
				}
				try (final PreparedStatement ps = conn.prepareStatement( "DELETE FROM d1ItemLinkage WHERE itmLink=?" )) {
					ps.setString( 1, this.followLink );
					ps.execute();
				}
				try (final PreparedStatement ps = conn.prepareStatement( "DELETE FROM d1Known WHERE guid=?" )) {
					ps.setString( 1, this.followLink );
					ps.execute();
				}
			}
			conn.commit();
		}
	}
	
	@Override
	public boolean equals(final Object obj) {
		return obj == this || this.followLink.equals( ((RecKnown) obj).getFollowLink() );
	}
	
	/**
	 * @return alias array
	 */
	public RecAlias[] getAliases() {
		return this.client.searchAliasesByFollowLink( this.followLink );
	}
	
	/**
	 * @return group array
	 */
	public RecFileGroup[] getFileGroups() {
		return this.client.searchFileGroupsByFollowLink( this.followLink, null, true, false );
	}
	
	/**
	 * @param listable
	 * @return group array
	 */
	public RecFileGroup[] getFileGroups(final boolean listable) {
		return this.client.searchFileGroupsByFollowLink( this.followLink, null, true, listable );
	}
	
	/**
	 * @return file array
	 */
	public RecFile[] getFiles() {
		return this.client.searchFilesByFollowLink( this.followLink, null, true );
	}
	
	/**
	 * @return group array
	 */
	public RecFileGroup[] getFileVariants() {
		return this.client.searchFileVariantsByFollowLink( this.followLink, null, true );
	}
	
	/**
	 * @return string
	 */
	public String getFollowLink() {
		return this.followLink;
	}
	
	/**
	 * @return string
	 */
	public String getName() {
		return this.name;
	}
	
	@Override
	public int hashCode() {
		return this.followLink.hashCode();
	}
	
	@Override
	public String toString() {
		return "KNOWN{guid=" + this.followLink + ", name=" + this.name + "}";
	}
}
