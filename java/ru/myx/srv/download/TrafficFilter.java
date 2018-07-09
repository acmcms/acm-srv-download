/*
 * Created on 28.01.2005
 */
package ru.myx.srv.download;

import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BasePrimitiveNumber;
import ru.myx.ae3.binary.TransferDescription;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.serve.ServeRequest;

final class TrafficFilter implements Traffic, TransferDescription {
	private static final long			DATE_STEP			= 200L;
	
	private static final float			DATE_MUL			= 1000.0f / TrafficFilter.DATE_STEP;
	
	private final DownloadServer		parent;
	
	private final String				name;
	
	private final CounterBlockFilter	counter;
	
	private Traffic						inner;
	
	private boolean						perAddress			= false;
	
	private int							commonLimit			= 1;
	
	private float						netWritten9;
	
	private float						netWritten8;
	
	private float						netWritten7;
	
	private float						netWritten6;
	
	private float						netWritten5;
	
	private float						netWritten4;
	
	private float						netWritten3;
	
	private float						netWritten2;
	
	private float						netWritten1;
	
	private int							netWritten;
	
	private long						netWrittenDate;
	
	private int							netWrittenSum;
	
	private int							trafficIteration	= 0;
	
	private final TrafficFilterBuffer[]	trafficBuffers		= { new TrafficFilterBuffer(), new TrafficFilterBuffer() };
	
	private float						speed5;
	
	private float						speed4;
	
	private float						speed3;
	
	private float						speed2;
	
	private float						speed1;
	
	TrafficFilter(final DownloadServer parent, final String name, final CounterBlock counter, final BaseObject data) {
		this.parent = parent;
		this.name = name;
		this.counter = new CounterBlockFilter( this, counter );
		this.setup( data );
		Report.info( "TRAFFIC/FILTER", "initialized: name=" + name );
	}
	
	@Override
	public final TransferDescription description() {
		return this;
	}
	
	@Override
	public final ReplyAnswer finalizeResponse(
			final ServeRequest query,
			final ReplyAnswer response,
			final boolean decrement) {
		final Traffic replaced = this.inner.replaceTraffic( query, response, this );
		if (this == replaced) {
			final String address = query.getSourceAddressExact();
			final int index = CounterBlockFilter.makeIndex( address );
			final CounterSegmentFilter counter = this.counter.getCounter( index, decrement );
			final TransferDescription transferClass = counter.incrementCheck( address );
			query.setAttribute( "execute", counter );
			return response.setAttribute( "Transfer-Class", Base.forUnknown( transferClass ) );
		}
		return replaced.finalizeResponse( query, response, decrement );
	}
	
	@Override
	public final Object getAttachment() {
		return this.inner.description().getAttachment();
	}
	
	@Override
	public int getCommonLimit() {
		return this.inner.getCommonLimit();
	}
	
	@Override
	public final int getNetReadByteRateLimit() {
		return this.inner.description().getNetReadByteRateLimit();
	}
	
	@Override
	public int getNetReadByteRateLimitLeft(final long time) {
		return 0;
	}
	
	@Override
	public int getNetWriteByteRateLimit() {
		return this.inner.description().getNetWriteByteRateLimit();
	}
	
	/**
	 * @param value
	 * @return int
	 */
	public final int getNetWriteByteRateLimit(final int value) {
		final int limit = this.inner.description().getNetWriteByteRateLimit();
		if (this.perAddress && value > 1 && limit > 0) {
			return limit / value;
		}
		return limit;
	}
	
	@Override
	public final int getNetWriteByteRateLimitLeft(final long time) {
		if (this.commonLimit != 0) {
			final long date = time / TrafficFilter.DATE_STEP;
			if (date > this.netWrittenDate) {
				synchronized (this) {
					if (date > this.netWrittenDate) {
						this.recalculateCommonLimit( this.netWritten, date );
						if (this.commonLimit == 0) {
							return 0;
						}
					}
				}
			}
			final int result = this.commonLimit - this.netWritten;
			return result <= 0
					? -1
					: result;
		}
		return 0;
	}
	
	@Override
	public final int getPriority() {
		return this.inner.description().getPriority();
	}
	
	final int getRateLimitTotalValue(final int limit, final int load, final long dateLast, final long dateCurrent) {
		{
			final TrafficFilterBuffer buffer = this.trafficBuffers[this.trafficIteration++ & 1];
			final double sum = buffer.flushSpeed( dateLast, dateCurrent, limit );
			if (true) {
				return 0;
			}
			Report.info( "SHAPER", "sqavg=" + Format.Compact.toDecimal( sum ) );
			final double pow = Math.sqrt( sum );
			final double check = 1.5 * Math.pow( load + 1, pow ) / (load + 1);
			if (check < 0.9) {
				Report.info( "SHAPER",
						"LOW, k!!! k=" + Format.Compact.toDecimal( check ) + ", sm=" + Format.Compact.toDecimal( sum )
						// + ", sv="
						// + Format.Compact.toDecimal(sav)
								+ ", pw="
								+ Format.Compact.toDecimal( pow ) );
				return (int) (0.9 * limit);
			}
			final double maxK = load * 1.5;
			if (Double.isNaN( check )) {
				return (int) (maxK * limit);
			}
			final double k = check > maxK
					? maxK
					: check;
			return (int) (k * limit);
		}
	}
	
	final float getSpeedAverage(final float speed) {
		this.speed5 = (speed + this.speed4 + this.speed3 + this.speed2 + this.speed1) / 5;
		
		this.speed4 = (speed + this.speed3 + this.speed2 + this.speed1) / 4;
		this.speed3 = (speed + this.speed2 + this.speed1) / 3;
		this.speed2 = (speed + this.speed1) / 2;
		this.speed1 = speed;
		return (this.speed1 + this.speed2 + this.speed3 + this.speed4 + this.speed5) / 5;
	}
	
	@Override
	public final int getStorageReadByteRateLimit() {
		return this.inner.description().getStorageReadByteRateLimit();
	}
	
	@Override
	public final int getStorageWriteByteRateLimit() {
		return this.inner.description().getStorageWriteByteRateLimit();
	}
	
	final int getWritten() {
		try {
			return this.netWrittenSum;
		} finally {
			this.netWrittenSum = 0;
		}
	}
	
	@Override
	public boolean isPerAddress() {
		return this.perAddress;
	}
	
	@Override
	public boolean isReplaceable(final TransferDescription description) {
		return false;
	}
	
	@Override
	public final boolean isWritable() {
		return this.inner.description().isWritable();
	}
	
	private final void recalculateCommonLimit(final int netWritten, final long date) {
		this.netWritten = 0;
		final long netWrittenDate = this.netWrittenDate;
		this.netWrittenDate = date;
		final int commonLimit = this.inner.getCommonLimit();
		if (commonLimit == 0) {
			this.commonLimit = 0;
			return;
		}
		final int dateDiff = (int) (date - netWrittenDate);
		final float limit = this.inner.getCommonLimit() / TrafficFilter.DATE_MUL;
		if (dateDiff > 8) {
			this.netWritten9 = 0.0f;
			this.netWritten8 = 0.0f;
			this.netWritten7 = 0.0f;
			this.netWritten6 = 0.0f;
			this.netWritten5 = 0.0f;
			this.netWritten4 = 0.0f;
			this.netWritten3 = 0.0f;
			this.netWritten2 = 0.0f;
			this.netWritten1 = 0.0f;
			this.commonLimit = (int) Math.ceil( limit );
		} else {
			final float diffAbsolute = limit - 1.0f * netWritten / dateDiff;
			for (int i = dateDiff; i > 0; --i) {
				this.netWritten9 = (this.netWritten9 + this.netWritten8 * 2) / 3.0f;
				this.netWritten8 = (this.netWritten8 + this.netWritten7 * 2) / 3.0f;
				this.netWritten7 = (this.netWritten7 + this.netWritten6 * 2) / 3.0f;
				this.netWritten6 = (this.netWritten6 + this.netWritten5 * 2) / 3.0f;
				this.netWritten5 = (this.netWritten5 + this.netWritten4 * 2) / 3.0f;
				this.netWritten4 = (this.netWritten4 + this.netWritten3 * 2) / 3.0f;
				this.netWritten3 = (this.netWritten3 + this.netWritten2 * 2) / 3.0f;
				this.netWritten2 = (this.netWritten2 + this.netWritten1 * 2) / 3.0f;
				this.netWritten1 = (this.netWritten1 + diffAbsolute * 2) / 3.0f;
			}
			final float diffTotal = +this.netWritten1
					* 4.0f
					+ this.netWritten2
					* 2.0f
					+ this.netWritten3
					* 1.5f
					+ this.netWritten4
					* 1.25f
					+ this.netWritten5
					* 1.125f
					+ this.netWritten6
					+ this.netWritten7
					+ this.netWritten8
					+ this.netWritten9;
			final float diffCurrent;
			{
				if (diffTotal > 0.0f) {
					if (diffTotal > limit * TrafficFilter.DATE_MUL) {
						diffCurrent = limit * TrafficFilter.DATE_MUL;
					} else {
						diffCurrent = diffTotal;
					}
				} else {
					diffCurrent = 0.0f;
				}
			}
			final int result = Math.round( limit + diffCurrent );
			if (result <= 0) {
				this.commonLimit = -1;
			} else {
				this.commonLimit = result;
			}
		}
	}
	
	@Override
	public Traffic replaceTraffic(final ServeRequest query, final ReplyAnswer response, final Traffic parent) {
		return parent;
	}
	
	@Override
	public final Object setAttachment(final Object attachment) {
		return this.inner.description().setAttachment( attachment );
	}
	
	@Override
	public final int setNetReadByteRateLimit(final int netByteReadLimit) {
		return this.inner.description().setNetReadByteRateLimit( netByteReadLimit );
	}
	
	@Override
	public final int setNetWriteByteRateLimit(final int netByteWriteLimit) {
		return this.inner.description().setNetWriteByteRateLimit( netByteWriteLimit );
	}
	
	@Override
	public final int setStorageReadByteRateLimit(final int storageByteReadLimit) {
		return this.inner.description().setStorageReadByteRateLimit( storageByteReadLimit );
	}
	
	@Override
	public final int setStorageWriteByteRateLimit(final int storageByteWriteLimit) {
		return this.inner.description().setStorageWriteByteRateLimit( storageByteWriteLimit );
	}
	
	@Override
	public final void setup(final BaseObject data) {
		final String type = Base.getString( data, "type", "" ).trim();
		if ("attribute".equals( type )) {
			if (this.inner != null && this.inner instanceof TAttribute) {
				this.inner.setup( data );
				this.perAddress = this.inner.isPerAddress();
				return;
			}
			final Traffic inner = new TAttribute( this.parent, this.name, this, data );
			if (this.inner != null) {
				this.inner.stop();
			}
			this.inner = inner;
			this.perAddress = inner.isPerAddress();
			return;
		}
		if ("counter".equals( type )) {
			if (this.inner != null && this.inner instanceof TCounter) {
				this.inner.setup( data );
				this.perAddress = this.inner.isPerAddress();
				return;
			}
			final Traffic inner = new TCounter( this.name, this, this.counter, data );
			if (this.inner != null) {
				this.inner.stop();
			}
			this.inner = inner;
			this.perAddress = inner.isPerAddress();
			return;
		}
		if ("common".equals( type )) {
			if (this.inner != null && this.inner instanceof TCommonLimit) {
				this.inner.setup( data );
				this.perAddress = this.inner.isPerAddress();
				return;
			}
			final Traffic inner = new TCommonLimit( this.name, this, this.counter, data );
			if (this.inner != null) {
				this.inner.stop();
			}
			this.inner = inner;
			this.perAddress = inner.isPerAddress();
			return;
		}
		if ("peer".equals( type )) {
			if (this.inner != null && this.inner instanceof TPeerLimit) {
				this.inner.setup( data );
				this.perAddress = this.inner.isPerAddress();
				return;
			}
			final Traffic inner = new TPeerLimit( this.name, data );
			if (this.inner != null) {
				this.inner.stop();
			}
			this.inner = inner;
			this.perAddress = inner.isPerAddress();
			return;
		}
		{
			if (this.inner != null && this.inner instanceof TPeerLimit) {
				this.inner.setup( data );
				this.perAddress = this.inner.isPerAddress();
				return;
			}
			final Traffic inner = new TPeerLimit( this.name, new BaseNativeObject( "limit", BasePrimitiveNumber.ZERO ) );
			this.stop();
			this.inner = inner;
			this.perAddress = inner.isPerAddress();
			return;
		}
	}
	
	@Override
	public void statsNetRead(final int bytes) {
		// empty
	}
	
	@Override
	public final void statsNetWritten(final int bytes) {
		if (this.commonLimit == 0) {
			this.netWritten += bytes;
			this.netWrittenSum += bytes;
		} else {
			final long date = System.currentTimeMillis() / TrafficFilter.DATE_STEP;
			if (date > this.netWrittenDate) {
				synchronized (this) {
					this.netWritten += bytes;
					this.netWrittenSum += bytes;
					if (date > this.netWrittenDate) {
						this.recalculateCommonLimit( this.netWritten, date );
					}
				}
			} else {
				this.netWritten += bytes;
				this.netWrittenSum += bytes;
			}
		}
	}
	
	/**
	 * @param bytes
	 * @param counter
	 */
	public final void statsNetWritten(final int bytes, final CounterFilter counter) {
		if (this.commonLimit == 0) {
			this.netWritten += bytes;
			this.netWrittenSum += bytes;
		} else {
			final long date = System.currentTimeMillis() / TrafficFilter.DATE_STEP;
			if (date > this.netWrittenDate) {
				synchronized (this) {
					this.netWritten += bytes;
					this.netWrittenSum += bytes;
					if (date > this.netWrittenDate) {
						this.recalculateCommonLimit( this.netWritten, date );
					}
				}
			} else {
				this.netWritten += bytes;
				this.netWrittenSum += bytes;
			}
		}
		final int limit = this.inner.description().getNetWriteByteRateLimit();
		if (limit > 0) {
			final int trafficIteration = this.trafficIteration;
			final TrafficFilterBuffer currentIteration = this.trafficBuffers[trafficIteration & 1];
			if (counter.statsNetWrittenIteration != trafficIteration) {
				counter.statsNetWrittenIndex = currentIteration.createIndex();
				counter.statsNetWrittenIteration = trafficIteration;
			}
			currentIteration.collectWritten( counter.statsNetWrittenIndex, bytes );
		}
	}
	
	@Override
	public void statsStorageRead(final int bytes) {
		// empty
	}
	
	@Override
	public void statsStorageWritten(final int bytes) {
		// empty
	}
	
	@Override
	public final void stop() {
		if (this.inner != null) {
			this.inner.stop();
		}
	}
	
	@Override
	public final String toString() {
		return "TF{" + this.inner.toString() + ", addr=" + this.perAddress + "}";
	}
}
