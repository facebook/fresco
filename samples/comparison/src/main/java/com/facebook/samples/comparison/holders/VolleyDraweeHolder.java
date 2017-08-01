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

package com.facebook.samples.comparison.holders;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import com.facebook.samples.comparison.instrumentation.InstrumentedDraweeView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/**
 * This is the Holder class for the RecycleView to use with Volley and Drawee
 */
public class VolleyDraweeHolder extends BaseViewHolder<InstrumentedDraweeView> {

  public VolleyDraweeHolder(
      Context context,
      View parentView,
      InstrumentedDraweeView view, PerfListener perfListener) {
    super(context, parentView, view, perfListener);
  }

  @Override
  protected void onBind(String uri) {
    mImageView.setImageURI(Uri.parse(uri));
  }
}
