package com.kostmo.tools.view.seekbar;


import com.kostmo.wallpaper.spiral.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.SeekBar;


public class RangeSeekBar extends SeekBar implements IntegerRangeSeekBarInterface {

	static final String TAG = "SpiralWallpaper"; 
	
	public RangeSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);

		initialize(context, attrs);
	}

    // =================================
	public void initialize(Context context, AttributeSet attrs) {
		
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
        int min = a.getInt(R.styleable.RangeSeekBar_rangeMin, 0);
        int max = a.getInt(R.styleable.RangeSeekBar_rangeMax, getMax());
        int default_value = a.getInt(R.styleable.RangeSeekBar_defaultValue, max);
        a.recycle();

        setRange(min, max);
        setValue(default_value);
	}
	
	int min_value, max_value;
    
    // =================================
    public void setRange(int min, int max) {
    	this.min_value = min;
    	this.max_value = max;
    	
    	setMax(max - min);
    }
    
    // =================================
    public int getValue() {
    	return this.min_value + getProgress();
    }
    
    // =================================
    public void setValue(int value) {
    	setProgress(value - this.min_value);
    }
    
    // =================================
    public int getMinValue() {
    	return this.min_value;
    }
    
    // =================================
    public int getMaxValue() {
    	return this.max_value;
    }
}