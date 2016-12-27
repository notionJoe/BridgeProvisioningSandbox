package com.getnotion.android.bridgeprovisioner;

import android.getnotion.android.bridgeprovisioner.R;
import android.support.test.espresso.*;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.getnotion.android.bridgeprovisioner.activities.AddNewBridgeActivity;
import com.getnotion.android.bridgeprovisioner.activities.LoginActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

@RunWith(AndroidJUnit4.class)
public class AddNewBridgeActivityTest {

    @Rule
    public ActivityTestRule<AddNewBridgeActivity> activityTestRule = new ActivityTestRule<>(AddNewBridgeActivity.class);

//    private ProvisioningIdleResource provisioningIdleResource;

    @Before
    public void registerIntentServiceIdlingResource() {
//        provisioningIdleResource = new ProvisioningIdleResource(activityTestRule.getActivity());
//        IdlingPolicies.setIdlingResourceTimeout(120, TimeUnit.SECONDS);
//        Espresso.registerIdlingResources(provisioningIdleResource);
    }

    @After
    public void unregisterIntentServiceIdlingResource() {
//        Espresso.unregisterIdlingResources(provisioningIdleResource);
    }

    @Test
    public void testLaunch() throws Exception {
        onView(withId(R.id.addNewBridgeFooterButton))
                .check(matches(withText(containsString("Select network"))));

        onView(withId(R.id.addNewBridgeFooterButton)).perform(click());

        onView(withId(R.id.addNewBridgeFooterButton))
                .check(matches(withText(containsString("Configure Bridge"))));

        onView(withId(R.id.systemNetworkFieldNetworkChooserTextView)).perform(click());

        onView(withText("LightHouse_West")).inRoot(isDialog()).perform(click());

        onView(withId(R.id.systemNetworkFieldPasswordEditText)).perform(typeTextIntoFocusedView("unpile_ongoing_jamb!!"));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.addNewBridgeFooterButton)).check(matches(withText(containsString("Configure Bridge"))));

        onView(withId(R.id.addNewBridgeFooterButton)).perform(click());

        onView(withId(R.id.addNewBridgeFooterButton)).check(matches(withText(containsString("Bridge Provisioned!"))));

    }
}