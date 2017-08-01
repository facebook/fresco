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
package com.facebook.fresco.samples.showcase.imagepipeline;

import android.net.Uri;
import android.util.Log;
import com.facebook.imagepipeline.cache.MediaIdExtractor;
import java.util.Locale;
import javax.annotation.Nullable;

public class ShowcaseMediaIdExtractor implements MediaIdExtractor {

  private static final String TAG = "ShowcaseMediaIdExtracto";

  @Nullable
  @Override
  public String getMediaIdFrom(Uri uri) {
    // To avoid polluting other samples, this will ignore anything but the monkey selfie
    String lastSegment = uri.getLastPathSegment();
    if (!lastSegment.startsWith("monkey-selfie")) {
      return null;
    }

    int lastDashIndex = lastSegment.lastIndexOf('-');
    if (lastDashIndex == -1) {
      return null;
    }

    String mediaId = lastSegment.substring(0, lastDashIndex);
    Log.e(TAG, String.format(Locale.getDefault(), "Extracted media ID: %s", mediaId));
    return mediaId;
  }
}
