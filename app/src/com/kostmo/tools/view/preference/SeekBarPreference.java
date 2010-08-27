package com.kostmo.tools.view.preference;

import com.kostmo.tools.view.seekbar.IntegerSeekBarInterface;
import com.kostmo.wallpaper.spiral.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener, IntegerSeekBarInterface {
	
	protected static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

	public static final int DEFAULT_MAX = 100;
	public static final int DEFAULT_VALUE = DEFAULT_MAX/2;
	
	protected SeekBar mSeekBar;

	int mMax;
	protected int mValue = 0;

	// ========================================================================
	public SeekBarPreference(Context context, AttributeSet attrs) { 
		super(context,attrs);
		
		mMax = attrs.getAttributeIntValue(ANDROID_NS, "max", DEFAULT_MAX);
	}

	// ========================================================================
	@Override 
	protected void onBindDialogView(View layout) {
		super.onBindDialogView(layout);
		
		mSeekBar = (SeekBar) layout.findViewById(R.id.seekbar);
		mSeekBar.setMax( getMax() );
		mSeekBar.setProgress(mValue);
		mSeekBar.setOnSeekBarChangeListener(this);
	}
	
	// ========================================================================
	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue) {
		super.onSetInitialValue(restore, defaultValue);
		
		if (restore) 
			mValue = shouldPersist() ? getPersistedInt(DEFAULT_VALUE) : 0;
		else 
			mValue = (Integer)defaultValue;
	}

    // ========================================================================
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInt(index, DEFAULT_VALUE);
	}
	
	// ========================================================================
	@Override
	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {

		if (shouldPersist())
			persistInt(value);
		callChangeListener(new Integer(value));
	}
	
	// ========================================================================
	@Override
	public void onStartTrackingTouch(SeekBar seek) {}
	
	@Override
	public void onStopTrackingTouch(SeekBar seek) {}

	// ========================================================================
	@Override
	public void setMax(int max) { mMax = max; }
	
	@Override
	public int getMax() { return mMax; }

	@Override
	public void setProgress(int progress) { 
		mValue = progress;
		if (mSeekBar != null)
			mSeekBar.setProgress(progress); 
	}
	
	@Override
	public int getProgress() { return mValue; }
}