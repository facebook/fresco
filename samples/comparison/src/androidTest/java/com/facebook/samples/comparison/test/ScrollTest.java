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
import android.widget.GridView;
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
  private static final int SCROLLS = 10;
  private static final int SCROLL_TIME_MS = 1000;
  private static final int BEFORE_SCROLL_TIME_MS = 1500;
  private static final int WAIT_FOR_IMAGES_INTERCHECK_MS = 1000;
  private static final int WAIT_BEFORE_TEST_END_MS = 5000;

  private MainActivity mActivity;
  private GridView mImageList;
  private Spinner mLoaderSelect;
  private Spinner mSourceSelect;

  public ScrollTest() {
    super(MainActivity.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mActivity = getActivity();
    mImageList = (GridView) mActivity.findViewById(R.id.image_grid);
    mLoaderSelect = (Spinner) mActivity.findViewById(R.id.loader_select);
    mSourceSelect = (Spinner) mActivity.findViewById(R.id.source_select);
    FLog.setMinimumLoggingLevel(FLog.INFO);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testFrescoNetwork() throws Exception {
    runScenario(MainActivity.FRESCO_INDEX, MainActivity.NETWORK_INDEX, true);
  }

  public void testFrescoOkhttpNetwork() throws Exception {
    runScenario(MainActivity.FRESCO_OKHTTP_INDEX, MainActivity.NETWORK_INDEX, true);
  }

  public void testGlideNetwork() throws Exception {
    runScenario(MainActivity.GLIDE_INDEX, MainActivity.NETWORK_INDEX, false);
  }

  public void testPicassoNetwork() throws Exception {
    runScenario(MainActivity.PICASSO_INDEX, MainActivity.NETWORK_INDEX, false);
  }

  public void testUilNetwork() throws Exception {
    runScenario(MainActivity.UIL_INDEX, MainActivity.NETWORK_INDEX, false);
  }

  public void testVolleyNetwork() throws Exception {
    runScenario(MainActivity.VOLLEY_INDEX, MainActivity.NETWORK_INDEX, false);
  }

  public void testDraweeVolleyNetwork() throws Exception {
    runScenario(MainActivity.VOLLEY_INDEX, MainActivity.NETWORK_INDEX, true);
  }

  public void testFrescoLocal() throws Exception {
    runScenario(MainActivity.FRESCO_INDEX, MainActivity.LOCAL_INDEX, true);
  }

  public void testFrescoOkhttpLocal() throws Exception {
    runScenario(MainActivity.FRESCO_OKHTTP_INDEX, MainActivity.LOCAL_INDEX, true);
  }

  public void testGlideLocal() throws Exception {
    runScenario(MainActivity.GLIDE_INDEX, MainActivity.LOCAL_INDEX, false);
  }

  public void testPicassoLocal() throws Exception {
    runScenario(MainActivity.PICASSO_INDEX, MainActivity.LOCAL_INDEX, false);
  }

  public void testUilLocal() throws Exception {
    runScenario(MainActivity.UIL_INDEX, MainActivity.LOCAL_INDEX, false);
  }

  /**
   * Runs the test for given library.
   */
  private void runScenario(int libraryIndex, int sourceIndex, boolean useDrawee) throws Exception {
    disableAnimatedImages();
    setUseDrawee(useDrawee);
    selectFramework(libraryIndex);
    selectSource(sourceIndex);
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
   * Selects the source from which to fetch the images.
   */
  private void selectSource(final int sourceIndex) {
    getInstrumentation().runOnMainSync(
        new Runnable() {
          @Override
          public void run() {
            mSourceSelect.setSelection(sourceIndex, true);
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
