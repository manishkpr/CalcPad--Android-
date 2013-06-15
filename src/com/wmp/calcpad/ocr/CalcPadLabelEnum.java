package com.wmp.calcpad.ocr;

/**
 * http://en.wikipedia.org/wiki/Bracket
 * http://www.asciitable.com/
 * @author josh
 *
 */

public enum CalcPadLabelEnum {
	Label_0	(0, 48, "0", CalcPadLabelTypeEnum.Operands), 
	Label_1 (1, 49, "1", CalcPadLabelTypeEnum.Operands),
	Label_2 (2, 50, "2", CalcPadLabelTypeEnum.Operands),
	Label_3 (3, 51, "3", CalcPadLabelTypeEnum.Operands),
	Label_4 (4, 52, "4", CalcPadLabelTypeEnum.Operands),
	Label_5 (5, 53, "5", CalcPadLabelTypeEnum.Operands),
	Label_6 (6, 54, "6", CalcPadLabelTypeEnum.Operands),
	Label_7 (7, 55, "7", CalcPadLabelTypeEnum.Operands),
	Label_8 (8, 56, "8", CalcPadLabelTypeEnum.Operands),
	Label_9 (9, 57, "9", CalcPadLabelTypeEnum.Operands),
	Label_DOT (10, 46, ".", CalcPadLabelTypeEnum.Operands), // '.' 
	Label_Open_Prenthesis (11, 40, "Open Parenthesis '('", CalcPadLabelTypeEnum.Block), // '('
	Label_Closed_Prenthesis (12, 41, "Closed Parenthesis ')'", CalcPadLabelTypeEnum.Block), // ')'
	Label_Plus (13, 43, "Addition '+'", CalcPadLabelTypeEnum.Operator), // '+' 
	Label_Minus (14, 45, "Subtraction '-'", CalcPadLabelTypeEnum.Operator), // '-' 
	Label_Multiply (15, 42, "Multiply '*'", CalcPadLabelTypeEnum.Operator), // '*'
	Label_Divide (16, 47, "Divide '/'", CalcPadLabelTypeEnum.Operator), // '/'
	Label_Pow (17, 94, "Power of '^'", CalcPadLabelTypeEnum.Operator), // '^'
	Label_Sqrt (18, 251, "Square Root 'Ã'", CalcPadLabelTypeEnum.Operator), 
	Label_Summation (19, 251, "Summation '·'", CalcPadLabelTypeEnum.Function), // '·' 		
	Label_PI (20, 227, "PI '¹'", CalcPadLabelTypeEnum.Constant), // '¹'
	Label_LT (21, 60, "Less Than '<'", CalcPadLabelTypeEnum.Operator), // '<'
	Label_GT (22, 62, "Greater Than '>'", CalcPadLabelTypeEnum.Operator), // '>'
	Label_Equals (23, 61, "Equals '='", CalcPadLabelTypeEnum.Operator), // '='
	Label_Function (24, 159, "Function 'Ä'", CalcPadLabelTypeEnum.Function), // 'Ä'
	Label_x (25, 120, "Variable x", CalcPadLabelTypeEnum.Text), // 'x'
	Label_y (26, 121, "Variable y", CalcPadLabelTypeEnum.Text), // 'y'
	Unknown(-1, 63, "Unknown", CalcPadLabelTypeEnum.Void);

	private final int _label; 
	private final int _asciiDecValue;
	private final String _friendlyName; 
	private final CalcPadLabelTypeEnum _type; 
	
	private CalcPadLabelEnum(int label, int asciiDecValue, String friendlyName, CalcPadLabelTypeEnum type) {
		this._label = label; 
		this._asciiDecValue = asciiDecValue; 
		this._friendlyName = friendlyName; 
		this._type = type; 
	}
	
	public int label(){ return _label; }
	
	public int value(){ return _asciiDecValue; }
	
	public CalcPadLabelTypeEnum type(){ return _type; }
	
	public String friendlyName(){ return _friendlyName; } 
	
	public char toChar(){ return (char)_asciiDecValue; }
	
	public String toString(){ return friendlyName(); }
}
