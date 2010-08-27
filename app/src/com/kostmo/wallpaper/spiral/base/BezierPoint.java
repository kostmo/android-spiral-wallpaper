package com.kostmo.wallpaper.spiral.base;

import android.graphics.Path;
import android.graphics.PointF;

public class BezierPoint {

	public PointF start, c0, c1, end;

	// ========================================================================
	BezierPoint(PointF start, PointF c0, PointF c1, PointF end) {
		this.start = start;
		this.c0 = c0;
		this.c1 = c1;
		this.end = end;
	}

	// ========================================================================
	public void drawCurveTo(Path path) {
		path.cubicTo(
				this.c0.x, this.c0.y,
				this.c1.x, this.c1.y,
				this.end.x, this.end.y);
	}

	// ========================================================================
	public void drawReverseCurveTo(Path path) {
		path.cubicTo(
				this.c1.x, this.c1.y,
				this.c0.x, this.c0.y,
				this.start.x, this.start.y);
	}
	

	// ========================================================================
	public BezierPoint getRot90() {
		return new BezierPoint(
				rotate90(this.start),
				rotate90(this.c0),
				rotate90(this.c1),
				rotate90(this.end)
		);
	}

	// ========================================================================
	public BezierPoint getRot180() {
		return new BezierPoint(
				rotate180(this.start),
				rotate180(this.c0),
				rotate180(this.c1),
				rotate180(this.end)
		);
	}
	
	// ========================================================================
	public static PointF rotate90(PointF point) {
		return new PointF(point.y, -point.x);
	}
	
	// ========================================================================
	public static PointF rotate180(PointF point) {
		PointF out = new PointF();
		out.set(point);
		out.negate();
		
		return out;
	}
}
