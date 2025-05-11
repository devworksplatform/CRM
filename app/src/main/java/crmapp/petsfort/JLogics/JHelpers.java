package crmapp.petsfort.JLogics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class JHelpers {

    public static String capitalize(String text) {
        if (text == null) {
            return null;
        }
        if (text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] words = text.split("\\s+"); // Split by any whitespace

        for (String word : words) {
            if (!word.isEmpty()) {
                // Capitalize the first letter, convert the rest to lowercase, and append.
                String firstLetter = word.substring(0, 1).toUpperCase();
                String restOfWord = word.substring(1).toLowerCase();
                result.append(firstLetter).append(restOfWord).append(" ");
            }
        }
        // Remove the trailing space and return the result.
        return result.toString().trim();
    }
    public static String convertUtcToIstAndFormat(String utcDateTimeString) {
        try {
            // Parse the UTC date-time string into an Instant
            java.time.Instant instant = java.time.Instant.parse(utcDateTimeString);

            // Define the Indian Standard Time (IST) timezone
            java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");

            // Convert the Instant to LocalDateTime in IST
            java.time.LocalDateTime istDateTime = java.time.LocalDateTime.ofInstant(instant, istZone);

            // Define the desired format
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a", java.util.Locale.ENGLISH);

            // Format the IST LocalDateTime and return the string
            return istDateTime.format(formatter);

        } catch (Exception e) {
            return "UTC: "+utcDateTimeString;
        }
    }

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


    public static class LoadingOverlay {

        private android.widget.ProgressBar progressBar;

        public void show(android.content.Context context) {
            if (!(context instanceof android.app.Activity)) return;

            android.app.Activity activity = (android.app.Activity) context;
            android.widget.FrameLayout rootLayout = (android.widget.FrameLayout) activity.getWindow().getDecorView();

            // Create full-screen overlay that blocks touches
            android.widget.FrameLayout overlay = new android.widget.FrameLayout(context) {
                @Override
                public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
                    return true; // Block all touch events from passing through
                }
            };

            android.widget.FrameLayout.LayoutParams overlayParams =
                    new android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    );
            overlay.setLayoutParams(overlayParams);
            overlay.setClickable(true); // Ensures it can intercept touch

            // Create and center the ProgressBar
            progressBar = new android.widget.ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
            android.widget.FrameLayout.LayoutParams progressParams =
                    new android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                    );
            progressParams.gravity = android.view.Gravity.CENTER;
            progressBar.setLayoutParams(progressParams);

            overlay.addView(progressBar);
            rootLayout.addView(overlay);

            // Tag overlay for removal
            progressBar.setTag(overlay);

            // Hide after 100ms
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    hide(context);
                }
            }, 100);
        }

        public void hide(android.content.Context context) {
            if (!(context instanceof android.app.Activity)) return;
            android.view.View overlay = (android.view.View) progressBar.getTag();
            if (overlay != null && overlay.getParent() != null) {
                ((android.view.ViewGroup) overlay.getParent()).removeView(overlay);
            }
        }
    }

}
