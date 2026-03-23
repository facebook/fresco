/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;

import android.widget.TextView;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import com.google.android.material.internal.NavigationMenuItemView;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ShowcaseRunTest {

  @Rule
  public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

  @Test
  public void testRunTheShowcaseApp() {
    openScreenFromMenu(R.string.welcome_nav_title);
    onView(withId(R.id.content_main)).check(matches(isDisplayed()));
    onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
    onView(allOf(isAssignableFrom(TextView.class), withParent(withId(R.id.toolbar))))
        .check(matches(withText(R.string.welcome_title)));
  }

  private void openScreenFromMenu(int title) {
    onView(withContentDescription(R.string.navigation_drawer_open)).perform(click());
    onView(allOf(withParent(isAssignableFrom(NavigationMenuItemView.class)), withText(title)))
        .perform(click());
  }
}
