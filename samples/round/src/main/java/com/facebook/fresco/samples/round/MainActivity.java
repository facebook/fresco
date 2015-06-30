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

package com.facebook.samples.round;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

  private static final Uri URI = Uri.parse(
      "http://apod.nasa.gov/apod/image/1410/20141008tleBaldridge001h990.jpg");
  private static final int WIDTH = 400;
  private static final int HEIGHT = 240;
  private static final float FOCUS_X = 0.454f;
  private static final float FOCUS_Y = 0.266f;
  private static final int RADIUS = 50;

  private LinearLayout mUnroundedColumn;
  private LinearLayout mRoundedColumn;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Fresco.initialize(this);
    setContentView(R.layout.activity_main);
    FLog.setMinimumLoggingLevel(FLog.VERBOSE);

    mUnroundedColumn = (LinearLayout) findViewById(R.id.unrounded);
    mRoundedColumn = (LinearLayout) findViewById(R.id.rounded);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WIDTH, HEIGHT);

    RoundingParams bitmapOnly = RoundingParams.fromCornersRadius(RADIUS)
        .setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY);
    RoundingParams overlayColor = RoundingParams.fromCornersRadius(RADIUS)
        .setRoundingMethod(RoundingParams.RoundingMethod.OVERLAY_COLOR)
        .setOverlayColor(Color.WHITE);
    GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(getResources());

    Set<ScaleType> useBitmapOnly = new HashSet<>();
    useBitmapOnly.add(ScaleType.CENTER_CROP);
    useBitmapOnly.add(ScaleType.CENTER);
    useBitmapOnly.add(ScaleType.FOCUS_CROP);

    for (ScaleType scaleType : ScaleType.values()) {
      builder.setActualImageScaleType(scaleType);
      if (scaleType == ScaleType.FOCUS_CROP) {
        builder.setActualImageFocusPoint(new PointF(FOCUS_X, FOCUS_Y));
      }
      builder.setRoundingParams(null);
      SimpleDraweeView unroundedImage = new SimpleDraweeView(this, builder.build());

      if (useBitmapOnly.contains(scaleType)) {
        builder.setRoundingParams(bitmapOnly);
      } else {
        builder.setRoundingParams(overlayColor);
      }
      SimpleDraweeView roundedImage = new SimpleDraweeView(this, builder.build());

      unroundedImage.setLayoutParams(params);
      roundedImage.setLayoutParams(params);
      mUnroundedColumn.addView(unroundedImage);
      mRoundedColumn.addView(roundedImage);
      unroundedImage.setImageURI(URI);
      roundedImage.setImageURI(URI);
    }
  }
}
