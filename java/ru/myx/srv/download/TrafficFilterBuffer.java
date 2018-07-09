/*
 * Created on 02.02.2005
 */
package ru.myx.srv.download;

class TrafficFilterBuffer {
	private static final int	INIT		= 256;
	
	private int[]				buffers		= new int[TrafficFilterBuffer.INIT];
	
	private int					bufferHead	= 0;
	
	void collectWritten(final int index, final int written) {
		this.buffers[index] += written;
	}
	
	int createIndex() {
		final int index = this.bufferHead++;
		if (index >= this.buffers.length) {
			synchronized (this) {
				if (index >= this.buffers.length) {
					final int[] newBuffers = new int[this.buffers.length * 2];
					System.arraycopy( this.buffers, 0, newBuffers, 0, this.buffers.length );
					this.buffers = newBuffers;
				}
			}
		}
		return index;
	}
	
	double flushSpeed(final long dateStarted, final long dateCurrent, final int limit) {
		if (this.bufferHead == 0) {
			return Double.NaN;
		}
		double sum = 0;
		final double k = 1000.0 / (dateCurrent - dateStarted) / this.bufferHead;
		for (int index = this.bufferHead - 1; index >= 0; index--) {
			final double value = k * this.buffers[index] / limit;
			sum += Math.sqrt( value );
			this.buffers[index] = 0;
		}
		this.bufferHead = 0;
		return sum;
	}
}
