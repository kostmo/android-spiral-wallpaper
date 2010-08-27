package com.kostmo.wallpaper.spiral.activity.prefs;

import com.kostmo.wallpaper.spiral.R;
import com.kostmo.wallpaper.spiral.activity.HypnoActivity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PrefsHypnoDisc extends PreferenceActivity {

    public static final String PREFKEY_REVERSE = "reverse";
    public static final String PREFKEY_SPEED = "speed";
    public static final String PREFKEY_ENABLE_CROP = "enable_crop";
    
    public static final String PREFKEY_ENABLE_COUNTER_ROTATOR = "enable_counter_rotator";
    public static final String PREFKEY_ANTIALIASING = "antialiasing";
    
    
    public static final int DEFAULT_TURN_COUNT = 4;
    
	// ========================================================================
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferenceManager().setSharedPreferencesName(HypnoActivity.SHARED_PREFS_NAME_HYPNODISC);
        addPreferencesFromResource(R.xml.hypnodisc_settings);
    }
}
