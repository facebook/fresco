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
package com.facebook.fresco.samples.showcase.misc;

import android.util.Log;
import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfData;
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener;
import com.facebook.drawee.backends.pipeline.info.ImagePerfUtils;
import com.facebook.drawee.backends.pipeline.info.VisibilityState;
import java.util.Locale;

public class LogcatImagePerfDataListener implements ImagePerfDataListener {

  private static final String TAG = "ImagePerf";

  @Override
  public void onImageLoadStatusUpdated(ImagePerfData imagePerfData,
      @ImageLoadStatus int imageLoadStatus) {
    Log.d(
        TAG,
        String.format(
            (Locale) null,
            "status=%s, data=%s",
            ImagePerfUtils.toString(imageLoadStatus),
            imagePerfData.createDebugString()));
  }

  @Override
  public void onImageVisibilityUpdated(
      ImagePerfData imagePerfData, @VisibilityState int visibility) {
    Log.d(
        TAG,
        String.format(
            (Locale) null,
            "visibility=%s, data=%s",
            ImagePerfUtils.toString(imagePerfData.getVisibilityState()),
            imagePerfData.createDebugString()));
  }
}
