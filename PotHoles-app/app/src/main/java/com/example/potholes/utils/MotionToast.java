package com.example.potholes.utils;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.eftimoff.androipathview.PathView;
import com.example.potholes.R;

import org.jetbrains.annotations.NotNull;

public class MotionToast extends Toast {

    private MotionToast(Context context) {
        super(context);
    }

    public static void display(@NotNull final Activity activity,
                               final int messageId,
                               final MotionToastType type) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MotionToast motionToast = new MotionToast(activity);
                switch (type) {
                    case INFO_MOTION_TOAST:
                        motionToast.setView(View.inflate(activity, R.layout.toast_info, null));
                        break;
                    case SUCCESS_MOTION_TOAST:
                        motionToast.setView(View.inflate(activity, R.layout.toast_success, null));
                        break;
                    case WARNING_MOTION_TOAST:
                        motionToast.setView(View.inflate(activity, R.layout.toast_warning, null));
                        break;
                    case ERROR_MOTION_TOAST:
                        motionToast.setView(View.inflate(activity, R.layout.toast_error, null));
                        break;
                    case FOUND_HOLE_TOAST:
                        motionToast.setView(View.inflate(activity,R.layout.toast_foundhole,null));
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                motionToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 25);
                motionToast.setDuration(LENGTH_LONG);
                TextView textViewMessage = motionToast.getView().findViewById(R.id.toast_message);
                textViewMessage.setText(activity.getResources().getString(messageId));
                final PathView icon = motionToast.getView().findViewById(R.id.toast_icon);
                icon.setFillAfter(true);
                PathView.AnimatorBuilder animatorBuilder = icon.getPathAnimator();
                animatorBuilder.delay(100);
                animatorBuilder.duration(3500);
                animatorBuilder.interpolator(new AccelerateDecelerateInterpolator());
                animatorBuilder.start();
                motionToast.show();
            }
        });
    }

}
