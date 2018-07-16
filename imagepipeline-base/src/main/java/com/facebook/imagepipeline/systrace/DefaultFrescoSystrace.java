/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.imagepipeline.systrace;

import android.os.Build;
import android.os.Trace;
import com.facebook.imagepipelinebase.BuildConfig;

public class DefaultFrescoSystrace implements FrescoSystrace.Systrace {

  @Override
  public void beginSection(String name) {
    if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      Trace.beginSection(name);
    }
  }

  @Override
  public FrescoSystrace.ArgsBuilder beginSectionWithArgs(String name) {
    if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      return new DefaultArgsBuilder(name);
    }

    return FrescoSystrace.NO_OP_ARGS_BUILDER;
  }

  @Override
  public void endSection() {
    if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      Trace.endSection();
    }
  }

  @Override
  public boolean isTracing() {
    return BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
  }

  /**
   * Handles adding args to a systrace section by naively appending them to the section name. This
   * functionality has a more intelligent implementation when using a tracer that writes directly to
   * ftrace instead of using Android's Trace class.
   */
  private static final class DefaultArgsBuilder implements FrescoSystrace.ArgsBuilder {

    private final StringBuilder mStringBuilder;

    public DefaultArgsBuilder(String name) {
      mStringBuilder = new StringBuilder(name);
    }

    @Override
    public void flush() {
      // 127 is the max name length according to
      // https://developer.android.com/reference/android/os/Trace.html
      if (mStringBuilder.length() > 127) {
        mStringBuilder.setLength(127);
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.beginSection(mStringBuilder.toString());
      }
    }

    @Override
    public FrescoSystrace.ArgsBuilder arg(String key, Object value) {
      mStringBuilder
          .append(';')
          .append(key)
          .append('=')
          .append(value == null ? "null" : value.toString());
      return this;
    }

    @Override
    public FrescoSystrace.ArgsBuilder arg(String key, int value) {
      mStringBuilder.append(';').append(key).append('=').append(Integer.toString(value));
      return this;
    }

    @Override
    public FrescoSystrace.ArgsBuilder arg(String key, long value) {
      mStringBuilder.append(';').append(key).append('=').append(Long.toString(value));
      return this;
    }

    @Override
    public FrescoSystrace.ArgsBuilder arg(String key, double value) {
      mStringBuilder.append(';').append(key).append('=').append(Double.toString(value));
      return this;
    }
  }
}
