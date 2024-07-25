/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.misc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.facebook.common.util.UriUtil;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.vito.view.VitoView;

/** Fragment shown on start-up */
public class WelcomeFragment extends BaseShowcaseFragment {

  private static final String URL_DOCUMENTATION = "https://frescolib.org/";
  private static final String URL_GITHUB = "https://github.com/facebook/fresco";

  private static final String CALLER_CONTEXT = "WelcomeFragment";

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_welcome, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final ImageView imageView = view.findViewById(R.id.image);
    VitoView.show(UriUtil.getUriForResourceId(R.drawable.logo), CALLER_CONTEXT, imageView);
    imageView.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            final RotateAnimation rotateAnimation =
                new RotateAnimation(
                    0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(1000);
            rotateAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            imageView.startAnimation(rotateAnimation);
          }
        });

    final Button buttonGitHub = view.findViewById(R.id.button_github);
    setUriIntent(buttonGitHub, URL_GITHUB);

    final Button buttonDocumentation = view.findViewById(R.id.button_documentation);
    setUriIntent(buttonDocumentation, URL_DOCUMENTATION);
  }

  private void setUriIntent(final Button button, final String url) {
    button.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            final Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
          }
        });
  }
}
