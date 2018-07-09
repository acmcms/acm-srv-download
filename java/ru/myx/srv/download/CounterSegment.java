/*
 * Created on 29.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ru.myx.ae3.base.BaseFunctionActAbstract;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.serve.ServeRequest;

/**
 * @author myx
 *
 *         Window - Preferences - Java - Code Style - Code Templates
 */
final class CounterSegment extends BaseFunctionActAbstract<ServeRequest, Void> {
	
	private int value = 0;

	private final Map<String, AtomicInteger> users;

	private final int userLimit;

	private static final short COUNTER_INITIAL = 1;

	@SuppressWarnings("unchecked")
	CounterSegment(final int userLimit) {
		super((Class<ServeRequest>) Convert.Any.toAny(ServeRequest.class), Void.class);
		this.users = new HashMap<>();
		this.userLimit = userLimit;
	}

	final void decrement(final String address) {
		
		synchronized (this) {
			this.value--;
			final AtomicInteger counter = this.users.get(address);
			if (counter != null) {
				if (counter.decrementAndGet() <= 0) {
					this.users.remove(address);
				}
			}
		}
	}

	@Override
	public final Void apply(final ServeRequest query) {
		
		this.decrement(query.getSourceAddress());
		return null;
	}

	final void fillLoad(final Map<String, AtomicInteger> load) {
		
		synchronized (this) {
			load.putAll(this.users);
		}
	}

	final int getValue() {
		
		return this.value;
	}

	final int incrementCheck(final String address) {
		
		synchronized (this) {
			final AtomicInteger counter = this.users.get(address);
			if (counter == null) {
				this.users.put(address, new AtomicInteger(CounterSegment.COUNTER_INITIAL));
			} else {
				if (counter.intValue() >= this.userLimit) {
					return CounterBlock.DENIED_ADDRESS;
				}
				counter.incrementAndGet();
			}
			this.value++;
		}
		return CounterBlock.GRANTED;
	}
}
