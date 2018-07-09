/*
 * Created on 25.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author myx
 */
class InputStreamSignedStereoMono16 extends InputStream {
	private final InputStream	parent;
	
	InputStreamSignedStereoMono16(final InputStream parent) {
		this.parent = new BufferedInputStream( parent, 128 * 1024 );
	}
	
	@Override
	public int read() throws IOException {
		final int s1 = this.parent.read();
		if (s1 == -1) {
			return -1;
		}
		final int s2 = this.parent.read();
		if (s2 == -1) {
			return -1;
		}
		final int s3 = this.parent.read();
		if (s3 == -1) {
			return -1;
		}
		final int s4 = this.parent.read();
		if (s4 == -1) {
			return -1;
		}
		return (128 + s2 & 0x000000FF) + (128 + s4 & 0x000000FF) >> 1;
	}
}
