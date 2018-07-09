/*
 * Created on 28.01.2005
 */
package ru.myx.srv.download;

import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferDescription;
import ru.myx.ae3.serve.ServeRequest;

interface Traffic {
	/**
	 * @return description
	 */
	TransferDescription description();
	
	/**
	 * @param query
	 * @param answer
	 * @param decrement
	 * @return answer
	 */
	ReplyAnswer finalizeResponse(final ServeRequest query, final ReplyAnswer answer, final boolean decrement);
	
	/**
	 * @return int
	 */
	int getCommonLimit();
	
	/**
	 * @return boolean
	 */
	boolean isPerAddress();
	
	/**
	 * @param query
	 * @param response
	 * @param parent
	 * @return traffic
	 */
	Traffic replaceTraffic(final ServeRequest query, final ReplyAnswer response, final Traffic parent);
	
	/**
	 * @param data
	 */
	void setup(final BaseObject data);
	
	/**
     * 
     */
	void stop();
}
