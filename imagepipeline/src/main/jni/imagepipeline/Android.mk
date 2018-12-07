# Copyright 2004-present Facebook. All Rights Reserved.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := imagepipeline
LOCAL_SRC_FILES := \
	exceptions.cpp \
	init.cpp \

CXX11_FLAGS := -std=c++11
LOCAL_CFLAGS += $(CXX11_FLAGS)
LOCAL_CFLAGS += -DLOG_TAG=\"libimagepipeline\"
LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_CFLAGS += $(FRESCO_CPP_CFLAGS)
LOCAL_EXPORT_CPPFLAGS := $(CXX11_FLAGS)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_LDLIBS := -llog -ljnigraphics
LOCAL_LDFLAGS += $(FRESCO_CPP_LDFLAGS)

LOCAL_STATIC_LIBRARIES += bitmaps
LOCAL_STATIC_LIBRARIES += memchunk
LOCAL_LDFLAGS += -Wl,--exclude-libs

include $(BUILD_SHARED_LIBRARY)
$(call import-module,bitmaps)
$(call import-module,memchunk)
