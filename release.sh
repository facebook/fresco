#!/bin/bash
# Copyright (c) Facebook, Inc. and its affiliates.
#
# This source code is licensed under the MIT license found in the
# LICENSE file in the root directory of this source tree.

rm -rf $HOME/.gradle/caches
rm -rf .gradle
./gradlew clean allclean build uploadArchives
