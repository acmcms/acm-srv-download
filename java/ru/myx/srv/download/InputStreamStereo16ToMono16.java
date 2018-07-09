/*
 * Created on 25.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class InputStreamStereo16ToMono16 extends InputStream {
	private static final int	B1SIZE	= 64 * 1024;
	
	private static final int	B2SIZE	= 32 * 1024;
	
	private final byte[]		buffer	= new byte[InputStreamStereo16ToMono16.B1SIZE];
	
	private final byte[]		result	= new byte[InputStreamStereo16ToMono16.B2SIZE];
	
	private final InputStream	parent;
	
	private int					position;
	
	private int					length;
	
	private final int			frameBlock;
	
	private long				frameLeft;
	
	InputStreamStereo16ToMono16(final InputStream parent, final int frameBlock, final long frameLength) {
		this.parent = parent;
		this.frameBlock = frameBlock;
		this.frameLeft = frameLength;
	}
	
	@Override
	public int read() throws IOException {
		if (this.position == -1) {
			return -1;
		}
		if (this.position >= this.length) {
			if (this.frameLeft < this.frameBlock) {
				this.position = -1;
				return -1;
			}
			final int readLength = this.parent.read( this.buffer );
			if (readLength <= 0) {
				this.position = -1;
				return -1;
			}
			for (int i = 0, j = 0; i < readLength; i += 4, j += 2) {
				final int left = (this.buffer[i + 0] & 0xFF) + (this.buffer[i + 1] << 8);
				final int right = (this.buffer[i + 2] & 0xFF) + (this.buffer[i + 3] << 8);
				final int value = (left + right) / 2;
				this.result[j + 0] = (byte) (value & 0xFF);
				this.result[j + 1] = (byte) (value >> 8);
			}
			this.position = 0;
			this.length = readLength >> 1;
			final int flength = this.length >> 1;
			this.frameLeft -= flength;
		}
		return this.result[this.position++] & 0x000000FF;
	}
}
