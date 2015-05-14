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

package com.facebook.samples.comparison.test;

import android.graphics.Point;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.view.Display;
import android.widget.ListView;
import android.widget.Spinner;

import com.facebook.common.logging.FLog;
import com.facebook.samples.comparison.MainActivity;
import com.facebook.samples.comparison.R;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instrumentation test that tests glide, picasso, uil and volley in the sample
 * app by scrolling down SCROLLS times.
 */
public class ScrollTest extends ActivityInstrumentationTestCase2<MainActivity> {
  private static final int SCROLLS = 30;
  private static final int SCROLL_TIME_MS = 1000;
  private static final int BEFORE_SCROLL_TIME_MS = 500;
  private static final int WAIT_FOR_IMAGES_INTERCHECK_MS = 1000;
  private static final int WAIT_BEFORE_TEST_END_MS = 5000;

  private MainActivity mActivity;
  private ListView mImageList;
  private Spinner mLoaderSelect;

  public ScrollTest() {
    super(MainActivity.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mActivity = getActivity();
    mImageList = (ListView) mActivity.findViewById(R.id.image_list);
    mLoaderSelect = (Spinner) mActivity.findViewById(R.id.loader_select);
    FLog.setMinimumLoggingLevel(FLog.INFO);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testFresco() throws Exception {
    runScenario(MainActivity.FRESCO_INDEX, true);
  }

  public void testFrescoOkhttp() throws Exception {
    runScenario(MainActivity.FRESCO_OKHTTP_INDEX, true);
  }

  public void testGlide() throws Exception {
    runScenario(MainActivity.GLIDE_INDEX, false);
  }

  public void testPicasso() throws Exception {
    runScenario(MainActivity.PICASSO_INDEX, false);
  }

  public void testUil() throws Exception {
    runScenario(MainActivity.UIL_INDEX, false);
  }

  public void testVolley() throws Exception {
    runScenario(MainActivity.VOLLEY_INDEX, false);
  }

  public void testDraweeVolley() throws Exception {
    runScenario(MainActivity.VOLLEY_INDEX, true);
  }

  /**
   * Runs the test for given library.
   */
  private void runScenario(int libraryIndex, boolean useDrawee) throws Exception {
    disableAnimatedImages();
    setUseDrawee(useDrawee);
    selectFramework(libraryIndex);
    TouchUtils.tapView(this, mImageList);
    waitForImages();
    scrollMultipleTimes(SCROLLS);
    Thread.sleep(WAIT_BEFORE_TEST_END_MS);
  }

  /**
   * Disables animated images in list view.
   */
  private void disableAnimatedImages() {
    getInstrumentation().runOnMainSync(
        new Runnable() {
          @Override
          public void run() {
            mActivity.setAllowAnimations(false);
          }
        });
  }

  /**
   * Disables or enables Drawee.
   */
  private void setUseDrawee(final boolean useDrawee) {
    getInstrumentation().runOnMainSync(
        new Runnable() {
          @Override
          public void run() {
            mActivity.setUseDrawee(useDrawee);
          }
        });
  }

  /**
   * Selects give library in the select component.
   */
  private void selectFramework(final int libraryIndex) {
    getInstrumentation().runOnMainSync(
        new Runnable() {
          @Override
          public void run() {
            mLoaderSelect.setSelection(libraryIndex, true);
          }
        });
  }

  /**
   * Waits until the list view is populated with content, that is
   * until list of images is downloaded.
   */
  private void waitForImages() throws Exception {
    final AtomicBoolean mImagesLoaded = new AtomicBoolean();
    while (!mImagesLoaded.get()) {
      Thread.sleep(WAIT_FOR_IMAGES_INTERCHECK_MS);
      getInstrumentation().runOnMainSync(
          new Runnable() {
            @Override
            public void run() {
              mImagesLoaded.set(mImageList.getAdapter().getCount() > 0);
            }
          });
    }
  }

  /**
   * Scrolls the list view given number of times.
   */
  private void scrollMultipleTimes(int times) throws Exception {
    final int height = getDisplayHeight();
    for (int i = 0; i < times; ++i) {
      Thread.sleep(BEFORE_SCROLL_TIME_MS);
      getInstrumentation().runOnMainSync(
          new Runnable() {
            @Override
            public void run() {
              mImageList.smoothScrollBy(height / 2, SCROLL_TIME_MS);
            }
          });
      Thread.sleep(SCROLL_TIME_MS);
    }
  }

  /**
   * Determines display's height.
   */
  private int getDisplayHeight() {
    Display display = mActivity.getWindowManager().getDefaultDisplay();
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
      return display.getHeight();
    } else {
      final Point size = new Point();
      display.getSize(size);
      return size.y;
    }
  }
}
