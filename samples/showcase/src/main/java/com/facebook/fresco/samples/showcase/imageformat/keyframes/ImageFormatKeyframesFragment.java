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
package com.facebook.fresco.samples.showcase.imageformat.keyframes;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.CustomImageFormatConfigurator;
import com.facebook.fresco.samples.showcase.R;

/**
 * Fragment using a SimpleDraweeView to display a Keyframes animation
 */
public class ImageFormatKeyframesFragment extends BaseShowcaseFragment {

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_keyframes, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    if (CustomImageFormatConfigurator.isKeyframesEnabled()) {
      initAnimation(view);
    }
  }

  @Override
  public int getTitleId() {
    return R.string.format_keyframes_title;
  }
  @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
  private void initAnimation(View view) {
    SimpleDraweeView simpleDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    simpleDraweeView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    DraweeController controller = Fresco.newDraweeControllerBuilder()
        .setOldController(simpleDraweeView.getController())
        .setUri("http://frescolib.org/static/sample-images/animation.keyframes")
        .setAutoPlayAnimations(true)
        .build();
    simpleDraweeView.setController(controller);
  }

}
