/*
 * Created on 17.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import ru.myx.ae1.AbstractPluginInstance;
import ru.myx.ae1.provide.ProvideStatus;
import ru.myx.ae1.storage.ListByMapEntryKey;
import ru.myx.ae2.indexing.ExecSearchInstruction;
import ru.myx.ae2.indexing.ExecSearchProgram;
import ru.myx.ae2.indexing.Indexing;
import ru.myx.ae2.indexing.IndexingDictionaryCached;
import ru.myx.ae2.indexing.IndexingDictionaryVfs;
import ru.myx.ae2.indexing.IndexingFinder;
import ru.myx.ae2.indexing.IndexingStemmer;
import ru.myx.ae3.Engine;
import ru.myx.ae3.act.Act;
import java.util.function.Function;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.cache.Cache;
import ru.myx.ae3.cache.CacheL2;
import ru.myx.ae3.cache.CacheType;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Create;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.status.StatusInfo;
import ru.myx.ae3.status.StatusRegistry;
import ru.myx.ae3.vfs.Entry;
import ru.myx.ae3.vfs.EntryContainer;
import ru.myx.jdbc.lock.Interest;
import ru.myx.jdbc.lock.Lock;
import ru.myx.jdbc.lock.LockManager;
import ru.myx.jdbc.queueing.RunnerDatabaseRequestor;
import ru.myx.query.SyntaxQuery;
import ru.myx.srv.download.client.ctrl.Node;

/** @author myx */
public class DownloadClient extends AbstractPluginInstance {
	
	private static final RecKnown[] EMPTY_KNOWN_ARRAY = new RecKnown[0];
	
	private static final RecFileGroup[] EMPTY_GROUP_ARRAY = new RecFileGroup[0];
	
	private static final RecFile[] EMPTY_FILE_ARRAY = new RecFile[0];
	
	private static final Map<String, String> REPLACEMENT_SEARCH_SORT = DownloadClient.createSearchReplacementSort();
	
	private static final Map<String, String> createSearchReplacementSort() {
		
		final Map<String, String> result = new HashMap<>();
		result.put("alphabet", "$title");
		result.put("history", "$created-");
		result.put("log", "$created");
		return result;
	}
	
	private static final String joinSqlString(final Collection<?> strs, final String token) {
		
		final StringBuilder result = new StringBuilder(128);
		for (final Object object : strs) {
			if (result.length() > 0) {
				result.append(token);
			}
			for (final StringTokenizer st = new StringTokenizer(object.toString(), "'", true); st.hasMoreTokens();) {
				final String next = st.nextToken();
				if (next.length() == 1 && next.charAt(0) == '\'') {
					result.append("''");
				} else {
					result.append(next);
				}
			}
		}
		return result.toString();
	}
	
	private String identity;
	
	private File cacheRoot;
	
	private String connection;
	
	private Enumeration<Connection> connectionSource;
	
	private final CreatorFolder creatorFolder = new CreatorFolder(this);
	
	private final CacheL2<RecFolder> cacheFolders = Cache.createL2("folders", CacheType.NORMAL_JAVA_SOFT);
	
	private final CreatorSource creatorSource = new CreatorSource(this);
	
	private final CacheL2<RecSource> cacheSources = Cache.createL2("sources", CacheType.NORMAL_JAVA_SOFT);
	
	private final CacheL2<RecFileGroup[]> cacheFileGroups = Cache.createL2("data", CacheType.NORMAL_JAVA_SOFT);
	
	private final CacheL2<RecKnown> cacheKnown = Cache.createL2("known", CacheType.NORMAL_JAVA_SOFT);
	
	private RunnerDatabaseRequestor searchLoader;
	
	private final Function<String, RecFile> sourceRecFile = new Function<String, RecFile>() {

		@Override
		public RecFile apply(String key) {

			return DownloadClient.this.searchByGuid(key);
		}
	};
	
	private boolean client;
	
	private TaskInterest taskInterest = null;
	
	private LockManager lockManager;
	
	private CacheL2<int[]> cacheDictionary;
	
	private IndexingDictionaryCached indexingDictionary;
	
	private Indexing currentIndexing;
	
	private IndexingFinder currentFinder;
	
	private final Map<String, Interest> interests = new HashMap<>();
	
	private final TreeSet<Integer> availableBuffer = new TreeSet<>();
	
	Set<Integer> available = Create.tempSet();
	
	/** @param guid
	 * @param srcHost
	 * @param srcPort
	 * @param idxHost
	 * @param idxPort
	 * @param index
	 * @param active
	 * @throws SQLException */
	public void addSource(final String guid, final String srcHost, final int srcPort, final String idxHost, final int idxPort, final boolean index, final boolean active)
			throws SQLException {
		
		try (final Connection conn = this.nextConnection()) {
			try (final PreparedStatement ps = conn.prepareStatement(
					"INSERT INTO d1Sources(srcGuid,srcHost,srcPort,idxHost,idxPort,srcCreated,srcMaintainer,srcChecked,srcIndex,srcActive,srcReady,srcHealth) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")) {
				ps.setString(1, guid);
				ps.setString(2, srcHost);
				ps.setInt(3, srcPort);
				ps.setString(4, idxHost);
				ps.setInt(5, idxPort);
				ps.setTimestamp(6, new Timestamp(Engine.fastTime()));
				ps.setString(7, "*");
				ps.setTimestamp(8, new Timestamp(0L));
				ps.setString(9, index
					? "Y"
					: "N");
				ps.setString(10, active
					? "Y"
					: "N");
				ps.setInt(11, 0);
				ps.setInt(12, 0);
				ps.execute();
			}
		}
	}
	
	void avaibility(final RecSource source, final boolean availability) {
		
		if (source.isActive()) {
			final boolean availabilityCalculated;
			try (final Connection conn = this.nextConnection()) {
				if (availability) {
					availabilityCalculated = true;
				} else {
					try (final PreparedStatement ps = conn.prepareStatement(
							"SELECT count(*) FROM d1SourceHistory WHERE srcLuid=? AND hstIdentity=? AND srcAlive=? AND logDate>?",
							ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY)) {
						ps.setInt(1, source.getLuid());
						ps.setString(2, this.identity);
						ps.setString(3, "Y");
						ps.setTimestamp(4, new Timestamp(Engine.fastTime() - 1000L * 60L * 30L));
						try (final ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								availabilityCalculated = rs.getInt(1) > 0;
							} else {
								availabilityCalculated = false;
							}
						}
					}
				}
				synchronized (this.availableBuffer) {
					if (availabilityCalculated) {
						if (this.availableBuffer.add(new Integer(source.getLuid()))) {
							this.available = Create.tempSet(this.availableBuffer);
						}
					} else {
						if (this.availableBuffer.remove(new Integer(source.getLuid()))) {
							this.available = Create.tempSet(this.availableBuffer);
						}
					}
				}
				try (final PreparedStatement ps = conn
						.prepareStatement("INSERT INTO d1SourceHistory(logDate,srcLuid,hstIdentity,srcHealth,srcReady,srcAlive) VALUES (?,?,?,?,?,?)")) {
					ps.setTimestamp(1, new Timestamp(Engine.fastTime()));
					ps.setInt(2, source.getLuid());
					ps.setString(3, this.identity);
					ps.setInt(4, (int) (source.getHealth() * 100));
					ps.setInt(5, (int) (source.getReady() * 100));
					ps.setString(6, availability
						? "Y"
						: "N");
					ps.execute();
				}
				return;
			} catch (final SQLException e) {
				Report.exception("DL_CLIENT", "Error checking and updating source history", e);
			}
		}
		synchronized (this.availableBuffer) {
			if (availability) {
				if (this.availableBuffer.add(new Integer(source.getLuid()))) {
					this.available = Create.tempSet(this.availableBuffer);
				}
			} else {
				if (this.availableBuffer.remove(new Integer(source.getLuid()))) {
					this.available = Create.tempSet(this.availableBuffer);
				}
			}
		}
	}
	
	/** @param source
	 * @param fix
	 * @return boolean */
	public boolean checkSource(final RecSource source, final boolean fix) {
		
		if (source.check()) {
			try (final Connection conn = this.nextConnection()) {
				if (conn == null) {
					throw new RuntimeException("Connection unavailable!");
				}
				{
					try (final PreparedStatement ps = conn
							.prepareStatement("UPDATE d1Sources SET srcGuid=?, srcMaintainer=?, srcChecked=?, srcHealth=?, srcReady=? WHERE srcLuid=?")) {
						ps.setString(1, source.getGuid());
						ps.setString(2, this.identity);
						ps.setTimestamp(3, new Timestamp(source.getChecked()));
						ps.setInt(4, (int) (source.getHealth() * 100));
						ps.setInt(5, (int) (source.getReady() * 100));
						ps.setInt(6, source.getLuid());
						ps.execute();
					}
				}
				if (fix) {
					try (final PreparedStatement ps = conn.prepareStatement(
							"INSERT INTO d1Queue(queFormation,queBusy,queQueued) SELECT i.itmLevel1Name,?,? FROM d1Items i LEFT OUTER JOIN d1KnownAliases a ON i.itmLevel1Name=a.alias LEFT OUTER JOIN d1Queue q ON i.itmLevel1Name=q.queFormation WHERE q.queFormation is NULL AND a.alias is NULL AND i.itmLevel1Name != '*' GROUP BY i.itmLevel1Name")) {
						ps.setTimestamp(1, new Timestamp(0L));
						ps.setTimestamp(2, new Timestamp(Engine.fastTime()));
						ps.execute();
					}
				}
				{
					final List<RecFolder> pending = new ArrayList<>();
					{
						final String query = new StringBuilder().append("SELECT f.fldLuid,f.fldParentLuid,f.fldName,f.fldChecked ").append("FROM d1Folders f ")
								.append("WHERE f.srcLuid=? ").append("AND (f.fldParentLuid=0 OR f.fldChecked<?) ")
								.append("GROUP BY f.fldLuid, f.fldParentLuid, f.fldName, f.fldChecked ").append("UNION ")
								.append("SELECT f.fldLuid,f.fldParentLuid,f.fldName,f.fldChecked ").append("FROM d1Folders f, d1Items i ")
								.append("WHERE i.fldLuid=f.fldLuid AND f.srcLuid=? ").append("AND i.itmPreview=? AND (f.fldParentLuid=0 OR f.fldChecked<?) ")
								.append("GROUP BY f.fldLuid, f.fldParentLuid, f.fldName, f.fldChecked ").append("ORDER BY f.fldChecked ASC").toString();
						try (final PreparedStatement ps = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
							ps.setMaxRows(50);
							ps.setInt(1, source.getLuid());
							ps.setTimestamp(2, new Timestamp(Engine.fastTime() - 1000L * 60L * 60L * 24L / 2L));
							ps.setInt(3, source.getLuid());
							ps.setString(4, "N");
							ps.setTimestamp(5, new Timestamp(Engine.fastTime() - 1000L * 60L * 60L * 24L / 8L));
							try (final ResultSet rs = ps.executeQuery()) {
								while (rs.next()) {
									final int luid = rs.getInt(1);
									final int parentLuid = rs.getInt(2);
									final String name = rs.getString(3);
									final long checked = rs.getTimestamp(4).getTime();
									pending.add(new RecFolder(this, luid, source.getLuid(), parentLuid, name, checked));
								}
							}
						}
					}
					if (pending.isEmpty()) {
						Report.info("DL_CLIENT", "New source detected - trying to create root folder record");
						RecFolder.create(conn, source.getLuid(), 0, ".");
					} else {
						for (final RecFolder folder : pending) {
							folder.check(conn);
						}
					}
				}
			} catch (final SQLException e) {
				Report.exception("DL_CLIENT", "Error while checking a source", e);
			}
			return true;
		}
		Report.info("DL_CLIENT", "Lost source connection: " + source);
		return false;
	}
	
	@Override
	public void destroy() {
		
		if (this.searchLoader != null) {
			this.searchLoader.stop();
			this.searchLoader = null;
		}
		if (this.lockManager != null) {
			this.lockManager.stop(this.identity);
			this.lockManager = null;
		}
		this.taskInterest.stop();
	}
	
	RecFolder getFolder(final int luid) {
		
		final String key = String.valueOf(luid);
		return this.cacheFolders.get(key, "", this.searchLoader, key, this.creatorFolder);
	}
	
	/** @return string */
	public String getIdentity() {
		
		return this.identity;
	}
	
	/** @param alias
	 * @return known array */
	public RecKnown[] getKnownByAlias(final String alias) {
		
		final RequestKnownByAlias request = new RequestKnownByAlias(this, alias);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	/** @param followLink
	 * @return known */
	public RecKnown getKnownByFollowLink(final String followLink) {
		
		if (followLink == null || followLink.trim().length() == 0) {
			return null;
		}
		final RequestKnownByFollowLink request = new RequestKnownByFollowLink(this, followLink);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	RunnerDatabaseRequestor getLoader() {
		
		return this.searchLoader;
	}
	
	@SuppressWarnings("static-method")
	String getMnemonicName() {
		
		return "Download";
	}
	
	RecSource getSource(final int luid) {
		
		final String key = String.valueOf(luid);
		return this.cacheSources.get(key, "", this.searchLoader, key, this.creatorSource);
	}
	
	/** @param key
	 * @return source */
	public RecSource getSource(final String key) {
		
		final RecSource[] sources = this.getSources(true);
		for (final RecSource source : sources) {
			if (source.getGuid().equals(key)) {
				return source;
			}
		}
		return null;
	}
	
	/** @param guid
	 * @return int */
	public final int getSourceFileCount(final String guid) {
		
		try (final Connection conn = this.nextConnection()) {
			try (final PreparedStatement ps = conn.prepareStatement(
					"SELECT count(*) FROM d1Items i INNER JOIN d1Folders f ON i.fldLuid=f.fldLuid INNER JOIN d1Sources s ON f.srcLuid=s.srcLuid WHERE s.srcGuid=?",
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY)) {
				ps.setString(1, guid);
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt(1);
					}
					return 0;
				}
			}
		} catch (final SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** @param all
	 * @return source array */
	public RecSource[] getSources(final boolean all) {
		
		if (this.searchLoader == null) {
			assert false : "No search loader? Stopped? id=" + this.identity;
			return new RecSource[0];
		}
		final RequestSources request = new RequestSources(this, all);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	/** @param source */
	public void interestCancel(final RecSource source) {
		
		final String guid = source.getGuid();
		synchronized (this.interests) {
			final Interest info = this.interests.get(guid);
			if (info != null) {
				Report.info("DL_CLIENT", "source dead, de-registering interest: " + info);
				this.lockManager.removeInterest(info);
				this.interests.remove(guid);
			}
		}
	}
	
	/** @param source */
	public void interestRegister(final RecSource source) {
		
		final String guid = source.getGuid();
		synchronized (this.interests) {
			final Interest check = this.interests.get(guid);
			if (check == null) {
				Report.info("DL_CLIENT", "source alive, registering interest: " + source);
				final Interest info = new Interest(guid, new TaskChecker(this, source));
				this.lockManager.addInterest(info);
				this.interests.put(guid, info);
			}
		}
	}
	
	/** @return connection */
	public Connection nextConnection() {
		
		return this.connectionSource.nextElement();
	}
	
	@Override
	public void register() {
		
		this.connectionSource = this.getServer().getConnections().get(this.connection);
		final BaseObject settingsPrivate = this.getSettingsPrivate();
		{
			String identity = Base.getString(settingsPrivate, "identity", "").trim();
			if (identity.length() == 0) {
				identity = Engine.createGuid();
				settingsPrivate.baseDefine("identity", identity);
			}
			this.identity = identity;
			this.commitPrivateSettings();
		}
		try {
			this.cacheRoot = new File(new File(Engine.PATH_CACHE, this.getServer().getZoneId()), "dls");
			this.cacheRoot.mkdirs();
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		this.getServer().getControlRoot().bind(new Node(this));
		this.taskInterest = new TaskInterest(this, this.client);
		((StatusRegistry) this.getServer().getRootContext().baseGet(ProvideStatus.REGISTRY_CONTEXT_KEY, BaseObject.UNDEFINED).baseValue()).register(new Status(this));
	}
	
	/** @param limit
	 * @param all
	 * @param timeout
	 * @param sort
	 * @param dateStart
	 * @param dateEnd
	 * @param filter
	 * @return file array */
	public List<RecFile> search(final int limit, final boolean all, final long timeout, final String sort, final long dateStart, final long dateEnd, final String filter) {
		
		if (this.currentFinder == null) {
			return null;
		}
		final String sortReplace = sort == null
			? null
			: DownloadClient.REPLACEMENT_SEARCH_SORT.get(sort);
		final ExecSearchProgram program = this.currentFinder.search(null, all, sortReplace == null
			? sort
			: sortReplace, dateStart, dateEnd, filter);
		if (program == null) {
			return null;
		}
		for (final long timeoutDate = timeout <= 0L
			? Long.MAX_VALUE
			: System.currentTimeMillis() + timeout; timeoutDate > System.currentTimeMillis();) {
			final ExecSearchInstruction instruction = program.nextItem();
			if (instruction == null) {
				break;
			}
			this.searchLoader.add(instruction);
			instruction.baseValue();
		}
		final Map.Entry<String, Object>[] result = program.baseValue();
		if (result == null || result.length == 0) {
			return null;
		}
		if (limit > 0 && result.length > limit) {
			final Map.Entry<String, Object>[] limited = Convert.Array.toAny(new Map.Entry[limit]);
			System.arraycopy(result, 0, limited, 0, limit);
			return new ListByMapEntryKey<>(limited, this.sourceRecFile);
		}
		return new ListByMapEntryKey<>(result, this.sourceRecFile);
	}
	
	/** @param followLink
	 * @return alias array */
	public RecAlias[] searchAliasesByFollowLink(final String followLink) {
		
		final RequestAliasesByFollowLink request = new RequestAliasesByFollowLink(this, followLink);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	/** @param guid
	 * @return file */
	public RecFile searchByGuid(final String guid) {
		
		final RequestByGuid request = new RequestByGuid(this, guid);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	final RecFile searchByGuid(final String guid, final Connection conn) {
		
		final StringBuilder query = new StringBuilder();
		query.append("SELECT i.itmCrc,i.itmLuid,i.fldLuid,i.itmName,i.itmSize,i.itmDate,i.itmType,i.itmComment,i.itmPreview,i.itmLevel2Name,i.itmLevel3Name ")
				.append("FROM d1Items i, d1Folders f, d1Sources s WHERE i.fldLuid=f.fldLuid AND f.srcLuid=s.srcLuid AND i.itmGuid=?")
				.append(" ORDER BY i.itmName ASC, s.srcReady DESC");
		try (final PreparedStatement ps = conn.prepareStatement(query.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, guid);
			try (final ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					final String md5 = rs.getString(1);
					final int itmLuid = rs.getInt(2);
					final int parentLuid = rs.getInt(3);
					final String name = rs.getString(4);
					final long size = rs.getLong(5);
					final long date = rs.getTimestamp(6).getTime();
					final String type = rs.getString(7);
					final String comment = rs.getString(8);
					final boolean preview = "Y".equals(rs.getString(9));
					final String level2Name = rs.getString(10);
					final String level3Name = rs.getString(11);
					return new RecFile(this, guid, itmLuid, parentLuid, name, md5, size, date, type, comment, preview, level2Name, level3Name, null);
				}
				return null;
			}
		} catch (final SQLException e) {
			throw new RuntimeException("searchByGuid", e);
		}
	}
	
	/** @param string
	 * @param typeFilter
	 * @param all
	 * @return file array */
	public final RecFile[] searchByMd5(final String string, final String typeFilter, final boolean all) {
		
		if (string == null || string.trim().length() == 0) {
			return DownloadClient.EMPTY_FILE_ARRAY;
		}
		final String key = "s5\n" + string + '\n' + typeFilter + '\n' + all;
		final RequestByMd5 request = new RequestByMd5(this, key, string, typeFilter, all);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	/** @param string
	 * @param typeFilter
	 * @param all
	 * @return file array */
	public final RecFile[] searchByName(final String string, final String typeFilter, final boolean all) {
		
		final String filter = SyntaxQuery.filterToWhere("i.itmSearchName", ":", null, null, false, string.toLowerCase());
		if (filter == null || filter.trim().length() == 0) {
			return DownloadClient.EMPTY_FILE_ARRAY;
		}
		final String key = "sn\n" + filter + '\n' + typeFilter + '\n' + all;
		final RequestByName request = new RequestByName(this, key, filter, typeFilter, all);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	/** @param alias
	 * @param typeFilter
	 * @param all
	 * @return group array */
	public RecFileGroup[] searchFileGroupsByAlias(final String alias, final String typeFilter, final boolean all) {
		
		if (alias == null || alias.trim().length() == 0) {
			return DownloadClient.EMPTY_GROUP_ARRAY;
		}
		final String key = "ga\n" + alias + '\n' + typeFilter + '\n' + all;
		final RequestGroupsByAlias request = new RequestGroupsByAlias(this, key, alias, typeFilter, all);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	/** @param guid
	 * @param typeFilter
	 * @param all
	 * @param described
	 * @return group array */
	public RecFileGroup[] searchFileGroupsByFollowLink(final String guid, final String typeFilter, final boolean all, final boolean described) {
		
		if (guid == null || guid.trim().length() == 0) {
			return DownloadClient.EMPTY_GROUP_ARRAY;
		}
		final String key = "gf\n" + guid + '\n' + typeFilter + '\n' + all + '\n' + described;
		return this.cacheFileGroups.get(key, "", this.searchLoader, key, new RequestGroupsByFollowLink(key, this, guid, typeFilter, all, described));
	}
	
	/** @param alias
	 * @param typeFilter
	 * @param all
	 * @return file array */
	public final RecFile[] searchFilesByAlias(final String alias, final String typeFilter, final boolean all) {
		
		if (alias == null || alias.trim().length() == 0) {
			return DownloadClient.EMPTY_FILE_ARRAY;
		}
		final RequestByAlias request = new RequestByAlias(this, alias, typeFilter, all);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	/** @param guid
	 * @param typeFilter
	 * @param all
	 * @return file array */
	public final RecFile[] searchFilesByFollowLink(final String guid, final String typeFilter, final boolean all) {
		
		if (guid == null || guid.trim().length() == 0) {
			return DownloadClient.EMPTY_FILE_ARRAY;
		}
		final RequestByFollowLink request = new RequestByFollowLink(this, guid, typeFilter, all);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	/** @param alias
	 * @param typeFilter
	 * @param all
	 * @return group array */
	public RecFileGroup[] searchFileVariantsByAlias(final String alias, final String typeFilter, final boolean all) {
		
		if (alias == null || alias.trim().length() == 0) {
			return DownloadClient.EMPTY_GROUP_ARRAY;
		}
		final String key = "va\n" + alias + '\n' + typeFilter + '\n' + all;
		final RequestVariantsByAlias request = new RequestVariantsByAlias(this, key, alias, typeFilter, all);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	/** @param guid
	 * @param typeFilter
	 * @param all
	 * @return group array */
	public RecFileGroup[] searchFileVariantsByFollowLink(final String guid, final String typeFilter, final boolean all) {
		
		if (guid == null || guid.trim().length() == 0) {
			return DownloadClient.EMPTY_GROUP_ARRAY;
		}
		final String key = "vf\n" + guid + '\n' + typeFilter + '\n' + all;
		final RequestVariantsByFollowLink request = new RequestVariantsByFollowLink(this, key, guid, typeFilter, all);
		this.searchLoader.add(request);
		return request.baseValue();
	}
	
	/** @param names
	 * @return known array */
	public RecKnown[] searchKnown(final String names) {
		
		final Set<String> aliases = Create.tempSet();
		final Set<RecKnown> pending = Create.tempSet();
		for (final StringTokenizer st = new StringTokenizer(names, "\n"); st.hasMoreTokens();) {
			final String name = st.nextToken().trim().toLowerCase();
			if (name.length() == 0) {
				continue;
			}
			final RecKnown cached = this.cacheKnown.get(name, "");
			if (cached == null) {
				aliases.add(name.toLowerCase());
			} else {
				pending.add(cached);
			}
		}
		if (aliases.isEmpty()) {
			return pending.toArray(new RecKnown[pending.size()]);
		}
		try (final Connection conn = this.nextConnection()) {
			if (conn == null) {
				throw new RuntimeException("Connection unavailable!");
			}
			{
				final StringBuilder query = new StringBuilder();
				query.append("SELECT k.guid,k.name,a.alias FROM d1Known k, d1KnownAliases a WHERE k.guid=a.guid AND a.alias in ('")
						.append(DownloadClient.joinSqlString(aliases, "','")).append("')");
				try (final PreparedStatement ps = conn.prepareStatement(query.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
					try (final ResultSet rs = ps.executeQuery()) {
						while (rs.next()) {
							final String guid = rs.getString(1);
							final String name = rs.getString(2);
							final String alias = rs.getString(3);
							final RecKnown known = new RecKnown(this, guid, name);
							this.cacheKnown.put(alias, "", known, 30L * 1000L * 60L);
							if (!name.toLowerCase().equals(alias)) {
								this.cacheKnown.put(name.toLowerCase(), "", known, 30L * 1000L * 60L);
							}
							pending.add(known);
						}
					}
				}
			}
			return pending.toArray(new RecKnown[pending.size()]);
		} catch (final SQLException e) {
			Report.exception("DL_CLIENT", "Error while searching for files", e);
			return DownloadClient.EMPTY_KNOWN_ARRAY;
		}
	}
	
	/** @param conn
	 * @param sequence
	 * @param type
	 * @param guid
	 * @param luid
	 * @throws SQLException */
	public final void serializeChange(final Connection conn, final int sequence, final String type, final String guid, final int luid) throws SQLException {
		
		try (final PreparedStatement ps = conn
				.prepareStatement("INSERT INTO d1ChangeQueue(evtId,evtDate,evtSequence,evtOwner,evtCmdType,evtCmdGuid,evtCmdLuid) VALUES (?,?,?,?,?,?,?)")) {
			ps.setString(1, Engine.createGuid());
			ps.setTimestamp(2, new Timestamp(System.currentTimeMillis())); // exact
			// precise
			// time!
			ps.setInt(3, sequence);
			ps.setString(4, this.identity);
			ps.setString(5, type);
			ps.setString(6, guid == null
				? "*"
				: guid);
			ps.setInt(7, luid);
			ps.execute();
		}
	}
	
	@Override
	public void setup() {
		
		final BaseObject info = this.getSettingsProtected();
		this.connection = Base.getString(info, "connection", "default");
		this.client = Convert.MapEntry.toBoolean(info, "client", false);
	}
	
	@Override
	public void start() {
		
		final BaseObject reflection = this.getServer().getRootContext().ri10GV;
		reflection.baseDefine("Download", Base.forUnknown(new StaticAPI(this)));
		this.lockManager = Lock.createManager(this.connectionSource, "d1Locks", this.identity);
		this.lockManager.start(this.identity);
		this.taskInterest.start();
		this.cacheDictionary = this.getServer().getCache().getCacheL2(this.getServer().getZoneId() + "-dls_dict");
		{
			final Entry rootEntry = this.getServer().getVfsRootEntry();
			final EntryContainer storageRoot = rootEntry.relativeFolderEnsure("storage/cache/dl/" + this.getIdentity());
			final EntryContainer dictExactRoot = storageRoot.relativeFolderEnsure("dict-exact");
			final EntryContainer dictInexactRoot = storageRoot.relativeFolderEnsure("dict-inxact");
			this.indexingDictionary = new IndexingDictionaryCached(new IndexingDictionaryVfs(dictExactRoot, dictInexactRoot, new DictionaryJdbc(this)), this.cacheDictionary);
		}
		this.currentIndexing = new Indexing(IndexingStemmer.SIMPLE_STEMMER, this.indexingDictionary, "d1Indices");
		final Map<String, String> finderReplacementFields = new HashMap<>();
		finderReplacementFields.put("$created", "i.itmCreated");
		finderReplacementFields.put("$modified", "i.itmDate");
		finderReplacementFields.put("$date", "i.itmDate");
		finderReplacementFields.put("$key", "i.itmSearchName");
		this.currentFinder = new IndexingFinder(
				this.currentIndexing.getStemmer(),
				this.currentIndexing.getDictionary(),
				"d1Indices",
				finderReplacementFields,
				"i.itmGuid",
				"i.itmDate",
				"d1Items i, d1Descriptions d WHERE ix.luid=i.itmLuid AND i.itmCrc=d.itmCrc AND d.itmHidden='N' AND (",
				"d1Items i WHERE ix.luid=i.itmLuid AND (");
		if (!this.client) {
			this.lockManager.addInterest(new Interest("update", new RunnerChangeUpdate(this, this.currentIndexing)));
		}
		this.searchLoader = new RunnerDatabaseRequestor("DLS-SEARCHER", this.connectionSource);
		Act.launchService(Exec.createProcess(null, "DLS-SEARCH: " + this.toString()), this.searchLoader);
	}
	
	final void statusFill(final StatusInfo data) {
		
		// data.put(
		// "API, changes created",
		// this.stsChangesCreated);
		// data.put(
		// "API, nested changes",
		// this.stsChangesNested);
		this.indexingDictionary.statusFill(data);
		this.searchLoader.statusFill(data);
	}
	
}
