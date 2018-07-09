/*
 * Created on 06.11.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.util.Map;

import ru.myx.ae3.Engine;
import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.help.Base64;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.serve.ServeRequest;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class CheckRefFixed implements CheckRef {
	private final String	user;
	
	private final String	pass;
	
	CheckRefFixed(final Map<?, ?> settings) {
		this( Convert.MapEntry.toString( settings, "user", "" ).trim(), Convert.MapEntry
				.toString( settings, "pass", "" ).trim() );
	}
	
	CheckRefFixed(final String user, final String pass) {
		this.user = user;
		this.pass = pass;
	}
	
	@Override
	public ReplyAnswer check(final ServeRequest query) {
		if (this.checkBasicAuthorization( query )) {
			return null;
		}
		return Reply.string( "CHECK/REF/FIXED", query, "Limited access!" ).setNoCaching().setPrivate().setFinal()
				.setCode( Reply.CD_UNAUTHORIZED );
	}
	
	private final boolean checkBasicAuthorization(final ServeRequest request) {
		final String authorization = Base.getString( request.getAttributes(), "Authorization", null );
		if (authorization == null) {
			return false;
		}
		if (!authorization.regionMatches( true, 0, "Basic", 0, 5 )) {
			return false;
		}
		final String auth;
		{
			final byte[] bytes = Base64.decode( authorization.substring( 6 ), true );
			if (bytes == null || bytes.length == 0) {
				return false;
			}
			auth = new String( bytes, Engine.CHARSET_UTF8 );
		}
		final int pos = auth.indexOf( ':' );
		if (pos == -1) {
			return false;
		}
		final String login = auth.substring( 0, pos );
		final String password = auth.substring( pos + 1 );
		return this.user.equals( login ) && this.pass.equals( password );
	}
}
