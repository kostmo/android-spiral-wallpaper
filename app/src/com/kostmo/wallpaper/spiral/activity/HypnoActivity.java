package com.kostmo.wallpaper.spiral.activity;

import com.kostmo.wallpaper.spiral.Market;
import com.kostmo.wallpaper.spiral.R;
import com.kostmo.wallpaper.spiral.activity.prefs.PrefsHypnoDisc;
import com.kostmo.wallpaper.spiral.view.HypnoView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class HypnoActivity extends Activity {

	static final String TAG = "HypnoActivity";

    public static final String SHARED_PREFS_NAME_HYPNODISC = "hypnodisc_settings";
	
	public static final String PREFKEY_SHOW_MAIN_INSTRUCTIONS = "PREFKEY_SHOW_MAIN_INSTRUCTIONS";
	private static final int DIALOG_INSTRUCTIONS = 1;

	final static int[] MONET_PAINTINGS = {R.drawable.monet0};
	
	SharedPreferences settings;
	PowerManager.WakeLock wl;

	HypnoView hypno_view;
	ImageView image_view;

	int painting_index = 0;
	boolean painting_visible = false;

    // ========================================================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hypno_activity);

		this.settings = getSharedPreferences(SHARED_PREFS_NAME_HYPNODISC, Context.MODE_PRIVATE);

		
		this.image_view = (ImageView) findViewById(R.id.image_view);
		this.hypno_view = (HypnoView) findViewById(R.id.hypno_view);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	this.wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Hypno");

    	if (savedInstanceState == null) {
			if (!settings.getBoolean(PREFKEY_SHOW_MAIN_INSTRUCTIONS, false)) {
				showDialog(DIALOG_INSTRUCTIONS);
			}
    	}
    	
    	
		final StateObject state = (StateObject) getLastNonConfigurationInstance();
		if (state != null) {
			this.painting_visible = state.painting_visible;
			this.painting_index = state.painting_index;
		}
		
    	updatePaintingVisibility();
	}

    // ========================================================================
    class StateObject {
    	int painting_index;
    	boolean painting_visible;
    }

    // ========================================================================
    @Override
    public Object onRetainNonConfigurationInstance() {
		StateObject state = new StateObject();
		state.painting_visible = this.painting_visible;
		state.painting_index = this.painting_index;
		return state;
    }
	
    // ========================================================================
	void cycleImage(boolean forward) {
		Log.d(TAG, "Cycling forward: " + forward);
		
		int increment = forward ? 1 : -1;
		this.painting_index = (this.painting_index + increment + MONET_PAINTINGS.length) % MONET_PAINTINGS.length;
		this.image_view.setImageResource(MONET_PAINTINGS[this.painting_index]);
	}
	
    // ========================================================================
	void togglePaintingVisibility() {
		this.painting_visible = !this.painting_visible;
    	updatePaintingVisibility();
	}
	
    // ========================================================================
	void updatePaintingVisibility() {
		this.image_view.setVisibility(this.painting_visible ? View.VISIBLE : View.GONE);
		this.hypno_view.setVisibility(this.painting_visible ? View.GONE : View.VISIBLE);
	}
	
    // ========================================================================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

    	boolean forward = true;
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_DOWN:
        	forward = !forward;
        	// XXX no break statement; we want to fall through
        case KeyEvent.KEYCODE_VOLUME_UP:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_CAMERA:
        case KeyEvent.KEYCODE_DPAD_CENTER:

        	cycleImage(forward);
        	togglePaintingVisibility();
        	
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
	
    // ========================================================================
    @Override
    protected void onResume() {
    	super.onResume();
		this.wl.acquire();
		
		this.hypno_view.adjustParameters(this.settings);
    }
    
    // ========================================================================
    @Override
    protected void onPause() {
    	super.onPause();
    	this.wl.release();
    }
    
    // ========================================================================
	@Override
	protected Dialog onCreateDialog(int id) {

        LayoutInflater factory = LayoutInflater.from(this);
        
		switch (id) {
		case DIALOG_INSTRUCTIONS:
		{
			final CheckBox reminder_checkbox;
			View tagTextEntryView = factory.inflate(R.layout.dialog_checkable_instructions, null);
			reminder_checkbox = (CheckBox) tagTextEntryView.findViewById(R.id.reminder_checkmark);

			((TextView) tagTextEntryView.findViewById(R.id.instructions_textview)).setText(R.string.instructions_home);

			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.dialog_title_instructions)
			.setView(tagTextEntryView)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(HypnoActivity.this);
					settings.edit().putBoolean(PREFKEY_SHOW_MAIN_INSTRUCTIONS, reminder_checkbox.isChecked()).commit();
				}
			})
			.create();
		}
		}

		return null;
	}
	
    // ========================================================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_main, menu);
		return true;
	}

	Menu saved_menu;
    // ========================================================================
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		saved_menu = menu;
		
		return true;
	}

    // ========================================================================
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_more_apps:
		{
			Uri market_uri = Uri.parse(Market.MARKET_AUTHOR_SEARCH_STRING);
			Intent i = new Intent(Intent.ACTION_VIEW, market_uri);
			if (Market.isIntentAvailable(this, i))
				startActivity(i);
			else
				Toast.makeText(this, "Android Market not available.", Toast.LENGTH_SHORT).show();
			return true;
		}
        case R.id.menu_preferences:
        {
        	startActivity(new Intent(this, PrefsHypnoDisc.class));
            return true;
        }
        case R.id.menu_help:
        {
			showDialog(DIALOG_INSTRUCTIONS);
            return true;
        }
		}

		return super.onOptionsItemSelected(item);
	}
}