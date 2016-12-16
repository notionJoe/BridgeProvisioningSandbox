package com.getnotion.android.bridgeprovisioner.views;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.getnotion.android.bridgeprovisioner.R;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheMDP on 9/22/15.
 */
public abstract class AbstractSlideUpDialog extends Dialog {

    public interface OnCancelListener {
        void onCancel(Dialog dialog);
    }
    OnCancelListener mCancelListener;

    ColorTheme mColorTheme;

    List<View> mColorDependentViews;

    AbstractSlideUpDialog(Context context) {
        super(context, R.style.DialogSlideAnim);
    }

    AbstractSlideUpDialog(Context context, int theme) {
        super(context, theme);
    }

    AbstractSlideUpDialog(Context context, boolean cancelable, DialogInterface.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    /**
     * You may be interested in adding a setContentView(View view) as well
     * @return layout resource id
     */
    abstract int getContentViewResId();

    @Override
    public void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);

        // Make the dialog width span the whole screen BEFORE setting content view
        getWindow().setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);

        // Make the dialog view section show up at the bottom
        getWindow().setGravity(Gravity.BOTTOM);

        setContentView(R.layout.slide_up_dialog_abstract_container);

        FrameLayout contentContainer = (FrameLayout)findViewById(R.id.content_container);
        int contentView = getContentViewResId();
        getLayoutInflater().inflate(contentView, contentContainer, true);

        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mCancelListener != null) {
                    mCancelListener.onCancel(AbstractSlideUpDialog.this);
                }
            }
        });

        mColorDependentViews = new ArrayList<>();
        mColorDependentViews.add(contentContainer);

        // After the content view is connected, set window attributes to have full width
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);
    }

    void cancelClicked() {
        if (mCancelListener != null) {
            mCancelListener.onCancel(this);
        } else {
            dismiss();
        }
    }

    public void setColorTheme(ColorTheme theme) {
        mColorTheme = theme;
        for (View v : mColorDependentViews) {
            v.setBackgroundResource(mColorTheme.mBackgroundColorResId);
        }
    }

    public enum ColorTheme {
        DARK_GREY(R.color.notion_dark_grey, R.drawable.rect_button_dark_grey),
        RED(R.color.notion_red, R.drawable.rect_button_red);

        int mBackgroundColorResId;
        int mButtonBackgroundResId;
        ColorTheme(int backgroundColorResId, int buttonBackgroundResId) {
            mBackgroundColorResId = backgroundColorResId;
            mButtonBackgroundResId = buttonBackgroundResId;
        }
    }
}
