/*
 * Created on 31.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.io.File;
import java.io.IOException;

import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferCollector;
import ru.myx.ae3.help.FileName;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.mime.MimeType;

/**
 * @author myx
 * 		
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class DescriberVideo implements Describer {
	
	@Override
	public void buildTemporaryFiles(final File source, final TransferCollector preview1, final TransferCollector preview2) {
		
		// empty
	}
	
	@Override
	public boolean describe(final String type, final File file, final BaseObject target) throws Exception {
		
		final StringBuilder result = new StringBuilder();
		result.append("video, ");
		result.append(type);
		final long size = file.length();
		if (size > 0) {
			result.append(", ").append(Format.Compact.toBytes(size)).append('B');
		}
		if (file.exists()) {
			result.append(", ").append(MimeType.forFile(file, type));
		} else {
			throw new IOException("Not exists!");
		}
		target.baseDefine("output", result.toString());
		return true;
	}
	
	@Override
	public String getMediaType() {
		
		return "video";
	}
	
	@Override
	public String getMediaTypeFor(final File file) {
		
		return FileName.extension(file) + " video";
	}
	
	@Override
	public int getVersion() {
		
		return 3;
	}
	
	@Override
	public boolean isPreviewAvailable() {
		
		return false;
	}
	
	@Override
	public boolean isThumbnailAvailable() {
		
		return false;
	}
}
