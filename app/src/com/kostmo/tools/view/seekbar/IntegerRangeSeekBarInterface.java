package com.kostmo.tools.view.seekbar;

public interface IntegerRangeSeekBarInterface {

    // =================================
    public void setRange(int min, int max);
    
    // =================================
    public int getValue();
    
    // =================================
    public void setValue(int value);
    
    // =================================
    public int getMinValue();
    
    // =================================
    public int getMaxValue();
}
