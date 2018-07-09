/*
 * Created on 29.07.2005
 */
package ru.myx.srv.download.client;

import java.util.Comparator;

final class ComparatorFileHistory implements Comparator<RecFile> {
	
	static final Comparator<RecFile>	INSTANCE	= new ComparatorFileHistory();
	
	private static final int check(final long v1, final long v2) {
		return v1 < v2
				? 1
				: v1 == v2
						? 0
						: -1;
	}
	
	private ComparatorFileHistory() {
		// ignore
	}
	
	@Override
	public final int compare(final RecFile o1, final RecFile o2) {
		return ComparatorFileHistory.check( o1.getDate(), o2.getDate() );
	}
}
