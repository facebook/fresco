/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.vito.view.VitoView;

/** Simple Vito fragment that just displays an image. */
public class VitoSimpleFragment extends BaseShowcaseFragment {

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_vito_simple, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final ImageView imageView = (ImageView) view.findViewById(R.id.image);
    VitoView.show(sampleUris().createSampleUri(), imageView);

    view.findViewById(R.id.btn_random_uri)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                VitoView.show(sampleUris().createSampleUri(), imageView);
              }
            });
  }
}
