package com.wmp.calcpad.parser.exception;

public class VariableNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String _variableLabel; 
	
	public VariableNotFoundException(String variableLabel){
		this._variableLabel = variableLabel;
	}
	
	public String getVariableLabel(){
		return _variableLabel; 
	}

}
