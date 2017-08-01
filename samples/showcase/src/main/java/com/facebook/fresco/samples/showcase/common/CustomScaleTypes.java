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

import android.graphics.Matrix;
import android.graphics.Rect;
import com.facebook.drawee.drawable.ScalingUtils;

/**
 * Custom scale type examples.
 */
public class CustomScaleTypes {

  public static final ScalingUtils.ScaleType FIT_X = new ScaleTypeFitX();
  public static final ScalingUtils.ScaleType FIT_Y = new ScaleTypeFitY();

  private static class ScaleTypeFitX extends ScalingUtils.AbstractScaleType {

    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float scale, dx, dy;
      scale = scaleX;
      dx = parentRect.left;
      dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }

  private static class ScaleTypeFitY extends ScalingUtils.AbstractScaleType {

    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float scale, dx, dy;
      scale = scaleY;
      dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
      dy = parentRect.top;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }
}
