/*
 * Created on 15.01.2005
 */
package ru.myx.srv.download;

import java.util.HashMap;
import java.util.Map;

import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.serve.ServeRequest;

/**
 * @author myx
 */
class Checker {
	private final Map<String, CheckRef>	checks;
	
	private final CheckRef				checkDefault;
	
	Checker(final BaseObject settings) {
		this.checks = new HashMap<>();
		final BaseArray checks = Convert.MapEntry.toCollection( settings, "referer", null );
		if (checks != null) {
			final int length = checks.length();
			for (int i = 0; i < length; ++i) {
				final BaseObject check = checks.baseGet( i, BaseObject.UNDEFINED );
				final String host = Base.getString( check, "host", "" ).trim();
				final String action = Base.getString( check, "action", "" ).trim();
				if ("allow".equals( action )) {
					this.checks.put( host, new CheckRefAllow() );
				} else //
				if ("fixed".equals( action )) {
					final String user = Base.getString( check, "user", "" ).trim();
					final String pass = Base.getString( check, "pass", "" ).trim();
					this.checks.put( host, new CheckRefFixed( user, pass ) );
				} else //
				if ("redirect".equals( action )) {
					final String target = Base.getString( check, "target", "" ).trim();
					this.checks.put( host, new CheckRefRedirect( target ) );
				} else {
					this.checks.put( host, new CheckRefDeny() );
				}
			}
		}
		this.checkDefault = this.checks.get( "*" );
	}
	
	ReplyAnswer check(final ServeRequest query) {
		final String host;
		{
			final String referer = Base.getString( query.getAttributes(), "Referer", "" ).trim();
			if (referer.length() == 0) {
				host = "";
			} else {
				if (referer.length() < 8) {
					host = "*";
				} else {
					if (query.getUrl().equals( referer )) {
						return Reply.stringForbidden( "CHECK/REF/EQU", //
								query,
								"Access forbidden: referer equals to a query!" );
					}
					if (referer.startsWith( "http://" )) {
						final int pos1 = referer.indexOf( '/', 7 );
						final int pos2 = referer.indexOf( ':', 7 );
						if (pos1 == -1 && pos2 == -1) {
							host = referer.substring( 7 );
						} else //
						if (pos1 == -1) {
							host = referer.substring( 7, pos2 );
						} else //
						if (pos2 == -1) {
							host = referer.substring( 7, pos1 );
						} else {
							host = referer.substring( 7, Math.min( pos1, pos2 ) );
						}
					} else {
						host = "*";
					}
				}
			}
		}
		final CheckRef check = this.checks.get( host );
		if (check == null) {
			if (this.checkDefault == null) {
				return null;
			}
			return this.checkDefault.check( query );
		}
		return check.check( query );
	}
}
