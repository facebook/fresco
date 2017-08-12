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

import android.graphics.drawable.Animatable;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.keyframes.KeyframesDrawable;

/**
 * Animation of KeyframesDrawables needs to be explicitly started and stopped.
 * AnimatableKeyframesDrawable wraps a KeyframesDrawables and allows Fresco to automatically start
 * and stop the animated as needed (as long as setAutoPlayAnimations(true) is called on the
 * DraweeController associated with the DraweeView used to display the image.
 */
class AnimatableKeyframesDrawable extends ForwardingDrawable implements Animatable {

  private final KeyframesDrawable mDrawable;
  private boolean mAnimating;

  AnimatableKeyframesDrawable(KeyframesDrawable drawable) {
    super(drawable);
    mDrawable = drawable;
  }

  @Override
  public void start() {
    mDrawable.startAnimation();
    mAnimating = true;
  }

  @Override
  public void stop() {
    mDrawable.stopAnimation();
    mAnimating = false;
  }

  @Override
  public boolean isRunning() {
    return mAnimating;
  }
}
