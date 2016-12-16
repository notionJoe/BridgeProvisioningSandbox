/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.views;

import android.content.Context;
import android.getnotion.android.bridgeprovisioner.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.ViewSwitcher;


/**
 * DataEntry Field that switches between an overview (collapsed) of
 * its detail view's input field values, to the detail view (expanded) that can
 * contain multiple different input fields. See for a good example --
 * it displays "FirstName LastName" on the overview and has separate input fields
 * for each FirstName & LastName in its details view.
 *
 * TODO: Start yankin' up common logic among all the fields to this guy. E.g. editField(),
 * expand(), collapse(), etc. Slowly but surely... :)
 */
public abstract class CollapsibleField extends ViewSwitcher {

    protected OnFieldSubmittedListener onFieldSubmittedListener;
    protected OnFieldValidationChangedListener onFieldValidationChangedListener;

    // Interface for relaying when a field has been submitted (collapsed)
    public interface OnFieldSubmittedListener {
        /**
         * Returns the instance of this field to allow for a single listener to act on multiple
         * CollapsibleFields and dispatch as necessary
         * @param collapsibleField CollapsibleField that was just submitted
         */
        void onFieldSubmitted(CollapsibleField collapsibleField);
    }

    // Interface for relaying when a field becomes valid/invalid (as per their individually assigned validation rules)
    public interface OnFieldValidationChangedListener {
        void fieldValid(CollapsibleField collapsibleField);
        void fieldInvalid(CollapsibleField collapsibleField);
    }

    protected boolean matchParentHeightWhenEditing = true;

    public CollapsibleField(Context context) {
        super(context);
    }

    public CollapsibleField(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the text of the supposed textView if the string != null....
     */
    public void setTextIfNotNull(TextView textView, String text) {
        if (text != null) {
            if (text.isEmpty()) {
                textView.setVisibility(GONE);
            } else {
                textView.setText(text);
            }
        }
    }

    public abstract boolean isValid();

    /**
     * Method for the implementation to optionally focus the first field within it's details view.
     * This will be invoked at the end of each {@link #editField()} call (if it's not already expanded).
     */
    protected abstract void focusFirstField();


    /**
     * Switches to the detail view where the field can be edited
     * Note: Probably should have done setCollapsed(boolean isCollapsed) instead, but oh well...
     */
    public void editField() {
        // Switch the view -- the fragment_***.xml styles are set up to have the details as 0, and overview as 1
        if (getDisplayedChild() != 0) {
            showNext();
        }
        focusFirstField();

        // Make sure we're not filling our parent's heigh when collapsed
        if (matchParentHeightWhenEditing) {
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            setLayoutParams(params);
        }
    }

    /**
     * Shows/hides the editIcon when field is collapsed -- should be impl in child as that's where
     * layouts are defined, but this is ok for now (child must declare edit icon id as 'editIcon'
     * @param isVisible whether or not icon is visible
     */
    public void setEditIconVisible(boolean isVisible) {
        View editIcon = findViewById(R.id.editIcon);
        if (editIcon != null) {
            editIcon.setVisibility(isVisible ? VISIBLE : INVISIBLE);
        }
    }

    /**
     * Method for the implementation to set all values of its collapsed view from that of its details view.
     * This will be invoked before switching views in on each {@link #collapseField()} call (if it's
     * not already collapsed).
     */
    protected abstract void setDetailsViewValues();

    /**
     * Switches from the detail view to the collapsed view and updates the collapsed view w/ the
     * latest values.
     * Note: Probably should have done setCollapsed(boolean isCollapsed) instead, but oh well...
     */
    public void collapseField() {
        // Switch the view -- the fragment_***.xml styles are set up to have the details as 0, and overview as 1
        if (getDisplayedChild() != 1) {
            setDetailsViewValues();
            showNext();

            // Make sure we're not filling our parent's height when collapsed
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            setLayoutParams(params);
        }
    }

    /**
     * Returns whether or not the field is currently collapsed. Within the view xml, the first child
     * of the ViewSwitcher is always the details view
     * @return boolean is the field collapsed
     */
    public boolean isCollapsed() {
        return (getDisplayedChild() == 1);

    }

    /**
     * Sets the onFieldSubmittedListener -- rudimentary impl, only one at a time and additional sets overwrite previous
     * @param onFieldSubmittedListener listener to be called when the field is submitted (collapsed)
     */
    public void setOnFieldSubmittedListener(OnFieldSubmittedListener onFieldSubmittedListener) {
        this.onFieldSubmittedListener = onFieldSubmittedListener;
    }

    /**
     * Sets the onFieldValidationChangedListener-- rudimentary impl, only one at a time and additional sets overwrite previous
     * @param onFieldValidationChangedListener listener to be called when the field is validation state has changed
     */
    public void setOnFieldValidationChangedListener(OnFieldValidationChangedListener onFieldValidationChangedListener) {
        this.onFieldValidationChangedListener = onFieldValidationChangedListener;
    }

    /**
     * Show the soft keyboard for the specified view
     * @param view view to show the keyboard for
     */
    public void showSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Hide the soft keyboard for the specified view
     * @param view view to hide the keyboard for
     */
    public void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if(imm.isAcceptingText()) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Whether or not this view matches its parent's height when expanded
     *
     * @return bool
     */
    public boolean isMatchParentHeightWhenEditing() {
        return matchParentHeightWhenEditing;
    }

    /**
     * Set whether or not this view should match its parent's height when expanded
     * @param matchParentHeightWhenEditing bool
     */
    public void setMatchParentHeightWhenEditing(boolean matchParentHeightWhenEditing) {
        this.matchParentHeightWhenEditing = matchParentHeightWhenEditing;
    }
}
