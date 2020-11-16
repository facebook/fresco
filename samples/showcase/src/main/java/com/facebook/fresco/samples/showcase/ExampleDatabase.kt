/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import com.facebook.fresco.samples.showcase.drawee.DraweeHierarchyFragment
import com.facebook.fresco.samples.showcase.drawee.DraweeSimpleFragment
import com.facebook.fresco.samples.showcase.misc.WelcomeFragment
import com.facebook.fresco.samples.showcase.settings.SettingsFragment
import com.facebook.fresco.samples.showcase.vito.FrescoVitoLithoImageOptionsConfigFragment
import com.facebook.fresco.samples.showcase.vito.FrescoVitoLithoSimpleFragment

object ExampleDatabase {

  val welcome = ExampleItem("Welcome") { WelcomeFragment() }
  val settings = ExampleItem("Settings", "Settings") { SettingsFragment() }

  val examples =
      listOf(
          ExampleCategory(
              "Drawee",
              listOf(
                  ExampleItem("Simple Drawee") { DraweeSimpleFragment() },
                  ExampleItem("Placeholder, Progress, Failure") { DraweeHierarchyFragment() })),
          ExampleCategory(
              "Vito",
              listOf(
                  ExampleItem("Vito Litho: Image Options configurator") {
                    FrescoVitoLithoImageOptionsConfigFragment()
                  },
                  ExampleItem("Vito Litho: Simple") { FrescoVitoLithoSimpleFragment() })))
}
