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
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.samples.zoomable.DoubleTapGestureListener;
import com.facebook.samples.zoomable.ZoomableDraweeView;

class MyPagerAdapter extends PagerAdapter {

  private static final String[] SAMPLE_URIS = {
          "https://www.gstatic.com/webp/gallery/1.sm.jpg",
          "https://www.gstatic.com/webp/gallery/2.sm.jpg",
          "https://www.gstatic.com/webp/gallery/3.sm.jpg",
          "https://www.gstatic.com/webp/gallery/4.sm.jpg",
          "https://www.gstatic.com/webp/gallery/5.sm.jpg",
  };

  private final int mItemCount;
  private boolean mAllowSwipingWhileZoomed = true;

  public MyPagerAdapter(int itemCount) {
    mItemCount = itemCount;
  }

  public void setAllowSwipingWhileZoomed(boolean allowSwipingWhileZoomed) {
    mAllowSwipingWhileZoomed = allowSwipingWhileZoomed;
  }

  public boolean allowsSwipingWhileZoomed() {
    return mAllowSwipingWhileZoomed;
  }

  public void toggleAllowSwipingWhileZoomed() {
    mAllowSwipingWhileZoomed = !mAllowSwipingWhileZoomed;
  }

  @Override
  public Object instantiateItem(ViewGroup container, int position) {
    FrameLayout page = (FrameLayout) container.getChildAt(position);
    if (page == null) {
      return null;
    }
    ZoomableDraweeView zoomableDraweeView =
            (ZoomableDraweeView) page.findViewById(R.id.zoomableView);
    zoomableDraweeView.setAllowTouchInterceptionWhileZoomed(mAllowSwipingWhileZoomed);
    // needed for double tap to zoom
    zoomableDraweeView.setIsLongpressEnabled(false);
    zoomableDraweeView.setTapListener(new DoubleTapGestureListener(zoomableDraweeView));
    DraweeController controller = Fresco.newDraweeControllerBuilder()
      .setUri(SAMPLE_URIS[position % SAMPLE_URIS.length])
      .setCallerContext("ZoomableApp-MyPagerAdapter")
      .build();
    zoomableDraweeView.setController(controller);
    page.requestLayout();
    return page;
  }

  @Override
  public void destroyItem(ViewGroup container, int position, Object object) {
    FrameLayout page = (FrameLayout) container.getChildAt(position);
    ZoomableDraweeView zoomableDraweeView = (ZoomableDraweeView) page.getChildAt(0);
    zoomableDraweeView.setController(null);
  }

  @Override
  public int getCount() {
    return mItemCount;
  }

  @Override
  public boolean isViewFromObject(View arg0, Object arg1) {
    return arg0 == arg1;
  }

  @Override
  public int getItemPosition(Object object) {
    // We want to create a new view when we call notifyDataSetChanged() to have the correct behavior
    return POSITION_NONE;
  }
}
