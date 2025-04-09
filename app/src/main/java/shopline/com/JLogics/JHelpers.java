package shopline.com.JLogics;

public class JHelpers {

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

}
