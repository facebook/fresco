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

import java.util.HashSet;
import java.util.Set;

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
import com.facebook.drawee.view.SimpleDraweeView;

public class MainActivity extends Activity {

  private static final Uri URI = Uri.parse(
      "http://cache-graphicslib.viator.com/graphicslib/3454/SITours/small-group-yosemite-tour-from-san-francisco-in-san-francisco-172092.jpg");
      //"http://apod.nasa.gov/apod/image/1410/20141008tleBaldridge001h990.jpg");
  private static final int WIDTH = 400;
  private static final int HEIGHT = 240;
  private static final float FOCUS_X = 0.454f;
  private static final float FOCUS_Y = 0.266f;
  private static final int RADIUS = 50;
  private static final boolean USE_TEMP_BITMAP_ROUNDING = true; // add an app setting for this
  private static final Set<ScaleType> SUPPORTS_BITMAP_ROUNDING;

  private LinearLayout mUnroundedColumn;
  private LinearLayout mRoundedColumn;
  private LinearLayout.LayoutParams mChildLayoutParams;
  private RoundingParams mRoundingBitmapOnly;
  private RoundingParams mRoundingOverlayColor;
  private RoundingParams mRoundingTempBitmap;

  static {
    SUPPORTS_BITMAP_ROUNDING = new HashSet<>();
    SUPPORTS_BITMAP_ROUNDING.add(ScaleType.FIT_XY);
    SUPPORTS_BITMAP_ROUNDING.add(ScaleType.CENTER_CROP);
    SUPPORTS_BITMAP_ROUNDING.add(ScaleType.FOCUS_CROP);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Fresco.initialize(this);
    setContentView(R.layout.activity_main);
    FLog.setMinimumLoggingLevel(FLog.VERBOSE);

    mUnroundedColumn = (LinearLayout) findViewById(R.id.unrounded);
    mRoundedColumn = (LinearLayout) findViewById(R.id.rounded);
    mChildLayoutParams = new LinearLayout.LayoutParams(WIDTH, HEIGHT);

    mRoundingBitmapOnly = RoundingParams.fromCornersRadius(RADIUS)
        .setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY);
    mRoundingOverlayColor = RoundingParams.fromCornersRadius(RADIUS)
        .setOverlayColor(Color.WHITE)
        .setRoundingMethod(RoundingParams.RoundingMethod.OVERLAY_COLOR);
    mRoundingTempBitmap = RoundingParams.fromCornersRadius(RADIUS)
        .setOverlayColor(Color.TRANSPARENT)
        .setRoundingMethod(RoundingParams.RoundingMethod.TEMP_BITMAP);

    GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(getResources());

    addImageWithScaleType(ScaleType.FIT_XY, builder);
    addImageWithScaleType(ScaleType.FIT_START, builder);
    addImageWithScaleType(ScaleType.FIT_CENTER, builder);
    addImageWithScaleType(ScaleType.FIT_END, builder);
    addImageWithScaleType(ScaleType.CENTER, builder);
    addImageWithScaleType(ScaleType.CENTER_INSIDE, builder);
    addImageWithScaleType(ScaleType.CENTER_CROP, builder);
    addImageWithScaleType(ScaleType.FOCUS_CROP, builder);
  }

  private void addImageWithScaleType(ScaleType scaleType, GenericDraweeHierarchyBuilder builder) {
    builder.setActualImageScaleType(scaleType);
    if (scaleType == ScaleType.FOCUS_CROP) {
      builder.setActualImageFocusPoint(new PointF(FOCUS_X, FOCUS_Y));
    }
    builder.setRoundingParams(null);
    SimpleDraweeView unroundedImage = new SimpleDraweeView(this, builder.build());

    if (USE_TEMP_BITMAP_ROUNDING) {
      builder.setRoundingParams(mRoundingTempBitmap);
    } else if (SUPPORTS_BITMAP_ROUNDING.contains(scaleType)) {
      builder.setRoundingParams(mRoundingBitmapOnly);
    } else {
      builder.setRoundingParams(mRoundingOverlayColor);
    }
    SimpleDraweeView roundedImage = new SimpleDraweeView(this, builder.build());

    unroundedImage.setLayoutParams(mChildLayoutParams);
    roundedImage.setLayoutParams(mChildLayoutParams);
    mUnroundedColumn.addView(unroundedImage);
    mRoundedColumn.addView(roundedImage);
    unroundedImage.setImageURI(URI);
    roundedImage.setImageURI(URI);
  }
}
