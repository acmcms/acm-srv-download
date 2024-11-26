/*
 * Created on 31.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.myx.ae3.Engine;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferCollector;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Create;
import ru.myx.ae3.help.FileName;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.ae3.xml.Xml;

/** @author myx
 *
 *         Window - Preferences - Java - Code Style - Code Templates */
class Share {

	private static final Describer DESCRIBER_DEFAULT = new DescriberDefault();

	private static final FileFilter FILE_FILTER = new FileFilter() {

		@Override
		public final boolean accept(final File pathname) {

			return !pathname.isHidden() && pathname.getName().charAt(0) != '.' && !pathname.getName().equals("CVS");
		}
	};

	private static String calcCrc(final File file) throws Exception {

		if (file.length() == 0) {
			return "*";
		}
		try {
			final MD5 md5 = new MD5();
			final byte[] buffer = new byte[32768];
			try (final RandomAccessFile raf = new RandomAccessFile(file, "r")) {
				for (;;) {
					final int read = raf.read(buffer);
					if (read <= 0) {
						break;
					}
					md5.doUpdate(buffer, 0, read);
				}
				return md5.asHex();
			}
		} catch (final IOException e) {
			Report.exception("DL_SRV", "while counting MD5, file=" + file, e);
			return "*";
		}
	}

	private static final Map<String, BaseObject> getMapsMap(final BaseArray maps) {

		final Map<String, BaseObject> result = Create.tempMap();
		if (maps != null) {
			final int length = maps.length();
			for (int i = 0; i < length; ++i) {
				final BaseObject current = maps.baseGet(i, BaseObject.UNDEFINED);
				final String name = Base.getString(current, "name", "");
				result.put(name, current);
			}
		}
		return result;
	}

	private static final Set<String> getNames(final BaseArray maps) {

		final Set<String> result = Create.tempSet();
		if (maps != null) {
			final int length = maps.length();
			for (int i = 0; i < length; ++i) {
				final BaseObject current = maps.baseGet(i, BaseObject.UNDEFINED);
				assert current != null : "NULL java value";
				result.add(Base.getString(current, "name", ""));
			}
		}
		return result;
	}

	private static final Map<?, ?> makeTypeMap(final BaseObject settings) {

		final Map<String, Object> types = new HashMap<>();
		types.put("txt", new DescriberText());
		types.put("text", new DescriberText());
		types.put("gif", new DescriberImageGif(settings));
		types.put("jpg", new DescriberImageJpeg(settings));
		types.put("jpeg", new DescriberImageJpeg(settings));
		types.put("png", new DescriberImagePng(settings));
		types.put("bmp", new DescriberImage(settings));
		types.put("tif", new DescriberImage(settings));
		types.put("tiff", new DescriberImage(settings));
		types.put("mp3", new DescriberAudio(settings));
		types.put("ogg", new DescriberAudio(settings));
		types.put("wav", new DescriberAudio(settings));
		types.put("wma", new DescriberAudio(settings));
		types.put("mid", new DescriberAudio(settings));
		types.put("midi", new DescriberAudio(settings));
		types.put("avi", new DescriberVideo());
		types.put("wmv", new DescriberVideo());
		types.put("mpg", new DescriberVideo());
		types.put("mpeg", new DescriberVideo());
		types.put("rar", new DescriberArchive());
		types.put("tar", new DescriberArchive());
		types.put("arj", new DescriberArchive());
		types.put("jar", new DescriberArchiveZip());
		types.put("zip", new DescriberArchiveZip());
		return types;
	}

	private final String name;

	private final File root;

	private final File cache1;

	private final File cache2;

	private final File queue;

	private final Checker checker;

	private final Traffic traffic;

	private File queueCurrent;

	private final Map<?, ?> types;

	Share(final DownloadServer parent, final String name, final File root, final BaseObject settings) {
		
		this.name = name;
		this.root = root;
		final File dlsRoot = new File(root, ".dls_cache");
		this.cache1 = new File(dlsRoot, "data");
		this.cache1.mkdirs();
		this.cache2 = new File(dlsRoot, "data2");
		this.cache2.mkdirs();
		this.queue = new File(dlsRoot, "queue");
		this.queue.mkdirs();
		this.queueCurrent = new File(this.queue, "q" + Engine.fastTime() + ".queue");
		final String traffic = Base.getString(settings, "traffic", "").trim();
		this.traffic = parent.getServerTraffics().get(traffic);
		final String check = Base.getString(settings, "check", "").trim();
		if (check.length() == 0) {
			this.checker = null;
		} else {
			this.checker = parent.getServerChecks().get(check);
		}
		this.types = Share.makeTypeMap(settings);
	}

	private BaseObject getFolders(final BaseObject result, final File folder, final File cache1, final File cache2) throws Exception {

		final File checked = new File(cache1, ".date");
		final File index = new File(cache1, ".index");
		final BaseObject indexData;
		if (index.exists()) {
			BaseObject xmlData;
			try {
				xmlData = Xml.toBase("getFolders", Transfer.createCopier(index), StandardCharsets.UTF_8, null, null, null);
				if (checked.lastModified() + 5 * 60_000L > Engine.fastTime()) {
					result.baseDefineImportAllEnumerable(xmlData);
					return result;
				}
			} catch (final Throwable t) {
				Report.exception("DL_SRV", "while loading index, file=" + index, t);
				xmlData = new BaseNativeObject();
			}
			indexData = xmlData;
		} else {
			indexData = new BaseNativeObject();
		}
		final BaseArray collFolders = Convert.MapEntry.toCollection(indexData, "folder", null);
		final BaseArray collFiles = Convert.MapEntry.toCollection(indexData, "file", null);
		final Set<String> checkFolders = Share.getNames(collFolders);
		final Set<String> checkFiles = Share.getNames(collFiles);
		final Map<String, BaseObject> childrenFolders = Share.getMapsMap(collFolders);
		final Map<String, BaseObject> childrenFiles = Share.getMapsMap(collFiles);
		final File[] folders = folder.listFiles(Share.FILE_FILTER);
		if (folders != null && folders.length > 0) {
			for (int i = folders.length - 1; i >= 0; --i) {
				final File current = folders[i];
				final String name = current.getName();
				if (current.isDirectory()) {
					childrenFolders.put(name, new BaseNativeObject().putAppend("name", name));
					checkFolders.remove(name);
				} else {
					final int pos = name.lastIndexOf('.');
					if (pos != -1) {
						final String typeName = name.substring(pos + 1).toLowerCase();
						final Describer describer = (Describer) Convert.MapEntry.toObject(this.types, typeName, Share.DESCRIBER_DEFAULT);
						if (describer != null) {
							final boolean preview;
							if (describer.isThumbnailAvailable()) {
								if (describer.isPreviewAvailable()) {
									final File previewFile1 = new File(cache1, name);
									final File previewFile2 = new File(cache2, name);
									if (!previewFile1.exists() || !previewFile2.exists()) {
										try (final DataOutputStream out = new DataOutputStream(new FileOutputStream(this.queueCurrent, true))) {
											out.writeUTF(current.getAbsolutePath().substring(this.root.getAbsolutePath().length()));
										}
										preview = previewFile1.isFile() && previewFile1.length() > 0L;
									} else {
										preview = previewFile1.length() > 0L;
									}
								} else {
									final File previewFile = new File(cache1, name);
									if (!previewFile.exists()) {
										try (final DataOutputStream out = new DataOutputStream(new FileOutputStream(this.queueCurrent, true))) {
											out.writeUTF(current.getAbsolutePath().substring(this.root.getAbsolutePath().length()));
										}
										preview = false;
									} else {
										preview = previewFile.length() > 0L;
									}
								}
							} else {
								preview = false;
							}
							final BaseObject cached = childrenFiles.get(name);
							final long modified = current.lastModified();
							final int version = describer.getVersion();
							if (cached != null && Convert.MapEntry.toLong(cached, "date", 0L) == modified && Convert.MapEntry.toInt(cached, "version", -1) == version
									&& Convert.MapEntry.toBoolean(cached, "preview", false) == preview) {
								childrenFiles.put(name, cached);
								checkFiles.remove(name);
							} else {
								final long size = current.length();
								final String crc = Share.calcCrc(current);
								final BaseObject data = new BaseNativeObject()//
										.putAppend("name", name)//
										.putAppend("type", describer.getMediaType())//
										.putAppend("extension", FileName.extension(current))//
										.putAppend("typename", describer.getMediaTypeFor(current))//
										.putAppend("size", size)//
										.putAppend("crc", crc)//
										.putAppend("date", Base.forDateMillis(modified))//
										.putAppend("version", version)//
										.putAppend("preview", preview)//
								;
								try {
									if (describer.describe(typeName, current, data)) {
										childrenFiles.put(name, data);
										checkFiles.remove(name);
									}
								} catch (final Exception e) {
									Report.exception("DL_SRV", "exception, file=" + current, e);
								}
							}
						}
					}
				}
			}
		}
		if (!checkFolders.isEmpty()) {
			childrenFolders.keySet().removeAll(checkFolders);
		}
		if (!checkFiles.isEmpty()) {
			childrenFiles.keySet().removeAll(checkFiles);
			for (final String name : checkFiles) {
				{
					final File preview1 = new File(cache1, name);
					if (preview1.exists()) {
						preview1.delete();
					}
				}
				{
					final File preview2 = new File(cache2, name);
					if (preview2.exists()) {
						preview2.delete();
					}
				}
			}
		}
		if (!childrenFolders.isEmpty()) {
			indexData.baseDefine("folder", Base.forArray(childrenFolders.values().toArray()));
		} else {
			indexData.baseDelete("folder");
		}
		if (!childrenFiles.isEmpty()) {
			indexData.baseDefine("file", Base.forArray(childrenFiles.values().toArray()));
		} else {
			indexData.baseDelete("file");
		}
		try (final FileOutputStream indexFile = new FileOutputStream(index)) {
			indexFile.write(Xml.toXmlString("index", indexData, false).getBytes(StandardCharsets.UTF_8));
		}
		if (!checked.exists()) {
			checked.createNewFile();
		} else {
			checked.setLastModified(Engine.fastTime());
		}
		result.baseDefineImportAllEnumerable(indexData);
		return result;
	}

	/** @throws Exception */
	@SuppressWarnings("resource")
	public void check() throws Exception {

		synchronized (this.queueCurrent) {
			if (this.queueCurrent.exists()) {
				this.queueCurrent = new File(this.queue, "q" + Engine.fastTime() + ".queue");
			}
		}
		final File queueCurrent = this.queueCurrent;
		final File[] queues = this.queue.listFiles(new FileFilter() {

			@Override
			public final boolean accept(final File pathname) {

				return pathname != queueCurrent;
			}
		});
		if (queues != null && queues.length > 0) {
			final File queue = queues[Engine.createRandom(queues.length)];
			try (final DataInputStream in = new DataInputStream(new FileInputStream(queue))) {
				final List<String> todo = new ArrayList<>();
				try {
					for (;;) {
						final String line = in.readUTF();
						if (line == null) {
							break;
						}
						if (!todo.contains(line)) {
							todo.add(line);
						}
					}
				} catch (final EOFException e) {
					// ignore
				}
				if (!todo.isEmpty()) {
					Collections.shuffle(todo);
				}
				for (final String line : todo) {
					final File source = new File(this.root, line);
					if (source.isFile()) {
						final String name = source.getName();
						final int pos = name.lastIndexOf('.');
						if (pos != -1) {
							final String typeName = name.substring(pos + 1).toLowerCase();
							final Describer describer = (Describer) Convert.MapEntry.toObject(this.types, typeName, Share.DESCRIBER_DEFAULT);
							if (describer != null && describer.isThumbnailAvailable()) {
								final File target1 = new File(this.cache1, line);
								target1.getParentFile().mkdirs();
								if (describer.isPreviewAvailable()) {
									final File target2 = new File(this.cache2, line);
									if (!target1.exists() || target1.length() > 0 && !target2.exists()) {
										final TransferCollector collector1 = target1.exists()
											? null
											: Transfer.createCollector();
										final TransferCollector collector2 = target2.exists()
											? null
											: Transfer.createCollector();
										final long timeout = 120L * 1000L * source.length() / (1000L * 1000L) + 60_000L;
										final HungDetector detector = new HungDetector(timeout, target1);
										describer.buildTemporaryFiles(source, collector1, collector2);
										detector.done = true;
										if (collector1 != null) {
											collector1.close();
											Transfer.toStream(collector1.toBuffer(), new FileOutputStream(target1), true);
										}
										if (collector2 != null) {
											collector2.close();
											Transfer.toStream(collector2.toBuffer(), new FileOutputStream(target2), true);
										}
										new File(target1.getParentFile(), ".date").delete();
									}
								} else {
									if (!target1.exists()) {
										final TransferCollector collector = Transfer.createCollector();
										final long timeout = 60L * 1000L * source.length() / (1000L * 1000L) + 30_000L;
										final HungDetector detector = new HungDetector(timeout, target1);
										describer.buildTemporaryFiles(source, collector, null);
										detector.done = true;
										collector.close();
										Transfer.toStream(collector.toBuffer(), new FileOutputStream(target1), true);
										new File(target1.getParentFile(), ".date").delete();
									}
								}
							}
						}
					}
				}
			} catch (final Throwable t) {
				Report.exception("DLS/SHARE", "While exhausting queue", t);
			}
			queue.delete();
		}
	}

	/** @param query
	 * @return answer */
	public final ReplyAnswer checkAccess(final ServeRequest query) {

		if (this.checker == null) {
			return null;
		}
		return this.checker.check(query);
	}

	/** @return file */
	public File getCache1() {

		return this.cache1;
	}

	/** @return file */
	public File getCache2() {

		return this.cache2;
	}

	/** @param result
	 * @param path */
	public void getFolders(final BaseObject result, final String path) {

		final File folder = new File(this.root, path);
		if (folder.isDirectory()) {
			final File cache1 = new File(this.cache1, path);
			if (!cache1.exists()) {
				cache1.mkdirs();
			}
			final File cache2 = new File(this.cache2, path);
			if (!cache2.exists()) {
				cache2.mkdirs();
			}
			try {
				synchronized (this.queueCurrent) {
					this.getFolders(result, folder, cache1, cache2);
				}
				result.baseDefine("type", "listing");
			} catch (final Exception e) {
				Report.exception("DLS/SHARE", "listing", e);
				result.baseDefine("type", "exception");
				result.baseDefine("exception", Base.forThrowable(e));
			}
		} else {
			result.baseDefine("type", "no_path");
		}
	}

	/** @return file */
	public File getRoot() {

		return this.root;
	}

	/** @return traffic */
	public Traffic getTrafficDownload() {

		return this.traffic;
	}

	@Override
	public String toString() {

		return "DLS/SHARE{ name=" + this.name + ", root=" + this.root + " }";
	}
}
