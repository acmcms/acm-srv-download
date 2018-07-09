/*
 * Created on 06.11.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.serve.ServeRequest;

/**
 * @author myx
 * 
 */
interface CheckRef {
	/**
	 * @param query
	 * @return answer
	 */
	ReplyAnswer check(final ServeRequest query);
}
