/*
 * Created on 31.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.io.File;
import java.io.RandomAccessFile;

import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.help.Format;

/**
 * @author myx
 * 		
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class DescriberImageGif extends DescriberImage {
	
	static final int getByteValue(final byte b) {
		
		return b < 0
			? 256 + b
			: (int) b;
	}
	
	DescriberImageGif(final BaseObject settings) {
		super(settings);
	}
	
	@Override
	public boolean describe(final String type, final File file, final BaseObject target) throws Exception {
		
		final StringBuilder result = new StringBuilder();
		result.append("image, ");
		result.append(type);
		final long size = file.length();
		if (size > 0) {
			result.append(", ").append(Format.Compact.toBytes(size)).append('B');
		}
		try (final RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			final byte[] image = new byte[16];
			raf.read(image);
			final int width = DescriberImageGif.getByteValue(image[7]) * 256 + DescriberImageGif.getByteValue(image[6]);
			final int height = DescriberImageGif.getByteValue(image[9]) * 256 + DescriberImageGif.getByteValue(image[8]);
			if (width > 0 && height > 0) {
				result.append(", ").append(width).append('*').append(height).append(" pixels");
				target.baseDefine("width", width);
				target.baseDefine("height", height);
			}
		}
		target.baseDefine("output", result.toString());
		return true;
	}
	
	@Override
	public int getVersion() {
		
		return 3;
	}
}
