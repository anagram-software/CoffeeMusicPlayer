package com.udeshcoffee.android.views;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Udathari on 2/21/2017.
 */

public class NonClickableToolbar extends Toolbar {

    public NonClickableToolbar(Context context) {
        super(context);
    }

    public NonClickableToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NonClickableToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}