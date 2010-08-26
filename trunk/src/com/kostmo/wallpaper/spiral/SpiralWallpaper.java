package com.kostmo.wallpaper.spiral;

import com.kostmo.tools.view.preference.ColorPreference;
import com.kostmo.wallpaper.spiral.activity.prefs.SpiralWallpaperSettings;
import com.kostmo.wallpaper.spiral.base.ArchimedeanSpiral;
import com.kostmo.wallpaper.spiral.base.LogarithmicSpiral;
import com.kostmo.wallpaper.spiral.base.SpiralGenerator;
import com.kostmo.wallpaper.spiral.base.LogarithmicSpiral.SolidType;
import com.kostmo.wallpaper.spiral.base.SpiralGenerator.SpiralType;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Join;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;


public class SpiralWallpaper extends WallpaperService {

	public static final String TAG = "SpiralWallpaper";

	public static final String SHARED_PREFS_NAME = "spiral_settings";

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
	@Override
	public Engine onCreateEngine() {
		return new SpiralEngine();
	}

	// ========================================================================
	class SpiralEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {

		final static int SPIRAL_SEGMENT_DIVISIONS = 5;

		private final Handler mHandler = new Handler();

		private final Paint mPaint = new Paint();
		private float mOffset;
		private float mTouchX = -1;
		private float mTouchY = -1;
		private long mStartTime;
		private float mCenterX;
		private float mCenterY;

		SpiralGenerator spiral_generator;
		float rotational_period;
		SpiralType assigned_spiral_type;

		boolean color_cycling_enabled = false;
		boolean continuous_color_cycling = false;
		private long future_color_cycling_time;


		final List<Integer> spiral_colors = new ArrayList<Integer>();
		int background_color;
		// ========================================================================
			void initializeSpiral(SpiralType spiral_type, float period_seconds, int turn_count, float pitch, SolidType solid_type, boolean antialiasing, boolean reversed) {

				this.assigned_spiral_type = spiral_type;
				this.rotational_period = period_seconds * (reversed ? -1 : 1);	// in seconds
				this.mPaint.setAntiAlias(antialiasing);

				switch (spiral_type) {
				case ARCHIMEDEAN:
					this.spiral_generator = new ArchimedeanSpiral(SPIRAL_SEGMENT_DIVISIONS, turn_count);
					break;
				case LOGARITHMIC:
					this.spiral_generator = new LogarithmicSpiral(SPIRAL_SEGMENT_DIVISIONS, turn_count, pitch, solid_type);
					break;
				}
			}

			// ====================================================================
			private final Runnable mDrawSpiral = new Runnable() {
				public void run() {
					drawFrame();
				}
			};
			private boolean mVisible;
			private SharedPreferences mPrefs;

			// ====================================================================
			SpiralEngine() {
				// Create a Paint to draw the lines for our cube
				final Paint paint = mPaint;
				paint.setColor(Color.WHITE);
				paint.setStrokeCap(Paint.Cap.ROUND);
				paint.setStrokeJoin(Join.BEVEL);
				paint.setStyle(Paint.Style.STROKE);

				mStartTime = SystemClock.elapsedRealtime();

				mPrefs = SpiralWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
				mPrefs.registerOnSharedPreferenceChangeListener(this);
				onSharedPreferenceChanged(mPrefs, null);
			}

			// ====================================================================
			List<Integer> getColorList(long now) {
				if (this.active_color_transition != null) {
					if (!this.active_color_transition.isExpired(now)) {
						return this.active_color_transition.interpolatedColorList(this.spiral_colors, now);
					}

					this.active_color_transition = null;
				} else if (this.continuous_color_cycling && this.color_cycling_enabled) {
					
					cycleRandomSpiralColors(now, false);
					return this.active_color_transition.interpolatedColorList(this.spiral_colors, now);
				}

				return this.spiral_colors;
			}

			// ====================================================================
			class ColorTransition {

				ColorTransition(long transition_start_time, float transition_duration_seconds, List<Integer> old_colors) {
					this.transition_start_time = transition_start_time;
					this.transition_end_time = transition_start_time + (long) (1000*transition_duration_seconds);
					this.old_colors = new ArrayList<Integer>(old_colors);
					this.interpolated_colors = new ArrayList<Integer>(old_colors);

					//        		Log.e(TAG, "Instantiating new ColorTransition with " + this.interpolated_colors.size() + " colors.");
				}

				public boolean isExpired(long now) {
					return now >= this.transition_end_time;
				}

				float getAlpha(long now) {
					return (now - this.transition_start_time) / (float) (this.transition_end_time - this.transition_start_time);
				}

				long transition_start_time, transition_end_time;

				final List<Integer> old_colors;
				final List<Integer> interpolated_colors;


				public List<Integer> interpolatedColorList(List<Integer> dst_colors, long now) {

					float alpha = getAlpha(now);
					for (int i=0; i<this.old_colors.size(); i++)
						this.interpolated_colors.set(i, interpolateColor(this.old_colors.get(i), dst_colors.get(i), alpha));

					return this.interpolated_colors;
				}

				int interpolateInt(int src, int dst, float alpha) {
					return (int) ((dst - src)*alpha) + src;
				}

				int interpolateColor(int src_color, int dst_color, float alpha) {
					int red = interpolateInt(Color.red(src_color), Color.red(dst_color), alpha);
					int green = interpolateInt(Color.green(src_color), Color.green(dst_color), alpha);
					int blue = interpolateInt(Color.blue(src_color), Color.blue(dst_color), alpha);
					return Color.rgb(red, green, blue);
				}
			}

			ColorTransition active_color_transition;

			// ====================================================================
			@Override
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

				int spiral_type_index = Integer.parseInt(prefs.getString(SpiralWallpaperSettings.PREFKEY_SPIRAL_TYPE, "1"));
				int solid_type_index = Integer.parseInt(prefs.getString(SpiralWallpaperSettings.PREFKEY_SOLID_TYPE, "2"));
				int turn_count = prefs.getInt(SpiralWallpaperSettings.PREFKEY_TURN_COUNT, SpiralWallpaperSettings.DEFAULT_TURN_COUNT);
				float pitch = prefs.getFloat(SpiralWallpaperSettings.PREFKEY_PITCH, SpiralWallpaperSettings.DEFAULT_PITCH);

				boolean antialiasing = prefs.getBoolean(SpiralWallpaperSettings.PREFKEY_ANTIALIASING, true);
				boolean reversed = prefs.getBoolean(SpiralWallpaperSettings.PREFKEY_REVERSE, false);
				float period_seconds = Float.parseFloat(prefs.getString(SpiralWallpaperSettings.PREFKEY_SPEED, "60"));

				this.color_cycling_enabled = prefs.getBoolean(SpiralWallpaperSettings.PREFKEY_ENABLE_COLOR_CYCLING, SpiralWallpaperSettings.DEFAULT_ENABLE_COLOR_CYCLING);
				this.continuous_color_cycling = prefs.getBoolean(SpiralWallpaperSettings.PREFKEY_ENABLE_CONSTANT_COLOR_CYCLING, false);
				if (this.color_cycling_enabled && !this.continuous_color_cycling) {
					updateCycleFutureTime();
				}

				populateColorList(prefs);


				this.background_color = prefs.getInt(SpiralWallpaperSettings.PREFKEY_COLOR_BACKGROUND, ColorPreference.FALLBACK_BACKGROUND_COLOR);

				initializeSpiral(
						SpiralType.values()[spiral_type_index],
						period_seconds,
						turn_count,
						pitch,
						SolidType.values()[solid_type_index],
						antialiasing,
						reversed
				);
			}

			// ====================================================================
			void populateColorList(SharedPreferences prefs) {
				this.spiral_colors.clear();
				this.spiral_colors.add( prefs.getInt(SpiralWallpaperSettings.PREFKEY_COLOR_PRIMARY, ColorPreference.FALLBACK_PRIMARY_COLOR) );
				this.spiral_colors.add( prefs.getInt(SpiralWallpaperSettings.PREFKEY_COLOR_SECONDARY, ColorPreference.FALLBACK_SECONDARY_COLOR) );
			}

			// ====================================================================
			void cycleRandomSpiralColors(long now, boolean update_future) {

				//        	Log.i(TAG, "Cycling spiral colors...");

				this.active_color_transition = new ColorTransition(now, COLOR_TRANSITION_SECONDS, spiral_colors);

				// XXX This approach doesn't work to prevent onSharedPreferenceChanged() from being called.
				//        	mPrefs.unregisterOnSharedPreferenceChangeListener(this);	// Temporarily disconnect listener:       	
				//        	SpiralWallpaperSettings.assignRandomSpiralColors(mPrefs);
				//        	populateColorList(mPrefs);
				//        	mPrefs.registerOnSharedPreferenceChangeListener(this);	// Reconnect listener

				spiral_colors.clear();
				spiral_colors.addAll( SpiralWallpaperSettings.populateNewRandomColors(this.mPrefs, 2) );

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
			void drawSpiral(Canvas c, int width, int height) {

				final long now = SystemClock.elapsedRealtime();

				if (!this.continuous_color_cycling && this.color_cycling_enabled && now > this.future_color_cycling_time) {
					cycleRandomSpiralColors(now, true);
				}


				float spiral_scale = (float) this.spiral_generator.getScale(new Point(width, height));

				c.save();
				c.translate(this.mCenterX, this.mCenterY);
				c.drawColor(this.background_color);	// Necessary to clear screen.

				float elapsed_seconds = ((float)(now - this.mStartTime)) / 1000;	// elapsed seconds


				float stroke_width = 0;
				switch (this.assigned_spiral_type) {
				case ARCHIMEDEAN:
					stroke_width = 0.5f;
					break;
				case LOGARITHMIC:
					int size = Math.max(width, height);
					int space_occupying_ratio = 32;
					stroke_width = (float) size/space_occupying_ratio/spiral_scale;
					break;
				}
				this.mPaint.setStrokeWidth( stroke_width );


				c.scale(spiral_scale, spiral_scale);
				c.rotate(360 * elapsed_seconds/this.rotational_period);
				this.spiral_generator.draw(c, this.mPaint, getColorList(now));

				c.restore();
			}

			// ====================================================================
			@Override
			public void onCreate(SurfaceHolder surfaceHolder) {
				super.onCreate(surfaceHolder);
				setTouchEventsEnabled(true);
			}

			// ====================================================================
			@Override
			public void onDestroy() {
				super.onDestroy();
				mHandler.removeCallbacks(mDrawSpiral);
			}

			// ====================================================================
			@Override
			public void onVisibilityChanged(boolean visible) {
				mVisible = visible;
				if (visible) {
					drawFrame();
				} else {
					mHandler.removeCallbacks(mDrawSpiral);
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
				mHandler.removeCallbacks(mDrawSpiral);
			}

			// ====================================================================
			@Override
			public void onOffsetsChanged(float xOffset, float yOffset,
					float xStep, float yStep, int xPixels, int yPixels) {
				mOffset = xOffset;
				drawFrame();
			}

			// ====================================================================
			@Override
			public void onTouchEvent(MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_MOVE) {
					mTouchX = event.getX();
					mTouchY = event.getY();
				} else {
					mTouchX = -1;
					mTouchY = -1;
				}
				super.onTouchEvent(event);
			}

			// ====================================================================
			void drawFrame() {
				final SurfaceHolder holder = getSurfaceHolder();
				final Rect frame = holder.getSurfaceFrame();
				final int width = frame.width();
				final int height = frame.height();

				Canvas c = null;
				try {
					c = holder.lockCanvas();
					if (c != null) {
						// draw something
						drawSpiral(c, width, height);

						//                    drawTouchPoint(c);
					}
				} finally {
					if (c != null) holder.unlockCanvasAndPost(c);
				}

				mHandler.removeCallbacks(mDrawSpiral);
				if (mVisible) {
					mHandler.postDelayed(mDrawSpiral, 1000 / 25);
				}
			}

			// ====================================================================
			void drawTouchPoint(Canvas c) {
				if (mTouchX >=0 && mTouchY >= 0) {
					c.drawCircle(mTouchX, mTouchY, 80, mPaint);
				}
			}
	}
}
