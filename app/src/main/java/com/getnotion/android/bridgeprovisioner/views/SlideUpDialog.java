package com.getnotion.android.bridgeprovisioner.views;

import android.app.Dialog;
import android.content.Context;
import android.getnotion.android.bridgeprovisioner.R;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SlideUpDialog extends AbstractSlideUpDialog {

    @Bind({R.id.cancel_button, R.id.confirm_button})
    List<View> mColorDependentButtons;

    @Bind(R.id.cancel_button)
    Button mCancelButton;
    @Bind(R.id.confirm_button)
    Button mConfirmButton;
    @Bind(R.id.title_textview)
    TextView mTitleTextView;
    @Bind(R.id.message_textview)
    TextView mMessageTextView;
    @Bind(R.id.vertical_divider)
    View mVerticalDivider;
    @Bind(R.id.horizontal_divider)
    View mHorizontalDivider;
    @Bind(R.id.button_container)
    View mButtonContainer;

    private String mTitle;
    private String mMessage;
    private String mCancelTitle;
    private String mConfirmTitle;
    private OnConfirmListener mConfirmListener;

    public interface OnConfirmListener {
        void onConfirm(Dialog dialog);
    }

    /**
     * Making all constructors protected so that it forces future developers
     * to user the builder pattern in {@link com.getnotion.android.views.dialogs.SlideUpDialog.Builder}
     */
    SlideUpDialog(Context context) {
        super(context);
    }

    SlideUpDialog(Context context, int theme) {
        super(context, theme);
    }

    SlideUpDialog(Context context, boolean cancelable, Dialog.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);

        // Makes background transparent
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        adjustViewsToState();

        // Must be called here, since the color dependent views have been laid out
        setColorTheme(mColorTheme);
    }

    public void adjustViewsToState() {

        mTitleTextView.setText(mTitle);
        mTitleTextView.setVisibility(mTitle == null ? View.GONE : View.VISIBLE);

        mMessageTextView.setText(mMessage);
        mMessageTextView.setVisibility(mMessage == null ? View.GONE : View.VISIBLE);

        mCancelButton.setText(mCancelTitle);
        mCancelButton.setVisibility(mCancelTitle == null ? View.GONE : View.VISIBLE);

        mConfirmButton.setText(mConfirmTitle);
        mConfirmButton.setVisibility(mConfirmTitle == null ? View.GONE : View.VISIBLE);

        mVerticalDivider.setVisibility(mCancelTitle == null ? View.GONE : View.VISIBLE);

        boolean hideButtons = mCancelTitle == null && mConfirmTitle == null;
        mHorizontalDivider.setVisibility(hideButtons ? View.GONE : View.VISIBLE);
        mButtonContainer.setVisibility(hideButtons ? View.GONE : View.VISIBLE);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mConfirmButton.getLayoutParams();
        if (mCancelTitle == null && mConfirmTitle != null) {
            // adjust layout rules to have confirm button span full width
            params.removeRule(RelativeLayout.RIGHT_OF);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            mConfirmButton.setLayoutParams(params); // causes layout update
        } else { // adjust layout rules to be half width
            params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.RIGHT_OF, mVerticalDivider.getId());
        }
        mConfirmButton.setLayoutParams(params); // causes layout update
    }

    @OnClick(R.id.cancel_button)
    public void onCancelClick(View v) {
        cancelClicked();
    }

    @OnClick(R.id.confirm_button)
    public void onConfirmClick(View v) {
        if (mConfirmListener != null) {
            mConfirmListener.onConfirm(this);
        } else {
            dismiss();
        }
    }

    @Override
    int getContentViewResId() {
        return R.layout.slide_up_dialog_two_buttons;
    }

    public void setColorTheme(ColorTheme theme) {
        super.setColorTheme(theme);
        for (View v : mColorDependentButtons) {
            v.setBackgroundResource(mColorTheme.mButtonBackgroundResId);
        }
    }

    /**
     * Builder to create SlideUpDialogs with ease of all the potential constructor parameters
     */
    public static class Builder {
        ColorTheme colorTheme = ColorTheme.DARK_GREY;
        String title;
        String message;
        String cancelTitle;
        String confirmTitle;
        OnCancelListener cancelClickListener;
        OnConfirmListener confirmClickListener;

        public Builder() {
        }

        public SlideUpDialog build(Context context) {
            SlideUpDialog dialog = new SlideUpDialog(context);
            dialog.mColorTheme = colorTheme;
            dialog.mTitle = title;
            dialog.mMessage = message;
            dialog.mCancelTitle = cancelTitle;
            dialog.mConfirmTitle = confirmTitle;
            dialog.mCancelListener = cancelClickListener;
            dialog.mConfirmListener = confirmClickListener;
            return dialog;
        }

        public Builder title(String s) {
            title = s;
            return this;
        }

        public Builder message(String s) {
            message = s;
            return this;
        }

        public Builder cancelTitle(String s) {
            cancelTitle = s;
            return this;
        }

        public Builder confirmTitle(String s) {
            confirmTitle = s;
            return this;
        }

        public Builder colorTheme(ColorTheme theme) {
            colorTheme = theme;
            return this;
        }

        public Builder cancelClickListener(OnCancelListener listener) {
            cancelClickListener = listener;
            return this;
        }

        public Builder confirmClickListener(OnConfirmListener listener) {
            confirmClickListener = listener;
            return this;
        }
    }

    /**
     * Creates SlideUpDialog with localized "Okay" and "Cancel" for button titles
     */
    public static class OkCancelBuilder extends Builder {
        public OkCancelBuilder() {
            super();
        }

        @Override
        public SlideUpDialog build(Context context) {
            SlideUpDialog dialog = super.build(context);
            dialog.mCancelTitle = context.getString(R.string.global_cancel);
            dialog.mConfirmTitle = context.getString(R.string.global_confirm);
            return dialog;
        }
    }

    /**
     * Creates SlideUpDialog with localized "Okay" button and no cancel button
     */
    public static class OkBuilder extends Builder {
        public OkBuilder() {
            super();
        }

        @Override
        public SlideUpDialog build(Context context) {
            SlideUpDialog dialog = super.build(context);
            dialog.mCancelTitle = null;
            dialog.mConfirmTitle = context.getString(R.string.global_confirm);
            return dialog;
        }
    }
}
