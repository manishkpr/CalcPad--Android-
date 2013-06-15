package com.wmp.calcpad.messaging;

public interface ICalcPadSubscriber {

	public void addObserver(ICalcPadObserver observer);
	
	public void removeObserver(ICalcPadObserver observer);
	
	public boolean broadcast(long eventId, String event);
}
