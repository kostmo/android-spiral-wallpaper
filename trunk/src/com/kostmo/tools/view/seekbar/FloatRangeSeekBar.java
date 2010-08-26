package com.kostmo.tools.view.seekbar;


import com.kostmo.wallpaper.spiral.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class FloatRangeSeekBar extends SeekBar implements FloatRangeSeekBarInterface {

	static final String TAG = "SpiralWallpaper"; 
	
	private int granularity = 100;
	
	

	float min_value, max_value;

    // =================================
	public FloatRangeSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);

		initialize(context, attrs);
	}

    // =================================
	public void initialize(Context context, AttributeSet attrs) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
        float min = a.getFloat(R.styleable.RangeSeekBar_rangeMin, 0);
        float max = a.getFloat(R.styleable.RangeSeekBar_rangeMax, getMax());
        float default_value = a.getFloat(R.styleable.RangeSeekBar_defaultValue, max);
        a.recycle();

        setRange(min, max);
        setValue(default_value);
	}
    
    // =================================
	@Override
    public void setRange(float min, float max) {
    	this.min_value = min;
    	this.max_value = max;
    	
    	setMax( (int) ((max - min) * this.granularity) );
    }
    
    // =================================
	@Override
    public float getValue() {
    	return getProgress() / (float) this.granularity + this.min_value;
    }

    // =================================
	@Override
    public void setValue(float value) {
    	setProgress( (int) ((value - this.min_value) * this.granularity) );
    }
    
    // =================================
	@Override
    public float getMinValue() {
    	return this.min_value;
    }
    
    // =================================
	@Override
    public float getMaxValue() {
    	return this.max_value;
    }

    // =================================
	@Override
	public void setGranularity(int granules) {
		this.granularity = granules;
	}
}