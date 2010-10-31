package com.kostmo.wallpaper.spiral.activity.prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.View;

import com.kostmo.tools.view.preference.ColorPreference;
import com.kostmo.tools.view.preference.ColorReceiverActivity;
import com.kostmo.tools.view.preference.SeekBarPreference;
import com.kostmo.wallpaper.spiral.Market;
import com.kostmo.wallpaper.spiral.R;
import com.kostmo.wallpaper.spiral.SpiralWallpaper;

public class SpiralWallpaperSettings extends PreferenceActivity implements ColorReceiverActivity {

	
    static final String TAG = "SpiralWallpaperSettings";
    
	
	
    public static final String PREFKEY_COLOR_PRIMARY = "color_primary";
    public static final String PREFKEY_COLOR_SECONDARY = "color_secondary";
    public static final String PREFKEY_COLOR_BACKGROUND = "color_background";
    
    public static final String PREFKEY_SPIRAL_TYPE = "spiral_type";
    public static final String PREFKEY_SOLID_TYPE = "solid_type";
    public static final String PREFKEY_PITCH = "pitch";
    public static final String PREFKEY_TURN_COUNT = "turn_count";
    
    public static final String PREFKEY_ANTIALIASING = "antialiasing";
    public static final String PREFKEY_REVERSE = "reverse";
    public static final String PREFKEY_SPEED = "speed";
    
    public static final String PREFKEY_GENERATE_RANDOM_COLORS = "generate_random_colors";
    
    public static final String PREFKEY_ENABLE_COLOR_CYCLING = "enable_color_cycling";
    public static final String PREFKEY_ENABLE_CONSTANT_COLOR_CYCLING = "enable_constant_color_cycling";
    public static final String PREFKEY_COLOR_CYCLER_MINUTES = "color_cycler_minutes";
    
    public static final String PREFKEY_ENABLE_FIXED_BRIGHTNESS = "enable_fixed_brightness";
    public static final String PREFKEY_ENABLE_FIXED_SATURATION = "enable_fixed_saturation";
    
    public static final String PREFKEY_FIXED_BRIGHTNESS_LEVEL = "fixed_brightness_level";
    public static final String PREFKEY_FIXED_SATURATION_LEVEL = "fixed_saturation_level";
    
    // ============
    
    
    public static final String BUNDLEKEY_PENDING_COLOR_PREFKEY = "pending_color_prefkey";
    
    // Defaults:
    public static final int DEFAULT_COLOR_CYCLER_MINUTES = 1;
    public static final float DEFAULT_PITCH = -0.3f;
    public static final int DEFAULT_TURN_COUNT = 4;
    public static final boolean DEFAULT_ENABLE_COLOR_CYCLING = true;
    

	private static final int DIALOG_COLORPICKER_DOWNLOAD = 2;
    
    
    public static final String[] RANDOMIZABLE_COLOR_PREFKEYS = {PREFKEY_COLOR_PRIMARY, PREFKEY_COLOR_SECONDARY};

    private static final int REQUEST_CODE_PICK_COLOR = 1;
    
    private String pending_color_prefkey;
    
	// ========================================================================
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferenceManager().setSharedPreferencesName(SpiralWallpaper.SHARED_PREFS_NAME);
        
        addPreferencesFromResource(R.xml.spiral_settings);

        
        final String[] dependent_prefkeys = new String[] {
        		PREFKEY_PITCH, PREFKEY_SOLID_TYPE, PREFKEY_COLOR_SECONDARY};
        
        Preference spiral_type_control = findPreference(PREFKEY_SPIRAL_TYPE);
        spiral_type_control.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int spiral_type_index = Integer.parseInt((String) newValue);
				
				for (String prefkey : dependent_prefkeys) {
					Preference pref = findPreference(prefkey);
					pref.setEnabled(spiral_type_index != 0);
				}
				return true;
			}
        });

        // Trigger our custom-made dependency handler to disable the items
        final SharedPreferences prefs = getSharedPreferences(SpiralWallpaper.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String pref_value = prefs.getString(spiral_type_control.getKey(), "0");
        spiral_type_control.getOnPreferenceChangeListener().onPreferenceChange(spiral_type_control, pref_value);
        
                
		findPreference(PREFKEY_GENERATE_RANDOM_COLORS).setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				
				assignRandomSpiralColors(prefs);
				for (String prefkey : RANDOMIZABLE_COLOR_PREFKEYS)
					((ColorPreference) findPreference(prefkey)).update();

				return true;
			}
		});
		
		final StateObject state = (StateObject) getLastNonConfigurationInstance();
		if (state != null) {
			this.pending_color_prefkey = state.pending_color_prefkey;
		} else {
			if (icicle.containsKey( BUNDLEKEY_PENDING_COLOR_PREFKEY )) {
				this.pending_color_prefkey = icicle.getString( BUNDLEKEY_PENDING_COLOR_PREFKEY );
			}
		}
    }

	// ========================================================================
    @Override
	protected
    void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	Log.d(TAG, "Called onSaveInstanceState()");
    	
    	outState.putString(BUNDLEKEY_PENDING_COLOR_PREFKEY, this.pending_color_prefkey);
    }
    
	// ========================================================================
	class StateObject {
		String pending_color_prefkey;
	}

	// ========================================================================
	@Override
	public Object onRetainNonConfigurationInstance() {

		StateObject state = new StateObject();
		state.pending_color_prefkey = this.pending_color_prefkey;
		return state;
	}
    
	// ========================================================================
    public static List<Integer> populateNewRandomColors(SharedPreferences prefs, int count) {

    	
    	float fixed_brightness_level = 0, fixed_saturation_level = 0;

    	boolean fixed_brightness = prefs.getBoolean(PREFKEY_ENABLE_FIXED_BRIGHTNESS, true);
    	if (fixed_brightness) {
//    		fixed_brightness_level = prefs.getFloat(PREFKEY_FIXED_BRIGHTNESS_LEVEL, 0.5f);
    		fixed_brightness_level = prefs.getInt(PREFKEY_FIXED_BRIGHTNESS_LEVEL, 50) / (float) SeekBarPreference.DEFAULT_MAX;
    	}

    	boolean fixed_saturation = prefs.getBoolean(PREFKEY_ENABLE_FIXED_SATURATION, true);
    	if (fixed_saturation) {
//    		fixed_saturation_level = prefs.getFloat(PREFKEY_FIXED_SATURATION_LEVEL, 0.5f);
    		fixed_saturation_level = prefs.getInt(PREFKEY_FIXED_SATURATION_LEVEL, 50) / (float) SeekBarPreference.DEFAULT_MAX;
    	}
    	
    	List<Integer> colors = new ArrayList<Integer>();

		Random random = new Random();
    	for (int i=0; i<count; i++) {
			int color = Color.HSVToColor(
					new float[] {
							360*random.nextFloat(),
							fixed_saturation ? fixed_saturation_level : random.nextFloat(),
							fixed_brightness ? fixed_brightness_level : random.nextFloat()
					}
			);
			
			colors.add(color);
    	}
    	
    	return colors;
    }

    // =============================================
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {

        switch (id) {
		case DIALOG_COLORPICKER_DOWNLOAD:
		{
			boolean has_android_market = Market.isIntentAvailable(this,
					Market.getMarketDownloadIntent(Market.PACKAGE_NAME_COLOR_PICKER));

			Log.d("Color Picker", "has_android_market? " + has_android_market);
			
			dialog.findViewById(android.R.id.button1).setVisibility(
					has_android_market ? View.VISIBLE : View.GONE);
			break;
		}
        default:
        	break;
        }
    }
    
	// ========================================================
	@Override
	protected Dialog onCreateDialog(int id) {
        
		switch (id) {
		case DIALOG_COLORPICKER_DOWNLOAD:
		{
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.download_color_picker)
			.setMessage(R.string.color_picker_modularization_explanation)
			.setPositiveButton(R.string.download_color_picker_market, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(Market.getMarketDownloadIntent(Market.PACKAGE_NAME_COLOR_PICKER));
				}
			})
			.setNeutralButton(R.string.download_color_picker_web, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					startActivity(new Intent(Intent.ACTION_VIEW, Market.APK_DOWNLOAD_URI_COLOR_PICKER));
				}
			})
			.create();
		}
		}
		return null;
	}
	
	// ========================================================================
    public static void assignRandomSpiralColors(SharedPreferences prefs) {

    	List<Integer> colors = populateNewRandomColors(prefs, 2);

		Editor prefs_edit = prefs.edit();
		int i=0;
		for (String prefkey : RANDOMIZABLE_COLOR_PREFKEYS) {
			prefs_edit.putInt(prefkey, colors.get(i));
			i++;
		}
		prefs_edit.commit();
    }
    
	// ========================================================================
	public void pickColor(int passed_color, Preference preference) {
		this.pending_color_prefkey = preference.getKey();
		
		Intent i = new Intent();
		i.setAction(Market.ACTION_PICK_COLOR);
		i.putExtra(Market.EXTRA_COLOR, passed_color);

		if (Market.isIntentAvailable(this, i)) {
			startActivityForResult(i, REQUEST_CODE_PICK_COLOR);
		} else {
			showDialog(DIALOG_COLORPICKER_DOWNLOAD);
		}
	}

	
	// ========================================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_CANCELED) {
	  	   	switch (requestCode) {
			case REQUEST_CODE_PICK_COLOR:
			{
				if (resultCode == RESULT_OK) {
					int color = data.getIntExtra(Market.EXTRA_COLOR, ColorPreference.FALLBACK_PRIMARY_COLOR);

					Preference p = findPreference(this.pending_color_prefkey);

					SharedPreferences prefs = getSharedPreferences(SpiralWallpaper.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
					prefs.edit().putInt(p.getKey(), color).commit();
					((ColorPreference) p).update();
				}
				break;
			}
	   		default:
		    	break;
		   }
		}
    }
}
