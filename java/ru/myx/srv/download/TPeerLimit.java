/*
 * Created on 28.01.2005
 */
package ru.myx.srv.download;

import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferDescription;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.serve.ServeRequest;

final class TPeerLimit implements Traffic {
	private final TransferDescription	description;
	
	private boolean						perAddress;
	
	TPeerLimit(final String name, final BaseObject data) {
		this.description = Transfer.createDescription( name, TransferDescription.PC_LOW );
		this.setup( data );
	}
	
	@Override
	public TransferDescription description() {
		return this.description;
	}
	
	@Override
	public ReplyAnswer finalizeResponse(
			final ServeRequest query,
			final ReplyAnswer answer,
			final boolean decrement) {
		return answer.setAttribute( "Transfer-Class", Base.forUnknown( this.description ) );
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
		return parent;
	}
	
	@Override
	public void setup(final BaseObject data) {
		this.description.setNetWriteByteRateLimit( Convert.MapEntry.toInt( data, "limit", 32768 ) );
		this.perAddress = Convert.MapEntry.toBoolean( data, "address", false );
	}
	
	@Override
	public void stop() {
		// ignore
	}
}
