/*
 * Created on 29.10.2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.util.HashMap;
import java.util.Map;

import ru.myx.ae3.base.BaseFunctionActAbstract;
import ru.myx.ae3.binary.TransferDescription;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.serve.ServeRequest;

/**
 * @author myx
 *
 *         Window - Preferences - Java - Code Style - Code Templates
 */
final class CounterSegmentFilter extends BaseFunctionActAbstract<ServeRequest, Void> {
	
	private int valueAll = 0;

	private int valueAddress = 0;

	private final Map<String, CounterFilter> users;

	private static final short COUNTER_INITIAL = 1;

	private final TrafficFilter trafficFilter;

	private final CounterSegment parent;

	@SuppressWarnings("unchecked")
	CounterSegmentFilter(final TrafficFilter trafficFilter, final CounterSegment parent) {
		super((Class<ServeRequest>) Convert.Any.toAny(ServeRequest.class), Void.class);
		this.trafficFilter = trafficFilter;
		this.parent = parent;
		this.users = new HashMap<>();
	}

	final void decrement(final String address) {
		
		synchronized (this) {
			this.valueAll--;
			final CounterFilter counter = this.users.get(address);
			if (counter != null) {
				if (counter.decrement() <= 0) {
					this.valueAddress--;
					this.users.remove(address);
				}
			}
		}
	}

	@Override
	public final Void apply(final ServeRequest query) {
		
		this.decrement(query.getSourceAddress());
		if (this.parent != null) {
			this.parent.apply(query);
		}
		return null;
	}

	final int getValueAddress() {
		
		return this.valueAddress;
	}

	final int getValueAll() {
		
		return this.valueAll;
	}

	final TransferDescription incrementCheck(final String address) {
		
		synchronized (this) {
			this.valueAll++;
			final CounterFilter counter = this.users.get(address);
			if (counter == null) {
				final CounterFilter result = new CounterFilter(this.trafficFilter, CounterSegmentFilter.COUNTER_INITIAL);
				this.users.put(address, result);
				this.valueAddress++;
				return result;
			}
			counter.increment();
			return counter;
		}
	}

	@Override
	public String toString() {
		
		return "CSF{parent=" + this.parent + "}";
	}
}
