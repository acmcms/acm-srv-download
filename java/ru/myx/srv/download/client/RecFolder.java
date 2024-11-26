/*
 * Created on 21.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

import ru.myx.ae3.Engine;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.xml.Xml;

/** @author myx
 *
 *         Window - Preferences - Java - Code Style - Code Templates */
public final class RecFolder {
	
	/** @param conn
	 * @param srcLuid
	 * @param parentLuid
	 * @param name
	 * @throws SQLException */
	public static final void create(final Connection conn, final int srcLuid, final int parentLuid, final String name) throws SQLException {
		
		try (final PreparedStatement ps = conn.prepareStatement("INSERT INTO d1Folders(srcLuid,fldParentLuid,fldName,fldCreated,fldChecked,fldCrc) VALUES (?,?,?,?,?,?)")) {
			ps.setInt(1, srcLuid);
			ps.setInt(2, parentLuid);
			ps.setString(3, name);
			ps.setTimestamp(4, new Timestamp(Engine.fastTime()));
			ps.setTimestamp(5, new Timestamp(0L));
			ps.setString(6, "*");
			ps.execute();
		}
		if (parentLuid == 0) {
			Report.info("DLC_FOLDER", "Root folder created");
		} else {
			Report.info("DLC_FOLDER", "Folder created, name=" + name);
		}
	}
	
	private final DownloadClient client;
	
	private final int luid;
	
	private final int srcLuid;
	
	private final int parentLuid;
	
	private final String name;
	
	private final long checked;
	
	private RecFolder parent;
	
	private RecSource source;
	
	RecFolder(final DownloadClient client, final int luid, final int srcLuid, final int parentLuid, final String name, final long checked) {
		
		this.client = client;
		this.luid = luid;
		this.srcLuid = srcLuid;
		this.parentLuid = parentLuid;
		this.name = name;
		this.checked = checked;
	}
	
	/** @param conn
	 * @throws SQLException */
	public void check(final Connection conn) throws SQLException {
		
		if (this.checked + 10 * 60_000L > Engine.fastTime()) {
			return;
		}
		final String url = this.getPathIndexing() + "@listing.xml";
		final String response;
		try {
			final ExecProcess ctx = Exec.createProcess(null, url);
			
			if (true) {
				// final BaseFunction function = Base.createFunction("url",
				// "return require('http').get.asString(url) || '';");
				final BaseFunction function = Base.createFunction("return require('http').get.asString(this) || '';");
				response = function.callSE0(ctx, Base.forString(url));
			} else {
				ctx.contextCreateMutableBinding("url", url, false);
				response = Evaluate.evaluateObject("require('http').get.asString(url) || ''", ctx, null).toString();
			}
		} catch (final Throwable e) {
			Report.warning("DLC_FOLDER", "[" + url + "] cannot connect: " + e);
			return;
		}
		final BaseObject listing;
		try {
			listing = Xml.toBase("folderListingParse", response, null, null, null);
		} catch (final Throwable e) {
			Report.warning("DLC_FOLDER", "[" + url + "] error parsing: " + e);
			return;
		}
		if (!Base.hasKeys(listing)) {
			Report.info("DLC_FOLDER", "Got empty listing:\r\n" + Xml.toXmlString("listing", listing, true));
		} else {
			{
				final Map<String, RecFolder> current = this.getFolders();
				final BaseArray folders = Convert.MapEntry.toCollection(listing, "folder", null);
				if (folders != null) {
					final int length = folders.length();
					for (int i = 0; i < length; ++i) {
						final BaseObject data = folders.baseGet(i, BaseObject.UNDEFINED);
						assert data != null : "NULL java object";
						if (!data.baseIsPrimitive()) {
							final String name = data.baseGet("name", BaseObject.UNDEFINED).baseToJavaString();
							if (current.remove(name) == null) {
								Report.info("DLC_FOLDER", "Got new folder: " + name);
								RecFolder.create(conn, this.srcLuid, this.getLuid(), name);
							} else {
								Report.info("DLC_FOLDER", "Got existing folder: " + name);
							}
						}
					}
				}
				for (final String key : current.keySet()) {
					Report.info("DLC_FOLDER", "Got dead folder: " + key);
				}
			}
			{
				final Map<String, RecFile> current = this.getFiles();
				final BaseArray files = Convert.MapEntry.toCollection(listing, "file", null);
				if (files != null) {
					int updated = 0;
					final int length = files.length();
					for (int i = 0; i < length; ++i) {
						final BaseObject data = files.baseGet(i, BaseObject.UNDEFINED);
						assert data != null : "NULL java object";
						if (!data.baseIsPrimitive()) {
							final String name = data.baseGet("name", BaseObject.UNDEFINED).baseToJavaString();
							final RecFile file = current.remove(name);
							if (file == null) {
								Report.info("DLC_FOLDER", "Got new file: " + name);
								final String md5 = Base.getString(data, "crc", "*");
								final int size = Convert.MapEntry.toInt(data, "size", 0);
								final long date = Convert.MapEntry.toLong(data, "date", 0L);
								final String type = Base.getString(data, "type", "n/a");
								final String comment = Base.getString(data, "output", "n/a");
								final boolean preview = Convert.MapEntry.toBoolean(data, "preview", false);
								RecFile.create(conn, this.client, md5, this.getLuid(), name, size, date, type, comment, preview);
							} else {
								final String md5 = Base.getString(data, "crc", "*");
								final int size = Convert.MapEntry.toInt(data, "size", 0);
								final long date = Convert.MapEntry.toLong(data, "date", 0L);
								final boolean preview = Convert.MapEntry.toBoolean(data, "preview", false);
								final NameParser parser = RecFile.checkParser(name, file);
								if (!md5.equals(file.getMd5()) || file.getSize() != size || file.getDate() / 1000L != date / 1000L || file.hasPreview() != preview
										|| parser != null && parser.checkEquals(file)) {
									if (file.update(conn, md5, size, date, preview, parser)) {
										updated++;
									}
								}
								if (Report.MODE_DEBUG) {
									Report.info("DLC_FOLDER", "Got existing file: " + name);
								}
							}
						}
					}
					if (updated > 0) {
						Report.info("DLC_FOLDER", "files updated: count=" + updated + ", folder=" + this.name);
					}
				}
				{
					int deleted = 0;
					for (final String name : current.keySet()) {
						if (Report.MODE_DEBUG) {
							Report.info("DLC_FOLDER", "Got dead file: " + name);
						}
						deleted++;
						RecFile.remove(conn, this.client, this.getLuid(), name);
					}
					if (deleted > 0) {
						Report.info("DLC_FOLDER", "files deleted: " + deleted);
					}
				}
			}
		}
		{
			final String query = "UPDATE d1Folders SET fldChecked=? WHERE fldLuid=?";
			try (final PreparedStatement ps = conn.prepareStatement(query)) {
				ps.setTimestamp(1, new Timestamp(Engine.fastTime()));
				ps.setInt(2, this.getLuid());
				ps.execute();
			}
		}
	}
	
	/** @return files */
	public final Map<String, RecFile> getFiles() {
		
		final RequestSubFiles request = new RequestSubFiles(this.client, this.luid);
		this.client.getLoader().add(request);
		return request.baseValue();
	}
	
	/** @return folders */
	public final Map<String, RecFolder> getFolders() {
		
		final RequestSubFolders request = new RequestSubFolders(this.client, this.luid, this.srcLuid);
		this.client.getLoader().add(request);
		return request.baseValue();
	}
	
	/** @return int */
	public final int getLuid() {
		
		return this.luid;
	}
	
	/** @return string */
	public final String getName() {
		
		return this.name;
	}
	
	/** @return folder */
	public final RecFolder getParent() {
		
		if (this.parent == null) {
			this.parent = this.client.getFolder(this.parentLuid);
		}
		return this.parent;
	}
	
	/** @return int */
	public final int getParentLuid() {
		
		return this.parentLuid;
	}
	
	/** @return string
	 * @throws SQLException */
	public final String getPath() throws SQLException {
		
		final RecFolder parent = this.getParent();
		if (parent == null) {
			final RecSource source = this.getSource();
			return "http://" + source.getHost() + ':' + source.getPort() + '/';
		}
		return parent.getPath() + this.name + '/';
	}
	
	/** @return string
	 * @throws SQLException */
	public final String getPathIndexing() throws SQLException {
		
		final RecFolder parent = this.getParent();
		if (parent == null) {
			final RecSource source = this.getSource();
			return "http://" + source.getHostIndexing() + ':' + source.getPortIndexing() + '/';
		}
		return parent.getPathIndexing() + this.name + '/';
	}
	
	/** @return string
	 * @throws SQLException */
	public final String getPathLocal() throws SQLException {
		
		final RecFolder parent = this.getParent();
		if (parent == null) {
			final RecSource source = this.getSource();
			return source.getLuid() + "/";
		}
		return parent.getPathLocal() + this.name + '/';
	}
	
	/** @return source */
	public final RecSource getSource() {
		
		if (this.source == null) {
			this.source = this.client.getSource(this.srcLuid);
		}
		return this.source;
	}
	
	/** @return int */
	public final int getSourceLuid() {
		
		return this.srcLuid;
	}
	
	@Override
	public final String toString() {
		
		return "FOLDER{ luid=" + this.luid + ", name=" + this.name + " }";
	}
}
