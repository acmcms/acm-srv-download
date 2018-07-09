/*
 * Created on 20.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client;

import ru.myx.ae3.Engine;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.xml.Xml;

/**
 * @author myx
 *
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class RecSource {
	
	private final DownloadClient client;

	private final int luid;

	private String guid;

	private final String srcHost;

	private final int srcPort;

	private final String idxHost;

	private final int idxPort;

	private final long created;

	private long checked;

	private final boolean index;

	private final boolean active;

	private String version;

	private double health;

	private double ready;

	/**
	 * @param client
	 * @param luid
	 * @param guid
	 * @param srcHost
	 * @param srcPort
	 * @param idxHost
	 * @param idxPort
	 * @param created
	 * @param checked
	 * @param index
	 * @param active
	 */
	public RecSource(
			final DownloadClient client,
			final int luid,
			final String guid,
			final String srcHost,
			final int srcPort,
			final String idxHost,
			final int idxPort,
			final long created,
			final long checked,
			final boolean index,
			final boolean active) {
		this.client = client;
		this.luid = luid;
		this.guid = guid;
		this.srcHost = srcHost;
		this.srcPort = srcPort;
		this.idxHost = idxHost;
		this.idxPort = idxPort;
		this.created = created;
		this.checked = checked;
		this.index = index;
		this.active = active;
		this.version = "n/a";
		this.health = 0.0;
		this.ready = 0.0;
	}

	/**
	 * @return boolean
	 */
	public boolean check() {
		
		final String url = "http://" + this.idxHost + ':' + this.idxPort + "/@welcome.xml";
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
			Report.warning("DLC_SERVER", "cannot connect [" + this.idxHost + ':' + this.idxPort + "]: " + e);
			return false;
		}
		final BaseObject welcome;
		try {
			welcome = Xml.toBase("sourceXml", response, null, null, null);
		} catch (final Throwable e) {
			Report.exception("DLC_SERVER", "cannot parse", e);
			return false;
		}
		final BaseObject welcomeInstance = welcome.baseGet("instance", BaseObject.UNDEFINED);
		assert welcomeInstance != null : "NULL java value";
		final String identity = Base.getString(welcomeInstance, "identity", "").trim();
		if (identity.length() == 0) {
			return false;
		}
		this.guid = identity;
		final BaseObject welcomeState = welcome.baseGet("state", BaseObject.UNDEFINED);
		this.health = Convert.MapEntry.toDouble(welcomeState, "health", 0.0);
		this.ready = Convert.MapEntry.toDouble(welcomeState, "ready", 0.0);
		final BaseObject welcomeImpl = welcome.baseGet("impl", BaseObject.UNDEFINED);
		this.version = Base.getString(welcomeImpl, "version", "n/a");
		this.checked = Engine.fastTime();
		return true;
	}

	/**
	 * @return date
	 */
	public final long getChecked() {
		
		return this.checked;
	}

	/**
	 * @return date
	 */
	public final long getCreated() {
		
		return this.created;
	}

	/**
	 * @return string
	 */
	public final String getGuid() {
		
		return this.guid;
	}

	/**
	 * @return double
	 */
	public final double getHealth() {
		
		return this.health;
	}

	/**
	 * @return string
	 */
	public final String getHost() {
		
		return this.srcHost;
	}

	/**
	 * @return string
	 */
	public final String getHostIndexing() {
		
		return this.idxHost;
	}

	/**
	 * @return int
	 */
	public final int getLuid() {
		
		return this.luid;
	}

	/**
	 * @return int
	 */
	public final int getPort() {
		
		return this.srcPort;
	}

	/**
	 * @return int
	 */
	public final int getPortIndexing() {
		
		return this.idxPort;
	}

	/**
	 * @return double
	 */
	public final double getReady() {
		
		return this.ready;
	}

	/**
	 * @return root folder
	 */
	public final RecFolder getRootFolder() {
		
		final RequestRootFolder request = new RequestRootFolder(this.client, this.luid);
		this.client.getLoader().add(request);
		return request.baseValue();
	}

	/**
	 * @return string
	 */
	public final String getVersion() {
		
		return this.version;
	}

	/**
	 * @return boolean
	 */
	public final boolean isActive() {
		
		return this.active;
	}

	/**
	 * @return boolean
	 */
	public final boolean isIndex() {
		
		return this.index;
	}

	@Override
	public final String toString() {
		
		return "SRV{ identity=" + this.guid + ", host=" + this.srcHost + ':' + this.srcPort + ", idx=" + this.idxHost + ':' + this.idxPort + ", index=" + this.index + ", active="
				+ this.active + ", checked=" + Format.Compact.toPeriod(Engine.fastTime() - this.checked) + " }";
	}
}
