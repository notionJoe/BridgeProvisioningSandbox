package com.getnotion.android.bridgeprovisioner.views;


import android.app.Dialog;
import android.content.DialogInterface;
import android.getnotion.android.bridgeprovisioner.R;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Generic success/informational fullscreen dialog fragment with single/double footer button
 * Ok/Cancel interaction
 */
public class SuccessDialogFragment extends DialogFragment {

    public static final String TAG = SuccessDialogFragment.class.getSimpleName();

    private static final String DIALOG_TITLE = "dialogTitle";
    private static final String DIALOG_MESSAGE = "dialogMessage";

    @Bind(R.id.titleTextView)
    TextView mTitleTextView;
    @Bind(R.id.messageTextView)
    TextView mMessageTextView;

    @Bind(R.id.vertical_divider)
    View mVerticalDivider;
    @Bind(R.id.horizontal_divider)
    View mHorizontalDivider;

    @Bind(R.id.button_container)
    View mButtonContainer;
    @Bind(R.id.confirm_button)
    Button mConfirmButton;
    @Bind(R.id.cancel_button)
    Button mCancelButton;

    protected String mTitle;
    protected String mMessage;
    protected String mCancelTitle;
    protected String mConfirmTitle;

    private int statusBarColor;

    private DialogInterface.OnClickListener onClickListener;


    public void setOnClickListener(DialogInterface.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }


    public static SuccessDialogFragment newInstance(String title, String message) {

        Bundle args = new Bundle();
        args.putString(DIALOG_TITLE, title);
        args.putString(DIALOG_MESSAGE, message);
        SuccessDialogFragment fragment = new SuccessDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getDialog() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getDialog().getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mTitle = getArguments().getString(DIALOG_TITLE, getResources().getString(R.string.successScreen_title));
            mMessage = getArguments().getString(DIALOG_MESSAGE, getResources().getString(R.string.successScreen_message));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_success, container, false);
        ButterKnife.bind(this, view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            statusBarColor = getActivity().getWindow().getStatusBarColor(); // grab current color to reset to onDestroyView

            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.notion_teal));
        }

        adjustViewsToState();
        return view;
    }

    /**
     * The system calls this only when creating the layout in a dialog.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);

        // Reset statusBar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(statusBarColor);
        }
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        FragmentTransaction ft = manager.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.add(android.R.id.content, this, tag);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();
    }


    public void adjustViewsToState() {

        mTitleTextView.setText(mTitle);
        mTitleTextView.setVisibility(mTitle == null ? View.GONE : View.VISIBLE);

        mMessageTextView.setText(mMessage != null ? mMessage : ""); // IconTextView doesn't have null check on setText
        mMessageTextView.setVisibility(mMessage == null ? View.GONE : View.VISIBLE);


        // IconButton setText() does not perform a null check
        if (mCancelTitle != null) {
            mCancelButton.setText(mCancelTitle);
            mCancelButton.setVisibility(View.VISIBLE);
        } else {
            mCancelButton.setVisibility(View.GONE);
        }

        if (mConfirmTitle != null) {
            mConfirmButton.setText(mConfirmTitle);
            mConfirmButton.setVisibility(View.VISIBLE);
        } else {
            mConfirmButton.setVisibility(View.GONE);
        }

        mVerticalDivider.setVisibility(mCancelTitle == null ? View.GONE : View.VISIBLE);

        boolean hideButtons = mCancelTitle == null && mConfirmTitle == null;
        mHorizontalDivider.setVisibility(hideButtons ? View.GONE : View.VISIBLE);
        mButtonContainer.setVisibility(hideButtons ? View.GONE : View.VISIBLE);

        RelativeLayout.LayoutParams confirmParams = (RelativeLayout.LayoutParams) mConfirmButton.getLayoutParams();
        RelativeLayout.LayoutParams cancelParams = (RelativeLayout.LayoutParams) mCancelButton.getLayoutParams();
        if (mCancelTitle == null && mConfirmTitle != null) {
            // adjust layout rules to have confirm button span full width
            confirmParams.removeRule(RelativeLayout.RIGHT_OF);
            confirmParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            mConfirmButton.setLayoutParams(confirmParams); // causes layout update
        } else if (mCancelTitle != null && mConfirmTitle == null) {
            // adjust layout rules to have cancel button span full width
            cancelParams.removeRule(RelativeLayout.START_OF);
            cancelParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
            mVerticalDivider.setVisibility(View.GONE);
            mConfirmButton.setVisibility(View.GONE);
            mCancelButton.setLayoutParams(cancelParams);
        } else  { // adjust layout rules to be half width
            confirmParams.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
            confirmParams.addRule(RelativeLayout.RIGHT_OF, mVerticalDivider.getId());
            mConfirmButton.setLayoutParams(confirmParams); // causes layout update
        }
    }

    // TODO: Click go through the main dialog, this is quick fic -- dig into why later. Maybe has to do with fullscreen?
    @OnClick(R.id.rootContainer)
    public void onContainerClicker(View v) {
    }

    @OnClick(R.id.cancel_button)
    public void onCancelClick(View v) {
        if (onClickListener != null) {
            onClickListener.onClick(SuccessDialogFragment.this.getDialog(), DialogInterface.BUTTON_NEGATIVE);
        }
        dismiss();
    }

    @OnClick(R.id.confirm_button)
    public void onConfirmClick(View v) {
        if (onClickListener != null) {
            onClickListener.onClick(SuccessDialogFragment.this.getDialog(), DialogInterface.BUTTON_POSITIVE);
        }
        dismiss();
    }

    /**
     * Builder to create SuccessDialogs with ease of all the potential constructor parameters
     */
    public static class Builder {
        String title;
        String message;
        String cancelTitle;
        String confirmTitle;
        DialogInterface.OnClickListener onClickListener;

        public Builder() {
        }

        public SuccessDialogFragment build() {
            SuccessDialogFragment dialog = SuccessDialogFragment.newInstance(title, message);
            dialog.mCancelTitle = cancelTitle;
            dialog.mConfirmTitle = confirmTitle;
            dialog.onClickListener = onClickListener;
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

        /**
         * Listener for when confirm/cancel is clicked. Please use the following to discern which
         * was pressed:
         * <pre>
         * <code>if (which == DialogInterface.BUTTON_POSITIVE) {
         *      // confirm
         * } else if (which == DialogInterface.BUTTON_NEGATIVE) {
         *      // cancel
         * }</code>
         * </pre>
         *
         * Note: Currently configured to dismiss the dialog after the listener is called
         *
         *  @param listener for when either confirm/cancel is
         * @return Builder
         */
        public Builder onClickListener(DialogInterface.OnClickListener listener) {
            onClickListener = listener;
            return this;
        }
    }
}

