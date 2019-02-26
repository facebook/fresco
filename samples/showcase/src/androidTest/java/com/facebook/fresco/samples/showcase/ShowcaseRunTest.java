/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
import static com.facebook.fresco.samples.showcase.DraweeViewHasImageMatcher.hasImage;
import static org.hamcrest.CoreMatchers.allOf;

import android.view.View;
import android.widget.TextView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import com.google.android.material.internal.NavigationMenuItemView;
import org.hamcrest.Matcher;
import org.junit.Ignore;
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

  @Test
  @Ignore /* TODO T34443404, Circle CI instrumentation test fails due to network issue */
  public void testShowImage() {
    openScreenFromMenu(R.string.drawee_simple_title);
    onView(withId(R.id.drawee_view)).perform(waitFor(5000));
    onView(withId(R.id.drawee_view)).check(matches(hasImage()));
  }

  private void openScreenFromMenu(int title) {
    onView(withContentDescription(R.string.navigation_drawer_open)).perform(click());
    onView(allOf(withParent(isAssignableFrom(NavigationMenuItemView.class)), withText(title)))
        .perform(click());
  }

  private static ViewAction waitFor(final long millis) {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return withId(R.id.drawee_view);
      }

      @Override
      public String getDescription() {
        return "Waiting for " + millis + " milliseconds.";
      }

      @Override
      public void perform(UiController uiController, final View view) {
        uiController.loopMainThreadForAtLeast(millis);
      }
    };
  }
}
