LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_CFLAGS    := -DHAVE_CONFIG_H
LOCAL_MODULE    := gif
LOCAL_SRC_FILES := \
	dgif_lib.c \
	gifalloc.c 
LOCAL_LDLIBS := -llog -ldl -landroid
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_SHARED_LIBRARY)
