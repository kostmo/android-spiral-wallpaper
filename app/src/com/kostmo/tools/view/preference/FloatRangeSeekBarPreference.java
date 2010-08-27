package com.kostmo.tools.view.preference;

import com.kostmo.tools.view.seekbar.FloatRangeSeekBar;
import com.kostmo.tools.view.seekbar.FloatRangeSeekBarInterface;
import com.kostmo.wallpaper.spiral.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class FloatRangeSeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener, FloatRangeSeekBarInterface {

	public static final String TAG = "FloatRangeSeekBarPreference";
	
	float initial_min_floatval, initial_max_floatval;
	float parallel_value;

	public static final float DEFAULT_MAX = 1;
	public static final float DEFAULT_MIN = -1;
	public static final float DEFAULT_VALUE = 0;
	
	
	FloatRangeSeekBar mSeekBar;
	TextView value_display;

	// ========================================================================
	void initAttrs(Context context, AttributeSet attrs) {
		// This method has to retrieve the attributes from XML and store them temporarily,
		// then when the Dialog is bound they will be used.
		Log.d(TAG, "Initializing attributes...");
		
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
        initial_min_floatval = a.getFloat(R.styleable.RangeSeekBar_rangeMin, DEFAULT_MIN);
        initial_max_floatval = a.getFloat(R.styleable.RangeSeekBar_rangeMax, DEFAULT_MAX);
        a.recycle();
	}
	
	// ========================================================================
	public FloatRangeSeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAttrs(context, attrs);
	}
	
	// ========================================================================
	public FloatRangeSeekBarPreference(Context context, AttributeSet attrs) { 
		super(context,attrs);
		initAttrs(context, attrs);
	}
	
	// ========================================================================
	@Override 
	protected void onBindDialogView(View layout) {
		super.onBindDialogView(layout);

		value_display = (TextView) layout.findViewById(R.id.value_display);

		mSeekBar = (FloatRangeSeekBar) layout.findViewById(R.id.seekbar);
        mSeekBar.setRange(initial_min_floatval, initial_max_floatval);
        
		mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setValue(parallel_value);
	}
	
	// ========================================================================
	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue) {
		super.onSetInitialValue(restore, defaultValue);
		// The SeekBar widget is not yet instantiated.
		if (restore) 
			parallel_value = shouldPersist() ? getPersistedFloat(DEFAULT_VALUE) : DEFAULT_VALUE;
		else 
			parallel_value = (Float) defaultValue;
	}

    // ========================================================================
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getFloat(index, DEFAULT_VALUE);
	}
	
	// ========================================================================
	@Override
	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {

		// We maintain a parallel instance of the value (that is, in this class
		// in addition to the FloatRangeSeekBar class) because the value must
		// persist in memory if the dialog is closed and later revisited in
		// the same PreferenceActivity session
		parallel_value = ((FloatRangeSeekBar) seek).getValue();
		value_display.setText( String.format("%.1f", parallel_value));
		
		if (shouldPersist())
			persistFloat( parallel_value );
		callChangeListener(new Integer(value));
	}
	
	// ========================================================================
	@Override
	public void onStartTrackingTouch(SeekBar seek) {}
	
	@Override
	public void onStopTrackingTouch(SeekBar seek) {}


    // =================================
	@Override
    public void setRange(float min, float max) {
    	mSeekBar.setRange(min, max);
    }
	
    // =================================
	@Override
    public float getValue() {
    	return mSeekBar.getValue();
    }

    // =================================
	@Override
    public void setValue(float value) {
		mSeekBar.setValue(value);
    }
    
    // =================================
	@Override
    public float getMinValue() {
    	return mSeekBar.getMinValue();
    }
    
    // =================================
	@Override
    public float getMaxValue() {
    	return mSeekBar.getMaxValue();
    }

    // =================================
	@Override
	public void setGranularity(int granules) {
		mSeekBar.setGranularity(granules);
	}
}