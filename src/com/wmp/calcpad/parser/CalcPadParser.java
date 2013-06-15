package com.wmp.calcpad.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import android.util.Log;

import com.wmp.calcpad.interrupter.CalcPadExpressionBuilder;
import com.wmp.calcpad.parser.exception.VariableNotFoundException;


/**
 * reference: 
 * Parsing: 
 * http://www.technical-recipes.com/2011/a-mathematical-expression-parser-in-java/
 *  
 * @author josh
 *
 */
public class CalcPadParser {
	
	// Associativity constants for operators  
    private static final int LEFT_ASSOC  = 0;  
    private static final int RIGHT_ASSOC = 1;  
   
    // Operators  
    private static final Map<String, int[]> OPERATORS = new HashMap<String, int[]>();  
    static   
    {  
        // Map<"token", []{precendence, associativity}>  
        OPERATORS.put("+", new int[] { 0, LEFT_ASSOC });  
        OPERATORS.put("-", new int[] { 0, LEFT_ASSOC });  
        OPERATORS.put("*", new int[] { 5, LEFT_ASSOC });  
        OPERATORS.put("/", new int[] { 5, LEFT_ASSOC });
        OPERATORS.put("^", new int[] { 7, LEFT_ASSOC });
    } 
    
    /** associated event **/ 
    public long eventId = -1; 
	
	private CalcPadExpressionBuilder _expression;
	
	private String _expressionAsString = "";
	
	private byte _infixParserResultStatus = 0; // not set 	
	
	private CalcPadVariable _cachedExpressionVariable; 
	
	
	public CalcPadParser(){
		
	}
	
	public CalcPadParser(CalcPadExpressionBuilder expression){
		this._expression = expression; 
	}
	
	public void setExpression( CalcPadExpressionBuilder expression ){
		this._expression = expression; 
	}
	
	public CalcPadExpressionBuilder getExpression(){
		return this._expression; 
	}
	
	public byte getInfixParserResultStatus(){
		return _infixParserResultStatus; 
	}
	
	public CalcPadVariable parse(List<CalcPadVariable> variables) throws VariableNotFoundException{
		if( this._expression == null ){
			return CalcPadVariable.createCalcPadVariable("");
		}
		
		// check if we have already pre-calcualted the value 
		String expressionsEvaluated = this._expression.buildExpression();
		
		// nothing there 
		if( expressionsEvaluated == null || expressionsEvaluated.length() == 0.0f ){
			return CalcPadVariable.createCalcPadVariable("");
		}
		
		if( _expressionAsString != null && _expressionAsString.equals(expressionsEvaluated) ){
			return _cachedExpressionVariable; 
		}
		
		if( _cachedExpressionVariable == null ){
			_cachedExpressionVariable = new CalcPadVariable(); 
		}					
		
		/*
		String[] RPNinput = infixToRPN(this._expressionAsString);
		
		StringBuilder sb = new StringBuilder();
		for( String s : RPNinput ){
			sb.append(s); 
		}
		
		Log.i("CalcPadParser", String.format("parse.infixToRPN result = %s", sb.toString()));
		
		_calculatedValue = RPNtoFloat(RPNinput);
		*/
				
				
		InfixParserResult infixParserResult = new InfixParserResult(); 
		
		if( ShuntingYardParser.parse(expressionsEvaluated, infixParserResult) ){
			
			_infixParserResultStatus = infixParserResult.status; 
			
			if( infixParserResult.status == InfixParserResult.STATUS_OK ){
				String infixValue = infixParserResult.outputToString(); 
				Log.i("CalcPadParser", "infix result: " + infixValue + " (from " + expressionsEvaluated + ")");						
				
				ExpressionTree expressionTree = new ExpressionTree(infixValue);
				expressionTree.build(variables);
				
				_cachedExpressionVariable.value = expressionTree.evaluate();
				
				if( expressionTree.isVariable() ){
					_cachedExpressionVariable.label = expressionTree.getVariableName();
				}
				
				Log.i("CalcPadParser", "ExpressionTree " + expressionTree.isVariable() + ", " + expressionTree.getStatus().toString()
						+ ", calculated value = " + _cachedExpressionVariable.value + " " + (expressionTree.isVariable() ? expressionTree.getVariableName() : "") );								
				
				//ReversePolishNotationResult rpnResult = new ReversePolishNotationResult(); 		
				//_calculatedValue = ReversePolishNotation.evaluate(infixValue, rpnResult);
			} else{
				Log.i("CalcPadParser", "infixParserResult.status = " + infixParserResult.status);
			}
		} else{
			Log.i("CalcPadParser", "infixParserResult.status = " + infixParserResult.status);
		}
		
		/*
		 String[] RPNinput = infixToRPN(this._expressionAsString);
		 
		
		StringBuilder sb = new StringBuilder();
		for( String s : RPNinput ){
			sb.append(s); 
		}
		*/		
		
		// cache 
		this._expressionAsString = expressionsEvaluated;
		
		return _cachedExpressionVariable; 
	}	
	
	public CalcPadVariable getResult(){
		return _cachedExpressionVariable;
	}
	
	public String toString(){
		if( this._expression == null ){
			return "Empty CalcPadParser";
		}
		
		// check if we have already pre-calcualted the value 
		String expressionsEvaluated = this._expression.buildExpression();
		
		if( this._expressionAsString != null ){
			
			if( this._expressionAsString.equals(expressionsEvaluated) ){
				if( expressionsEvaluated.charAt(expressionsEvaluated.length()-1) == '=' ){
					expressionsEvaluated += " " + _cachedExpressionVariable.value; 
				} else if( expressionsEvaluated.length() >= 2 && expressionsEvaluated.charAt(expressionsEvaluated.length()-2) == '=' ){
					String[] split = expressionsEvaluated.split("=");
					if( split.length == 2 ){
						expressionsEvaluated += " = " + _cachedExpressionVariable.value;
					}
				} 
			} 			
		} 
		
		return expressionsEvaluated; 								
	}		
   
    // Test if token is an operator  
    private static boolean isOperator(String token)   
    {  
        return OPERATORS.containsKey(token);  
    }   
   
    // Test associativity of operator token  
    private static boolean isAssociative(String token, int type)   
    {  
        if (!isOperator(token))   
        {  
            throw new IllegalArgumentException("Invalid token: " + token);  
        }  
          
        if (OPERATORS.get(token)[1] == type) {  
            return true;  
        }  
        return false;  
    }  
   
    // Compare precedence of operators.      
    private static final int cmpPrecedence(String token1, String token2)   
    {  
        if (!isOperator(token1) || !isOperator(token2))   
        {  
            throw new IllegalArgumentException("Invalid tokens: " + token1  
                    + " " + token2);  
        }  
        return OPERATORS.get(token1)[0] - OPERATORS.get(token2)[0];  
    } 
    
    // Convert infix expression format into reverse Polish notation  
    public static String[] infixToRPN(String input)   
    {  
    	// ignore '=' 
    	int count = input.length(); 
    	
    	if( input.charAt(count-1) == '=' ){
    		count--; 
    	}
    	
    	String[] inputTokens = new String[count];
    	for( int i=0; i<count; i++ ){    		
    		inputTokens[i] = String.valueOf(input.charAt(i));
    	}
    	
        ArrayList<String> out = new ArrayList<String>();  
        Stack<String> stack = new Stack<String>();  
          
        // For each token  
        for (String token : inputTokens)   
        {  
            // If token is an operator  
            if (isOperator(token))   
            {    
                // While stack not empty AND stack top element   
                // is an operator  
                while (!stack.empty() && isOperator(stack.peek()))   
                {                      
                    if ((isAssociative(token, LEFT_ASSOC)         &&   
                         cmpPrecedence(token, stack.peek()) <= 0) ||   
                        (isAssociative(token, RIGHT_ASSOC)        &&   
                         cmpPrecedence(token, stack.peek()) < 0))   
                    {  
                        out.add(stack.pop()); 
                        out.add(" "); // add space between operators 
                        continue;  
                    }  
                    break;  
                }  
                // Push the new operator on the stack  
                stack.push(token);  
            }   
            // If token is a left bracket '('  
            else if (token.equals("("))   
            {  
                stack.push(token);  //   
            }   
            // If token is a right bracket ')'  
            else if (token.equals(")"))   
            {                  
                while (!stack.empty() && !stack.peek().equals("("))   
                {  
                    out.add(stack.pop());   
                }  
                stack.pop();   
            }   
            // If token is a number  
            else   
            {  
                out.add(token);   
            }  
        }  
        while (!stack.empty())  
        {  
            out.add(stack.pop());   
        }  
        String[] output = new String[out.size()];  
        return out.toArray(output);  
    }
    
    public static float RPNtoFloat(String[] tokens)  
    {          
        Stack<String> stack = new Stack<String>();  
          
        // For each token   
        for (String token : tokens)   
        {  
            // If the token is a value push it onto the stack  
            if (!isOperator(token))   
            {  
                stack.push(token);                  
            }  
            else  
            {  
                // Token is an operator: pop top two entries  
                float d2 = Float.valueOf( stack.pop() );  
                float d1 = Float.valueOf( stack.pop() );  
                  
                //Get the result  
                float result = token.compareTo("+") == 0 ? d1 + d2 :   
                                token.compareTo("-") == 0 ? d1 - d2 :  
                                token.compareTo("*") == 0 ? d1 * d2 :  
                                token.compareTo("/") == 0 ? d1 / d2 : 
                                token.compareTo("^") == 0 ? (float)Math.pow(d1, d2) :
                                		0;                 
                                  
                // Push result onto stack  
                stack.push( String.valueOf( result ));                                                  
            }                          
        }          
          
        return Float.valueOf(stack.pop());  
    }
	
}
