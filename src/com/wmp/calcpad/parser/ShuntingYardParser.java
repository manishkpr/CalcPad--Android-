package com.wmp.calcpad.parser;

import java.util.Stack;

/**
 * Ported from: 
 * http://en.wikipedia.org/wiki/Shunting_yard_algorithm
 * @author josh
 */
public final class ShuntingYardParser {

	public static boolean parse(String infix, InfixParserResult result){
		
		if( result == null ){
			return false; 
		}
		
		// reset so we can re-use the same object 
		result.reset();
		
		if( infix == null || infix.trim().length() == 0 ){			
			result.status = InfixParserResult.STATUS_ERR_EMPTY_INPUT;
			return false; 
		}
		
		char[] input = infix.toCharArray(); // infix input 				
		Stack<Character> output = new Stack<Character>(); // output 
		Stack<Character> stack = new Stack<Character>();  // operator stack
		char stackChar;          // used for record stack element
		
		int idx = 0;
		int len = input.length;
		
		for( idx = 0; idx < len; idx++ ){
			char c = input[idx];
			
			//if( c != ' ' ){
				/*if( c == '=' ){
					if( idx < len-1 ){
						output.push(Character.valueOf(c));
					}					
				}			
				// If the token is a number (identifier), then add it to the output queue.
				else*/
				if( isIdent(c) ){
					output.push(Character.valueOf(c));
				}
				else if( isText(c) ){
					output.push( Character.valueOf(c) );
				}
				// If the token is a function token, then push it onto the stack.
				else if( isFunction(c) ){
					stack.push(Character.valueOf(c));
				}
				// If the token is a function argument separator (e.g., a comma):
				else if(c == ',')   {
	                boolean pe = false;
	                while(stack.size() > 0)   {
	                	stackChar = stack.peek().charValue();
	                    if(stackChar == '(')  {
	                        pe = true;
	                        break;
	                    }
	                    else  {
	                        // Until the token at the top of the stack is a left parenthesis,
	                        // pop operators off the stack onto the output queue.
	                        output.push(Character.valueOf(stackChar)); 
	                        stack.pop(); 
	                    }
	                }
	                // If no left parentheses are encountered, either the separator was misplaced
	                // or parentheses were mismatched.
	                if(!pe)   {
	                    result.status = InfixParserResult.STATUS_ERR_SEPARATOR_OR_PARENTHESES_MISMATCHED;	
	                    result.errorIndex = idx;
	                    return false;
	                }
	            }
				// If the token is an operator, op1, then:
				else if(isOperator(c))  {
					while(stack.size() > 0)    {
	                    stackChar = stack.peek().charValue();
	                    
	                    // While there is an operator token, op2, at the top of the stack
	                    // op1 is left-associative and its precedence is less than or equal to that of op2,
	                    // or op1 has precedence less than that of op2,
	                    // Let + and ^ be right associative.
	                    // Correct transformation from 1^2+3 is 12^3+
	                    // The differing operator priority decides pop / push
	                    // If 2 operators have equal priority then associativity decides.
	                    if(isOperator(stackChar) &&
	                        ((getOPLeftAssoc(c) && (getOPPreced(c) <= getOPPreced(stackChar))) ||
	                           (getOPPreced(c) < getOPPreced(stackChar))))   {
	                        // Pop op2 off the stack, onto the output queue;
	                    	output.push(Character.valueOf(stackChar));
	                    	output.push(Character.valueOf(' '));
	                        stack.pop(); 	                        
	                    }	
	                    //else if( stackChar == ' ' ){
	                    //	
	                    //}
	                    else{
	                    	
	                        break;
	                    }
	                }
	                // push op1 onto the stack.
					stack.push(Character.valueOf(c));	                
				}
				// If the token is a left parenthesis, then push it onto the stack.
	            else if(c == '(')   {
	            	stack.push(Character.valueOf(c));	                
	            }
				// If the token is a right parenthesis:
	            else if(c == ')')    {
	                boolean pe = false;
	                // Until the token at the top of the stack is a left parenthesis,
	                // pop operators off the stack onto the output queue
	                while(stack.size() > 0)     {
	                    stackChar = stack.peek().charValue();
	                    if(stackChar == '(')    {
	                        pe = true;
	                        break;
	                    }
	                    else  {
	                        output.push(Character.valueOf(stackChar)); 
	                        stack.pop(); 
	                    }
	                }
	                // If the stack runs out without finding a left parenthesis, then there are mismatched parentheses.
	                if(!pe)  {
	                	result.status = InfixParserResult.STATUS_ERR_PARENTHESES_MISMATCHED;
	                	result.errorIndex = idx;
	                    return false;
	                }
	                // Pop the left parenthesis from the stack, but not onto the output queue.
	                stack.pop();
	                // If the token at the top of the stack is a function token, pop it onto the output queue.
	                if(stack.size() > 0)   {
	                    stackChar = stack.peek().charValue();
	                    if(isFunction(stackChar))   {
	                    	output.push(Character.valueOf(stackChar)); 
	                        stack.pop();	                        
	                    }
	                }
	            }
	            else if( isSeparater(c) ){
	            	if( output.size() >= 1 && !isSeparater(output.peek().charValue()) ){  
	    	        	output.push(Character.valueOf(' '));
	    	        }	            	
	            }
				// unknown token 
	            else  {
	            	result.status = InfixParserResult.STATUS_ERR_UNKNOWN_TOKEN;
	            	result.errorIndex = idx;
	                return false; // Unknown token
	            }
			//}
		}
		
		// When there are no more tokens to read:
	    // While there are still operator tokens in the stack:
	    while(stack.size() > 0)  {
	    	stackChar = stack.peek().charValue();	       
	        if(stackChar == '(' || stackChar == ')')   {
	        	result.status = InfixParserResult.STATUS_ERR_PARENTHESES_MISMATCHED;	            
	            return false;
	        }
	        if( output.size() >0 && !isSeparater(output.peek().charValue()) ){  
	        	output.push(Character.valueOf(' '));
	        }
	        output.push(Character.valueOf(stackChar));
            stack.pop();
	    }
	    
	    result.output = new char[output.size()];
	    
	    for( int i=0; i<output.size(); i++ ){
	    	result.output[i] = output.get(i).charValue();
	    }
		
		return true; 
	}
	
	/**
	 * operators
	 * precedence   operators       associativity
	 * 4            ^               right to left
	 * 3            * / %           left to right
	 * 2            + -             left to right
	 * 1            =               right to left
	 */
	private static int getOPPreced(char c)
	{
	    switch(c)    {
	        case '^':
	            return 4;
	        case '*':  case '/': case '%':
	            return 3;
	        case '+': case '-':
	            return 2;
	        case '=':
	            return 1;
	    }
	    return 0;
	}
	
	private static boolean getOPLeftAssoc(char c)
	{
	    switch(c)    {
	        // left to right
	        case '*': case '/': case '%': case '+': case '-': case '^':
	            return true;
	        // right to left
	        case '=': 
	            return false;
	    }
	    return false;
	}
	
	private static int getOPArgCount(char c)
	{
	    switch(c)  {
	        case '*': case '/': case '%': case '+': case '-': case '=':
	            return 2;
	        case '^':
	            return 1;
	        default:
	            return c - 'A';
	    }	    	    
	}
	
	private static boolean isOperator(char c){
		return (c == '+' || c == '-' || c == '/' || c == '*' || c == '^' || c == '%' || c == '=');
	}	
	
	private static boolean isFunction(char c){
		return (c >= 'A' && c <= 'Z');
	}
	
	private static boolean isIdent(char c){
		return ((c >= '0' && c <= '9') || c == '.');
	}
	
	private static boolean isText(char c){
		return (c >= 'a' && c <= 'z');
	}
	
	private static boolean isSeparater(char c){
		return c == ' ';
	}
	
}
