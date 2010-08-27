package com.kostmo.wallpaper.spiral.view;

import com.kostmo.wallpaper.spiral.activity.prefs.PrefsHypnoDisc;
import com.kostmo.wallpaper.spiral.base.ArchimedeanSpiral;
import com.kostmo.wallpaper.spiral.base.SpiralGenerator;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;


public class HypnoView extends SurfaceView implements SurfaceHolder.Callback {

	// A Java animation of a Hypno-Disc is here: http://www.diamond-jim.com/hypnodisc/
	// XXX The hypnodisc appears to use 3 Archimedean spirals of different sizes.
	
	static final String TAG = "HypnoView";

	final static int SPIRAL_SEGMENT_DIVISIONS = 5;

	
	final static int BACKGROUND_COLOR = Color.WHITE;
	
	
	
//	SpiralGenerator spiral_generator = new LogarithmicSpiral(SPIRAL_SEGMENT_DIVISIONS, 30, -6, SolidType.DOUBLE);
	SpiralGenerator spiral_generator_outer = new ArchimedeanSpiral(SPIRAL_SEGMENT_DIVISIONS, 15);
	SpiralGenerator spiral_generator_middle = new ArchimedeanSpiral(SPIRAL_SEGMENT_DIVISIONS, 10);

	float rotational_period = 0.4f;
	private long mStartTime;

	private final Paint mPaint = new Paint();
	private final Paint mAnnulusPaint = new Paint();

	private final Handler mHandler = new Handler();

    final List<Integer> spiral_colors = new ArrayList<Integer>();


	private boolean enable_counter_rotator = true;
	private boolean enable_crop = false;
	private boolean reverse = false;

    // ========================================================================
    public void adjustParameters(SharedPreferences settings) {

    	this.enable_counter_rotator = settings.getBoolean(PrefsHypnoDisc.PREFKEY_ENABLE_COUNTER_ROTATOR, true);

    	boolean antialiasing = settings.getBoolean(PrefsHypnoDisc.PREFKEY_ANTIALIASING, false);
    	this.mPaint.setAntiAlias(antialiasing);

    	this.reverse = settings.getBoolean(PrefsHypnoDisc.PREFKEY_REVERSE, false);
    	this.enable_crop = settings.getBoolean(PrefsHypnoDisc.PREFKEY_ENABLE_CROP, false);
    }

    // ========================================================================
    float getPeriod() {
    	return this.reverse ? this.rotational_period : -this.rotational_period;
    }

    // ========================================================================
    Bitmap makeSpiralBitmap(int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
//        canvas.drawColor(Color.TRANSPARENT);

		// Draw middle ring
		float annulus_radius = Math.min(w, h)/2f;

		RectF r = new RectF(0, 0, w, h);
        // draw into our offscreen bitmap
        int sc = canvas.saveLayer(r, null,
				  					Canvas.MATRIX_SAVE_FLAG |
                                  Canvas.CLIP_SAVE_FLAG |
                                  Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                                  Canvas.CLIP_TO_LAYER_SAVE_FLAG);

        canvas.translate(r.centerX(), r.centerY());
        
        this.mAnnulusPaint.setStyle(Style.FILL);
        this.mAnnulusPaint.setAntiAlias(true);
		canvas.drawCircle(0, 0, annulus_radius, this.mAnnulusPaint);
		
		
		float middle_spiral_scale = (float) this.spiral_generator_middle.getScale(new Point(canvas.getWidth(), canvas.getHeight()));


		this.mPaint.setXfermode( new PorterDuffXfermode(PorterDuff.Mode.SRC_IN) );
		canvas.scale(middle_spiral_scale, middle_spiral_scale);
		
		boolean was_antialiasing = this.mPaint.isAntiAlias();
        this.mPaint.setAntiAlias(true);
		this.spiral_generator_middle.draw(canvas, this.mPaint, this.spiral_colors);
        this.mPaint.setAntiAlias(was_antialiasing);
		

		this.mPaint.setXfermode(null);
        canvas.restoreToCount(sc);

        return bm;
    }


    Bitmap inner_ring, middle_ring;
    // ========================================================================
	void init() {

		// Create a Paint to draw the lines for our cube
		this.mPaint.setColor(Color.BLACK);
		this.mPaint.setAntiAlias(false);
		this.mPaint.setStrokeCap(Paint.Cap.ROUND);
		this.mPaint.setStrokeJoin(Join.BEVEL);
		this.mPaint.setStyle(Paint.Style.STROKE);
		this.mPaint.setStrokeWidth( 0.5f );
		this.mPaint.setFilterBitmap(true);


		this.mAnnulusPaint.set(this.mPaint);
		this.mAnnulusPaint.setAntiAlias(true);
		this.mAnnulusPaint.setColor(BACKGROUND_COLOR);
		this.mAnnulusPaint.setStyle(Style.STROKE);

		this.mStartTime = SystemClock.elapsedRealtime();
	}

    // ========================================================================
	public HypnoView(Context context) {
		super(context);
		init();
	}

    // ========================================================================
	public HypnoView(Context context, AttributeSet attrs) {
		super(context, attrs);

		init();

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		// create thread only; it's actually started in surfaceCreated()
		drawing_runnable = new DrawingRunnable(holder);
	}

    // ========================================================================
	class DrawingRunnable implements Runnable {

		DrawingRunnable(SurfaceHolder holder) {
			this.mSurfaceHolder = holder;
		}
		SurfaceHolder mSurfaceHolder;


		@Override
		public void run() {
			Canvas c = null;
			try {
				c = mSurfaceHolder.lockCanvas(null);
				if (c == null) return;
				synchronized (mSurfaceHolder) {
					doDraw(c);
				}
			} finally {
				// do this in a finally so that if an exception is thrown
				// during the above, we don't leave the Surface in an
				// inconsistent state
				if (c != null) {
					mSurfaceHolder.unlockCanvasAndPost(c);
				}
			}


			mHandler.removeCallbacks(drawing_runnable);

			boolean visible = true;
			if (visible) {
				mHandler.post(drawing_runnable);
			}
		}
	}

    // ========================================================================
	protected void doDraw(Canvas canvas) {
		//		Log.d(TAG, "Drawing on canvas: (" + canvas.getWidth() + "x" + canvas.getHeight() + ")");

		canvas.drawColor(BACKGROUND_COLOR);	// Necessary to clear screen.

		canvas.translate(canvas.getWidth()/2, canvas.getHeight()/2);

		
		long now = SystemClock.elapsedRealtime();
		float elapsed_seconds = ((float)(now - this.mStartTime)) / 1000;	// elapsed seconds
		float theta = 360 * elapsed_seconds/getPeriod();
		

		if (this.enable_crop) {
			RectF r0 = getOutsetRect( Math.min(canvas.getWidth(), canvas.getHeight())/2 );
			canvas.clipRect(r0);
		}

		if (this.canvas_dimensions != null) {
			float outer_spiral_scale = (float) this.spiral_generator_outer.getScale(this.canvas_dimensions);
	
			canvas.save();
			canvas.rotate(theta);
			canvas.scale(outer_spiral_scale, outer_spiral_scale);
			this.spiral_generator_outer.draw(canvas, this.mPaint, this.spiral_colors);
			canvas.restore();
		}


		if (this.enable_counter_rotator) {

			canvas.rotate(theta);

			canvas.scale(-1, 1);
			if (this.middle_ring != null)
				canvas.drawBitmap(
						this.middle_ring,
						-this.middle_ring.getWidth()/2,
						-this.middle_ring.getHeight()/2,
						this.mPaint);

			canvas.scale(-1, 1);
			if (this.inner_ring != null)
				canvas.drawBitmap(
						this.inner_ring,
						-this.inner_ring.getWidth()/2,
						-this.inner_ring.getHeight()/2,
						this.mPaint);
		}
	}

    // ========================================================================
	RectF getOutsetRect(float radius) {
		RectF r = new RectF();
		r.inset(-radius, -radius);
		return r;
	}

    // ========================================================================
	/** The thread that actually draws the animation */
	private Thread drawing_thread;
	protected DrawingRunnable drawing_runnable;

    // ========================================================================
	void runDrawingThread() {
		drawing_thread = new Thread(drawing_runnable);
		drawing_thread.start();
	}

    // ========================================================================
	/* Callback invoked when the surface dimensions change. */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

    // ========================================================================
	/*
	 * Callback invoked when the Surface has been created and is ready to be
	 * used.
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		runDrawingThread();
	}

    // ========================================================================
	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode

	}

    // ========================================================================
	Point canvas_dimensions = new Point();
	
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    	Rect rect = new Rect(l, t, r, b);
        int size = Math.min(rect.width(), rect.height());
        int middle_size = 2*size/3;
        int inner_size = size/3;
        
        this.middle_ring = makeSpiralBitmap(middle_size, middle_size);
        this.inner_ring = makeSpiralBitmap(inner_size, inner_size);
        
        this.canvas_dimensions = new Point(rect.width(), rect.height());
    }
}
