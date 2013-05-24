package com.hulzenga.ultrasonicradar;

import java.util.concurrent.ArrayBlockingQueue;

import com.hulzenga.ultrasonicradar.util.Converter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class RadarView extends View{
	
	public static final String TAG = "RADAR_VIEW";
	public static final float SENSOR_X_DISTANCE = 3.75f;
	public static final float SENSOR_Y_DISTANCE = 0.50f;
	
	private boolean lineDrawingMode = false;
	
	private float mWidth = 0.0f;
	private float mHeight = 0.0f;
	private float mCmToPix = 0.0f;
	private Paint mGridLinesPaint;
	private Paint mGridTextPaint;
	private Paint mMeasurementPaint;
	private Paint mSweepPaint;
	private Paint mSweepRangePaint;
	private Paint mLinePaint;
	private int sweepAngle = 90;
	
	private float cx;
	private float cy;
	
	private int mArraySize = 255;
	private float[] mMeasurementPointX = new float[mArraySize];
	private float[] mMeasurementPointY = new float[mArraySize];
	private int mMeasurementPointIndex = 0;	

	private int  mMaxDistance = 200;
	private int mNumberOfScanLines = 10;
	private int mScanLineArraySize = mNumberOfScanLines*4;
	private float[] mScanlineX = new float[mScanLineArraySize];
	private float[] mScanlineY = new float[mScanLineArraySize];
	private int mScanlineIndex = 0;
	
	public RadarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		loadPaints();		
	}
	
	private void loadPaints() {		
		mGridLinesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGridLinesPaint.setStyle(Style.STROKE);
		mGridLinesPaint.setStrokeWidth(2.0f);
		mGridLinesPaint.setColor(Color.GRAY);
		mGridLinesPaint.setAlpha(127);
		
		mGridTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mGridTextPaint.setColor(Color.GREEN);		
		mGridTextPaint.setTextSize(10.0f);
		
		mMeasurementPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mMeasurementPaint.setStyle(Style.STROKE);
		mMeasurementPaint.setColor(Color.RED);
		mMeasurementPaint.setStrokeWidth(4.0f);
		
		mSweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mSweepPaint.setStyle(Style.STROKE);
		mSweepPaint.setColor(Color.GREEN);
		mSweepPaint.setStrokeWidth(4.0f);
		
		mSweepRangePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mSweepRangePaint.setStyle(Style.STROKE);
		mSweepRangePaint.setColor(Color.YELLOW);
		mSweepRangePaint.setStrokeWidth(8.0f);
		mSweepRangePaint.setAlpha(150);
		
		mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLinePaint.setStyle(Style.STROKE);
		mLinePaint.setColor(Color.RED);
		mLinePaint.setStrokeWidth(1.0f);
		mLinePaint.setAlpha(127);
	}
	
	@Override
	//TODO: simplify these calculations
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
				
		canvas.drawCircle(cx, cy, mHeight/2.5f, mGridLinesPaint);
		canvas.drawCircle(cx, cy, mHeight*2.0f/7.5f, mGridLinesPaint);
		canvas.drawCircle(cx, cy, mHeight*1.0f/7.5f, mGridLinesPaint);
		
		for (double theta = 0.0; theta < 2.0*Math.PI; theta += (30.0/180.0)*Math.PI) {
			canvas.drawLine(cx, mHeight/2, 
					cx - mHeight/2.0f*(float)Math.cos(theta), 
					mHeight/2.0f - mHeight/2.0f*(float)Math.sin(theta), 
					mGridLinesPaint);
		}
		
		double c = (sweepAngle + 45) * (Math.PI / 180) ;
				
		canvas.drawLine(cx + dx(c, (int) mMaxDistance)-4, cy + dy(c, (int) mMaxDistance)-4, 
				cx + dx(c, (int) mMaxDistance)+4, cy + dy(c, (int) mMaxDistance)+4, mSweepRangePaint);
		canvas.drawLine(cx + dx(-c, (int) mMaxDistance)-4, cy + dy(-c, (int) mMaxDistance)-4, 
				cx + dx(-c, (int) mMaxDistance)+4, cy + dy(-c, (int) mMaxDistance)+4, mSweepRangePaint);
		
		if (sweepAngle < 45) {
			double d = (45 - sweepAngle) * (Math.PI / 180);
			canvas.drawLine(cx + dx(d, (int) mMaxDistance)-4, cy + dy(d, (int) mMaxDistance)-4, 
					cx + dx(d, (int) mMaxDistance)+4, cy + dy(d, (int) mMaxDistance)+4, mSweepRangePaint);
			canvas.drawLine(cx + dx(-d, (int) mMaxDistance)-4, cy + dy(-d, (int) mMaxDistance)-4, 
					cx + dx(-d, (int) mMaxDistance)+4, cy + dy(-d, (int) mMaxDistance)+4, mSweepRangePaint);
		}
		
		canvas.drawText(String.valueOf(mMaxDistance) + "+", cx - mHeight/2.0f - 10, cy + 5, mGridTextPaint);
		canvas.drawText(String.valueOf(3*mMaxDistance/4), cx - mHeight/2.5f - 10, cy + 5, mGridTextPaint);
		canvas.drawText(String.valueOf(2*mMaxDistance/4), cx - mHeight*2.0f/7.5f - 10, cy + 5, mGridTextPaint);
		canvas.drawText(String.valueOf(1*mMaxDistance/4), cx - mHeight*1.0f/7.5f - 10, cy + 5, mGridTextPaint);
		canvas.drawText(String.valueOf(1*mMaxDistance/4), cx + mHeight*1.0f/7.5f - 10, cy + 5, mGridTextPaint);
		canvas.drawText(String.valueOf(2*mMaxDistance/4), cx + mHeight*2.0f/7.5f - 10, cy + 5, mGridTextPaint);
		canvas.drawText(String.valueOf(3*mMaxDistance/4), cx + mHeight/2.5f - 10, cy + 5, mGridTextPaint);
		canvas.drawText(String.valueOf(mMaxDistance) + "+", cx + mHeight/2.0f - 10, cy + 5, mGridTextPaint);
		
		mSweepPaint.setAlpha(80);
		for (int i = 0, j = ((mScanlineIndex+mScanLineArraySize-4)%mScanLineArraySize); i < mNumberOfScanLines; i++, j = (j+mScanLineArraySize-4)%mScanLineArraySize) {
			canvas.drawLine(mScanlineX[j+0], mScanlineY[j+0], mScanlineX[j+1], mScanlineY[j+1], mSweepPaint);
			canvas.drawLine(mScanlineX[j+2], mScanlineY[j+2], mScanlineX[j+3], mScanlineY[j+3], mSweepPaint);
			mSweepPaint.setAlpha(80-i*((80 - 1)/mNumberOfScanLines));
		}
		
		//this is really convoluted and there should be a better way to do this
		boolean first = true;
		boolean second = true;
		int lastJ = 0;
		int secondLastJ = 0;
		mMeasurementPaint.setAlpha(256);
		for (int i = 0, j = ((mMeasurementPointIndex+mArraySize-1)%mArraySize); i < mArraySize; i++, j = ((j+mArraySize-1)%mArraySize)) {
			canvas.drawPoint(mMeasurementPointX[j], mMeasurementPointY[j], mMeasurementPaint);
			mMeasurementPaint.setAlpha(256 - (int) (i * (256.0f/mArraySize)));
		}
	}
	
	public void addMeasurement(int angle, int rDistance, int lDistance) {
		double theta = angle*Math.PI/1024.5;
		double sensorAngle = 0.0;
		
		float xOffset = 0.0f;
		float yOffset = 0.0f;
		
		sensorAngle = theta + Math.PI/4.0;
		xOffset = mCmToPix * (SENSOR_X_DISTANCE*((float) Math.cos(theta)) - SENSOR_Y_DISTANCE*((float) Math.sin(theta)));
		yOffset = mCmToPix * (SENSOR_X_DISTANCE*((float) Math.sin(theta)) + SENSOR_Y_DISTANCE*((float) Math.cos(theta)));
		if (rDistance != 0) {
			mMeasurementPointX[mMeasurementPointIndex] = cx + dx(sensorAngle, rDistance) + xOffset;
			mMeasurementPointY[mMeasurementPointIndex] = cy + dy(sensorAngle, rDistance) + yOffset;			
		} else {
			mMeasurementPointX[mMeasurementPointIndex] = cx + dx(sensorAngle, (int) mMaxDistance);
			mMeasurementPointY[mMeasurementPointIndex] = cy + dy(sensorAngle, (int) mMaxDistance);
		}
		mMeasurementPointIndex = (mMeasurementPointIndex + 1) % mArraySize;
		
		mScanlineX[mScanlineIndex] = cx + xOffset;
		mScanlineY[mScanlineIndex] = cy + yOffset;
		mScanlineIndex++;
		mScanlineX[mScanlineIndex] = cx + dx(sensorAngle, (int) mMaxDistance);
		mScanlineY[mScanlineIndex] = cy + dy(sensorAngle, (int) mMaxDistance);
		mScanlineIndex++;		
		
		sensorAngle = theta - Math.PI/4.0;
		xOffset = mCmToPix * (-SENSOR_X_DISTANCE*((float) Math.cos(theta)) + SENSOR_Y_DISTANCE*((float) Math.sin(theta)));
		yOffset = mCmToPix * (-SENSOR_X_DISTANCE*((float) Math.sin(theta)) - SENSOR_Y_DISTANCE*((float) Math.cos(theta)));
		if (lDistance != 0) {			
			mMeasurementPointX[mMeasurementPointIndex] = cx + dx(sensorAngle, lDistance) + xOffset;
			mMeasurementPointY[mMeasurementPointIndex] = cy + dy(sensorAngle, lDistance) + yOffset;			
		} else {
			mMeasurementPointX[mMeasurementPointIndex] = cx + dx(sensorAngle, (int) mMaxDistance);
			mMeasurementPointY[mMeasurementPointIndex] = cy + dy(sensorAngle, (int) mMaxDistance);
		}
		mMeasurementPointIndex = (mMeasurementPointIndex + 1) % mArraySize;
		
		mScanlineX[mScanlineIndex] = cx + xOffset;
		mScanlineY[mScanlineIndex] = cy + yOffset;
		mScanlineIndex++;
		mScanlineX[mScanlineIndex] = cx + dx(sensorAngle, (int) mMaxDistance);
		mScanlineY[mScanlineIndex] = cy + dy(sensorAngle, (int) mMaxDistance);
		mScanlineIndex = (mScanlineIndex + 1) % mScanLineArraySize;	
		
		this.postInvalidate();		
	}
	
	public void setSweepAngle(int angle) {
		sweepAngle = angle;
		this.postInvalidate();
	}
	
	private float dx(double angle, int distance) {
		return (float) (mCmToPix*distance*Math.sin(angle));
	}
	
	private float dy(double angle, int distance) {
		return (float) (-mCmToPix*distance*Math.cos(angle));
	}
	
	public void adjustBuffer(int sweep, int stepInterval) {
		int stepsInSweep = 8 * Converter.degreeToAngle(sweep);
		mArraySize = stepsInSweep / stepInterval;
		flush();
	}

	public void flush() {
		mMeasurementPointX = new float[mArraySize];
		mMeasurementPointY = new float[mArraySize];
		mMeasurementPointIndex = 0;	
		
		mScanlineX = new float[mScanLineArraySize];
		mScanlineY = new float[mScanLineArraySize];
		mScanlineIndex = 0;
		this.postInvalidate();
	}
	
	public void setMaxDistance(int distance) {
		mMaxDistance = distance;
		mCmToPix = (mHeight/2.0f)/mMaxDistance;
		flush();
		this.postInvalidate();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {		
		super.onSizeChanged(w, h, oldw, oldh);
		mWidth = (float) w;
		mHeight = (float) h;
		cx = mWidth / 2.0f;
		cy = mHeight / 2.0f;
		mCmToPix = (mHeight/2.0f)/mMaxDistance;
	}	
}
