package com.wmp.calcpad.parser;

public class CalcPadVariable {

	public String label;	
	public float value = 0.0f; 
	
	public CalcPadVariable(){		
		
	}
	
	public CalcPadVariable(char asciiChar, float value ){		
		this.label = Character.toString(asciiChar); 
		this.value = value; 
	}
	
	public CalcPadVariable(String label, float value ){
		this.label = label; 
		this.value = value; 
	}
	
	public String getVariableName(){
		return this.label == null ? "" : this.label;
	}
	
	public float getValue(){
		return this.value;
	}
	
	public static CalcPadVariable createCalcPadVariable( String label ){
		return new CalcPadVariable(label, 0.0f);
	}
	
	public static CalcPadVariable createExpressionVariable( String label, float value ){
		return new CalcPadVariable(label, value);
	}
	
	public boolean isLabelSet(){
		return label != null && label.length() > 0; 
	}
	
}
