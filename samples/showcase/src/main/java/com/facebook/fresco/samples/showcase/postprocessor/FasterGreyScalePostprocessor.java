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
package com.facebook.fresco.samples.showcase.postprocessor;

import android.graphics.Bitmap;
import com.facebook.imagepipeline.request.BasePostprocessor;

/**
 * Applies a grey-scale effect on the bitmap using the more efficient {@link Bitmap#setPixels(int[],
 * int, int, int, int, int, int)} method.
 */
public class FasterGreyScalePostprocessor extends BasePostprocessor {

  @Override
  public void process(Bitmap bitmap) {
    final int w = bitmap.getWidth();
    final int h = bitmap.getHeight();
    final int[] pixels = new int[w * h];

    /*
     * Using {@link Bitmap#getPixels} reduces the number of Java-JNI calls and passes all the image
     * pixels in one call. This allows us to edit all the data in the Java world and then hand back
     * the final result later.
     */
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        final int offset = y * w + x;
        pixels[offset] = SlowGreyScalePostprocessor.getGreyColor(pixels[offset]);
      }
    }

    bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
  }
}
