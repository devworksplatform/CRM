package shopline.com.JLogics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;
public class JHelpers {


    public static String formatDoubleToRupeesString(double value) {
        // Use Locale for India to get the correct grouping separators
        Locale indiaLocale = new Locale("en", "IN");

        // Get the number instance for the locale
        NumberFormat numberFormat = NumberFormat.getNumberInstance(indiaLocale);

        // Configure DecimalFormat specifics if needed (though defaults often work well)
        if (numberFormat instanceof DecimalFormat) {
            DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
            // Ensure trailing zeros aren't always shown, but allow up to 2 decimal places
            decimalFormat.setMaximumFractionDigits(2);
            // The default minimum fraction digits is 0, which handles the ".0" -> "" case.
            // decimalFormat.setMinimumFractionDigits(0); // Usually default
        }


        // Format the number
        return numberFormat.format(value);
    }


    public static void runAfterDelay(android.app.Activity activity, int _delay, Callbacker.Timer callbackTimer) {
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callbackTimer.onEnd();
                    }
                });
            }
        }, _delay);
    }

    public static void TransitionManager(final android.view.View _view, final double _duration) {
        android.widget.LinearLayout viewgroup =(  android.widget.LinearLayout) _view;
        android.transition.AutoTransition autoTransition = new android.transition.AutoTransition();
        autoTransition.setDuration((long)_duration);
        autoTransition.setInterpolator(new android.view.animation.DecelerateInterpolator());
        android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
    }



    public static  class JValueAnimator {

        public static void animate(int start, int end, int durationMs, Callbacker.onAnimateUpdate onAnimateUpdate) {
            ValueAnimator animator = ValueAnimator.ofInt(start, end);
            animator.setDuration(durationMs);
            animator.addUpdateListener(animation -> {
                int animatedValue = (int) animation.getAnimatedValue();
                onAnimateUpdate.onUpdate(animatedValue);
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    onAnimateUpdate.onEnd();
                }
            });
            animator.start();
        }
    }

}
