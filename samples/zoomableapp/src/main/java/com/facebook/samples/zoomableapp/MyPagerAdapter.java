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

package com.facebook.samples.zoomableapp;

import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.samples.zoomable.ZoomableDraweeView;

class MyPagerAdapter extends PagerAdapter {

  public Object instantiateItem(ViewGroup container, int position) {
    FrameLayout page = (FrameLayout) container.getChildAt(position);
    ZoomableDraweeView zoomableDraweeView = (ZoomableDraweeView) page.getChildAt(0);
    DraweeController controller = Fresco.newDraweeControllerBuilder()
      .setUri("https://www.gstatic.com/webp/gallery/1.sm.jpg")
      .build();
    zoomableDraweeView.setController(controller);
    zoomableDraweeView.setTapListener(createTapListener(position));
    page.requestLayout();
    return page;
  }

  public void destroyItem(ViewGroup container, int position, Object object) {
    FrameLayout page = (FrameLayout) container.getChildAt(position);
    ZoomableDraweeView zoomableDraweeView = (ZoomableDraweeView) page.getChildAt(0);
    zoomableDraweeView.setController(null);
  }

  @Override
  public int getCount() {
    return 3;
  }

  @Override
  public boolean isViewFromObject(View arg0, Object arg1) {
    return arg0 == arg1;
  }

  private GestureDetector.SimpleOnGestureListener createTapListener(final int position) {
    return new GestureDetector.SimpleOnGestureListener() {
      @Override
      public void onLongPress(MotionEvent e) {
        Log.d("MyPagerAdapter", "onLongPress: " + position);
      }
    };
  }
}
