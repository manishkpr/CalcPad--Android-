package com.wmp.calcpad.interrupter;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import com.wmp.calcpad.ocr.CalcPadLabelEnum;
import com.wmp.calcpad.ocr.CalcPadLabelTypeEnum;
import com.wmp.calcpad.parser.CalcPadVariable;

public class CalcPadExpressionBuilder {
	
	private List<Token> _tokens = new Vector<Token>(); 	
	
	//private List<String> _variables = null;  

	private PointF _avgTopAndBottom = new PointF(0.0f, 0.0f);
	
	private PointF _avgWidthHeight = new PointF(0.0f, 0.0f);
	
	private PointF _maxWidthHeight = new PointF(0.0f, 0.0f);		

	private RectF _boundingBox = new RectF();
	
	private CalcPadExpressionBuilderDataSource _datasource = null; 
	
	public CalcPadExpressionBuilder(CalcPadExpressionBuilderDataSource datasource){
		_datasource = datasource; 
	}		
	
	public boolean inBounds( org.opencv.core.Rect rect, boolean isExpression ){
		return inBounds( openCvRectToRectF(rect), isExpression );
	}
	
	public boolean inBounds( Rect rect, boolean isExpression ){
		return inBounds( new RectF(rect.left, rect.top, rect.right, rect.bottom), isExpression ); 
	}	
	
	public boolean inBounds( RectF rect, boolean isExpression ){
		if( _boundingBox.contains(rect)){
			return true; 
		} 
		/* else if( RectF.intersects(_boundingBox, rect) ){
			//return true; 
		} */
		else{
			
			float avgWidth = (!isExpression && rect.width() > _avgWidthHeight.x) ? rect.width() : _avgWidthHeight.x;
			//float avgHeight = (!isExpression && rect.height() > _avgWidthHeight.y) ? rect.height() : _avgWidthHeight.y;
			float hDisThreshold = avgWidth;
			//float vDisThreshold = avgHeight;
			
			// discard if y position is out of 'reach'  
			//if( Math.abs(rect.centerY() - _boundingBox.centerY()) > vDisThreshold ){
			//	return false; 
			//} else if( rect.bottom < _boundingBox.top ){
			if( rect.bottom < _boundingBox.top ){
				return false; 
			} else if( rect.top > _boundingBox.bottom ){
				return false; 
			}
			
			// check if left or right edge is close enough to the bounding box (allow the user to  
			if( rect.right <= _boundingBox.left ){
				float dis = _boundingBox.left - rect.right; 
				if( dis < hDisThreshold ){
					return true; 
				}
			} else if( rect.left >= _boundingBox.right ){
				float dis = rect.left - _boundingBox.right; 
				if( dis < hDisThreshold ){
					return true; 
				}
			} else{				
				return true; 
			}
		}
		
		return false; 
	}
	
	public boolean inBounds( CalcPadExpressionBuilder otherExpressionBuilder ){
		if( this._avgWidthHeight.x > otherExpressionBuilder._avgWidthHeight.x ){
			return this.inBounds( otherExpressionBuilder.getBoundingBox(), true );
		} else{
			return otherExpressionBuilder.inBounds( this.getBoundingBox(), true );
		}
	}
	
	public void addItem(char asciiChar, org.opencv.core.Rect rect){
		RectF rectF = openCvRectToRectF(rect);		
		addItem(asciiChar, rectF);
	}	
	
	public void addItem(char asciiChar, RectF rectF){		
		//Log.i( "CalcPadExpressionBuilder.addItem", "Adding " + asciiChar + " to " + toShortString());
		
		expandBoundingBox(rectF);				
		
		Token item = new Token(asciiChar, rectF, _datasource.getLabelTypeForAsciiValue(asciiChar));
		_tokens.add(item);	
		
		sortTokens(); 
		
		updateAverages(); 
	}	
	
	public void addExpression(CalcPadExpressionBuilder expression){
		for( Token token : expression._tokens ){
			addItem(token.asciiChar, token.boundingBox);
		}
	}
	
	public String buildExpression( CalcPadVariable... variables ){
		
		StringBuilder expression = new StringBuilder();
		StringBuilder variable = new StringBuilder(); 
			
		CalcPadLabelTypeEnum previousType = CalcPadLabelTypeEnum.Void; 
		
		boolean previousWasPOW = false; 
		boolean previousWasEquals = false; 
		
		for( int i=0; i<_tokens.size(); i++ ){
			Token token = _tokens.get(i);
			
			if( i != 0 ){								
				
				if( token.type != previousType ){
					
					/*if( previousType == CalcPadLabelTypeEnum.Text ){
						if( variables != null ){
							for(ExpressionVariable var : variables ){
								if( var.label.equals(variable.toString()) ){
									expression.append(Float.toString(var.value));
									variable.delete(0, variable.length());
									break; 
								}
							}
						}
						
						if( variable.length() > 0 ){
							expression.append(variable.toString());
							
							if( _variables == null ){
								_variables = new Vector<String>(); 
							}
							
							if( !_variables.contains(variable.toString())){
								_variables.add(variable.toString()); // add to variables
							}							 
							variable.delete(0, variable.length());
						}
					
					}*/
					
					/*if( previousType == CalcPadLabelTypeEnum.Text ){
						if( variables != null ){
							// work our way backwards building up the text to test if it was a function or variable - if variable
							
							// loop backwards until we find the ' ' 
							StringBuilder sb = new StringBuilder(); 
							while(expression.charAt(expression.length()-1) != ' ' && expression.length() > 0){
								sb.insert(0, expression.charAt(expression.length()-1));
								expression.deleteCharAt(expression.length()-1);
							}
							
							if( sb.length() > 0 ){
								String sbAsString = sb.toString(); 
								
								for(ExpressionVariable variable : variables ){
									if( variable.label.equals(sbAsString) ){
										expression.append(Float.toString(variable.value));
										sb.delete(0, sb.length());
										break; 
									}
								}
								
								// didn't find an associated variable so put the variable back in the expression 
								if( sb.length() > 0 ){
									expression.append(sb.toString());
								}
							}
						}
					}*/
					
					expression.append( ' ' );
				}
			} else{
				
			} 						
						
			// append the token
			//if( token.type != CalcPadLabelTypeEnum.Text ){
			//	expression.append(token.asciiChar);
			//} else{
				//variable.append(token.asciiChar);
			//}
			
			expression.append(token.asciiChar);
			
			previousWasPOW = checkForPOW(i, expression, previousWasPOW);
			
			previousWasEquals = checkForEquals(i, expression, previousWasEquals);
			
			previousType = token.type;
		}
		
		return expression.toString(); 
	}	
	
	/**
	 * check for the need for a '^' operator 
	 * @param token; current token 
	 * @param i; current index
	 * @param evaluatedExpression; currently built up expression 
	 */
	private boolean checkForPOW( int i, StringBuilder evaluatedExpression, boolean previousWasPOW ){
		
		if( i == 0 ){
			return false; 
		}
		
		Token previousToken = _tokens.get(i-1);
		Token currentToken = _tokens.get(i);		
				
		if( previousToken.type != CalcPadLabelTypeEnum.Operands || currentToken.type != CalcPadLabelTypeEnum.Operands ){
			return false; 
		}
		
		// if current token is smaller (75% smaller) than the previous token and the current tokens bottom is above or equal to the centre of the previous token 
		if( currentToken.boundingBox.height() <= (_maxWidthHeight.y * 0.75f) && currentToken.boundingBox.bottom <= _boundingBox.centerY() ){
			if( !previousWasPOW ){
				evaluatedExpression.insert(evaluatedExpression.length()-1, ' ' );
				evaluatedExpression.insert(evaluatedExpression.length()-1, CalcPadLabelEnum.Label_Pow.toChar() );
				evaluatedExpression.insert(evaluatedExpression.length()-1, ' ' );
			} 			
			return true; 
		}				
		
		return false; 
	}
	
	/**
	 * check for an '=' operator 
	 * @param token
	 * @param i
	 * @param evaluatedExpression
	 */
	private boolean checkForEquals(int i, StringBuilder evaluatedExpression, boolean previousWasEquals ){
		if( evaluatedExpression.length() <= 2 ){
			return false; 
		}
		
		Token previousToken = _tokens.get(i-1);
		Token currentToken = _tokens.get(i);
		
		if( previousToken.asciiChar != CalcPadLabelEnum.Label_Minus.toChar() || currentToken.asciiChar != CalcPadLabelEnum.Label_Minus.toChar() ){
			return false; 
		}
		
		if( currentToken.boundingBox.centerX() > previousToken.boundingBox.left && currentToken.boundingBox.centerX() < previousToken.boundingBox.right ){
			// remove both 			
			evaluatedExpression.deleteCharAt(evaluatedExpression.length()-1);
			evaluatedExpression.deleteCharAt(evaluatedExpression.length()-1);
			evaluatedExpression.append(CalcPadLabelEnum.Label_Equals.toChar());
			return true; 
		}		
		
		return false; 
	}
	
	private void expandBoundingBox(RectF rect){
		
		//Log.i("CalcPadExpressionBuilder.expandBoundingBox", "Adding " + rect.toString() + " to " + _boundingBox.toString() );
		
		if( _tokens.size() == 0 ){
			_boundingBox.set(rect.left, rect.top, rect.right, rect.bottom);
		} else{
			_boundingBox.set(
					(rect.left < _boundingBox.left) ? rect.left : _boundingBox.left, 
							(rect.top < _boundingBox.top) ? rect.top : _boundingBox.top, 
									(rect.right > _boundingBox.right) ? rect.right : _boundingBox.right, 
											(rect.bottom > _boundingBox.bottom) ? rect.bottom : _boundingBox.bottom);
		}
		
	}		
	
	private void updateAverages(){
		float sumW = 0; 
		float sumH = 0; 
		float sumTop = 0; 
		float sumBottom = 0; 
		
		for( Token token : _tokens){
			sumW += token.boundingBox.width(); 
			sumH += token.boundingBox.height();
			sumTop += token.boundingBox.top; 
			sumBottom += token.boundingBox.bottom;
			
			_maxWidthHeight.x = token.boundingBox.width() > _maxWidthHeight.x ? token.boundingBox.width() :_maxWidthHeight.x;
			_maxWidthHeight.y = token.boundingBox.height() > _maxWidthHeight.y ? token.boundingBox.height() :_maxWidthHeight.y;
		}
		
		_avgWidthHeight.x = sumW / (float)_tokens.size();
		_avgWidthHeight.y = sumH / (float)_tokens.size(); 
		_avgTopAndBottom.x = sumTop / (float)_tokens.size(); 
		_avgTopAndBottom.y = sumBottom / (float)_tokens.size(); 
	}
	
	public static RectF openCvRectToRectF(org.opencv.core.Rect rect){
		return new RectF(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
	}
	
	public static Rect openCvRectToRect(org.opencv.core.Rect rect){
		return new Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
	}
	
	public RectF getBoundingBox(){
		return _boundingBox; 
	}
	
	/** sort the tokens based on x position */ 
	protected synchronized void sortTokens(){
		Collections.sort(_tokens, new Comparator<Token>() {

			@Override
			public int compare(Token lhs, Token rhs) {
				float lhsX = -1;
				float rhsX = -1; 
				
				lhsX = lhs.boundingBox.left;
				rhsX = rhs.boundingBox.left; 
				
				if( lhsX < rhsX ){
					return -1; 
				} else if( lhsX > rhsX ){
					return 1; 
				} else{
					return 0; 
				}				
			}
		});				
	}
	
	/*public int getVariablesCount(){
		if( _variables == null ){
			return 0; 
		} else{
			return _variables.size();
		}
	}
	
	public Iterator<String> getVariablesIterator(){
		if( _variables == null ){
			return null; 
		} else{
			return _variables.iterator();
		}
	}*/
	
	@SuppressLint("DefaultLocale")
	public String toString(){
		return String.format("%s (%s) (avg:%f,%f)", buildExpression(), getBoundingBox().toString(), _avgWidthHeight.x, _avgWidthHeight.y);
	}
	
	public String toShortString(){
		StringBuilder sb = new StringBuilder(); 
		for(Token token : _tokens){
			sb.append(token.asciiChar);			
		}
		
		return sb.toString(); 
	}
	
	@SuppressLint("DefaultLocale")
	public class Token{
		
		public char asciiChar; 
		public RectF boundingBox;
		public CalcPadLabelTypeEnum type; 
		
		public Token(char asciiChar, RectF boundingBox){
			this(asciiChar, boundingBox, CalcPadLabelTypeEnum.Void);
		}
		
		public Token(char asciiChar, RectF boundingBox, CalcPadLabelTypeEnum type){
			this.asciiChar = asciiChar; 
			this.boundingBox = boundingBox;
			this.type = type; 
		}
		
		public String toString(){
			return String.format("%c (%f,%f,%f,%f", asciiChar, boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom);
		}
	}
	
	
	
}
