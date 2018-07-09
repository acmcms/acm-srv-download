/*
 * Created on 31.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.io.File;

import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferCollector;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
interface Describer {
	/**
	 * @param source
	 * @param preview1
	 * @param preview2
	 * @throws Exception
	 */
	void buildTemporaryFiles(final File source, final TransferCollector preview1, final TransferCollector preview2)
			throws Exception;
	
	/**
	 * @param type
	 * @param file
	 * @param target
	 * @return boolean
	 * @throws Exception
	 */
	boolean describe(final String type, final File file, final BaseObject target) throws Exception;
	
	/**
	 * @return string
	 */
	String getMediaType();
	
	/**
	 * @param file
	 * @return string
	 */
	String getMediaTypeFor(final File file);
	
	/**
	 * @return integer
	 */
	int getVersion();
	
	/**
	 * @return boolean
	 */
	boolean isPreviewAvailable();
	
	/**
	 * @return boolean
	 */
	boolean isThumbnailAvailable();
}
