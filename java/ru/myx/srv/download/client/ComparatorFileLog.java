/*
 * Created on 29.07.2005
 */
package ru.myx.srv.download.client;

import java.util.Comparator;

final class ComparatorFileLog implements Comparator<RecFile> {
	
	static final Comparator<RecFile>	INSTANCE	= new ComparatorFileLog();
	
	private static final int check(final long v1, final long v2) {
		return v1 > v2
				? 1
				: v1 == v2
						? 0
						: -1;
	}
	
	private ComparatorFileLog() {
		// ignore
	}
	
	@Override
	public final int compare(final RecFile o1, final RecFile o2) {
		return ComparatorFileLog.check( o1.getDate(), o2.getDate() );
	}
}
