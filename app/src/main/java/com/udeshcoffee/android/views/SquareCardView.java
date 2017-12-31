package com.udeshcoffee.android.views;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

/**
 * Created by Udathari on 2/21/2017.
 */

public class SquareCardView extends CardView {

    public SquareCardView(Context context) {
        super(context);
    }

    public SquareCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int side = Math.min(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(side, side); // This is the key that will make the height equivalent to its width
    }
}