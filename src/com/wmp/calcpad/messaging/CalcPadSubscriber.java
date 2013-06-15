package com.wmp.calcpad.messaging;

import java.util.List;
import java.util.Vector;

public class CalcPadSubscriber implements ICalcPadSubscriber {
	
	protected List<ICalcPadObserver> observers = new Vector<ICalcPadObserver>(); 
	
	public void addObserver(ICalcPadObserver observer){
		this.observers.add(observer);
	}
	
	public void removeObserver(ICalcPadObserver observer){
		this.observers.remove(observer);
	}
	
	public boolean broadcast(long eventId, String event){
		return broadcast(eventId, event, this);
	}
	
	public boolean broadcast(long eventId, String event, Object caller){
		int count = this.observers.size();
		
		for( int idx=count-1; idx>=0; idx-- ){
			if( this.observers.get(idx) == null ){
				this.observers.remove(idx);
			} else{
				if( this.observers.get(idx).OnCalcPadEvent(eventId, event, this) ){
					return true; 
				}
			}
		}
		
		return false; 
	}
	
}
