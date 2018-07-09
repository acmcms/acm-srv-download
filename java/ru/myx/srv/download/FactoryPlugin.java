package ru.myx.srv.download;

import ru.myx.ae1.AcmPluginFactory;
import ru.myx.ae1.PluginInstance;
import ru.myx.ae3.base.BaseObject;
import ru.myx.srv.download.client.DownloadClient;

/*
 * Created on 07.10.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
final class FactoryPlugin implements AcmPluginFactory {
	
	private static final String[] VARIETY = {
			"ACMMOD:DOWNLOAD"
	};
	
	@Override
	public final PluginInstance produce(final String variant, final BaseObject attributes, final Object source) {

		return new DownloadClient();
	}
	
	@Override
	public final String[] variety() {

		return FactoryPlugin.VARIETY;
	}
}
