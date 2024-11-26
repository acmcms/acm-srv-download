/*
 * Created on 30.06.2004
 */
package ru.myx.srv.download.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.myx.ae1.storage.ModuleInterface;
import ru.myx.ae2.indexing.Indexing;
import ru.myx.ae3.Engine;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Create;
import ru.myx.ae3.report.Report;
import ru.myx.jdbc.lock.Runner;
import ru.myx.util.EntrySimple;

/** @author myx */
final class RunnerChangeUpdate implements Runnable, Runner {

	private static final String OWNER = "D1/UPDATE";

	private static final int LIMIT_BULK_TASKS = 50;

	private static final int LIMIT_BULK_UPGRADE = 25;

	private static final int SEQ_HIGH = 4255397;

	private static final Set<String> SYSTEM_KEYS = RunnerChangeUpdate.createSystemKeys();

	private static final Set<String> createSystemKeys() {

		final Set<String> result = Create.tempSet();
		result.add("$key");
		result.add("$description");
		result.add("$level2");
		return result;
	}

	private static List<Object> doMaintainFixIndicesGetThem0(final Connection conn) throws SQLException {

		final String query = "SELECT t.itmGuid,t.itmLuid FROM d1Items t LEFT OUTER JOIN d1Indexed i ON t.itmLuid=i.luid WHERE i.luid is NULL AND t.itmCreated<?";
		try (final PreparedStatement ps = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			ps.setTimestamp(1, new Timestamp(Engine.fastTime() - 60_000L * 60L));
			try (final ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					final List<Object> result = new ArrayList<>();
					do {
						result.add(rs.getString(1));
						result.add(Integer.valueOf(rs.getInt(2)));
					} while (rs.next());
					return result;
				}
				return null;
			} catch (final SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final void eventDone(final Connection conn, final String evtId) throws SQLException {

		try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM d1ChangeQueue WHERE evtId=?")) {
			ps.setString(1, evtId);
			ps.execute();
		}
	}

	private final DownloadClient server;

	private final Indexing indexing;

	private final int indexingVersion;

	private long lastHealth;

	private boolean destroyed = false;

	RunnerChangeUpdate(final DownloadClient server, final Indexing indexing) {
		
		this.server = server;
		this.indexing = indexing;
		this.indexingVersion = indexing.getVersion();
	}

	private final void doClean(final Connection conn, final Map<String, Object> task) throws Throwable {

		final int luid = Convert.MapEntry.toInt(task, "evtCmdLuid", -1);
		if (luid != -1) {
			this.doCleanIndices(conn, luid);
		}
	}

	private final void doCleanIndices(final Connection conn, final int lnkLuid) throws Throwable {

		Report.event(RunnerChangeUpdate.OWNER, "CLEANING_INDEX", "luid=" + lnkLuid);
		this.indexing.doDelete(conn, lnkLuid);
		try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM d1Indices WHERE luid=?")) {
			ps.setInt(1, lnkLuid);
			ps.execute();
		}
	}

	private final boolean doCreate(final Connection conn, final Map<String, Object> task) throws Throwable {

		final String guid = Convert.MapEntry.toString(task, "evtCmdGuid", "").trim();
		final RecFile file = this.server.searchByGuid(guid);
		if (file != null) {
			final int luid = file.getKeyLocal();
			Report.event(RunnerChangeUpdate.OWNER, "INDEXING", "create file, guid=" + guid + ", luid=" + luid);
			this.doIndex(conn, file);
			this.eventIndexed(conn, file, true);
		}
		return false;
	}

	private final void doIndex(final Connection conn, final RecFile file) throws Throwable {

		final Map<String, Object> data = Create.tempMap();
		data.put("$key", file.getName());
		data.put("$description", file.getDescription(conn));
		data.put("$level2", file.getLevel2Name());
		this.indexing.doIndex(conn, null, null, ModuleInterface.STATE_PUBLISH, RunnerChangeUpdate.SYSTEM_KEYS, data, false, file.getKeyLocal());
	}

	private void doMaintain(final Connection conn, final BaseObject settings) {

		final int lastVersion = Convert.MapEntry.toInt(settings, "runnerVersion", 0);
		final long nextClean = Convert.MapEntry.toLong(settings, "deadCleanupDate", 0L);
		if (lastVersion < this.getVersion() || nextClean < Engine.fastTime()) {
			settings.baseDefine("deadCleanupDate", Base.forDateMillis(Engine.fastTime() + 60_000L * 60L * 24L));
			try {
				conn.setAutoCommit(false);
				try {
					this.doMaintainDropDuplicates(conn);
					conn.commit();
				} catch (final Throwable t) {
					try {
						conn.rollback();
					} catch (final Throwable tt) {
						// ignore
					}
					Report.exception(RunnerChangeUpdate.OWNER, "Unexpected exception while collecting garbage in storage", t);
					settings.baseDefine("deadCleanupDate", Base.forDateMillis(Engine.fastTime() + 30L * 60_000L));
				}
			} catch (final Throwable t) {
				try {
					conn.rollback();
				} catch (final Throwable tt) {
					// ignore
				}
				Report.exception(RunnerChangeUpdate.OWNER, "Unexpected exception while collecting garbage in storage", t);
				settings.baseDefine("deadCleanupDate", Base.forDateMillis(Engine.fastTime() + 30L * 60_000L));
			} finally {
				try {
					conn.setAutoCommit(true);
				} catch (final Throwable t) {
					// ignore
				}
			}
			settings.baseDefine("runnerVersion", this.getVersion());
			this.server.commitProtectedSettings();
		}
	}

	private void doMaintainClearSources(final Connection conn, final BaseObject settings) {

		final long nextClean = Convert.MapEntry.toLong(settings, "sourcesCleanupDate", 0L);
		if (nextClean < Engine.fastTime()) {
			try {
				try (final PreparedStatement ps = conn.prepareStatement("DELETE FROM d1SourceHistory WHERE logDate<?")) {
					ps.setTimestamp(1, new Timestamp(Engine.fastTime() - 60_000L * 60L * 24L * 7L));
					ps.execute();
				}
				settings.baseDefine("sourcesCleanupDate", Base.forDateMillis(Engine.fastTime() + 60_000L * 60L * 24L));
			} catch (final SQLException e) {
				settings.baseDefine("sourcesCleanupDate", Base.forDateMillis(Engine.fastTime() + 60_000L * 60L));
				throw new RuntimeException(e);
			} finally {
				this.server.commitProtectedSettings();
			}
		}
	}

	private void doMaintainDropDuplicates(final Connection conn) throws SQLException {

		List<Integer> result = null;
		{
			final String query = "SELECT MAX(itmLuid) as del FROM d1Items GROUP BY itmCrc, itmName, fldLuid HAVING count(*)>1";
			try (final PreparedStatement ps = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
				try (final ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						result = new ArrayList<>();
						do {
							result.add(Integer.valueOf(rs.getInt(1)));
						} while (rs.next());
					} else {
						return;
					}
				} catch (final SQLException e) {
					throw new RuntimeException(e);
				}
			}
		}
		for (final Integer luid : result) {
			{
				final String query = "DELETE FROM d1ItemLinkage WHERE itmGuid IN (SELECT itmGuid FROM d1Items WHERE itmLuid=?)";
				try (final PreparedStatement ps = conn.prepareStatement(query)) {
					ps.setInt(1, luid.intValue());
					ps.execute();
				}
			}
			{
				this.server.serializeChange(conn, 0, "clean", null, luid.intValue());
			}
			{
				final String query = "DELETE FROM d1Items WHERE itmLuid=?";
				try (final PreparedStatement ps = conn.prepareStatement(query)) {
					ps.setInt(1, luid.intValue());
					ps.execute();
				}
			}
		}
	}

	private void doMaintainIndexing(final Connection conn, final BaseObject settings) {

		final int indexingVersion = Convert.MapEntry.toInt(settings, "indexingVersion", 0);
		if (indexingVersion != this.indexingVersion) {
			try {
				this.server.serializeChange(conn, RunnerChangeUpdate.SEQ_HIGH, "upgrade-index", "*", this.indexingVersion);
			} catch (final SQLException e) {
				throw new RuntimeException(e);
			}
			settings.baseDefine("indexingVersion", this.indexingVersion);
			this.server.commitProtectedSettings();
		}
		final long nextClean = Convert.MapEntry.toLong(settings, "indexCheckDate", 0L);
		if (nextClean < Engine.fastTime()) {
			try {
				try {
					final List<Object> links = RunnerChangeUpdate.doMaintainFixIndicesGetThem0(conn);
					if (links != null && !links.isEmpty()) {
						for (int i = links.size(); i > 0;) {
							final int luid = ((Integer) links.get(--i)).intValue();
							final String guid = (String) links.get(--i);
							this.server.serializeChange(conn, 0, "update", guid, luid);
						}
					}
				} catch (final RuntimeException e) {
					throw e;
				}
				settings.baseDefine("indexCheckDate", Base.forDateMillis(Engine.fastTime() + 60_000L * 60L * 24L));
			} catch (final SQLException e) {
				settings.baseDefine("indexCheckDate", Base.forDateMillis(Engine.fastTime() + 60_000L * 60L));
				throw new RuntimeException(e);
			} finally {
				this.server.commitProtectedSettings();
			}
		}
	}

	private void doMaintainUpdateHealth(final Connection conn) {

		if (this.lastHealth < Engine.fastTime()) {
			this.lastHealth = Engine.fastTime() + 60_000L * 7L;
			try {
				try (final PreparedStatement ps = conn.prepareStatement("UPDATE d1Sources SET srcReady=0, srcHealth=0 WHERE (srcMaintainer=? AND srcChecked<?) OR srcChecked<?")) {
					ps.setString(1, this.server.getIdentity());
					ps.setTimestamp(2, new Timestamp(Engine.fastTime() - 60_000L * 7L));
					ps.setTimestamp(3, new Timestamp(Engine.fastTime() - 60_000L * 15L));
					ps.execute();
				}
			} catch (final SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private final void doUpdate(final Connection conn, final Map<String, Object> task) throws Throwable {

		final String guid = Convert.MapEntry.toString(task, "evtCmdGuid", "").trim();
		final int luid = Convert.MapEntry.toInt(task, "evtCmdLuid", -1);
		this.doUpdate(conn, guid, luid);
	}

	private final void doUpdate(final Connection conn, final String lnkId, final int luid) throws Throwable {

		final RecFile file = this.server.searchByGuid(lnkId, conn);
		if (file != null) {
			Report.event(RunnerChangeUpdate.OWNER, "INDEXING", "update file data, guid=" + lnkId + ", luid=" + luid);
			this.doIndex(conn, file);
			this.eventIndexed(conn, file, false);
			return;
		}
		if (luid != -1) {
			Report.event(RunnerChangeUpdate.OWNER, "CLEANING", "luid=" + luid);
			this.indexing.doDelete(conn, luid);
		}
	}

	private final void doUpdateAll(final Connection conn, final Map<String, Object> task) throws Throwable {

		final String crc = Convert.MapEntry.toString(task, "evtCmdGuid", "").trim();
		final List<Object> linkData = new ArrayList<>();
		try (final PreparedStatement ps = conn
				.prepareStatement("SELECT t.itmGuid,t.itmLuid FROM d1Items t WHERE t.itmCrc=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			ps.setString(1, crc);
			try (final ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					linkData.add(rs.getString(1));
					linkData.add(Integer.valueOf(rs.getInt(2)));
				}
			}
		}
		if (linkData.size() > 16) {
			for (int i = linkData.size(); i > 0;) {
				final int luid = ((Integer) linkData.get(--i)).intValue();
				final String id = (String) linkData.get(--i);
				this.server.serializeChange(conn, 0, "update", id, luid);
			}
		} else {
			for (int i = linkData.size(); i > 0;) {
				final int luid = ((Integer) linkData.get(--i)).intValue();
				final String id = (String) linkData.get(--i);
				this.doUpdate(conn, id, luid);
			}
		}
	}

	private final void doUpgradeIndex(final Connection conn, final int toVersion) throws Throwable {

		if (this.indexingVersion < toVersion) {
			this.server.serializeChange(conn, RunnerChangeUpdate.SEQ_HIGH, "upgrade-index", "*", toVersion);
			return;
		}
		final List<Map.Entry<String, Integer>> toUpgrade = new ArrayList<>();
		try (final PreparedStatement ps = conn.prepareStatement(
				"SELECT t.itmGuid,t.itmLuid FROM d1Items t, d1Indexed i WHERE t.itmLuid=i.luid AND i.idxVersion<?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY)) {
			ps.setInt(1, toVersion);
			ps.setMaxRows(RunnerChangeUpdate.LIMIT_BULK_UPGRADE);
			try (final ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					toUpgrade.add(new EntrySimple<>(rs.getString(1), Integer.valueOf(rs.getInt(2))));
				}
			}
		}
		if (!toUpgrade.isEmpty()) {
			if (toUpgrade.size() == RunnerChangeUpdate.LIMIT_BULK_UPGRADE) {
				this.server.serializeChange(conn, RunnerChangeUpdate.SEQ_HIGH, "upgrade-index", "*", this.indexingVersion);
			}
			for (final Map.Entry<String, Integer> entry : toUpgrade) {
				this.server.serializeChange(conn, 0, "update", entry.getKey(), entry.getValue().intValue());
			}
		}
	}

	private final void doUpgradeIndex(final Connection conn, final Map<String, Object> task) throws Throwable {

		final int toVersion = Convert.MapEntry.toInt(task, "evtCmdLuid", -1);
		this.doUpgradeIndex(conn, toVersion);
	}

	private final void eventIndexed(final Connection conn, final RecFile file, final boolean created) throws SQLException {

		if (created) {
			try {
				try (final PreparedStatement ps = conn.prepareStatement("INSERT INTO d1Indexed(luid,idxVersion,itmIndexed) VALUES (?,?,?)")) {
					ps.setInt(1, file.getKeyLocal());
					ps.setInt(2, this.indexingVersion);
					ps.setTimestamp(3, new Timestamp(Engine.fastTime()));
					ps.execute();
				}
			} catch (final SQLException e) {
				try (final PreparedStatement ps = conn.prepareStatement("UPDATE d1Indexed SET idxVersion=?, itmIndexed=? WHERE luid=?")) {
					ps.setInt(1, this.indexingVersion);
					ps.setTimestamp(2, new Timestamp(Engine.fastTime()));
					ps.setInt(3, file.getKeyLocal());
					ps.execute();
				}
			}
		} else {
			final int updated;
			try (final PreparedStatement ps = conn.prepareStatement("UPDATE d1Indexed SET idxVersion=?, itmIndexed=? WHERE luid=?")) {
				ps.setInt(1, this.indexingVersion);
				ps.setTimestamp(2, new Timestamp(Engine.fastTime()));
				ps.setInt(3, file.getKeyLocal());
				updated = ps.executeUpdate();
			}
			if (updated == 0) {
				try (final PreparedStatement ps = conn.prepareStatement("INSERT INTO d1Indexed(luid,idxVersion,itmIndexed) VALUES (?,?,?)")) {
					ps.setInt(1, file.getKeyLocal());
					ps.setInt(2, this.indexingVersion);
					ps.setTimestamp(3, new Timestamp(Engine.fastTime()));
					ps.execute();
				}
			}
		}
	}

	@Override
	public int getVersion() {

		return 10;
	}

	@Override
	public void run() {

		if (this.destroyed) {
			return;
		}
		final Connection conn;
		try {
			conn = this.server.nextConnection();
			if (conn == null) {
				if (!this.destroyed) {
					Act.later(this.server.getServer().getRootContext(), this, 10_000L);
				}
				return;
			}
		} catch (final Throwable t) {
			if (!this.destroyed) {
				Act.later(this.server.getServer().getRootContext(), this, 10_000L);
			}
			return;
		}
		boolean highLoad = false;
		try {
			// do maintenance
			{
				final BaseObject settings = this.server.getSettingsProtected();
				this.doMaintainUpdateHealth(conn);
				this.doMaintainClearSources(conn, settings);
				this.doMaintain(conn, settings);
				this.doMaintainIndexing(conn, settings);
				this.server.commitProtectedSettings();
			}
			// do update
			{
				final List<Map<String, Object>> tasks;
				try {
					try (final PreparedStatement ps = conn.prepareStatement(
							"SELECT evtId,evtCmdType,evtCmdGuid,evtCmdLuid FROM d1ChangeQueue ORDER BY evtDate ASC, evtSequence ASC",
							ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY)) {
						ps.setMaxRows(RunnerChangeUpdate.LIMIT_BULK_TASKS);
						try (final ResultSet rs = ps.executeQuery()) {
							if (rs.next()) {
								tasks = new ArrayList<>();
								do {
									final Map<String, Object> task = Create.tempMap();
									task.put("evtId", rs.getString(1));
									task.put("evtCmdType", rs.getString(2));
									task.put("evtCmdGuid", rs.getString(3));
									task.put("evtCmdLuid", Integer.valueOf(rs.getInt(4)));
									tasks.add(task);
								} while (rs.next());
							} else {
								tasks = null;
							}
						}
					}
				} catch (final SQLException e) {
					throw new RuntimeException(e);
				}
				if (tasks != null) {
					highLoad = tasks.size() >= RunnerChangeUpdate.LIMIT_BULK_TASKS;
					try {
						conn.setAutoCommit(false);
						for (final Map<String, Object> task : tasks) {
							final String taskType = Convert.MapEntry.toString(task, "evtCmdType", "").trim();
							if ("create".equals(taskType)) {
								highLoad |= this.doCreate(conn, task);
							} else //
							if ("update".equals(taskType)) {
								this.doUpdate(conn, task);
							} else //
							if ("update-all".equals(taskType)) {
								this.doUpdateAll(conn, task);
							} else //
							if ("clean".equals(taskType)) {
								this.doClean(conn, task);
							} else //
							if ("upgrade-index".equals(taskType)) {
								this.doUpgradeIndex(conn, task);
							}
							RunnerChangeUpdate.eventDone(conn, Convert.MapEntry.toString(task, "evtId", "").trim());
							conn.commit();
						}
					} catch (final Throwable e) {
						Report.exception(RunnerChangeUpdate.OWNER, "Exception while updating a download database", e);
					}
				}
			}
		} finally {
			try {
				conn.close();
			} catch (final Throwable t) {
				// ignore
			}
			if (!this.destroyed) {
				Act.later(
						this.server.getServer().getRootContext(),
						this,
						highLoad
							? 5_000L
							: 30_000L);
			}
		}
	}

	@Override
	public void start() {

		this.destroyed = false;
		Act.later(this.server.getServer().getRootContext(), this, 10_000L);
	}

	@Override
	public void stop() {

		this.destroyed = true;
	}

	@Override
	public String toString() {

		return "Runner change update {server=" + this.server + "}";
	}
}
