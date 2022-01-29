/*
 * Created on 17.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import ru.myx.ae1.AbstractPluginInstance;
import ru.myx.ae1.handle.Handle;
import ru.myx.ae1.know.AbstractServer;
import ru.myx.ae1.know.Server;
import ru.myx.ae3.Engine;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Create;
import ru.myx.ae3.help.Text;
import ru.myx.ae3.serve.ServeRequest;
import ru.myx.ae3.xml.Xml;

/** @author myx
 *
 *         Window - Preferences - Java - Code Style - Code Templates */
public class DownloadServer extends AbstractServer {
	
	private static final int DEF_MAX_CONN_TOTAL = 1000;
	
	private static final int DEF_MAX_CONN_ADDRESS = 2;
	
	private static final Function<DownloadServer, Object> TASK_MAINTENANCE = new TaskMaintenance();
	
	private final static ReplyAnswer createXmlResponse(final ServeRequest query, final BaseObject result) {
		
		return Reply.string("DLSRV", query, Xml.toXmlString("response", result, false))//
				.setContentType("text/xml")//
				.setNoCaching()//
				.setPrivate()//
				.setEncoding(StandardCharsets.UTF_8)//
				.setFinal();
	}
	
	private static ReplyAnswer respondPlayAsx(final ServeRequest request) {
		
		final String url = request.getUrl();
		final int pos = url.indexOf('?');
		final String identifier = pos == -1
			? url
			: url.substring(0, pos);
		final String target = identifier.substring(0, identifier.length() - "/@play.asx".length());
		final String content = new StringBuilder().append("<ASX Version=\"3.0\">").append("<ENTRY>").append("<REF HREF=\"").append(Text.encodeUri(target, StandardCharsets.UTF_8))
				.append("\"/>").append("</ENTRY>").append("</ASX>").toString();
		return Reply.binary("DLSRV", request, Transfer.createBuffer(content.getBytes()), "play.asx").setPrivate().setFinal();
	}
	
	private static ReplyAnswer respondPlayM3u(final ServeRequest request) {
		
		final String url = request.getUrl();
		final int pos = url.indexOf('?');
		final String identifier = pos == -1
			? url
			: url.substring(0, pos);
		final String target = identifier.substring(0, identifier.length() - "/@play.m3u".length());
		final String content = "#EXTM3U\r\n" + Text.encodeUri(target, StandardCharsets.UTF_8);
		return Reply.binary("DLSRV", request, Transfer.createBuffer(content.getBytes()), "play.m3u").setPrivate().setFinal();
	}
	
	private static ReplyAnswer respondPlayPls(final ServeRequest request) {
		
		final String url = request.getUrl();
		final int pos = url.indexOf('?');
		final String identifier = pos == -1
			? url
			: url.substring(0, pos);
		final String target = identifier.substring(0, identifier.length() - "/@play.pls".length());
		final String content = "[playlist]\r\nNumberOfEntries=1\r\nVersion=2\r\nFile1=" + Text.encodeUri(target, StandardCharsets.UTF_8);
		return Reply.binary("DLSRV", request, Transfer.createBuffer(content.getBytes()), "play.pls").setPrivate().setFinal();
	}
	
	private static ReplyAnswer respondPreviewAsx(final ServeRequest request) {
		
		final String url = request.getUrl();
		final int pos = url.indexOf('?');
		final String identifier = pos == -1
			? url
			: url.substring(0, pos);
		final String target = identifier.substring(0, identifier.length() - "/@preview.asx".length()) + "/@preview.audio";
		final String content = new StringBuilder().append("<ASX Version=\"3.0\">").append("<ENTRY>").append("<REF HREF=\"").append(Text.encodeUri(target, StandardCharsets.UTF_8))
				.append("\"/>").append("</ENTRY>").append("</ASX>").toString();
		return Reply.binary("DLSRV", request, Transfer.createBuffer(content.getBytes()), "preview.asx").setPrivate().setFinal();
	}
	
	private static ReplyAnswer respondPreviewM3u(final ServeRequest request) {
		
		final String url = request.getUrl();
		final int pos = url.indexOf('?');
		final String identifier = pos == -1
			? url
			: url.substring(0, pos);
		final String target = identifier.substring(0, identifier.length() - "/@preview.m3u".length()) + "/@preview.audio";
		final String content = "#EXTM3U\r\n" + Text.encodeUri(target, StandardCharsets.UTF_8);
		return Reply.binary("DLSRV", request, Transfer.createBuffer(content.getBytes()), "preview.m3u").setPrivate().setFinal();
	}
	
	private static ReplyAnswer respondPreviewPls(final ServeRequest request) {
		
		final String url = request.getUrl();
		final int pos = url.indexOf('?');
		final String identifier = pos == -1
			? url
			: url.substring(0, pos);
		final String target = identifier.substring(0, identifier.length() - "/@preview.pls".length()) + "/@preview.audio";
		final String content = "[playlist]\r\nNumberOfEntries=1\r\nVersion=2\r\nFile1=" + Text.encodeUri(target, StandardCharsets.UTF_8);
		return Reply.binary("DLSRV", request, Transfer.createBuffer(content.getBytes()), "preview.pls").setPrivate().setFinal();
	}
	
	private final String check;
	
	private final AbstractPluginInstance settingsSelf;
	
	private final AbstractPluginInstance settingsRoots;
	
	private final long startDate;
	
	private final CounterBlock counter;
	
	private final int maxConnTotal;
	
	private final int maxConnAddress;
	
	private Map<String, Share> serverRoots;
	
	private long serverSettingsDate;
	
	private Map<String, Checker> serverChecks;
	
	private Map<String, TrafficFilter> serverTraffics;
	
	/** @param id
	 * @param check
	 * @param attributes
	 */
	public DownloadServer(final String id, final String check, final BaseObject attributes) {
		
		super(id, Base.getString(attributes, "domain", id), Exec.currentProcess());
		this.check = check;
		this.startDate = Engine.fastTime();
		this.settingsSelf = new AbstractPluginInstance() {
			// empty
		};
		{
			final Properties serverPluginProperties = new Properties();
			serverPluginProperties.setProperty("id", "$$self");
			this.settingsSelf.setup(this, serverPluginProperties);
		}
		this.settingsRoots = new AbstractPluginInstance() {
			// empty
		};
		{
			final Properties serverPluginProperties = new Properties();
			serverPluginProperties.setProperty("id", "shares");
			this.settingsRoots.setup(this, serverPluginProperties);
		}
		Act.later(this.getRootContext(), DownloadServer.TASK_MAINTENANCE, this, 10000L);
		final BaseObject selfSettings = this.settingsSelf.getSettingsProtected();
		this.maxConnTotal = Convert.MapEntry.toInt(selfSettings, "max-conn-total", DownloadServer.DEF_MAX_CONN_TOTAL);
		this.maxConnAddress = Convert.MapEntry.toInt(selfSettings, "max-conn-address", DownloadServer.DEF_MAX_CONN_ADDRESS);
		selfSettings.baseDefine("max-conn-total", this.maxConnTotal);
		selfSettings.baseDefine("max-conn-address", this.maxConnAddress);
		this.settingsSelf.commitProtectedSettings();
		this.counter = new CounterBlock(this.maxConnTotal, this.maxConnAddress);
	}
	
	@Override
	public boolean absorb(final ServeRequest request) {
		
		final ReplyAnswer response;
		if ("DELETE".equals(request.getVerb())) {
			final Server server = Handle.getServer(this.check);
			response = server != null
				? this.respondDeleteFile(request)
				: Reply.stringForbidden("DLS", request, "Check failed");
		} else //
		if ("CREATE".equals(request.getVerb())) {
			final Server server = Handle.getServer(this.check);
			response = server != null
				? this.respondCreateFile(request)
				: Reply.stringForbidden("DLS", request, "Check failed");
		} else {
			final String identifierOriginal = request.getResourceIdentifier();
			if ("/@welcome.xml".equals(identifierOriginal)) {
				response = this.respondWelcome(request);
			} else //
			if ("/@load.xml".equals(identifierOriginal)) {
				response = this.respondLoad(request);
			} else //
			if (identifierOriginal.endsWith("/@listing.xml")) {
				response = this.respondFolders(request);
			} else //
			if (identifierOriginal.endsWith("/@listing.htm")) {
				response = this.respondFoldersHtm(request);
			} else //
			if (identifierOriginal.endsWith("/@play.m3u")) {
				response = DownloadServer.respondPlayM3u(request);
			} else //
			if (identifierOriginal.endsWith("/@play.pls")) {
				response = DownloadServer.respondPlayPls(request);
			} else //
			if (identifierOriginal.endsWith("/@play.asx")) {
				response = DownloadServer.respondPlayAsx(request);
			} else //
			if (identifierOriginal.endsWith("/@preview.m3u")) {
				response = DownloadServer.respondPreviewM3u(request);
			} else //
			if (identifierOriginal.endsWith("/@preview.pls")) {
				response = DownloadServer.respondPreviewPls(request);
			} else //
			if (identifierOriginal.endsWith("/@preview.asx")) {
				response = DownloadServer.respondPreviewAsx(request);
			} else //
			if (identifierOriginal.endsWith("/@preview.audio")) {
				response = this.respondPreviewAudio(request);
			} else //
			if (identifierOriginal.endsWith("/@preview.image")) {
				response = this.respondPreviewImage1(request);
			} else //
			if (identifierOriginal.endsWith("/@preview2.image")) {
				response = this.respondPreviewImage2(request);
			} else {
				final String address = request.getSourceAddress();
				final int index = CounterBlock.makeIndex(address);
				final int action = this.counter.incrementCheck(index, address);
				if (action == CounterBlock.GRANTED) {
					request.setAttribute("execute", this.counter.getCounter(index));
					final boolean download = !identifierOriginal.endsWith(".open");
					response = this.respondDownload(
							request, //
							identifierOriginal.endsWith(".rename"),
							download //
					);
				} else //
				if (action == CounterBlock.DENIED_TOTAL) {
					response = Reply.string(
							"DLSRV", //
							request,
							"Connection limit (total): " + "system is out of concurrent connection limit of " + this.maxConnTotal + " connections!" //
					) //
							.setCode(Reply.CD_BUSY)//
							.setAttribute("Retry-After", 60)//
							.setPrivate()//
							.setNoCaching()//
							.setFinal();
				} else //
				if (action == CounterBlock.DENIED_ADDRESS) {
					response = Reply.string(
							"DLSRV", //
							request,
							"Connection limit (per address): " + "your IP(" + address + ") is out of concurrent connection limit of " + this.maxConnAddress + " connections!" //
					) //
							.setCode(Reply.CD_BUSY) //
							.setAttribute("Retry-After", 60)//
							.setPrivate() //
							.setNoCaching() //
							.setFinal();
				} else {
					response = Reply.string(
							"DLSRV", //
							request,
							"Connection limit (unknown)!" //
					) //
							.setCode(Reply.CD_BUSY) //
							.setAttribute("Retry-After", 60) //
							.setPrivate() //
							.setNoCaching() //
							.setFinal();
				}
			}
		}
		request.getResponseTarget().apply(response);
		return true;
	}
	
	final CounterBlock getLoadCounter() {
		
		return this.counter;
	}
	
	final Map<String, Checker> getServerChecks() {
		
		if (this.serverChecks == null) {
			synchronized (this) {
				if (this.serverChecks == null) {
					this.serverChecks = this.internCreateServerChecks();
					this.serverSettingsDate = Engine.fastTime();
				}
			}
		}
		final boolean rebuild;
		if (this.serverSettingsDate < Engine.fastTime() - 1000L * 60L) {
			synchronized (this) {
				if (this.serverSettingsDate < Engine.fastTime() - 1000L * 60L) {
					rebuild = true;
					this.serverSettingsDate = Engine.fastTime();
				} else {
					rebuild = false;
				}
			}
		} else {
			rebuild = false;
		}
		if (rebuild) {
			Act.whenIdle(null, new ServerSettngsRebuilderTask(), this);
		}
		return this.serverChecks;
	}
	
	private final String getServerIdentity() {
		
		final String serverIdentity;
		{
			final String identityCheck = Base.getString(this.settingsSelf.getSettingsPrivate(), "identity", "").trim();
			if (identityCheck.length() == 0) {
				serverIdentity = Engine.createGuid();
				this.settingsSelf.getSettingsPrivate().baseDefine("identity", serverIdentity);
				this.settingsSelf.commitPrivateSettings();
			} else {
				serverIdentity = identityCheck;
			}
		}
		return serverIdentity;
	}
	
	final Map<String, Share> getServerRoots() {
		
		if (this.serverRoots == null) {
			synchronized (this) {
				if (this.serverRoots == null) {
					this.serverRoots = this.internCreateServerRoots();
					this.serverSettingsDate = Engine.fastTime();
				}
			}
		}
		final boolean rebuild;
		if (this.serverSettingsDate < Engine.fastTime() - 1000L * 60L) {
			synchronized (this) {
				if (this.serverSettingsDate < Engine.fastTime() - 1000L * 60L) {
					rebuild = true;
					this.serverSettingsDate = Engine.fastTime();
				} else {
					rebuild = false;
				}
			}
		} else {
			rebuild = false;
		}
		if (rebuild) {
			Act.whenIdle(null, new Runnable() {

				@Override
				public void run() {

					DownloadServer.this.rebuildServerSettings();
				}
			});
		}
		return this.serverRoots;
	}
	
	final Map<String, TrafficFilter> getServerTraffics() {
		
		if (this.serverTraffics == null) {
			synchronized (this) {
				if (this.serverTraffics == null) {
					this.serverTraffics = this.internCreateServerTraffics();
					this.serverSettingsDate = Engine.fastTime();
				}
			}
		}
		final boolean rebuild;
		if (this.serverSettingsDate < Engine.fastTime() - 1000L * 60L) {
			synchronized (this) {
				if (this.serverSettingsDate < Engine.fastTime() - 1000L * 60L) {
					rebuild = true;
					this.serverSettingsDate = Engine.fastTime();
				} else {
					rebuild = false;
				}
			}
		} else {
			rebuild = false;
		}
		if (rebuild) {
			Act.whenIdle(null, new Runnable() {

				@Override
				public void run() {

					DownloadServer.this.rebuildServerSettings();
				}
			});
		}
		return this.serverTraffics;
	}
	
	private final Map<String, Checker> internCreateServerChecks() {
		
		final Map<String, Checker> serverChecks = Create.tempMap();
		final BaseArray checks = Convert.MapEntry.toCollection(this.settingsRoots.getSettingsProtected(), "check", null);
		if (checks != null) {
			final int length = checks.length();
			for (int i = 0; i < length; ++i) {
				final BaseObject check = checks.baseGet(i, BaseObject.UNDEFINED);
				final String name = Base.getString(check, "name", "").trim();
				if (name.length() > 0) {
					serverChecks.put(name, new Checker(check));
				}
			}
		}
		return serverChecks;
	}
	
	private final Map<String, Share> internCreateServerRoots() {
		
		final BaseArray shares = Convert.MapEntry.toCollection(this.settingsRoots.getSettingsProtected(), "share", null);
		final Map<String, Share> serverRoots = Create.tempMap();
		if (shares != null) {
			final int length = shares.length();
			for (int i = 0; i < length; ++i) {
				final BaseObject share = shares.baseGet(i, BaseObject.UNDEFINED);
				final String name = Base.getString(share, "name", "").trim();
				final String path = Base.getString(share, "path", "").trim();
				if (name.length() > 0 && path.length() > 0) {
					serverRoots.put(name, new Share(this, name, new File(path), share));
				}
			}
		}
		return serverRoots;
	}
	
	private final Map<String, TrafficFilter> internCreateServerTraffics() {
		
		final Map<String, TrafficFilter> serverTraffics = Create.tempMap();
		final BaseArray traffics = Convert.MapEntry.toCollection(this.settingsRoots.getSettingsProtected(), "traffic", null);
		if (traffics != null) {
			final int length = traffics.length();
			for (int i = 0; i < length; ++i) {
				final BaseObject traffic = traffics.baseGet(i, BaseObject.UNDEFINED);
				final String name = Base.getString(traffic, "name", "").trim();
				if (name.length() > 0) {
					if (this.serverTraffics != null) {
						final TrafficFilter existing = this.serverTraffics.get(name);
						if (existing != null) {
							existing.setup(traffic);
							serverTraffics.put(name, existing);
							continue;
						}
					}
					serverTraffics.put(name, new TrafficFilter(this, name, this.counter, traffic));
				}
			}
		}
		return serverTraffics;
	}
	
	final void rebuildServerSettings() {
		
		this.serverRoots = this.internCreateServerRoots();
		this.serverChecks = this.internCreateServerChecks();
		this.serverTraffics = this.internCreateServerTraffics();
	}
	
	private ReplyAnswer respondCreateFile(final ServeRequest request) {
		
		final String share;
		final String path;
		{
			final String original = request.getResourceIdentifier();
			final String identifier = original.substring(0, original.length());
			final int startPos = identifier.indexOf('/', 1);
			if (startPos == -1) {
				share = null;
				path = "/";
			} else {
				share = identifier.substring(1, startPos);
				path = identifier.substring(startPos + 1);
			}
		}
		final BaseObject result = new BaseNativeObject();
		result.baseDefine("url", request.getUrl());
		if (path == null) {
			result.baseDefine("error", "invalid request: " + request.getResourceIdentifier());
		} else {
			if (share == null) {
				final Map<String, Share> roots = this.getServerRoots();
				final BaseArrayDynamic<Object> folders = BaseObject.createArray();
				for (final String name : roots.keySet()) {
					folders.baseDefaultPush(new BaseNativeObject("name", name));
				}
				result.baseDefine("type", "shares");
				result.baseDefine("folder", folders);
			} else {
				result.baseDefine("share", share);
				result.baseDefine("path", path);
				final Share root = this.getServerRoots().get(share);
				if (root == null) {
					result.baseDefine("type", "no_share");
				} else {
					final ReplyAnswer pre = root.checkAccess(request);
					if (pre != null) {
						return pre;
					}
					final File file = new File(root.getRoot(), path);
					if (file.isFile()) {
						file.delete();
						result.baseDefine("type", "deleted");
					}
					result.baseDefine("type", "no_path");
				}
			}
		}
		return DownloadServer.createXmlResponse(request, result);
	}
	
	private ReplyAnswer respondDeleteFile(final ServeRequest request) {
		
		final String share;
		final String path;
		{
			final String original = request.getResourceIdentifier();
			final String identifier = original.substring(0, original.length());
			final int startPos = identifier.indexOf('/', 1);
			if (startPos == -1) {
				share = null;
				path = "/";
			} else {
				share = identifier.substring(1, startPos);
				path = identifier.substring(startPos + 1);
			}
		}
		final BaseObject result = new BaseNativeObject();
		result.baseDefine("url", request.getUrl());
		if (path == null) {
			result.baseDefine("error", "invalid request: " + request.getResourceIdentifier());
		} else {
			if (share == null) {
				final Map<String, Share> roots = this.getServerRoots();
				final BaseArrayDynamic<Object> folders = BaseObject.createArray();
				for (final String name : roots.keySet()) {
					folders.baseDefaultPush(new BaseNativeObject("name", name));
				}
				result.baseDefine("type", "shares");
				result.baseDefine("folder", folders);
			} else {
				result.baseDefine("share", share);
				result.baseDefine("path", path);
				final Share root = this.getServerRoots().get(share);
				if (root == null) {
					result.baseDefine("type", "no_share");
				} else {
					final ReplyAnswer pre = root.checkAccess(request);
					if (pre != null) {
						return pre;
					}
					final File file = new File(root.getRoot(), path);
					if (file.isFile()) {
						file.delete();
						result.baseDefine("type", "deleted");
					}
					result.baseDefine("type", "no_path");
				}
			}
		}
		return DownloadServer.createXmlResponse(request, result);
	}
	
	private ReplyAnswer respondDownload(final ServeRequest request, final boolean rename, final boolean download) {
		
		final String share;
		final String path;
		{
			final String identifier;
			if (!download) {
				final String original = request.getResourceIdentifier();
				identifier = original.substring(0, original.length() - ".open".length());
			} else {
				if (rename) {
					final String original = request.getResourceIdentifier();
					identifier = original.substring(0, original.length() - ".rename".length());
				} else {
					identifier = request.getResourceIdentifier();
				}
			}
			final int startPos = identifier.indexOf('/', 1);
			if (startPos == -1) {
				share = null;
				path = "/";
			} else {
				share = identifier.substring(1, startPos);
				path = identifier.substring(startPos + 1);
			}
		}
		final BaseObject result = new BaseNativeObject();
		result.baseDefine("url", request.getUrl());
		if (path == null) {
			result.baseDefine("error", "invalid request: " + request.getResourceIdentifier());
		} else {
			if (share == null) {
				final Map<String, Share> roots = this.getServerRoots();
				final BaseArrayDynamic<Object> folders = BaseObject.createArray();
				for (final String name : roots.keySet()) {
					folders.baseDefaultPush(new BaseNativeObject("name", name));
				}
				result.baseDefine("type", "shares");
				result.baseDefine("folder", folders);
			} else {
				result.baseDefine("share", share);
				result.baseDefine("path", path);
				final Share root = this.getServerRoots().get(share);
				if (root == null) {
					result.baseDefine("type", "no_share");
				} else {
					final ReplyAnswer pre = root.checkAccess(request);
					if (pre != null) {
						return pre;
					}
					final File file = new File(root.getRoot(), path);
					if (file.isFile()) {
						final Traffic traffic = root.getTrafficDownload();
						final ReplyAnswer answer;
						if (rename) {
							answer = Reply.file("DLSRV", request, file, file.getName() + ".rename");
							if (download) {
								answer.setAttribute("Content-Disposition", "attachment; filename=\"" + file.getName() + ".rename\"");
							}
							answer.setPrivate().setFinal();
						} else {
							answer = Reply.file("DLSRV", request, file, file.getName());
							if (download) {
								answer.setAttribute("Content-Disposition", "attachment; filename=\"" + file.getName() + '"');
							}
							answer.setPrivate().setFinal();
						}
						if (traffic != null) {
							return traffic.finalizeResponse(request, answer, true);
						}
						return answer;
					}
					result.baseDefine("type", "no_path");
				}
			}
		}
		return DownloadServer.createXmlResponse(request, result);
	}
	
	private ReplyAnswer respondFolders(final ServeRequest request) {
		
		final String share;
		final String path;
		{
			final String identifier = request.getResourceIdentifier();
			final int endPos = identifier.lastIndexOf("/@");
			final int startPos = identifier.indexOf('/', 1);
			if (endPos == -1) {
				share = null;
				path = null;
			} else {
				if (startPos == -1) {
					share = null;
					path = "/";
				} else {
					share = identifier.substring(1, startPos);
					path = endPos == startPos
						? ""
						: identifier.substring(startPos + 1, endPos);
				}
			}
		}
		final BaseObject result = new BaseNativeObject();
		result.baseDefine("url", request.getUrl());
		if (path == null) {
			result.baseDefine("error", "invalid request: " + request.getResourceIdentifier());
		} else {
			if (share == null) {
				final Map<String, Share> roots = this.getServerRoots();
				final BaseArrayDynamic<Object> folders = BaseObject.createArray();
				for (final String name : roots.keySet()) {
					folders.baseDefaultPush(new BaseNativeObject("name", name));
				}
				result.baseDefine("type", "shares");
				result.baseDefine("folder", folders);
			} else {
				result.baseDefine("share", share);
				result.baseDefine("path", path);
				final Share root = this.getServerRoots().get(share);
				if (root == null) {
					result.baseDefine("type", "no_share");
				} else {
					root.getFolders(result, path);
				}
			}
		}
		return DownloadServer.createXmlResponse(request, result);
	}
	
	private ReplyAnswer respondFoldersHtm(final ServeRequest request) {
		
		final String share;
		final String path;
		{
			final String identifier = request.getResourceIdentifier();
			final int endPos = identifier.lastIndexOf("/@");
			final int startPos = identifier.indexOf('/', 1);
			if (endPos == -1) {
				share = null;
				path = null;
			} else {
				if (startPos == -1) {
					share = null;
					path = "/";
				} else {
					share = identifier.substring(1, startPos);
					path = endPos == startPos
						? ""
						: identifier.substring(startPos + 1, endPos);
				}
			}
		}
		final Share root;
		final BaseObject result = new BaseNativeObject();
		result.baseDefine("url", request.getUrl());
		if (path == null) {
			result.baseDefine("error", "invalid request: " + request.getResourceIdentifier());
			root = null;
		} else {
			if (share == null) {
				final Map<String, Share> roots = this.getServerRoots();
				final BaseArrayDynamic<Object> folders = BaseObject.createArray();
				for (final String name : roots.keySet()) {
					folders.baseDefaultPush(new BaseNativeObject("name", name));
				}
				result.baseDefine("type", "shares");
				result.baseDefine("folder", folders);
				root = null;
			} else {
				result.baseDefine("share", share);
				result.baseDefine("path", path);
				root = this.getServerRoots().get(share);
				if (root == null) {
					result.baseDefine("type", "no_share");
				} else {
					root.getFolders(result, path);
				}
			}
		}
		final StringBuilder html = new StringBuilder().append("<html><title>").append(share).append('/').append(path).append("</title><body>");
		if (share != null) {
			html.append("<a href=../@listing.htm><b>&nbsp;.&nbsp;.&nbsp;</b></a><br>");
		}
		final BaseArray folders = Convert.MapEntry.toCollection(result, "folder", null);
		if (folders != null) {
			final int length = folders.length();
			for (int i = 0; i < length; ++i) {
				final BaseObject current = folders.baseGet(i, BaseObject.UNDEFINED);
				final String name = Base.getString(current, "name", "");
				html.append("<a href=").append('"').append(name).append("/@listing.htm").append('"').append('>').append(name).append("</a><br>");
			}
		}
		html.append("<p>");
		final BaseArray files = Convert.MapEntry.toCollection(result, "file", null);
		if (files != null) {
			final int length = files.length();
			for (int i = 0; i < length; ++i) {
				final BaseObject current = files.baseGet(i, BaseObject.UNDEFINED);
				final String name = Base.getString(current, "name", "");
				html.append("<p><b>").append(name).append("</b><br><small>").append(current.baseGet("output", BaseObject.UNDEFINED));
				html.append("<br>[<a href=").append('"').append(name).append('"').append(">download</a>]");
				html.append("&nbsp;[<a href=").append('"').append(name).append(".open").append('"').append(">open</a>]");
				html.append("&nbsp;[<a href=").append('"').append(name).append(".rename").append('"').append(">download RENAME</a>]");
				final String type = Base.getString(current, "type", "");
				if ("image".equals(type)) {
					if (Convert.MapEntry.toBoolean(current, "preview", false)) {
						html.append("&nbsp[<a target=_blank href=").append('"').append(name).append("/@preview2.image").append('"').append(">preview2</a>]");
					}
					html.append("&nbsp[<a target=_blank href=").append('"').append(name).append('"').append(">open</a>]");
					if (Convert.MapEntry.toBoolean(current, "preview", false)) {
						html.append("<br><img src=").append('"').append(name).append("/@preview.image").append('"').append('>');
					}
				}
				if ("audio".equals(type)) {
					html.append("&nbsp[<a href=").append('"').append(name).append("/@play.m3u").append('"').append(">play M3U</a>]");
					html.append("&nbsp[<a href=").append('"').append(name).append("/@play.pls").append('"').append(">play PLS</a>]");
					html.append("&nbsp[<a href=").append('"').append(name).append("/@play.asx").append('"').append(">play ASX</a>]");
					if (Convert.MapEntry.toBoolean(current, "preview", false)) {
						html.append("&nbsp[<a href=").append('"').append(name).append("/@preview.m3u").append('"').append(">preview M3U</a>]");
						html.append("&nbsp[<a href=").append('"').append(name).append("/@preview.pls").append('"').append(">preview PLS</a>]");
						html.append("&nbsp[<a href=").append('"').append(name).append("/@preview.asx").append('"').append(">preview ASX</a>]");
					}
				}
				if ("video".equals(type)) {
					html.append("&nbsp[<a href=").append('"').append(name).append("/@play.m3u").append('"').append(">play</a>]");
				}
				html.append("</small></p>");
			}
		}
		html.append("</body></html>");
		return Reply.string("DLSRV", request, html.toString()).setContentType("text/html").setNoCaching().setPrivate().setEncoding(StandardCharsets.UTF_8).setFinal();
	}
	
	private ReplyAnswer respondLoad(final ServeRequest request) {
		
		final Map<String, AtomicInteger> load = Create.tempMap();
		this.counter.fillLoad(load);
		return DownloadServer.createXmlResponse(request, Base.forUnknown(load));
	}
	
	private ReplyAnswer respondPreviewAudio(final ServeRequest request) {
		
		final String share;
		final String path;
		{
			final String identifier = request.getResourceIdentifier().substring(0, request.getResourceIdentifier().length() - "/@preview.audio".length());
			final int startPos = identifier.indexOf('/', 1);
			if (startPos == -1) {
				share = null;
				path = "/";
			} else {
				share = identifier.substring(1, startPos);
				path = identifier.substring(startPos + 1);
			}
		}
		final BaseObject result = new BaseNativeObject();
		result.baseDefine("url", request.getUrl());
		if (path == null) {
			result.baseDefine("error", "invalid request: " + request.getResourceIdentifier());
		} else {
			if (share == null) {
				final Map<String, Share> roots = this.getServerRoots();
				final BaseArrayDynamic<Object> folders = BaseObject.createArray();
				for (final String name : roots.keySet()) {
					folders.baseDefaultPush(new BaseNativeObject("name", name));
				}
				result.baseDefine("type", "shares");
				result.baseDefine("folder", folders);
			} else {
				result.baseDefine("share", share);
				result.baseDefine("path", path);
				final Share root = this.getServerRoots().get(share);
				if (root == null) {
					result.baseDefine("type", "no_share");
				} else {
					final ReplyAnswer pre = root.checkAccess(request);
					if (pre != null) {
						return pre;
					}
					final File file = new File(root.getCache1(), path);
					if (file.isFile()) {
						final Traffic traffic = root.getTrafficDownload();
						final ReplyAnswer answer = Reply.file("DLSRV", request, file, "preview.mp3").setPrivate().setFinal();
						if (traffic != null) {
							return traffic.finalizeResponse(request, answer, false);
						}
						return answer;
					}
					result.baseDefine("type", "no_path");
				}
			}
		}
		return DownloadServer.createXmlResponse(request, result);
	}
	
	private ReplyAnswer respondPreviewImage1(final ServeRequest request) {
		
		final String share;
		final String path;
		{
			final String identifier = request.getResourceIdentifier().substring(0, request.getResourceIdentifier().length() - "/@preview.image".length());
			final int startPos = identifier.indexOf('/', 1);
			if (startPos == -1) {
				share = null;
				path = "/";
			} else {
				share = identifier.substring(1, startPos);
				path = identifier.substring(startPos + 1);
			}
		}
		final BaseObject result = new BaseNativeObject();
		result.baseDefine("url", request.getUrl());
		if (path == null) {
			result.baseDefine("error", "invalid request: " + request.getResourceIdentifier());
		} else {
			if (share == null) {
				final Map<String, Share> roots = this.getServerRoots();
				final BaseArrayDynamic<Object> folders = BaseObject.createArray();
				for (final String name : roots.keySet()) {
					folders.baseDefaultPush(new BaseNativeObject("name", name));
				}
				result.baseDefine("type", "shares");
				result.baseDefine("folder", folders);
			} else {
				result.baseDefine("share", share);
				result.baseDefine("path", path);
				final Share root = this.getServerRoots().get(share);
				if (root == null) {
					result.baseDefine("type", "no_share");
				} else {
					final ReplyAnswer pre = root.checkAccess(request);
					if (pre != null) {
						return pre;
					}
					final File file = new File(root.getCache1(), path);
					if (file.isFile()) {
						return Reply.file("DLSRV", request, file, "preview.jpg").setPrivate().setFinal();
					}
					result.baseDefine("type", "no_path");
				}
			}
		}
		return DownloadServer.createXmlResponse(request, result);
	}
	
	private ReplyAnswer respondPreviewImage2(final ServeRequest request) {
		
		final String share;
		final String path;
		{
			final String identifier = request.getResourceIdentifier().substring(0, request.getResourceIdentifier().length() - "/@preview.image".length());
			final int startPos = identifier.indexOf('/', 1);
			if (startPos == -1) {
				share = null;
				path = "/";
			} else {
				share = identifier.substring(1, startPos);
				path = identifier.substring(startPos + 1);
			}
		}
		final BaseObject result = new BaseNativeObject();
		result.baseDefine("url", request.getUrl());
		if (path == null) {
			result.baseDefine("error", "invalid request: " + request.getResourceIdentifier());
		} else {
			if (share == null) {
				final Map<String, Share> roots = this.getServerRoots();
				final BaseArrayDynamic<Object> folders = BaseObject.createArray();
				for (final String name : roots.keySet()) {
					folders.baseDefaultPush(new BaseNativeObject("name", name));
				}
				result.baseDefine("type", "shares");
				result.baseDefine("folder", folders);
			} else {
				result.baseDefine("share", share);
				result.baseDefine("path", path);
				final Share root = this.getServerRoots().get(share);
				if (root == null) {
					result.baseDefine("type", "no_share");
				} else {
					final ReplyAnswer pre = root.checkAccess(request);
					if (pre != null) {
						return pre;
					}
					final File file = new File(root.getCache2(), path);
					if (file.isFile() && file.length() > 0L) {
						return Reply.file("DLSRV", request, file, "preview.jpg").setPrivate().setFinal();
					}
					final File original = new File(root.getRoot(), path);
					return Reply.file("DLSRV", request, original, "original.jpg").setPrivate().setFinal();
				}
			}
		}
		return DownloadServer.createXmlResponse(request, result);
	}
	
	private ReplyAnswer respondWelcome(final ServeRequest request) {
		
		final BaseArrayDynamic<Object> support = BaseObject.createArray();
		support.baseDefaultPush(Base.forString("folders"));
		support.baseDefaultPush(Base.forString("auth"));
		
		final BaseObject instance = new BaseNativeObject()//
				.putAppend("identity", this.getServerIdentity())//
				.putAppend("started", Base.forDateMillis(this.startDate))//
				.putAppend("shares", this.getServerRoots().size())//
		;

		final BaseObject impl = new BaseNativeObject()//
				.putAppend("support", support)//
				.putAppend("version", "1.03")//
		;

		final int currentConnections = this.counter.getValue();
		final BaseObject state = new BaseNativeObject()//
				.putAppend("load", currentConnections)//
				.putAppend("ready", Math.max(1.0 * (this.maxConnTotal - currentConnections) / this.maxConnTotal, 0.0d))//
				.putAppend("health", 1.0)//
				.putAppend("uptime", Engine.fastTime() - this.startDate)//
		;

		final BaseObject limits = new BaseNativeObject()//
				.putAppend("connections-total", this.maxConnTotal)//
				.putAppend("connections-address", this.maxConnAddress)//
		;

		final BaseObject welcome = new BaseNativeObject()//
				.putAppend("instance", instance)//
				.putAppend("impl", impl)//
				.putAppend("state", state)//
				.putAppend("limits", limits)//
		;
		return DownloadServer.createXmlResponse(request, welcome);
	}
}
