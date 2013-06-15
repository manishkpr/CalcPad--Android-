package com.wmp.calcpad.interrupter;

import com.wmp.calcpad.ocr.CalcPadLabelTypeEnum;

public interface CalcPadExpressionBuilderDataSource {
	
	public CalcPadLabelTypeEnum getLabelTypeForAsciiValue(char asciiValue);
}
