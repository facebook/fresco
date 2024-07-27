#!/bin/bash
# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# This source code is licensed under the MIT license found in the
# LICENSE file in the root directory of this source tree.

if [[ $1 == "--local" || $1 == "-l" ]]; then
  # Local build with less verbose output
  ./gradlew test assembleDebug assembleDebugAndroidTest
else
  # Standard CI build command with maximum verbose output
  ./gradlew test assembleDebug assembleDebugAndroidTest --info
fi
