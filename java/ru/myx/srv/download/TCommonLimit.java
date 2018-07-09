/*
 * Created on 28.01.2005
 */
package ru.myx.srv.download;

import ru.myx.ae3.act.Act;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferDescription;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.serve.ServeRequest;

final class TCommonLimit implements Runnable, Traffic {
	private final CounterBlockFilter	counter;
	
	private final TrafficFilter			parent;
	
	private final TransferDescription	description;
	
	private int							limit	= 0;
	
	private boolean						perAddress;
	
	private boolean						stopped	= false;
	
	private long						lastLoop;
	
	private int							lastValue;
	
	TCommonLimit(final String name,
			final TrafficFilter parent,
			final CounterBlockFilter counter,
			final BaseObject data) {
		this.counter = counter;
		this.parent = parent;
		this.description = Transfer.createDescription( name, TransferDescription.PC_LOW );
		this.setup( data );
		Act.whenIdle( null, this );
		this.lastLoop = System.currentTimeMillis();
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
		return response.setAttribute( "Transfer-Class", Base.forUnknown( this.description ) );
	}
	
	@Override
	public int getCommonLimit() {
		return this.limit;
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
	public final void run() {
		if (this.stopped) {
			return;
		}
		final int lastValue = this.lastValue;
		final int load = this.perAddress
				? this.counter.getValueAddress()
				: this.counter.getValueAll();
		final float speed;
		final float speedAvg;
		final int value;
		final int limitK;
		final long time = System.currentTimeMillis();
		synchronized (this) {
			final int written = this.parent.getWritten();
			final float intervalBig = (time - this.lastLoop) / 1000.0f;
			speed = written / intervalBig;
			speedAvg = this.parent.getSpeedAverage( speed );
			limitK = this.parent.getRateLimitTotalValue( this.limit, load, this.lastLoop, time );
			this.lastLoop = time;
			if (load == 0) {
				value = limitK;
			} else {
				value = limitK / load;
			}
			this.description.setNetWriteByteRateLimit( value );
			this.lastValue = value;
		}
		{
			final char direction = value == lastValue
					? '='
					: value < lastValue
							? '-'
							: '+';
			Report.info( "SHAPER",
					this.description + ", shape(" + direction + "):\t ttl=" + Format.Compact.toBytes( this.limit )
					// + "B\t eff="
					// + Format.Compact.toBytes(limitK)
					// + "B\t per="
					// + Format.Compact.toBytes(value)
							+ "B\t cnt="
							+ load
							+ "/"
							+ this.counter.getValueAll()
							+ "\t sm="
							+ Format.Compact.toBytes( speed )
							+ " \t sa="
							+ Format.Compact.toDecimal( speedAvg / this.limit ) );
		}
		Act.later( null, this, 5000L );
	}
	
	@Override
	public void setup(final BaseObject data) {
		final int limit = Convert.MapEntry.toInt( data, "limit", 512 * 1024 );
		if (this.limit == 0) {
			this.description.setNetWriteByteRateLimit( limit );
		}
		this.limit = limit;
		this.perAddress = Convert.MapEntry.toBoolean( data, "address", false );
	}
	
	@Override
	public void stop() {
		this.stopped = true;
	}
}
