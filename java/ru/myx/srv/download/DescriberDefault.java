/*
 * Created on 31.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.io.File;

import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferCollector;
import ru.myx.ae3.help.FileName;
import ru.myx.ae3.help.Format;

/**
 * @author myx
 * 		
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class DescriberDefault implements Describer {
	
	@Override
	public void buildTemporaryFiles(final File source, final TransferCollector preview1, final TransferCollector preview2) {
		
		// empty
	}
	
	@Override
	public boolean describe(final String type, final File file, final BaseObject target) throws Exception {
		
		final StringBuilder result = new StringBuilder();
		result.append("unknown, ");
		result.append(type);
		final long size = file.length();
		if (size > 0) {
			result.append(", ").append(Format.Compact.toBytes(size)).append('B');
		}
		target.baseDefine("output", result.toString());
		return true;
	}
	
	@Override
	public String getMediaType() {
		
		return "unknown";
	}
	
	@Override
	public String getMediaTypeFor(final File file) {
		
		return FileName.extension(file) + " file";
	}
	
	@Override
	public int getVersion() {
		
		return 4;
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
