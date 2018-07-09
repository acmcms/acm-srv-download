/*
 * Created on 29.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import ru.myx.ae3.binary.TransferDescription;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
final class CounterFilter extends Number implements TransferDescription {
	/**
     * 
     */
	private static final long	serialVersionUID			= -4467799275658851812L;
	
	private final TrafficFilter	trafficFilter;
	
	private short				value;
	
	int							statsNetWrittenIteration	= -1;
	
	int							statsNetWrittenIndex		= 0;
	
	CounterFilter(final TrafficFilter trafficFilter, final short initial) {
		this.trafficFilter = trafficFilter;
		this.value = initial;
	}
	
	final int decrement() {
		return --this.value;
	}
	
	@Override
	public final double doubleValue() {
		return this.value;
	}
	
	@Override
	public final float floatValue() {
		return this.value;
	}
	
	@Override
	public final Object getAttachment() {
		return this.trafficFilter.getAttachment();
	}
	
	@Override
	public final int getNetReadByteRateLimit() {
		return this.trafficFilter.getNetReadByteRateLimit();
	}
	
	@Override
	public final int getNetReadByteRateLimitLeft(final long time) {
		return this.trafficFilter.getNetReadByteRateLimitLeft( time );
	}
	
	@Override
	public final int getNetWriteByteRateLimit() {
		return this.trafficFilter.getNetWriteByteRateLimit( this.value );
	}
	
	@Override
	public final int getNetWriteByteRateLimitLeft(final long time) {
		return this.trafficFilter.getNetWriteByteRateLimitLeft( time );
	}
	
	@Override
	public final int getPriority() {
		return this.trafficFilter.getPriority();
	}
	
	@Override
	public final int getStorageReadByteRateLimit() {
		return this.trafficFilter.getStorageReadByteRateLimit();
	}
	
	@Override
	public final int getStorageWriteByteRateLimit() {
		return this.trafficFilter.getStorageWriteByteRateLimit();
	}
	
	final int getValue() {
		return this.value;
	}
	
	final int increment() {
		return this.value++;
	}
	
	@Override
	public final int intValue() {
		return this.value;
	}
	
	@Override
	public boolean isReplaceable(final TransferDescription description) {
		return false;
	}
	
	@Override
	public final boolean isWritable() {
		return this.trafficFilter.isWritable();
	}
	
	@Override
	public final long longValue() {
		return this.value;
	}
	
	@Override
	public final Object setAttachment(final Object attachment) {
		return this.trafficFilter.setAttachment( attachment );
	}
	
	@Override
	public final int setNetReadByteRateLimit(final int netByteReadLimit) {
		return this.trafficFilter.setNetReadByteRateLimit( netByteReadLimit );
	}
	
	@Override
	public final int setNetWriteByteRateLimit(final int netByteWriteLimit) {
		return this.trafficFilter.setNetWriteByteRateLimit( netByteWriteLimit );
	}
	
	@Override
	public final int setStorageReadByteRateLimit(final int storageByteReadLimit) {
		return this.trafficFilter.setStorageReadByteRateLimit( storageByteReadLimit );
	}
	
	@Override
	public final int setStorageWriteByteRateLimit(final int storageByteWriteLimit) {
		return this.trafficFilter.setStorageWriteByteRateLimit( storageByteWriteLimit );
	}
	
	@Override
	public final void statsNetRead(final int bytes) {
		// empty
	}
	
	@Override
	public final void statsNetWritten(final int bytes) {
		this.trafficFilter.statsNetWritten( bytes, this );
	}
	
	@Override
	public final void statsStorageRead(final int bytes) {
		// empty
	}
	
	@Override
	public final void statsStorageWritten(final int bytes) {
		// empty
	}
	
	@Override
	public final String toString() {
		return Integer.toString( this.value );
	}
}
