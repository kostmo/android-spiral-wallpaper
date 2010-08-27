package com.kostmo.tools.view.preference;

import com.kostmo.tools.view.SwatchView;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ColorPreference extends Preference {

	public static final String TAG = "ColorPreference";

	public static final int FALLBACK_PRIMARY_COLOR = 0xFF7f7fe6;
	public static final int FALLBACK_SECONDARY_COLOR = 0xFF7fe67f;
	public static final int FALLBACK_BACKGROUND_COLOR = 0xFF000000;

	ColorReceiverActivity receiver_activity;

    // ========================================================================
	public ColorPreference(Context context) {
		super(context);
		init(context);
	}
	
	public ColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

    // ========================================================================
	void init(Context context) {
		this.receiver_activity = (ColorReceiverActivity) context;
	}

    // ========================================================================
	@Override
	protected void onClick() {
		int color = getPersistedInt(FALLBACK_PRIMARY_COLOR);
		this.receiver_activity.pickColor(color, this);
	}
	
    // ========================================================================
    public static class ViewHolderGuess {
    	public TextView guess_holder;
    }

    // ========================================================================
	@Override
	public View getView(View convertView, ViewGroup parent) {
		View v = super.getView(convertView, parent);
		
//		ImageView image_view = (ImageView) v.findViewById(android.R.id.icon);
		SwatchView swatch_view = (SwatchView) v.findViewById(android.R.id.icon);

		int color = getPersistedInt(FALLBACK_PRIMARY_COLOR);
		swatch_view.setColor(color);
		
		/*
		PaintDrawable p = new PaintDrawable( color );
        p.setIntrinsicHeight(32);
        p.setIntrinsicWidth(32);
        p.setCornerRadius(6);
		image_view.setImageDrawable( p );
        */

		return v;
	}

    // ========================================================================
	public void update() {
		notifyChanged();
	}

    // ========================================================================
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getColor(index, FALLBACK_PRIMARY_COLOR);
	}

    // ========================================================================
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		persistInt(restoreValue ? getPersistedInt(FALLBACK_PRIMARY_COLOR) : (Integer) defaultValue);
	}
} 