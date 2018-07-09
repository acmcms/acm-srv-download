/**
 * 
 */
package ru.myx.srv.download.client;

import ru.myx.ae3.status.StatusInfo;
import ru.myx.ae3.status.StatusProvider;

final class Status implements StatusProvider {
	private final DownloadClient	parent;
	
	Status(final DownloadClient parent) {
		this.parent = parent;
	}
	
	@Override
	public final StatusProvider[] childProviders() {
		return null;
	}
	
	@Override
	public final String statusDescription() {
		return "D1 client (id=" + this.parent.getMnemonicName() + ")";
	}
	
	@Override
	public final void statusFill(final StatusInfo data) {
		this.parent.statusFill( data );
	}
	
	@Override
	public final String statusName() {
		return this.parent.getMnemonicName();
	}
}
