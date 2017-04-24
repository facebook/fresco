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
package com.facebook.fresco.samples.showcase.drawee;

import java.util.Arrays;
import java.util.List;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.facebook.common.internal.Preconditions;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.common.SimpleScaleTypeAdapter;

/**
 * A {@link Fragment} that illustrates using rounded corners with Fresco.
 */
public class DraweeRoundedCornersFragment extends BaseShowcaseFragment {
  // TODO It looks like BITMAP_ONLY actually works for all scale types?
  /**
   * With these scale types, the contents of the picture will be clipped,
   * either using a shader (on API level < 21) or View.setOutlineProvider (on API level 21+).
   * For all other scale types we'll paint the rounded corners with a solid color.
   */
  private static final List<ScaleType> CLIP_SCALETYPES = Arrays.asList(
      ScaleType.CENTER_CROP,
      ScaleType.FOCUS_CROP,
      ScaleType.FIT_XY);

  private RoundingParams.RoundingMethod mPreviousRoundingMethod = getRoundingMethodForScaleType(ScaleType.CENTER);

  private int mWindowBackgroundColor;
  private int mColorPrimary;

  private SimpleDraweeView mDraweeRound;
  private SimpleDraweeView mDraweeRadius;
  private SimpleDraweeView mDraweeSome;
  private SimpleDraweeView mDraweeFancy;

  public DraweeRoundedCornersFragment() {
    // Required empty public constructor
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_rounded_corners_title;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_rounded_corners, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    findDrawees(view);
    initColors();

    final Spinner scaleType = (Spinner) view.findViewById(R.id.scaleType);
    final SimpleScaleTypeAdapter scaleTypeAdapter = SimpleScaleTypeAdapter.createForAllScaleTypes();
    scaleType.setAdapter(scaleTypeAdapter);
    scaleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final SimpleScaleTypeAdapter.Entry spinnerEntry =
            (SimpleScaleTypeAdapter.Entry) scaleTypeAdapter.getItem(position);
        final ScaleType scaleType = spinnerEntry.scaleType;

        RoundingParams.RoundingMethod roundingMethod = getRoundingMethodForScaleType(scaleType);
        changeDraweeViewScaleType(mDraweeRound, scaleType, roundingMethod, spinnerEntry.focusPoint);
        changeDraweeViewScaleType(mDraweeRadius, scaleType, roundingMethod, spinnerEntry.focusPoint);
        changeDraweeViewScaleType(mDraweeSome, scaleType, roundingMethod, spinnerEntry.focusPoint);
        changeDraweeViewScaleType(mDraweeFancy, scaleType, roundingMethod, spinnerEntry.focusPoint);

        if (roundingMethod != mPreviousRoundingMethod) {
          Toast.makeText(
              getContext(),
              getString(R.string.drawee_rounded_corners_toast, roundingMethod),
              Toast.LENGTH_SHORT).show();
        }
        mPreviousRoundingMethod = roundingMethod;
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    final CheckBox borders = (CheckBox) view.findViewById(R.id.borders);
    borders.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setShowBorder(mDraweeRound, isChecked);
        setShowBorder(mDraweeRadius, isChecked);
        setShowBorder(mDraweeSome, isChecked);
        setShowBorder(mDraweeFancy, isChecked);
      }
    });

    final Resources res = getResources();
    final RoundingParams fancyRoundingParams =
        RoundingParams.fromCornersRadii(
            res.getDimensionPixelSize(R.dimen.drawee_rounded_corners_fancy_top_left),
            res.getDimensionPixelSize(R.dimen.drawee_rounded_corners_fancy_top_right),
            res.getDimensionPixelSize(R.dimen.drawee_rounded_corners_fancy_bottom_right),
            res.getDimensionPixelSize(R.dimen.drawee_rounded_corners_fancy_bottom_left));
    mDraweeFancy.getHierarchy().setRoundingParams(
        fancyRoundingParams);
  }

  private void findDrawees(View view) {
    mDraweeRound = (SimpleDraweeView) view.findViewById(R.id.drawee_round);
    mDraweeRadius = (SimpleDraweeView) view.findViewById(R.id.drawee_radius);
    mDraweeSome = (SimpleDraweeView) view.findViewById(R.id.drawee_some);
    mDraweeFancy = (SimpleDraweeView) view.findViewById(R.id.drawee_fancy);
  }

  @SuppressWarnings("ResourceType")
  private void initColors() {
    final TypedArray attrs =
        getActivity().getTheme().obtainStyledAttributes(R.style.AppTheme, new int[]{
            R.attr.colorPrimary,
            android.R.attr.windowBackground});
    try {
      mColorPrimary = attrs.getColor(0, Color.BLACK);
      mWindowBackgroundColor = attrs.getColor(1, Color.BLUE);
    } finally {
      attrs.recycle();
    }
  }

  private void changeDraweeViewScaleType(
      SimpleDraweeView draweeView,
      ScaleType scaleType,
      RoundingParams.RoundingMethod roundingMethod,
      @Nullable PointF focusPoint) {
    final GenericDraweeHierarchy hierarchy = draweeView.getHierarchy();
    hierarchy.setActualImageScaleType(scaleType);
    hierarchy.setActualImageFocusPoint(focusPoint != null ? focusPoint : new PointF(0.5f, 0.5f));

    final RoundingParams roundingParams = Preconditions.checkNotNull(draweeView.getRoundingParams());
    if (roundingMethod == RoundingParams.RoundingMethod.OVERLAY_COLOR) {
      roundingParams.setOverlayColor(mWindowBackgroundColor);
    } else {
      roundingParams.setRoundingMethod(roundingMethod);
    }
    draweeView.setRoundingParams(roundingParams);
  }

  private RoundingParams.RoundingMethod getRoundingMethodForScaleType(ScaleType scaleType) {
    // TODO It looks like BITMAP_ONLY actually works for all scale types?
//    if (1 == 1) {
//      return RoundingParams.RoundingMethod.BITMAP_ONLY;
//    }
    if (CLIP_SCALETYPES.contains(scaleType)) {
      if (Build.VERSION.SDK_INT >= 21) {
        return RoundingParams.RoundingMethod.OUTLINE;
      } else {
        return RoundingParams.RoundingMethod.BITMAP_ONLY;
      }
    } else {
      return RoundingParams.RoundingMethod.OVERLAY_COLOR;
    }
  }

  private void setShowBorder(SimpleDraweeView draweeView, boolean show) {
    final RoundingParams roundingParams =
        Preconditions.checkNotNull(draweeView.getHierarchy().getRoundingParams());
    if (show) {
      roundingParams.setBorder(
          mColorPrimary,
          getResources().getDimensionPixelSize(R.dimen.drawee_rounded_corners_border_width));
    } else {
      roundingParams.setBorder(Color.TRANSPARENT, 0);
    }
    draweeView.getHierarchy().setRoundingParams(roundingParams);
  }
}
