package com.hulzenga.ultrasonicradar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class IntensityView extends View{

	private final String TAG = "INTENSITY_VIEW";
	
	private float mWidth = 0.0f;	
	private float mHeight = 0.0f;
	private float mScaleX = 0.0f;
	private float mScaleY = 0.0f;

	//TODO: change to sweep dependent buffer size
	private final int INTENSITY_BUFFER_SIZE = 50; 
	private LinkedList<Float> mIntensityBuffer = new LinkedList<Float>();
	private ArrayList<Integer> mTriggerBuffer = new ArrayList<Integer>();
	
	private Paint mLinePaint;
	private Paint mLevelPaint;
	private Paint mTriggerPaint;
	
	private float mMaxIntensity = 10.0f;
	private float mTriggerLevel = 0.0f;
	
	public IntensityView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLinePaint.setColor(Color.RED);
		mLinePaint.setStyle(Style.STROKE);
		mLinePaint.setStrokeWidth(3.0f);
		
		mLevelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLevelPaint.setColor(Color.GRAY);
		mLevelPaint.setStyle(Style.STROKE);
		mLevelPaint.setStrokeWidth(2.0f);
		mLevelPaint.setAlpha(127);
		
		mTriggerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTriggerPaint.setColor(Color.YELLOW);
		mTriggerPaint.setStyle(Style.STROKE);
		mTriggerPaint.setStrokeWidth(4.0f);
		mTriggerPaint.setAlpha(63);
		
	}

	@Override
	protected void onDraw(Canvas canvas) {
				
		Iterator<Float> iter = mIntensityBuffer.iterator();
		if (iter.hasNext()) {
			Float next, last = iter.next();
			int i = 0;
			while(iter.hasNext()) {
				next = iter.next();
				canvas.drawLine(i*mScaleX, mHeight - last*mScaleY, (i+1)*mScaleX, mHeight - next*mScaleY, mLinePaint);				
				
				i++;
				last = next;
			}
		}
		
		//draw trigger level
		canvas.drawLine(0.0f, mHeight - mTriggerLevel*mScaleY, mWidth, mHeight - mTriggerLevel*mScaleY, mLevelPaint);
		
		//There should be a more elegant way to do this
		if (mTriggerBuffer.size() > 0) {
			
			for (Integer triggerPoint: mTriggerBuffer) {
				canvas.drawLine(triggerPoint*mScaleX, 0.0f, triggerPoint*mScaleX, mHeight, mTriggerPaint);
			}
			for (int i = 0; i < mTriggerBuffer.size(); i++) {
				mTriggerBuffer.set(i, mTriggerBuffer.get(i)-1);
			}
			if (mTriggerBuffer.get(0) < 0) {
				mTriggerBuffer.remove(0);
			}	
		}
			
	}
	
	public void addIntensity(float intensity) {
		if (mIntensityBuffer.size() >= INTENSITY_BUFFER_SIZE) {
			mIntensityBuffer.removeFirst();
		} 
		mIntensityBuffer.add(intensity);
		this.postInvalidate();
	}
	
	public void setLevel(float level) {
		mTriggerLevel = level;
		mMaxIntensity = mTriggerLevel *1.35f;
		mScaleY = (mHeight/mMaxIntensity);
	}
	
	public void addTriggerPoint() {
		mTriggerBuffer.add(INTENSITY_BUFFER_SIZE - 1);
	}
	
	public void flush() {
		mIntensityBuffer = new LinkedList<Float>();
		mTriggerBuffer = new ArrayList<Integer>();
		this.postInvalidate();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {		
		super.onSizeChanged(w, h, oldw, oldh);
		mWidth = (float) w;
		mScaleX = (mWidth/INTENSITY_BUFFER_SIZE);
		mHeight = (float) h;	
		mScaleY = (mHeight/mMaxIntensity);
	}
}
