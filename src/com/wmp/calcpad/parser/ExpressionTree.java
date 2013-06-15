package com.wmp.calcpad.parser;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.Vector;

import android.util.Log;

import com.wmp.calcpad.parser.exception.VariableNotFoundException;

public class ExpressionTree {
	
	public enum ExpressionTreeStatusEnum{
		OK, 
		Variable_Required
	}

	public class TreeNode{
		public boolean leafNode; 
		public TreeNode leftNode; 
		public TreeNode rightNode; 
		public float value; 
		public String token; 
		
		public TreeNode( float value, String token, boolean leafNode){
			this.value = value; 
			this.token = token; 
			this.leafNode = leafNode; 
		}
		
		public String toString(){
			return String.format("TreeNode %f %s", value, token );
		}
		
		public String safeToken(){
			return token == null ? "" : token; 
		}
	}
	
	private TreeNode _root; 
	
	private String _expression;
	
	private ExpressionTreeStatusEnum _status = ExpressionTreeStatusEnum.OK;
	
	private boolean _isVariable = false; 
	
	private String _variableTokenToCheck = null; 
	
	private int _tokenCount = 0; 
	
	public ExpressionTree(String expression){
		_expression = expression;		
	}
	
	public void build(List<CalcPadVariable> variables) throws VariableNotFoundException {		
		Scanner scanner = new Scanner(_expression);
		_root = rebuild(scanner, variables);
		
		if( _variableTokenToCheck != null ){
			boolean missingVariable = false; 
			
			// check that the root is == '=' and the left is == _variableTokenToCheck
			if( !_root.safeToken().equals("=") ){
				missingVariable = true; 
			}
			if( _root.leftNode == null || !_root.leftNode.safeToken().equals(_variableTokenToCheck) ){
				missingVariable = true; 				
			}
			
			if( !missingVariable ){
				_isVariable = true; 
			} else{
				if( variables != null ){
					for( CalcPadVariable variable : variables ){
						if( variable.label.equals(_variableTokenToCheck) ){														
							// find tree node with token = _variableTokenToCheck
							TreeNode node = searchTreeNodeForToken(_root, _variableTokenToCheck);
							
							if( node != null ){
								node.value = variable.value;
								missingVariable = false;
							}
						}
					}
				}
				
				if( missingVariable ){
					throw new VariableNotFoundException(_variableTokenToCheck);
				}
			}
		}
	}
	
	private TreeNode rebuild(Scanner scanner, List<CalcPadVariable> variables) throws VariableNotFoundException {						
		_isVariable = false; 
		_tokenCount = 0;
		_variableTokenToCheck = null; 
		 
		String nextToken; 
		TreeNode treeNode = null; 
		
		Stack<TreeNode> _nodeStack = new Stack<ExpressionTree.TreeNode>();
		
		while( scanner.hasNext() ){
			_tokenCount++; 
			nextToken = scanner.next(); 
			
			Log.i( "Adding token", "adding token " + nextToken );
			
			if( isLeaf( nextToken ) ){
				treeNode = new TreeNode(getValue(nextToken, variables), nextToken, true);
				_nodeStack.push( treeNode ); 
			} else{			 
				treeNode = new TreeNode(0.0f, nextToken, false);
				//treeNode.leftNode = build( scanner );
				//treeNode.rightNode = build( scanner );
				if( _nodeStack.size() > 0 ){
					treeNode.rightNode = _nodeStack.pop();
				}
				if( _nodeStack.size() > 0 ){
					treeNode.leftNode = _nodeStack.pop();
				}
				_nodeStack.push( treeNode );
			}
		}				
		
		return _nodeStack.pop(); 
	}	
	
	public TreeNode searchTreeNodeForToken( TreeNode token, String searchToken ){
		if( token.safeToken().equals(searchToken) ){
			return token; 
		}
		
		if( token.leftNode != null ){
			TreeNode foundnode = searchTreeNodeForToken(token.leftNode, searchToken);
			if( foundnode != null ){
				return foundnode; 
			}
		}
		
		if( token.rightNode != null ){
			TreeNode foundnode = searchTreeNodeForToken(token.rightNode, searchToken);
			if( foundnode != null ){
				return foundnode; 
			}
		}
		
		return null; 
	}
	
	/**
	 * Evaluate the expression and return its value.
	 * All the work is done in the private recursive method.
	 * @return  the value of the expression tree.
	 */
	public float evaluate (){  
		return _root == null ? 0.0f : evaluate ( _root ) ;  
	}
	
	private float evaluate( TreeNode node ){
		if( node == null ){
			return 0.0f; 
		}
		
		float result = 0.0f; 
		
		if( node.leafNode ){
			result = node.value; 			
		} else{
			float leftValue = 0.0f; 
			float rightValue = 0.0f; 
			String token = node.token; 
			
			leftValue = evaluate( node.leftNode );
			rightValue = evaluate( node.rightNode ); 		
			
			if( token.equals("=") ){
				if( isVariable() ){
					result = rightValue; 
				} else{
					if( node.leftNode == null ){
						result = rightValue; 
					} else{
						result = (leftValue == rightValue ? 1.0f : -1.0f); // boolean operation
					}
				}
			} else if( token.equals("+") ){
				result = leftValue + rightValue;  
			} else if( token.equals("-") ){
				result = leftValue - rightValue;  
			} else if( token.equals("/") ){
				result = leftValue / rightValue; 
			} else if( token.equals("*") ){
				result = leftValue * rightValue; 
			} else if( token.equals("^") ){
				result = (float)Math.pow(leftValue, rightValue); 
			} else if( token.equals("cos") ){
				 
			} else if( token.equals("sin") ){
				 
			} else{
				 
			}
			
		}
		
		return result; 
	}
	
	public boolean isVariable(){
		return _isVariable && _root.leftNode != null; 
	}
	
	public String getVariableName(){
		if( isVariable() ){
			return _root.leftNode.token; 
		} else{
			return null; 
		}
	}
	
	private boolean isLeaf( String token ){
		if( token.equals("=") ){
			return false; 
		} else if( token.equals("+") ){
			return false; 
		} else if( token.equals("-") ){
			return false; 
		} else if( token.equals("/") ){
			return false; 
		} else if( token.equals("*") ){
			return false; 
		} else if( token.equals("^") ){
			return false; 
		} else if( token.equals("cos") ){
			return false; 
		} else if( token.equals("sin") ){
			return false; 
		} else{
			return true; 
		}
	}
	
	private float getValue( String token, List<CalcPadVariable> variables ) throws VariableNotFoundException{
		try{
			float value = Float.parseFloat(token);
			return value; 
		} catch(NumberFormatException e ){
			
		}	
		
		try{
			float value = getConstant(token);
			return value; 
		} catch( NumberFormatException e ){
			
		}
		
		if( _tokenCount == 1 ){
			_variableTokenToCheck = token; 
			return 0.0f; 
		} else{
			if( variables != null ){
				for( CalcPadVariable variable : variables ){
					if( variable.label.equals(token)){
						return variable.value;
					}
				}
			}
			
			throw new VariableNotFoundException(token);
			
		}
	}	
	
	private float getConstant( String token ) throws NumberFormatException{
		if( token.equals( "PI" ) || token.equals( "pi" ) ){
			return (float)Math.PI;
		} else{
			throw new NumberFormatException("Constant not found"); 
		}
	}					
	
	public ExpressionTreeStatusEnum getStatus(){
		return _status; 
	}
}
