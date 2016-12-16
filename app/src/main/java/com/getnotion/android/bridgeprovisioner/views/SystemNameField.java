/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.views;

import android.content.Context;
import android.getnotion.android.bridgeprovisioner.R;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * CollapsibleField for displaying/editing the System Name
 */
public class SystemNameField extends CollapsibleField {

    private static final String TAG = SystemNameField.class.getSimpleName();

    // Collapsed View
    @Bind(R.id.systemNameTextView)
    TextView systemNameTextView;
    // Expanded view
    @Bind(R.id.systemNameEditText)
    EditText systemNameEditText;
    @Bind(R.id.systemNameError)
    TextView systemNameError;

    public SystemNameField(Context context, AttributeSet attrs) {
        super(context, attrs);

        setMeasureAllChildren(false);

        // Inflate view/inject view members
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.view_field_system_name, this, true);
        ButterKnife.bind(this, view);

        // Submit on
        systemNameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Validation
                    if (TextUtils.isEmpty(systemNameEditText.getText().toString())) {
                        systemNameError.setVisibility(View.VISIBLE);
                        // Update field styles
                        systemNameEditText.getBackground().setColorFilter(getResources().getColor(R.color.notion_red), PorterDuff.Mode.SRC_ATOP);
                        systemNameEditText.requestFocus();
                        return true;
                    }
                    collapseField();
                    if (onFieldSubmittedListener != null) {
                        onFieldSubmittedListener.onFieldSubmitted(SystemNameField.this);
                    }
                }
                return false;
            }
        });
        // Move cursor to end
        systemNameEditText.post(new Runnable() {
            @Override
            public void run() {
                systemNameEditText.setSelection(systemNameEditText.getText().length());
            }
        });
    }

    @Override
    public void collapseField() {
        super.collapseField();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(systemNameEditText.getWindowToken(), 0);
        }
    }

    public String getSystemName() {
        return systemNameEditText.getText().toString();
    }

    @Override
    public boolean isValid() {
        return !TextUtils.isEmpty(systemNameEditText.getText().toString());
    }

    @Override
    protected void focusFirstField() {
        systemNameEditText.requestFocus();
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(systemNameEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    protected void setDetailsViewValues() {
        // Update the fullName field in the overView
        systemNameTextView.setText(systemNameEditText.getText().toString()); // ToString else the underline from suggest will linger
    }
}
