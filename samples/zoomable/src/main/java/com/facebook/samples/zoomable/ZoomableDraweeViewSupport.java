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

package com.facebook.samples.zoomable;

import android.content.Context;
import android.util.AttributeSet;
import com.facebook.drawee.generic.GenericDraweeHierarchy;

/**
 * DraweeView that has zoomable capabilities.
 * <p>
 * Once the image loads, pinch-to-zoom and translation gestures are enabled.
 */
public class ZoomableDraweeViewSupport extends ZoomableDraweeView {

  private static final Class<?> TAG = ZoomableDraweeViewSupport.class;

  public ZoomableDraweeViewSupport(Context context, GenericDraweeHierarchy hierarchy) {
    super(context, hierarchy);
  }

  public ZoomableDraweeViewSupport(Context context) {
    super(context);
  }

  public ZoomableDraweeViewSupport(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ZoomableDraweeViewSupport(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected Class<?> getLogTag() {
    return TAG;
  }

  @Override
  protected ZoomableController createZoomableController() {
    return AnimatedZoomableControllerSupport.newInstance();
  }
}
