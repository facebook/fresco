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
package com.facebook.fresco.samples.showcase.common;

import android.graphics.drawable.Animatable;
import android.view.View;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.GenericDraweeView;

/**
 * Simple View click listener that toggles a DraweeView's animation if an animated image is
 * currently displayed.
 */
public class ToggleAnimationClickListener implements View.OnClickListener {

  private final GenericDraweeView mDraweeView;

  public ToggleAnimationClickListener(GenericDraweeView draweeView) {
    mDraweeView = draweeView;
  }

  @Override
  public void onClick(View v) {
    DraweeController controller = mDraweeView.getController();
    if (controller == null) {
      return;
    }
    Animatable animatable = controller.getAnimatable();
    if (animatable == null) {
      return;
    }
    if (animatable.isRunning()) {
      animatable.stop();
    } else {
      animatable.start();
    }
  }
}
