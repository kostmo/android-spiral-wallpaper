package com.kostmo.wallpaper.spiral.base;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Paint.Style;
import android.util.FloatMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class LogarithmicSpiral extends SpiralGenerator {
	
	float pitch_multiplier;
	
	List<BezierPoint> reversed_bezier_points;
	SolidType solid_type = SolidType.DOUBLE;
	Path solid_path;
	
	public enum SolidType {
		NONE, DOUBLE, QUAD
	}

	// ========================================================================
	public float getPitchMultiplier() {
		return this.pitch_multiplier;
	}

	// ========================================================================
	public LogarithmicSpiral(int arc_divisions, int periods, float pitch, SolidType solid_type) {
		super(arc_divisions, periods);
		
		this.pitch_multiplier = pitch;
		initialize();
		
		this.solid_type = solid_type;
		
		this.reversed_bezier_points = new ArrayList<BezierPoint>(this.bezier_points);
		Collections.reverse(this.reversed_bezier_points);

		
		switch (this.solid_type) {
		case DOUBLE:
			this.solid_path = createHalfSolidPath();
			break;
		case QUAD:
			this.solid_path = createQuarterSolidPath();
			break;
		default:
			this.solid_path = getSimplePath();
		}
	}

	// ========================================================================
	float getPitch() {
		return this.pitch_multiplier != 0 ? 1/(float) (this.pitch_multiplier * TWO_PI) : 0;
	}

	// ========================================================================
	@Override
	float spiralLength(float theta) {
		float pitch = getPitch();
		return FloatMath.sqrt(1 + pitch*pitch) * this.spiralRadius(theta) / pitch;
	}

	// ========================================================================
	@Override
	float spiralSlope(float theta) {

		float cos = FloatMath.cos(theta);
		float sin = FloatMath.sin(theta);
		
		float dy = sin * getPitch() + cos;
		float dx = cos * getPitch() - sin;

		return dy/dx;
	}

	// ========================================================================
	@Override
	float spiralRadius(float theta) {
		return (float) Math.exp(getPitch()*theta);
	}
	
	// ========================================================================
	@Override
	public float getScale(Point screen_dimensions) {
		float half_diagonal = PointF.length(screen_dimensions.x, screen_dimensions.y)/2f;
		if (this.pitch_multiplier > 0) {
			
			float max_radius = spiralRadius(this.periods * TWO_PI);

			return half_diagonal / max_radius;
			
		} else return half_diagonal;
	}
		
	// ========================================================================
	Path createReversePath() {
		Path path = new Path();

		PointF start_point = getReversedStartingPoint();
		path.moveTo(start_point.x, start_point.y);
		for (BezierPoint bp : this.reversed_bezier_points) {
			bp.drawReverseCurveTo(path);
		}

		return path;
	}
	
	// ========================================================================
	public Path createHalfSolidPath() {

		Path solid_spiral = new Path(getSimplePath());
		
		if (this.pitch_multiplier > 0) {
			PointF last_pt = this.getEndingPoint();
			solid_spiral.lineTo(last_pt.x, last_pt.x);
			solid_spiral.lineTo(-last_pt.x, last_pt.x);
			solid_spiral.lineTo(-last_pt.x, last_pt.y);
		}
		

		PointF start_point = BezierPoint.rotate180( getReversedStartingPoint() );
		solid_spiral.lineTo(start_point.x, start_point.y);
		for (BezierPoint bp : this.reversed_bezier_points) {
			bp.getRot180().drawReverseCurveTo(solid_spiral);
		}
		

		if (this.pitch_multiplier < 0) {

			solid_spiral.lineTo(-1,  0);
			solid_spiral.lineTo(-1, -1);
			solid_spiral.lineTo( 1, -1);
			solid_spiral.lineTo( 1,  0);
		}

		
		solid_spiral.close();
		
		return solid_spiral;
	}
	

	
	// ========================================================================
	public Path createQuarterSolidPath() {

		Path solid_spiral = new Path(getSimplePath());

		PointF last_pt = this.getEndingPoint();
		if (this.pitch_multiplier > 0) {
			solid_spiral.lineTo(last_pt.x, -last_pt.x);
		}

		PointF start_point = BezierPoint.rotate90( getReversedStartingPoint() );
		solid_spiral.lineTo(start_point.x, start_point.y);
		for (BezierPoint bp : this.reversed_bezier_points) {
			bp.getRot90().drawReverseCurveTo(solid_spiral);		
		}

		if (this.pitch_multiplier < 0) {
			solid_spiral.lineTo( 1, -1 );
		}


		solid_spiral.close();
		
		return solid_spiral;
	}
	
	
	// ========================================================================
	PointF getReversedStartingPoint() {
		return this.reversed_bezier_points.get(0).end;
	}

	// ========================================================================
	PointF getReversedEndingPoint() {
		return this.reversed_bezier_points.get(this.reversed_bezier_points.size() - 1).start;
	}

	// ========================================================================
	@Override
	public Path getPath() {
		return this.solid_path;
	}

	// ========================================================================
	@Override
	public void draw(Canvas c, Paint p, List<Integer> spiral_colors) {

        p.setStyle(Style.FILL);
        if (spiral_colors.size() > 0)
            p.setColor(spiral_colors.get(0));

		
		switch (this.solid_type) {
		case DOUBLE:
		{
			c.drawPath(getPath(), p);
			break;
		}	
		case QUAD:
		{
            c.drawPath(getPath(), p);

            c.rotate(180);
            
	        if (spiral_colors.size() > 1)
	            p.setColor(spiral_colors.get(1));

            c.drawPath(getPath(), p);

			break;
		}	
		default:
			super.draw(c, p, spiral_colors);
		}
	}
}
