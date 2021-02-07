/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.animation2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** Simple standalone activity that holds a Fragment. */
public class StandaloneActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_standalone);
  }
}
