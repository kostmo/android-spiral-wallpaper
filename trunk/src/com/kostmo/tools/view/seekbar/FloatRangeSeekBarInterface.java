package com.kostmo.tools.view.seekbar;

public interface FloatRangeSeekBarInterface {


    // =================================
    public void setGranularity(int granules);
	
    // =================================
    public void setRange(float min, float max);
    
    // =================================
    public float getValue();
    
    // =================================
    public void setValue(float value);
    
    // =================================
    public float getMinValue();
    
    // =================================
    public float getMaxValue();
}
