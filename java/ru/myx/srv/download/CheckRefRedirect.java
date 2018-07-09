/*
 * Created on 15.01.2005
 */
package ru.myx.srv.download;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.serve.ServeRequest;

/**
 * @author myx
 * 
 */
class CheckRefRedirect implements CheckRef {
	private final String	target;
	
	CheckRefRedirect(final String target) {
		this.target = target;
	}
	
	@Override
	public ReplyAnswer check(final ServeRequest query) {
		return Reply.redirect( "CHECK/REF/REDIRECT", query, false, this.target );
	}
}
