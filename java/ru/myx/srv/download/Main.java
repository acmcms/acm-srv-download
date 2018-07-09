package ru.myx.srv.download;

import ru.myx.ae3.produce.Produce;

/*
 * Created on 27.09.2003
 * 
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
/**
 * @author barachta
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public final class Main {
	/**
	 * @param args
	 */
	public static final void main(final String[] args) {
		// System.setProperty("tritonus.lame.bitrate", "8");
		// System.setProperty("tritonus.lame.quality", "lowest");
		//
		System.out.println( "RU.MYX.AE1SRV.DOWNLOAD: server & client type factory is initializing..." );
		System.out.println( "RU.MYX.AE1SRV.DOWNLOAD: mp3 encoding: bitrate="
				+ System.getProperty( "tritonus.lame.bitrate", "tritonus.lame.bitrate=undefined" )
				+ ", quality="
				+ System.getProperty( "tritonus.lame.quality", "tritonus.lame.quality=undefined" ) );
		Produce.registerFactory( new FactoryHost() );
		Produce.registerFactory( new FactoryPlugin() );
		System.out.println( "RU.MYX.AE1SRV.DOWNLOAD: done." );
	}
}
