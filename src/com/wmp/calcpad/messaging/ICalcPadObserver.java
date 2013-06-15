package com.wmp.calcpad.messaging;

public interface ICalcPadObserver {

	public boolean OnCalcPadEvent( long eventId, String event, Object caller);
	
}
