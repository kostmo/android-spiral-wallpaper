package com.kostmo.wallpaper.spiral;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Join;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.kostmo.wallpaper.ColorCyclingWallpaper;
import com.kostmo.wallpaper.spiral.activity.prefs.SpiralWallpaperSettings;
import com.kostmo.wallpaper.spiral.base.SpiralGenerator;
import com.kostmo.wallpaper.spiral.base.SpiralGenerator.SpiralType;


public class DecoWallpaper extends ColorCyclingWallpaper {

	public static final String TAG = "DecoWallpaper";

	public static final String SHARED_PREFS_NAME = "deco_settings";

	// ========================================================================
	@Override
	public Engine onCreateEngine() {
		return new DecoEngine();
	}

	// ========================================================================
	class DecoEngine extends ColorCyclingEngine implements SharedPreferences.OnSharedPreferenceChangeListener {

		private Paint mPaint;

		SpiralGenerator spiral_generator;
		float rotational_period;
		SpiralType assigned_spiral_type;

		// ====================================================================
		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(true);
		}

		// ====================================================================
		@Override
		public void onOffsetsChanged(float xOffset, float yOffset,
				float xStep, float yStep, int xPixels, int yPixels) {
			this.mOffset = xOffset;
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
		// ========================================================================
		void initializeDeco(float period_seconds, int turn_count, float pitch, boolean antialiasing, boolean reversed) {
			this.rotational_period = period_seconds * (reversed ? -1 : 1);	// in seconds
			this.mPaint.setAntiAlias(antialiasing);
		}

		// ====================================================================
		@Override
		protected void setUp() {

			this.mPaint = new Paint();
			
			final Paint paint = this.mPaint;
			paint.setColor(Color.WHITE);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStrokeJoin(Join.BEVEL);
			paint.setStyle(Paint.Style.STROKE);
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

		// ====================================================================
		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			super.onSharedPreferenceChanged(prefs, key);

			int turn_count = prefs.getInt(SpiralWallpaperSettings.PREFKEY_TURN_COUNT, SpiralWallpaperSettings.DEFAULT_TURN_COUNT);
			float pitch = prefs.getFloat(SpiralWallpaperSettings.PREFKEY_PITCH, SpiralWallpaperSettings.DEFAULT_PITCH);

			boolean antialiasing = prefs.getBoolean(SpiralWallpaperSettings.PREFKEY_ANTIALIASING, true);
			boolean reversed = prefs.getBoolean(SpiralWallpaperSettings.PREFKEY_REVERSE, false);
			float period_seconds = Float.parseFloat(prefs.getString(SpiralWallpaperSettings.PREFKEY_SPEED, "60"));

			initializeDeco(
					period_seconds,
					turn_count,
					pitch,
					antialiasing,
					reversed
			);
		}

		// ====================================================================
		/** Makes a wiggly horizontal path */
        private Path makeFollowPath(int steps) {
        	
        	// TODO Use order-2 beziers instead
            Path p = new Path();
            p.moveTo(0, 0);
            for (int i = 0; i < steps; i++) {
                p.lineTo(i/(float) steps, 0.2f * (float) Math.pow(-1, i));
            }
            return p;
        }
		
		// ====================================================================
		void drawDeco(Canvas c, int width, int height, long now) {

			this.mPaint.setStrokeWidth( 0.1f );

			c.scale(width, height);
			c.translate(-0.5f, 0f);
			Path path = makeFollowPath(7);

            mPaint.setPathEffect(new CornerPathEffect(0.1f));
            c.drawPath(path, mPaint);
			
		}

		// ====================================================================
		void drawTouchPoint(Canvas c) {
			if (mTouchX >=0 && mTouchY >= 0) {
				c.drawCircle(mTouchX, mTouchY, 80, mPaint);
			}
		}

		// ====================================================================
		@Override
		protected String getPrivatePrefsName() {
			return SHARED_PREFS_NAME;
		}

		// ====================================================================
		@Override
		protected void drawDesign(Canvas c, int width, int height, long now, int layer) {
			drawDeco(c, width, height, now);
		}
	}
}
