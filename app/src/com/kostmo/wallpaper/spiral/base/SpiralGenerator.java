package com.kostmo.wallpaper.spiral.base;


import Jama.Matrix;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Paint.Style;
import android.util.FloatMath;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public abstract class SpiralGenerator {
	
	public static final String TAG = "SpiralGenerator";

	final static float TWO_PI = (float) (2*Math.PI);
	int arc_divisions = 5;
	int periods = 3;
	
	List<BezierPoint> bezier_points;
	Path simple_path;
	
    public enum SpiralType {
    	ARCHIMEDEAN, LOGARITHMIC
    }

	// ========================================================================
    public int getPeriods() {
    	return this.periods;
    }
    
	// ========================================================================
	public SpiralGenerator(int arc_divisions, int periods) {
		this.arc_divisions = arc_divisions;
		this.periods = periods;
	}

	// ========================================================================
	protected void initialize() {
		float arc_span = TWO_PI / this.arc_divisions;
		this.bezier_points = generateBezierPoints(arc_span, this.periods);
		this.simple_path = createPath();
	}

	// ========================================================================
	abstract float spiralLength(float curve_start_angle);
	abstract float spiralRadius(float angle);
	abstract float spiralSlope(float angle);
	public abstract float getScale(Point screen_dimensions);
	public abstract Path getPath();

	// ========================================================================
	public void draw(Canvas c, Paint p, List<Integer> spiral_colors) {

        p.setStyle(Style.STROKE);
        if (spiral_colors.size() > 0) {
        	int color = spiral_colors.get(0);
        	p.setColor(color);
        }
		c.drawPath(getPath(), p);
	}

	// ========================================================================
	protected Path getSimplePath() {
		return this.simple_path;
	}

	// ========================================================================
	private List<BezierPoint> getBezierPoints() {
		return this.bezier_points;
	}

	// ========================================================================
	private Path createPath() {
		Path path = new Path();

		PointF start_point = getStartingPoint();
		path.moveTo(start_point.x, start_point.y);
		for (BezierPoint bp : this.bezier_points) {
			bp.drawCurveTo(path);
		}

		return path;
	}

	// ========================================================================
	private List<BezierPoint> generateBezierPoints(float incremental_angle, int periods) {
		List<BezierPoint> points = new ArrayList<BezierPoint>();
		
		int iteration_count = this.arc_divisions * periods;
		for (int i=0; i<iteration_count; i++) {

			float iteration_angle = i * incremental_angle;
			points.add( solveBezierControlPoints(
					incremental_angle, iteration_angle) );
		}
		
		return points;
	}

	// ========================================================================
	private PointF spiralPoint(float angle) {
		float radius = spiralRadius(angle);
		return new PointF(
				FloatMath.cos(angle) * radius,
				FloatMath.sin(angle) * radius
		);
	}

	// ========================================================================
	// Accepts a 4-element float
	private void cubicBezierCoefficients(double t, double[] coefficients) {
		double one_minus_t = 1-t;
		coefficients[0] = one_minus_t*one_minus_t*one_minus_t; 
		coefficients[1] = 3*one_minus_t*one_minus_t * t;
		coefficients[2] = 3*one_minus_t * t*t;
		coefficients[3] = t*t*t;
	}

	// ========================================================================
	double[] mc = new double[4];
	private BezierPoint solveBezierControlPoints(float arc_angle, float curve_start_angle) {

		float curve_end_angle = curve_start_angle + arc_angle;
		float curve_mid_angle = (curve_start_angle + curve_end_angle)/2;

		PointF curve_start_point = this.spiralPoint(curve_start_angle);
		PointF curve_end_point = this.spiralPoint(curve_end_angle);
		PointF curve_mid_point = this.spiralPoint(curve_mid_angle);

		// FIXME: This sometimes produces NaN.
		float midtime = (this.spiralLength(curve_end_angle) - this.spiralLength(curve_mid_angle)) / (this.spiralLength(curve_end_angle) - this.spiralLength(curve_start_angle));
//		float midtime = 0.5f;
		
		this.cubicBezierCoefficients(midtime, this.mc);	// midpoint coefficients


		double s0 = this.spiralSlope(curve_start_angle);	// The slope at the start of the curve
		double s1 = this.spiralSlope(curve_end_angle);	// The slope at the end of the curve

//		 Create an 8x8 matrix with a 8x1 solution column as follows:
//		 eq0: [x0, x1, x2, x3, y0, y1, y2, y3] = [s0]
//		                      .                   .
//		                      .                   .
//		                      .                   .
//		 eq7: [x0, x1, x2, x3, y0, y1, y2, y3] = [s7]

		double[][] A_array = {
			{1, 0, 0, 0, 0, 0, 0, 0},	// X and Y coordinates of endpoints
			{0, 0, 0, 0, 1, 0, 0, 0},
			{0, 0, 0, 1, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 1},
			{mc[0], mc[1], mc[2], mc[3], 0, 0, 0, 0},	// Midpoint x-coefficients
			{0, 0, 0, 0, mc[0], mc[1], mc[2], mc[3]},	// Midpoint y-coefficients
			{s0, -s0, 0, 0, -1, 1, 0, 0},	// Slope at start point
			{0, 0, -s1, s1, 0, 0, 1, -1},	// Slope at end point
		};

		double[][] b_array = {
			{curve_start_point.x},
			{curve_start_point.y},
			{curve_end_point.x},
			{curve_end_point.y},
			{curve_mid_point.x},
			{curve_mid_point.y},
			{0},
			{0}
		};

        Matrix A = new Matrix(A_array);
        Matrix b = new Matrix(b_array);
        
//        debugMatrix(A, "A");
//        debugMatrix(b, "b");
        
        try {
	        Matrix x = A.solve(b);
			return new BezierPoint(
					new PointF( (float) x.get(0, 0), (float) x.get(4, 0)),
					new PointF((float) x.get(1, 0), (float) x.get(5, 0)),
					new PointF((float) x.get(2, 0), (float) x.get(6, 0)),
					new PointF((float) x.get(3, 0), (float) x.get(7, 0))
			);
        } catch (Exception e) {
        	Log.e(getClass().getSimpleName(), e.getMessage());
        	debugMatrix(A, "A");

			return new BezierPoint(
					new PointF(),
					new PointF(),
					new PointF(),
					new PointF()
			);
        }
	}
	
	// ========================================================================
	private static String printPoint(PointF point) {
		return String.format("(%.2f, %.2f)", point.x, point.y);
	}

	// ========================================================================
	private void debugMatrix(Matrix matrix, String name) {

		String TAG = getClass().getSimpleName();
		Log.d(TAG, "Printing matrix " + name + ":");

		StringWriter sw = new StringWriter();
		matrix.print(new PrintWriter(sw), 8, 2);
		Log.i(TAG, sw.toString());
	}

	// ========================================================================
	private void debugCurve() {
		String TAG = getClass().getSimpleName();
		Log.d(TAG, "First Bezier coords:");
		
		BezierPoint first_bezier_coords = getBezierPoints().get(0);
		Log.i(TAG, "Start point: " +  printPoint(first_bezier_coords.start));
		Log.i(TAG, "Control point 0: " + printPoint(first_bezier_coords.c0));
		Log.i(TAG, "Control point 1: " + printPoint(first_bezier_coords.c1));
		Log.i(TAG, "End point: " + printPoint(first_bezier_coords.end));
	}

	// ========================================================================
	private PointF getStartingPoint() {
		return this.bezier_points.get(0).start;
	}
	
	protected PointF getEndingPoint() {
		return this.bezier_points.get(this.bezier_points.size() - 1).end;
	}
}
