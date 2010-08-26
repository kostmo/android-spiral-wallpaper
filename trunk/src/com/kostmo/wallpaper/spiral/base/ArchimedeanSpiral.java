package com.kostmo.wallpaper.spiral.base;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.FloatMath;


public class ArchimedeanSpiral extends SpiralGenerator {

	// ========================================================================
	public ArchimedeanSpiral(int arcDivisions, int periods) {
		super(arcDivisions, periods);
		initialize();
	}

	// ========================================================================
	@Override
	float spiralLength(float theta) {
		float length = ( (theta * FloatMath.sqrt(1 + theta*theta)) + (float) Math.log(theta + FloatMath.sqrt(1 + theta*theta)) ) / 2;
		return length;
	}

	// ========================================================================
	public float getScale(Point screen_dimensions) {

		float half_diagonal = PointF.length(screen_dimensions.x, screen_dimensions.y)/2f;
		float max_radius = spiralRadius((this.periods - 1) * TWO_PI);

		return half_diagonal / max_radius;
	}
	
	
	// ========================================================================
	@Override
	float spiralSlope(float theta) {
		float radius = this.spiralRadius(theta);
		float cos = FloatMath.cos(theta);
		float sin = FloatMath.sin(theta);
		
		float dy = sin/TWO_PI + radius*cos;
		float dx = cos/TWO_PI - radius*sin;
		
		return dy/dx;
	}

	// ========================================================================
	@Override
	float spiralRadius(float theta) {
		return theta/TWO_PI;
	}

	// ========================================================================
	@Override
	public Path getPath() {
		return getSimplePath();
	}
}
