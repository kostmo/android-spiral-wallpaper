package com.kostmo.wallpaper.spiral;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Paint.Join;
import android.util.Log;

import com.kostmo.wallpaper.ColorCyclingWallpaper;
import com.kostmo.wallpaper.spiral.activity.prefs.SpiralWallpaperSettings;
import com.kostmo.wallpaper.spiral.base.ArchimedeanSpiral;
import com.kostmo.wallpaper.spiral.base.LogarithmicSpiral;
import com.kostmo.wallpaper.spiral.base.SpiralGenerator;
import com.kostmo.wallpaper.spiral.base.LogarithmicSpiral.SolidType;
import com.kostmo.wallpaper.spiral.base.SpiralGenerator.SpiralType;


public class SpiralWallpaper extends ColorCyclingWallpaper {

	public static final String TAG = "SpiralWallpaper";

	public static final String SHARED_PREFS_NAME = "spiral_settings";

	// ========================================================================
	@Override
	public Engine onCreateEngine() {
		return new SpiralEngine();
	}

	// ========================================================================
	class SpiralEngine extends ColorCyclingEngine implements SharedPreferences.OnSharedPreferenceChangeListener {

		final static int SPIRAL_SEGMENT_DIVISIONS = 5;


		private Paint mPaint;

		SpiralGenerator spiral_generator;
		float rotational_period;
		SpiralType assigned_spiral_type;

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
		@Override
		protected void setUp() {

			mPaint = new Paint();
			
			final Paint paint = mPaint;
			paint.setColor(Color.WHITE);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStrokeJoin(Join.BEVEL);
			paint.setStyle(Paint.Style.STROKE);
		}

		// ====================================================================
		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			
			Log.d(TAG, "Calling onSharedPreferenceChanged() in SpiralWallpaper");
			super.onSharedPreferenceChanged(prefs, key);

			
			int spiral_type_index = Integer.parseInt(prefs.getString(SpiralWallpaperSettings.PREFKEY_SPIRAL_TYPE, "1"));
			int solid_type_index = Integer.parseInt(prefs.getString(SpiralWallpaperSettings.PREFKEY_SOLID_TYPE, "2"));
			int turn_count = prefs.getInt(SpiralWallpaperSettings.PREFKEY_TURN_COUNT, SpiralWallpaperSettings.DEFAULT_TURN_COUNT);
			float pitch = prefs.getFloat(SpiralWallpaperSettings.PREFKEY_PITCH, SpiralWallpaperSettings.DEFAULT_PITCH);

			boolean antialiasing = prefs.getBoolean(SpiralWallpaperSettings.PREFKEY_ANTIALIASING, true);
			boolean reversed = prefs.getBoolean(SpiralWallpaperSettings.PREFKEY_REVERSE, false);
			float period_seconds = Float.parseFloat(prefs.getString(SpiralWallpaperSettings.PREFKEY_SPEED, "60"));


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
		void drawSpiral(Canvas c, int width, int height, long now, int layer) {

			float spiral_scale = (float) this.spiral_generator.getScale(new Point(width, height));
			float elapsed_seconds = ((float)(now - this.mStartTime)) / 1000;	// elapsed seconds

			float stroke_width = 0.5f;
			switch (this.assigned_spiral_type) {
			case ARCHIMEDEAN:	// Keep default
				break;
			case LOGARITHMIC:
				int size = Math.max(width, height);
				int space_occupying_ratio = 32;
				stroke_width = (float) size/space_occupying_ratio/spiral_scale;
				break;
			}
			this.mPaint.setStrokeWidth( stroke_width );

			if (layer != 0) {
				this.mPaint.setXfermode( new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN) );
			} else {
				this.mPaint.setXfermode( null );
			}

			c.save();
			c.scale(spiral_scale, spiral_scale);
			float rotation = 360 * elapsed_seconds/this.rotational_period;
			if (layer != 0) {
				rotation *= 1.5f;
			}
			c.rotate(rotation);
			this.spiral_generator.draw(c, this.mPaint, getColorList(now));
			c.restore();
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
			drawSpiral(c, width, height, now, layer);
		}
	}
}
