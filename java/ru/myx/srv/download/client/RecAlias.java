/*
 * Created on 04.02.2005
 */
package ru.myx.srv.download.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Set;

import ru.myx.ae3.help.Create;
import ru.myx.ae3.help.Text;

/**
 * @author myx
 * 
 */
public final class RecAlias {
	private final DownloadClient	client;
	
	private final Set<String>		followLinks;
	
	private final String			alias;
	
	RecAlias(final DownloadClient client, final String followLink, final String name) {
		this.client = client;
		this.followLinks = Create.tempSet();
		this.followLinks.add( followLink );
		this.alias = name.toLowerCase();
	}
	
	/**
	 * @param followLink
	 */
	public final void add(final String followLink) {
		this.followLinks.add( followLink );
	}
	
	/**
	 * @param addFollowLinks
	 * @param removeFollowLinks
	 * @throws Exception
	 */
	public final void doUpdateConnections(
			final Collection<String> addFollowLinks,
			final Collection<String> removeFollowLinks) throws Exception {
		if ((addFollowLinks == null || addFollowLinks.isEmpty())
				&& (removeFollowLinks == null || removeFollowLinks.isEmpty())) {
			return;
		}
		try (final Connection conn = this.client.nextConnection()) {
			try {
				conn.setAutoCommit( false );
				if (removeFollowLinks != null && !removeFollowLinks.isEmpty()) {
					try (final PreparedStatement ps = conn
							.prepareStatement( "DELETE FROM d1ItemLinkage WHERE itmGuid in (SELECT i.itmGuid FROM d1Items i, d1Known k, d1KnownAliases a WHERE a.guid=k.guid and a.alias=i.itmLevel1Name and a.alias=?) AND itmLink IN ('"
									+ Text.join( removeFollowLinks, "','" )
									+ "')" )) {
						ps.setString( 1, this.alias );
						ps.execute();
					}
					try (final PreparedStatement ps = conn
							.prepareStatement( "DELETE FROM d1KnownAliases WHERE alias=? AND guid IN ('"
									+ Text.join( removeFollowLinks, "','" )
									+ "')" )) {
						ps.setString( 1, this.alias );
						ps.execute();
					}
				}
				if (addFollowLinks != null && !addFollowLinks.isEmpty()) {
					for (final String followLink : addFollowLinks) {
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
						try (final PreparedStatement ps = conn
								.prepareStatement( "DELETE FROM d1KnownAliases WHERE alias=? AND guid=?" )) {
							ps.setString( 1, this.alias );
							ps.setString( 2, followLink );
							ps.execute();
						}
						try (final PreparedStatement ps = conn
								.prepareStatement( "INSERT INTO d1KnownAliases(alias,guid) VALUES (?, ?)" )) {
							ps.setString( 1, this.alias );
							ps.setString( 2, followLink );
							ps.execute();
						}
					}
					try (final PreparedStatement ps = conn
							.prepareStatement( "DELETE FROM d1ItemLinkage WHERE itmGuid in (SELECT i.itmGuid FROM d1Items i, d1Known k, d1KnownAliases a WHERE a.guid=k.guid and a.alias=i.itmLevel1Name and a.alias=?) AND itmLink IN ('"
									+ Text.join( addFollowLinks, "','" )
									+ "')" )) {
						ps.setString( 1, this.alias );
						ps.execute();
					}
					try (final PreparedStatement ps = conn
							.prepareStatement( "INSERT INTO d1ItemLinkage(itmGuid,itmLink) SELECT i.itmGuid,k.guid FROM d1Items i, d1Known k, d1KnownAliases a WHERE a.guid=k.guid and a.alias=i.itmLevel1Name and a.alias=? AND k.guid IN ('"
									+ Text.join( addFollowLinks, "','" )
									+ "')" )) {
						ps.setString( 1, this.alias );
						ps.execute();
					}
				}
				conn.commit();
			} catch (final Exception e) {
				try {
					conn.rollback();
				} catch (final Throwable t) {
					// ignore
				}
				throw e;
			}
		}
	}
	
	/**
	 * @return group array
	 */
	public final RecFileGroup[] getFileGroups() {
		return this.client.searchFileGroupsByAlias( this.alias, null, true );
	}
	
	/**
	 * @return file array
	 */
	public final RecFile[] getFiles() {
		return this.client.searchFilesByAlias( this.alias, null, true );
	}
	
	/**
	 * @return group array
	 */
	public final RecFileGroup[] getFileVariants() {
		return this.client.searchFileVariantsByAlias( this.alias, null, true );
	}
	
	/**
	 * @return collection
	 */
	public final Collection<String> getFollowLinks() {
		return this.followLinks;
	}
	
	/**
	 * @return known array
	 */
	public final RecKnown[] getKnown() {
		return this.client.getKnownByAlias( this.alias );
	}
	
	/**
	 * @return string
	 */
	public final String getName() {
		return this.alias;
	}
}
