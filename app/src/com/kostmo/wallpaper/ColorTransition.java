package com.kostmo.wallpaper;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;

public class ColorTransition {

	ColorTransition(long transition_start_time, float transition_duration_seconds, List<Integer> old_colors) {
		this.transition_start_time = transition_start_time;
		this.transition_end_time = transition_start_time + (long) (1000*transition_duration_seconds);
		this.old_colors = new ArrayList<Integer>(old_colors);
		this.interpolated_colors = new ArrayList<Integer>(old_colors);

// 		Log.e(TAG, "Instantiating new ColorTransition with " + this.interpolated_colors.size() + " colors.");
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
