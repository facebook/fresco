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


package com.facebook.samples.comparison.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;

import com.facebook.samples.comparison.instrumentation.Instrumented;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/**
 * Base class for the list view adapter.
 *
 * <p>Subclasses are responsible for downloading images in the correct image loader,
 * and creating Views that can host that loader's views.
 *
 * <p>The {@link #clear()} method should also be overridden to also clear the
 * loader's memory cache.
 */
public abstract class ImageListAdapter<V extends View & Instrumented>
  extends ArrayAdapter<String> {

  private final PerfListener mPerfListener;

  public ImageListAdapter(Context context, int resource, PerfListener perfListener) {
    super(context, resource);
    mPerfListener = perfListener;
  }

  private int calcDesiredSize(int parentWidth, int parentHeight) {
    int orientation = getContext().getResources().getConfiguration().orientation;
    int desiredSize = (orientation == Configuration.ORIENTATION_LANDSCAPE) ?
        parentHeight / 2 : parentHeight / 3;
    return Math.min(desiredSize, parentWidth);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    V view = getViewClass().isInstance(convertView) ? (V) convertView : createView();

    int size = calcDesiredSize(parent.getWidth(), parent.getHeight());
    updateViewLayoutParams(view, size, size);

    String uri = getItem(position);
    view.initInstrumentation(uri, mPerfListener);
    bind(view, uri);
    return view;
  }

  private static void updateViewLayoutParams(View view, int width, int height) {
    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
    if (layoutParams == null || layoutParams.height != width || layoutParams.width != height) {
      layoutParams = new AbsListView.LayoutParams(width, height);
      view.setLayoutParams(layoutParams);
    }
  }

  /** The View subclass used by this adapter's image loader. */
  protected abstract Class<V> getViewClass();

  /** Create a View instance of the correct type. */
  protected abstract V createView();

  /** Load an image of the specified uri into the view, asynchronously. */
  protected abstract void bind(V view, String uri);

  /** Releases any resources and tears down the adapter. */
  public abstract void shutDown();
}
