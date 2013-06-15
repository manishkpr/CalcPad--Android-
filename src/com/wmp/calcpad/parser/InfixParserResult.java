package com.wmp.calcpad.parser;

public class InfixParserResult {
	
	public static final byte STATUS_OK = 1;
	public static final byte STATUS_OK_UNASSIGNED_VARIABLES = 2;
	
	public static final byte STATUS_ERR_GENERIC = 10;
	public static final byte STATUS_ERR_EMPTY_INPUT = 11;
	public static final byte STATUS_ERR_SEPARATOR_OR_PARENTHESES_MISMATCHED = 12; 
	public static final byte STATUS_ERR_PARENTHESES_MISMATCHED = 13;
	public static final byte STATUS_ERR_UNKNOWN_TOKEN = 14;
	
	public byte status = InfixParserResult.STATUS_OK; 
	public int errorIndex = -1;
	public char[] output;	
	
	public InfixParserResult(){
		
	}
	
	public void reset(){
		status = InfixParserResult.STATUS_OK; 
		errorIndex = -1;
		output = null; 
	}
	
	public String outputToString(){
		if( output == null || output.length == 0 ){
			return new String(); 
		} else{
			return new String(output);
		}
	}
	
	public String outputToStringInReverse(){
		if( output == null || output.length == 0 ){
			return new String(); 
		} else{
			char[] outputInReverse = new char[output.length];
			for( int i=0; i<output.length; i++ ){
				outputInReverse[i] = output[output.length-(i+1)];
			}
			return new String(outputInReverse);
		}
	}
}
