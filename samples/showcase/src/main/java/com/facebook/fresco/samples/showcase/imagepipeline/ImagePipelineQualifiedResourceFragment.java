/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imagepipeline;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.facebook.common.util.UriUtil;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.vito.view.VitoView;

/** Simple drawee fragment that just displays an image. */
public class ImagePipelineQualifiedResourceFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "ImagePipelineQualifiedResourceFragment";

  /**
   * This package name can pointing to another module that you include, but that does not share the
   * same "R" file as your main application.
   */
  private static final String PACKAGE_NAME = "com.facebook.fresco.samples.showcase";

  private static final int RESOURCE_ID = R.drawable.logo;

  private static final Uri QUALIFIED_RESOURCE_URI =
      UriUtil.getUriForQualifiedResource(PACKAGE_NAME, RESOURCE_ID);

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_imagepipeline_qualified_resource, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    ImageView image = view.findViewById(R.id.image);
    VitoView.show(QUALIFIED_RESOURCE_URI, CALLER_CONTEXT, image);
  }
}
