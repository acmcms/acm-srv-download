/*
 * Created on 07.11.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import ru.myx.ae3.act.Act;
import ru.myx.ae3.binary.TransferDescription;
import ru.myx.ae3.help.Validate;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
final class CounterBlockFilter {
	private static final int	VALUE_COUNT	= Validate.Fit.value( Act.PEAK_LOAD, 64, 128 );
	
	private static final int	VALUE_MASK	= CounterBlockFilter.VALUE_COUNT - 1;
	
	static final int makeIndex(final String address) {
		return address.hashCode() & CounterBlockFilter.VALUE_MASK;
	}
	
	private final CounterBlock				parent;
	
	private final CounterSegmentFilter[]	counterDecrement;
	
	private final CounterSegmentFilter[]	counterIgnore;
	
	CounterBlockFilter(final TrafficFilter trafficFilter, final CounterBlock parent) {
		this.parent = parent;
		this.counterDecrement = new CounterSegmentFilter[CounterBlockFilter.VALUE_COUNT];
		this.counterIgnore = new CounterSegmentFilter[CounterBlockFilter.VALUE_COUNT];
		for (int i = CounterBlockFilter.VALUE_MASK; i >= 0; --i) {
			this.counterDecrement[i] = new CounterSegmentFilter( trafficFilter, parent.getCounter( i ) );
			this.counterIgnore[i] = new CounterSegmentFilter( trafficFilter, null );
		}
	}
	
	final CounterSegmentFilter getCounter(final int index, final boolean decrement) {
		if (decrement) {
			return this.counterDecrement[index];
		}
		return this.counterIgnore[index];
	}
	
	final int getValueAddress() {
		int result = 0;
		for (int i = CounterBlockFilter.VALUE_MASK; i >= 0; --i) {
			result += this.counterDecrement[i].getValueAddress();
			result += this.counterIgnore[i].getValueAddress();
		}
		return result;
	}
	
	final int getValueAll() {
		int result = 0;
		for (int i = CounterBlockFilter.VALUE_MASK; i >= 0; --i) {
			result += this.counterDecrement[i].getValueAll();
			result += this.counterIgnore[i].getValueAll();
		}
		return result;
	}
	
	final TransferDescription incrementCheck(final int index, final String address, final boolean decrement) {
		if (decrement) {
			return this.counterDecrement[index].incrementCheck( address );
		}
		return this.counterIgnore[index].incrementCheck( address );
	}
	
	@Override
	public String toString() {
		return "CBF{parent=" + this.parent + "}";
	}
}
