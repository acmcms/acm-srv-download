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
class DescriberImageJpeg extends DescriberImage {
	
	static final int getByteValue(final byte b) {
		
		return b < 0
			? 256 + b
			: (int) b;
	}
	
	DescriberImageJpeg(final BaseObject settings) {
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
			int width = 0;
			int height = 0;
			int current = 2;
			final byte[] image = new byte[16];
			while (true) {
				raf.seek(current);
				raf.read(image);
				// cycle through segments
				final int segmentHeader = DescriberImageJpeg.getByteValue(image[0]);
				final int segmentType = DescriberImageJpeg.getByteValue(image[1]);
				if (segmentHeader != 255) {
					break;
				}
				if (segmentType == 192 || segmentType == 194) {
					// Image info segment
					width = DescriberImageJpeg.getByteValue(image[7]) * 256 + DescriberImageJpeg.getByteValue(image[8]);
					height = DescriberImageJpeg.getByteValue(image[5]) * 256 + DescriberImageJpeg.getByteValue(image[6]);
					break;
				}
				final int currentSegmentSize = DescriberImageJpeg.getByteValue(image[2]) * 256 + DescriberImageJpeg.getByteValue(image[3]);
				current += currentSegmentSize + 2;
			}
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
