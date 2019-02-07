# Copyright 2004-present Facebook. All Rights Reserved.
APP_BUILD_SCRIPT := Android.mk

APP_ABI := armeabi-v7a arm64-v8a x86 x86_64

APP_MK_DIR := $(dir $(lastword $(MAKEFILE_LIST)))
NDK_MODULE_PATH := $(APP_MK_DIR)$(HOST_DIRSEP)$(APP_MK_DIR)../../../nativedeps/merge

APP_STL := c++_static

# Make sure every shared lib includes a .note.gnu.build-id header
APP_LDFLAGS := -Wl,--build-id

NDK_TOOLCHAIN_VERSION := clang


# We link our libs with static stl implementation. Because of that we need to
# hide all stl related symbols to make them unaccessible from the outside.
# We also need to make sure that our library does not use any stl functions
# coming from other stl implementations as well

# This hides all symbols exported from libgnustl_static
FRESCO_CPP_LDFLAGS := -Wl,--gc-sections
