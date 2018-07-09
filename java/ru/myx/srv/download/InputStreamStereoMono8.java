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
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class InputStreamStereoMono8 extends InputStream {
	private final InputStream	parent;
	
	InputStreamStereoMono8(final InputStream parent) {
		this.parent = new BufferedInputStream( parent, 128 * 1024 );
	}
	
	@Override
	public int read() throws IOException {
		final int s1 = this.parent.read();
		if (s1 == -1) {
			return -1;
		}
		final int s2 = this.parent.read();
		if (s1 == -1) {
			return -1;
		}
		return (s1 + s2) / 2;
	}
}
