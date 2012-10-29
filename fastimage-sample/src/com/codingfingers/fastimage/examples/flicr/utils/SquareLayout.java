package com.codingfingers.fastimage.examples.flicr.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class SquareLayout extends RelativeLayout {

public float mScale = 1.0f;

public SquareLayout(Context context) {
    super(context);
}

public SquareLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
}

 @Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);
    
    // width wins
    height = width;

    super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
    );
}
}
