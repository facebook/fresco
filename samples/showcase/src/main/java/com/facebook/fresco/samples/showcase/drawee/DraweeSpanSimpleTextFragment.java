/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.span.DraweeSpan;
import com.facebook.drawee.span.DraweeSpanStringBuilder;
import com.facebook.drawee.span.SimpleDraweeSpanTextView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.common.SimpleScaleTypeAdapter;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;

/** Simple fragment that displays text with inline images using {@link DraweeSpan}. */
public class DraweeSpanSimpleTextFragment extends BaseShowcaseFragment {

  private SimpleDraweeSpanTextView mDraweeSpanTextView;
  private ScalingUtils.ScaleType mScaleType;
  private Uri mInlineImageUri;
  private Uri mInlineAnimatedImageUri;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_span_simple, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    mInlineImageUri = sampleUris().createSampleUri(ImageUriProvider.ImageSize.M);
    mInlineAnimatedImageUri = sampleUris().createGifUriWithPause(ImageUriProvider.ImageSize.M);

    mDraweeSpanTextView = (SimpleDraweeSpanTextView) view.findViewById(R.id.drawee_text_view);
    final Spinner scaleType = (Spinner) view.findViewById(R.id.scaleType);

    final SimpleScaleTypeAdapter scaleTypeAdapter = SimpleScaleTypeAdapter.createForAllScaleTypes();
    scaleType.setAdapter(scaleTypeAdapter);
    scaleType.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final SimpleScaleTypeAdapter.Entry spinnerEntry =
                (SimpleScaleTypeAdapter.Entry) scaleTypeAdapter.getItem(position);
            mScaleType = spinnerEntry.scaleType;
            updateText();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    updateText();
  }

  private void updateText() {
    // The # will be replaced with the image.
    String text = getString(R.string.drawee_span_simple_text);
    int imagePosition = text.indexOf('#');

    DraweeSpanStringBuilder draweeSpanStringBuilder = new DraweeSpanStringBuilder(text);

    DraweeHierarchy draweeHierarchy =
        GenericDraweeHierarchyBuilder.newInstance(getResources())
            .setPlaceholderImage(new ColorDrawable(Color.RED))
            .setActualImageScaleType(mScaleType)
            .build();
    DraweeController controller =
        Fresco.newDraweeControllerBuilder().setUri(mInlineImageUri).build();

    draweeSpanStringBuilder.setImageSpan(
        getContext(), /* Context */
        draweeHierarchy, /* hierarchy to be used */
        controller, /* controller to be used to update the hierarchy */
        imagePosition, /* image index within the text */
        200, /* image width */
        200, /* image height */
        false, /* auto resize */
        DraweeSpan.ALIGN_CENTER); /* alignment */

    int imagePosition2 = text.indexOf('@');

    DraweeHierarchy draweeAnimatedHierarchy =
        GenericDraweeHierarchyBuilder.newInstance(getResources())
            .setPlaceholderImage(new ColorDrawable(Color.RED))
            .setActualImageScaleType(mScaleType)
            .build();
    DraweeController animatedController =
        Fresco.newDraweeControllerBuilder()
            .setUri(mInlineAnimatedImageUri)
            .setAutoPlayAnimations(true)
            .build();

    draweeSpanStringBuilder.setImageSpan(
        getContext(), /* Context */
        draweeAnimatedHierarchy, /* hierarchy to be used */
        animatedController, /* controller to be used to update the hierarchy */
        imagePosition2, /* image index within the text */
        200, /* image width */
        200, /* image height */
        false, /* auto resize */
        DraweeSpan.ALIGN_CENTER); /* alignment */

    mDraweeSpanTextView.setDraweeSpanStringBuilder(draweeSpanStringBuilder);
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_span_simple_title;
  }
}
