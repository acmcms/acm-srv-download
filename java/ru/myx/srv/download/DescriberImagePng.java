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
class DescriberImagePng extends DescriberImage {
	
	static final int getByteValue(final byte b) {
		
		return b < 0
			? 256 + b
			: (int) b;
	}
	
	DescriberImagePng(final BaseObject settings) {
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
			final byte[] image = new byte[32];
			raf.read(image);
			final int width = DescriberImagePng.getByteValue(image[8 + 4 + 4 + 0]) * 256 * 256 * 256 + DescriberImagePng.getByteValue(image[8 + 4 + 4 + 1]) * 256 * 256
					+ DescriberImagePng.getByteValue(image[8 + 4 + 4 + 2]) * 256 + DescriberImagePng.getByteValue(image[8 + 4 + 4 + 3]);
			final int height = DescriberImagePng.getByteValue(image[8 + 4 + 4 + 4]) * 256 * 256 * 256 + DescriberImagePng.getByteValue(image[8 + 4 + 4 + 5]) * 256 * 256
					+ DescriberImagePng.getByteValue(image[8 + 4 + 4 + 6]) * 256 + DescriberImagePng.getByteValue(image[8 + 4 + 4 + 7]);
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
