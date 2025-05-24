package crmapp.petsfort.layouts;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import crmapp.petsfort.R;

public class LinearBg extends LinearLayout {

    private LinearLayout container;

    int[][] gradientColors1 = new int[][] {
            {0xFFFFFFFF, 0xFFF8F8F8},  // Pure white to very light gray-white
            {0xFFFDFDFD, 0xFFF0F0F0},  // Almost white to soft gray
            {0xFFFAFAFA, 0xFFEFEFEF},  // Soft white to gentle light gray
            {0xFFFFFFFF, 0xFFF5F5F5},  // Bright white to light ivory
            {0xFFF9F9F9, 0xFFF2F2F2},  // Soft white shades
            {0xFFFCFCFC, 0xFFF7F7F7},  // Very subtle white gradient
            {0xFFFEFEFE, 0xFFF9F9F9},  // Almost pure whites
            {0xFFF7F7F7, 0xFFF3F3F3},  // Gentle white gradients
            {0xFFF8F8F8, 0xFFEEEEEE},  // Clean whites to soft grays
            {0xFFFFFFFF, 0xFFFDFDFD},  // Bright white to almost white
    };

    int[][] gradientColors = new int[][] {
            {0xFFFFFFFF, 0xFFCCCCCC},  // Pure white to soft mid gray
            {0xFFFDFDFD, 0xFFBFBFBF},  // Almost white to muted mid gray
            {0xFFFAFAFA, 0xFFB3B3B3},  // Soft white to gentle mid gray
            {0xFFFFFFFF, 0xFFD1D1D1},  // Bright white to light gray
            {0xFFF9F9F9, 0xFFB8B8B8},  // Soft white to medium light gray
            {0xFFFCFCFC, 0xFFBFBFBF},  // Very subtle white to medium gray
            {0xFFFEFEFE, 0xFFC4C4C4},  // Almost pure white to medium gray
            {0xFFF7F7F7, 0xFFADADAD},  // Gentle white to medium gray
            {0xFFF8F8F8, 0xFFAAAAAA},  // Clean white to medium gray
            {0xFFFFFFFF, 0xFFBEBEBE},  // Bright white to medium gray
    };

    int[][] gradientColors2 = new int[][] {
            {0xFFFFFFFF, 0xFFE1E1E1},  // Pure white to soft light gray
            {0xFFFDFDFD, 0xFFD6D6D6},  // Almost white to muted gray
            {0xFFFAFAFA, 0xFFCFCFCF},  // Soft white to gentle mid gray
            {0xFFFFFFFF, 0xFFE6E6E6},  // Bright white to light ivory-gray
            {0xFFF9F9F9, 0xFFDCDCDC},  // Soft white shades to subtle gray
            {0xFFFCFCFC, 0xFFDEDEDE},  // Very subtle white gradient to light gray
            {0xFFFEFEFE, 0xFFE3E3E3},  // Almost pure whites to light gray
            {0xFFF7F7F7, 0xFFCECECE},  // Gentle white gradients to muted gray
            {0xFFF8F8F8, 0xFFC7C7C7},  // Clean whites to soft gray
            {0xFFFFFFFF, 0xFFDCDCDC},  // Bright white to very light gray
    };


    private int currentIndex = 0;
    private GradientDrawable gradientDrawable;
    private ValueAnimator animator;

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

        // Initial gradient drawable with first gradient colors
        gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                gradientColors[currentIndex]
        );
        setBackground(gradientDrawable);

        // Start animation loop
        animateGradient();
    }

    private void animateGradient() {
        int nextIndex = (currentIndex + 1) % gradientColors.length;

        final int startColor1 = gradientColors[currentIndex][0];
        final int startColor2 = gradientColors[currentIndex][1];
        final int endColor1 = gradientColors[nextIndex][0];
        final int endColor2 = gradientColors[nextIndex][1];

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(30000); // Slow 30s transition
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);

        ArgbEvaluator evaluator = new ArgbEvaluator();

        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();

            int newColor1 = (int) evaluator.evaluate(fraction, startColor1, endColor1);
            int newColor2 = (int) evaluator.evaluate(fraction, startColor2, endColor2);

            gradientDrawable.setColors(new int[]{newColor1, newColor2});
            setBackground(gradientDrawable);
        });

        animator.addListener(new android.animation.Animator.AnimatorListener() {
            @Override public void onAnimationStart(android.animation.Animator animation) {}
            @Override public void onAnimationEnd(android.animation.Animator animation) {}
            @Override public void onAnimationCancel(android.animation.Animator animation) {}
            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {
                currentIndex = (currentIndex + 1) % gradientColors.length;
            }
        });

        animator.start();
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
