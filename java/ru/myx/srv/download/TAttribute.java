/*
 * Created on 28.01.2005
 */
package ru.myx.srv.download;

import java.util.HashMap;
import java.util.Map;

import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferDescription;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.serve.ServeRequest;

final class TAttribute implements Traffic {
	private final DownloadServer		server;
	
	private final TrafficFilter			parent;
	
	private final TransferDescription	description;
	
	private String						attribute;
	
	private boolean						perAddress;
	
	private Map<String, TrafficFilter>	replacements;
	
	private TrafficFilter				defaultFilter;
	
	private BaseObject				data;
	
	private boolean						setup	= true;
	
	TAttribute(final DownloadServer server, final String name, final TrafficFilter parent, final BaseObject data) {
		this.server = server;
		this.parent = parent;
		this.description = Transfer.createDescription( name, TransferDescription.PC_DEFAULT );
		this.setup( data );
	}
	
	@Override
	public TransferDescription description() {
		return this.description;
	}
	
	@Override
	public ReplyAnswer finalizeResponse(
			final ServeRequest query,
			final ReplyAnswer response,
			final boolean decrement) {
		if (!this.setup) {
			synchronized (this) {
				if (!this.setup) {
					this.setupIntern();
				}
			}
		}
		return response.setAttribute( "Transfer-Class", Base.forUnknown( this.description ) );
	}
	
	@Override
	public int getCommonLimit() {
		return 0;
	}
	
	@Override
	public boolean isPerAddress() {
		return this.perAddress;
	}
	
	@Override
	public Traffic replaceTraffic(final ServeRequest query, final ReplyAnswer response, final Traffic parent) {
		if (!this.setup) {
			synchronized (this) {
				if (!this.setup) {
					this.setupIntern();
				}
			}
		}
		final String value = Base.getString( query.getAttributes(), this.attribute, "" ).trim();
		final TrafficFilter filter = this.replacements.get( value );
		if (filter == null) {
			return this.defaultFilter;
		}
		return filter;
	}
	
	@Override
	public void setup(final BaseObject data) {
		this.data = data;
		this.setup = false;
	}
	
	private void setupIntern() {
		this.attribute = Base.getString( this.data, "attribute", "" ).trim();
		this.perAddress = Convert.MapEntry.toBoolean( this.data, "address", false );
		final BaseArray options = Convert.MapEntry.toCollection( this.data, "option", null );
		final Map<String, TrafficFilter> replacements = new HashMap<>();
		final int length = options.length();
		for (int i = 0; i < length; ++i) {
			final BaseObject option = options.baseGet( i, BaseObject.UNDEFINED );
			final String trafficName = Base.getString( option, "traffic", "" ).trim();
			final TrafficFilter traffic = this.server.getServerTraffics().get( trafficName );
			if (traffic != null) {
				replacements.put( Base.getString( option, "value", "" ).trim(), traffic );
			}
		}
		this.replacements = replacements;
		final BaseObject option = this.data.baseGet( "default", BaseObject.UNDEFINED );
		final String trafficName = Base.getString( option, "traffic", "" ).trim();
		final TrafficFilter traffic = this.server.getServerTraffics().get( trafficName );
		if (traffic == null) {
			this.defaultFilter = this.parent;
		} else {
			this.defaultFilter = traffic;
		}
	}
	
	@Override
	public void stop() {
		// empty
	}
}
