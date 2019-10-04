/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;

/** Simple drawee fragment that just displays an image. */
public class DraweeSimpleFragment extends BaseShowcaseFragment {

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_simple, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final Uri uri = sampleUris().createSampleUri(ImageUriProvider.ImageSize.M);

    final SimpleDraweeView simpleDraweeView =
        (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    simpleDraweeView.setImageURI(uri);

    view.findViewById(R.id.btn_random_uri)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                simpleDraweeView.setImageURI(sampleUris().createSampleUri());
              }
            });
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_simple_title;
  }
}
