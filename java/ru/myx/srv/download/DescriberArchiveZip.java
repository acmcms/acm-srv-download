/*
 * Created on 10.02.2005
 */
package ru.myx.srv.download;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.help.Format;

/**
 * @author myx
 * 		
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class DescriberArchiveZip extends DescriberArchive {
	
	@Override
	public boolean describe(final String type, final File file, final BaseObject target) throws Exception {
		
		final StringBuilder result = new StringBuilder();
		result.append("archive, ");
		result.append(type);
		final long size = file.length();
		if (size > 0) {
			result.append(", ").append(Format.Compact.toBytes(size)).append('B');
		}
		int entryCount = 0;
		int entrySize = 0;
		long date = 0L;
		long maxDate = 0L;
		try (final ZipFile zipFile = new ZipFile(file)) {
			for (final Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
				final ZipEntry entry = entries.nextElement();
				entryCount++;
				entrySize += entry.getSize();
				if (entry.getName().endsWith(".bin")) {
					date = entry.getTime();
				}
				if (date == 0L) {
					final long newDate = entry.getTime();
					if (newDate > maxDate) {
						maxDate = newDate;
					}
				}
			}
		}
		if (date > 0L && date != file.lastModified()) {
			file.setLastModified(date);
			target.baseDefine("date", Base.forDateMillis(date));
		} else {
			if (maxDate > 0L && maxDate != file.lastModified()) {
				file.setLastModified(maxDate);
				target.baseDefine("date", Base.forDateMillis(maxDate));
			}
		}
		result.append(", ").append(entryCount).append(" files, ").append(Format.Compact.toBytes(entrySize)).append("B unpacked");
		target.baseDefine("files", entryCount);
		target.baseDefine("unpacked", entrySize);
		target.baseDefine("output", result.toString());
		return true;
	}
}
