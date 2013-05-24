package com.hulzenga.ultrasonicradar.util;

public class Converter {

	private static final float STEPS_PER_ROTATION = 2048.0f;
		
	/*
	 * degree := angle in degrees from 0 to 360
	 * angle  := angle defined in stepper motor steps
	 * rads   := angle defined in radians
	 */
	
	public static int angleToDegree(int angle) {
		return (int) (360 * (angle/STEPS_PER_ROTATION));
	}
	
	public static int degreeToAngle(int degree) {
		return (int) (STEPS_PER_ROTATION * (degree/360.0f));
	}
}
