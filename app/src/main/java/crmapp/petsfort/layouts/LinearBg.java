package crmapp.petsfort.layouts;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import crmapp.petsfort.R;

public class LinearBg extends LinearLayout {

    private LinearLayout container;

    public LinearBg(Context context) {
        super(context);
        init(context);
    }

    public LinearBg(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LinearBg(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.linear_bg, this, true);
        container = findViewById(R.id.container);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (container != null && child != container) {
            container.addView(child, index, params);
        } else {
            super.addView(child, index, params);
        }
    }
}
