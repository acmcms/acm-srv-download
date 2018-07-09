/*
 * Created on 15.01.2005
 */
package ru.myx.srv.download;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.serve.ServeRequest;

class CheckRefDeny implements CheckRef {
	@Override
	public ReplyAnswer check(final ServeRequest query) {
		return Reply.stringForbidden( "CHECK/REF/DENY", //
				query,
				"Access forbidden!" );
	}
}
