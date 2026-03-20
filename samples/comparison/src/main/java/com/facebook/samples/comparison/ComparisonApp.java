/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison;

import android.app.Application;
import android.content.Context;
import com.facebook.imagepipeline.stetho.FrescoStethoPlugin;
import com.meta.dumpapp.internal.DumperPluginsProvider;
import com.meta.dumpapp.internal.Stetho;
import com.meta.dumpapp.internal.cli.DumperPlugin;

public class ComparisonApp extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    final Context context = this;
    Stetho.initialize(
        Stetho.newInitializerBuilder(context)
            .enableDumpapp(
                new DumperPluginsProvider() {
                  @Override
                  public Iterable<DumperPlugin> get() {
                    return new Stetho.DefaultDumperPluginsBuilder(context)
                        .provide(new FrescoStethoPlugin())
                        .finish();
                  }
                })
            .build());
  }
}
