package com.wmp.utils;

import java.util.Random;

public final class Mathf {
	
	public static float TO_RADIANS = (1 / 180.0f) * (float)Math.PI;
	public static float TO_DEGREES = (1 / (float)Math.PI) * 180;
	
	public static Random rand = new Random(); 	

	/**
	 * converts radians to degrees
	 * @param rad; radians 
	 * @return degrees
	 */
	public static float ToDegress( float rad ){
		return rad * TO_DEGREES;
	}
	
	/**
	 * converts degrees to radians 
	 * @param deg
	 * @return
	 */
	public static float ToRadians( float deg ){
		return deg * TO_RADIANS;
	}
	
	public static float GetRandomFloat(){
    	return rand.nextFloat(); 
    }
    
	public static float GetRandomFloat( float s, float e){
    	return s + ((e-s)*rand.nextFloat());
    }
     
	public static int GetRandomInt( int s, int e){
    	return s + rand.nextInt(e);
    }
	
	public static float Lerp(float value1, float value2, float t){
		return value1 + (value2 - value1) * t; 
	}
	
	public static float Elastic(float value1, float value2, float t, int passes){
		float nt = (float)((1.0f-Math.cos(t * Math.PI * passes)) *
	               (1.0f - t)) + t;
		
		return value1 + (value2 - value1) * nt; 
	}

}
