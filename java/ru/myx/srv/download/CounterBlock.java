/*
 * Created on 07.11.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ru.myx.ae3.act.Act;
import ru.myx.ae3.help.Validate;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
final class CounterBlock {
	static final int			GRANTED			= 0;
	
	static final int			DENIED_ADDRESS	= 1;
	
	static final int			DENIED_TOTAL	= 2;
	
	private static final int	VALUE_COUNT		= Validate.Fit.value( Act.PEAK_LOAD, 64, 128 );
	
	private static final int	VALUE_MASK		= CounterBlock.VALUE_COUNT - 1;
	
	static final int makeIndex(final String address) {
		return (address.hashCode() & 0x7FFFFFFF) % CounterBlock.VALUE_COUNT;
	}
	
	private final CounterSegment[]	counter;
	
	private final int				totalLimit;
	
	CounterBlock(final int totalLimit, final int userLimit) {
		this.totalLimit = totalLimit;
		this.counter = new CounterSegment[CounterBlock.VALUE_COUNT];
		for (int i = CounterBlock.VALUE_MASK; i >= 0; --i) {
			this.counter[i] = new CounterSegment( userLimit );
		}
	}
	
	final void fillLoad(final Map<String, AtomicInteger> load) {
		for (int i = CounterBlock.VALUE_MASK; i >= 0; --i) {
			this.counter[i].fillLoad( load );
		}
	}
	
	final CounterSegment getCounter(final int index) {
		return this.counter[index];
	}
	
	final int getValue() {
		int result = 0;
		for (int i = CounterBlock.VALUE_MASK; i >= 0; --i) {
			result += this.counter[i].getValue();
		}
		return result;
	}
	
	final int incrementCheck(final int index, final String address) {
		final int totalValue = this.getValue();
		if (totalValue >= this.totalLimit) {
			return CounterBlock.DENIED_TOTAL;
		}
		return this.counter[index].incrementCheck( address );
	}
}
