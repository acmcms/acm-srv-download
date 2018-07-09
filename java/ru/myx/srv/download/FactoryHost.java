package ru.myx.srv.download;

/*
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
import ru.myx.ae1.know.Server;
import ru.myx.ae3.Engine;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.produce.ObjectFactory;
import ru.myx.ae3.report.Report;

/**
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
class FactoryHost implements ObjectFactory<Object, Server> {
	
	private static final Class<?>[] TARGETS = {
			Server.class
	};

	private static final Class<?>[] SOURCES = null;

	private static final String[] VARIETY = {
			"ae1:DOWNLOAD_SERVER", "ae1:DOWNLOAD_SRVR"
	};

	@Override
	public boolean accepts(final String variant, final BaseObject attributes, final Class<?> source) {
		
		return true;
	}

	@Override
	public Server produce(final String variant, final BaseObject attributes, final Object source) {
		
		Report.info("FACTORY/DOWNLOAD_SERVER", "Production request: type=" + variant);
		final String id = Base.getString(attributes, "id", Engine.createGuid());
		final String check = Base.getString(attributes, "check", "").trim();
		return new DownloadServer(id, check, attributes);
	}

	@Override
	public Class<?>[] sources() {
		
		return FactoryHost.SOURCES;
	}

	@Override
	public Class<?>[] targets() {
		
		return FactoryHost.TARGETS;
	}

	@Override
	public String[] variety() {
		
		return FactoryHost.VARIETY;
	}
}
