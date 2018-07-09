/*
 * Created on 19.04.2006
 */
package ru.myx.srv.download;

final class TaskRestarter implements Runnable {
	@Override
	public void run() {
		try {
			Runtime.getRuntime().exit( -27 );
		} catch (final Throwable t) {
			t.printStackTrace();
			try {
				Thread.sleep( 20000L );
			} catch (final InterruptedException ie) {
				// ignore
			}
			Runtime.getRuntime().halt( -28 );
		}
	}
}
