package com.kostmo.wallpaper;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import com.kostmo.tools.view.preference.ColorPreference;
import com.kostmo.wallpaper.spiral.activity.prefs.SpiralWallpaperSettings;

public abstract class ColorCyclingWallpaper extends WallpaperService {

	public static final String TAG = "ColorCyclingWallpaper";

	public static final int MS_PER_MINUTE = 1000*60;
	final static float COLOR_TRANSITION_SECONDS = 5;

	// ========================================================================
	@Override
	public void onCreate() {
		super.onCreate();
	}

	// ========================================================================
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	// ========================================================================
	abstract protected class ColorCyclingEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {

		protected final Handler mHandler = new Handler();

		protected float mOffset;
		protected float mTouchX = -1;
		protected float mTouchY = -1;
		protected long mStartTime;
		private float mCenterX;
		private float mCenterY;

		protected boolean color_cycling_enabled = false;
		protected boolean continuous_color_cycling = false;
		protected long future_color_cycling_time;


		protected final List<Integer> cycling_colors = new ArrayList<Integer>();
		protected int background_color;

		// ====================================================================
		protected final Runnable mDrawWallpaper = new Runnable() {
			public void run() {
				drawFrame();
			}
		};

		protected boolean mVisible;
		protected SharedPreferences mPrefs;


		// ====================================================================
		protected abstract void drawDesign(Canvas c, int width, int height, long now, int layer);
		abstract protected String getPrivatePrefsName();
		abstract protected void setUp();
		
		// ====================================================================
		protected ColorCyclingEngine() {
			this.mStartTime = SystemClock.elapsedRealtime();

			this.mPrefs = ColorCyclingWallpaper.this.getSharedPreferences(getPrivatePrefsName(), Context.MODE_PRIVATE);
			this.mPrefs.registerOnSharedPreferenceChangeListener(this);
			
			
			setUp();
			
			
			onSharedPreferenceChanged(this.mPrefs, null);
		}

		// ====================================================================
		protected List<Integer> getColorList(long now) {
			if (this.active_color_transition != null) {
				if (!this.active_color_transition.isExpired(now)) {
					return this.active_color_transition.interpolatedColorList(this.cycling_colors, now);
				}

				this.active_color_transition = null;
			} else if (this.continuous_color_cycling && this.color_cycling_enabled) {

				cycleRandomColors(now, false);
				return this.active_color_transition.interpolatedColorList(this.cycling_colors, now);
			}

			return this.cycling_colors;
		}

		// ====================================================================

		ColorTransition active_color_transition;

		// ====================================================================
		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

			this.color_cycling_enabled = prefs.getBoolean(SpiralWallpaperSettings.PREFKEY_ENABLE_COLOR_CYCLING, SpiralWallpaperSettings.DEFAULT_ENABLE_COLOR_CYCLING);
			this.continuous_color_cycling = prefs.getBoolean(SpiralWallpaperSettings.PREFKEY_ENABLE_CONSTANT_COLOR_CYCLING, false);
			if (this.color_cycling_enabled && !this.continuous_color_cycling) {
				updateCycleFutureTime();
			}

			populateColorList(prefs);

			this.background_color = prefs.getInt(SpiralWallpaperSettings.PREFKEY_COLOR_BACKGROUND, ColorPreference.FALLBACK_BACKGROUND_COLOR);
		}

		// ====================================================================
		void populateColorList(SharedPreferences prefs) {
			this.cycling_colors.clear();
			this.cycling_colors.add( prefs.getInt(SpiralWallpaperSettings.PREFKEY_COLOR_PRIMARY, ColorPreference.FALLBACK_PRIMARY_COLOR) );
			this.cycling_colors.add( prefs.getInt(SpiralWallpaperSettings.PREFKEY_COLOR_SECONDARY, ColorPreference.FALLBACK_SECONDARY_COLOR) );
		}

		// ====================================================================
		protected void cycleRandomColors(long now, boolean update_future) {

//        	Log.i(TAG, "Cycling colors...");

			this.active_color_transition = new ColorTransition(now, COLOR_TRANSITION_SECONDS, cycling_colors);

			// XXX This approach doesn't work to prevent onSharedPreferenceChanged() from being called.
//        	mPrefs.unregisterOnSharedPreferenceChangeListener(this);	// Temporarily disconnect listener:       	
//        	SpiralWallpaperSettings.assignRandomSpiralColors(mPrefs);
//        	populateColorList(mPrefs);
//        	mPrefs.registerOnSharedPreferenceChangeListener(this);	// Reconnect listener

			cycling_colors.clear();
			cycling_colors.addAll( SpiralWallpaperSettings.populateNewRandomColors(this.mPrefs, 2) );

			if (update_future)
				updateCycleFutureTime();
		}

		// ====================================================================
		void updateCycleFutureTime() {
//        	long millisecond_offset = (long) (COLOR_TRANSITION_SECONDS * 1000);

			int cycle_minutes = mPrefs.getInt(SpiralWallpaperSettings.PREFKEY_COLOR_CYCLER_MINUTES, SpiralWallpaperSettings.DEFAULT_COLOR_CYCLER_MINUTES);
//        	Log.d(TAG, "Will cycle spiral colors in " + cycle_minutes + " minutes");
			this.future_color_cycling_time = SystemClock.elapsedRealtime() + MS_PER_MINUTE*cycle_minutes;
		}

		// ====================================================================
		@Override
		public void onDestroy() {
			super.onDestroy();
			mHandler.removeCallbacks(mDrawWallpaper);
		}

		// ====================================================================
		@Override
		public void onVisibilityChanged(boolean visible) {
			mVisible = visible;
			if (visible) {
				drawFrame();
			} else {
				mHandler.removeCallbacks(mDrawWallpaper);
			}
		}

		// ====================================================================
		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			// store the center of the surface, so we can draw the cube in the right spot
			mCenterX = width/2.0f;
			mCenterY = height/2.0f;
			drawFrame();
		}

		// ====================================================================
		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
		}

		// ====================================================================
		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(mDrawWallpaper);
		}

		// ====================================================================
		protected void drawFrame() {
			final SurfaceHolder holder = getSurfaceHolder();
			final Rect frame = holder.getSurfaceFrame();
			final int width = frame.width();
			final int height = frame.height();

			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {

					c.drawColor(this.background_color);	// Necessary to clear screen.
					c.translate(this.mCenterX, this.mCenterY);

					
					final long now = SystemClock.elapsedRealtime();
					if (!this.continuous_color_cycling && this.color_cycling_enabled && now > this.future_color_cycling_time) {
						cycleRandomColors(now, true);
					}
					
					drawDesign(c, width, height, now, 0);

				}
			} finally {
				if (c != null) holder.unlockCanvasAndPost(c);
			}

			mHandler.removeCallbacks(mDrawWallpaper);
			if (mVisible) {
				mHandler.postDelayed(mDrawWallpaper, 1000 / 25);
			}
		}
	}
}
