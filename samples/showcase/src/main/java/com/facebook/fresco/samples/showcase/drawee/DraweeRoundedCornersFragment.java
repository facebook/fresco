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

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PointF;
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
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider.ImageSize;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link Fragment} that illustrates using rounded corners with Fresco.
 */
public class DraweeRoundedCornersFragment extends BaseShowcaseFragment {
  private static final List<ScaleType> BITMAP_ONLY_SCALETYPES = Arrays.asList(
      ScaleType.CENTER_CROP,
      ScaleType.FOCUS_CROP,
      ScaleType.FIT_XY);

  private ScaleType mPreviousScaleType = ScaleType.CENTER;

  private int mWindowBackgroundColor;
  private int mColorPrimary;

  private SimpleDraweeView mDraweeRound;
  private SimpleDraweeView mDraweeRadius;
  private SimpleDraweeView mDraweeSome;
  private SimpleDraweeView mDraweeSomeRtl;
  private SimpleDraweeView mDraweeFancy;

  private CheckBox mShowBordersCheck;
  private CheckBox mScaleInsideBordersCheck;

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

    final ImageUriProvider imageUriProvider = ImageUriProvider.getInstance(getContext());
    mDraweeRound.setImageURI(imageUriProvider.createSampleUri(ImageSize.L));
    mDraweeRadius.setImageURI(imageUriProvider.createSampleUri(ImageSize.L));
    mDraweeSome.setImageURI(imageUriProvider.createSampleUri(ImageSize.L));
    mDraweeSomeRtl.setImageURI(imageUriProvider.createSampleUri(ImageSize.L));
    mDraweeFancy.setImageURI(imageUriProvider.createSampleUri(ImageSize.L));

    final Spinner scaleType = (Spinner) view.findViewById(R.id.scaleType);
    final SimpleScaleTypeAdapter scaleTypeAdapter = SimpleScaleTypeAdapter.createForAllScaleTypes();
    scaleType.setAdapter(scaleTypeAdapter);
    scaleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final SimpleScaleTypeAdapter.Entry spinnerEntry =
            (SimpleScaleTypeAdapter.Entry) scaleTypeAdapter.getItem(position);
        final ScaleType scaleType = spinnerEntry.scaleType;

        changeDraweeViewScaleType(mDraweeRound, scaleType, spinnerEntry.focusPoint);
        changeDraweeViewScaleType(mDraweeRadius, scaleType, spinnerEntry.focusPoint);
        changeDraweeViewScaleType(mDraweeSome, scaleType, spinnerEntry.focusPoint);
        changeDraweeViewScaleType(mDraweeSomeRtl, scaleType, spinnerEntry.focusPoint);
        changeDraweeViewScaleType(mDraweeFancy, scaleType, spinnerEntry.focusPoint);

        if (BITMAP_ONLY_SCALETYPES.contains(scaleType) &&
            !BITMAP_ONLY_SCALETYPES.contains(mPreviousScaleType)) {
          Toast.makeText(
              getContext(),
              R.string.drawee_rounded_corners_bitmap_only_toast,
              Toast.LENGTH_SHORT).show();
        } else if (!BITMAP_ONLY_SCALETYPES.contains(scaleType) &&
            BITMAP_ONLY_SCALETYPES.contains(mPreviousScaleType)) {
          Toast.makeText(
              getContext(),
              R.string.drawee_rounded_corners_overlay_color_toast,
              Toast.LENGTH_SHORT).show();
        }
        mPreviousScaleType = scaleType;
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    mShowBordersCheck = view.findViewById(R.id.borders);
    mShowBordersCheck.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateRounding();
          }
        });

    mScaleInsideBordersCheck = view.findViewById(R.id.scaleInside);
    mScaleInsideBordersCheck.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateRounding();
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
    mDraweeSomeRtl = (SimpleDraweeView) view.findViewById(R.id.drawee_some_rtl);
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
      @Nullable PointF focusPoint) {
    final GenericDraweeHierarchy hierarchy = draweeView.getHierarchy();
    hierarchy.setActualImageScaleType(scaleType);
    hierarchy.setActualImageFocusPoint(focusPoint != null ? focusPoint : new PointF(0.5f, 0.5f));

    final RoundingParams roundingParams = Preconditions.checkNotNull(hierarchy.getRoundingParams());
    if (BITMAP_ONLY_SCALETYPES.contains(scaleType)) {
      roundingParams.setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY);
    } else {
      roundingParams.setOverlayColor(mWindowBackgroundColor);
    }
    hierarchy.setRoundingParams(roundingParams);
  }

  private void updateRounding() {
    boolean showBorder = mShowBordersCheck.isChecked();
    boolean scaleInsideBorder = showBorder && mScaleInsideBordersCheck.isChecked();
    setShowBorder(mDraweeRound, showBorder, scaleInsideBorder);
    setShowBorder(mDraweeRadius, showBorder, scaleInsideBorder);
    setShowBorder(mDraweeSome, showBorder, scaleInsideBorder);
    setShowBorder(mDraweeSomeRtl, showBorder, scaleInsideBorder);
    setShowBorder(mDraweeFancy, showBorder, scaleInsideBorder);
  }

  private void setShowBorder(SimpleDraweeView draweeView, boolean show, boolean scaleInside) {
    final RoundingParams roundingParams =
        Preconditions.checkNotNull(draweeView.getHierarchy().getRoundingParams());
    if (show) {
      roundingParams.setBorder(
          mColorPrimary,
          getResources().getDimensionPixelSize(R.dimen.drawee_rounded_corners_border_width));
      roundingParams.setScaleDownInsideBorders(scaleInside);
    } else {
      roundingParams.setBorder(Color.TRANSPARENT, 0);
    }
    draweeView.getHierarchy().setRoundingParams(roundingParams);
  }
}
