LOCAL_PATH:= $(call my-dir)

JPEGTURBO_CFLAGS := -DJPEG_LIB_VERSION=80 -Wno-attributes

JPEGTURBO_SRC_FILES := \
	jaricom.c \
	jcapimin.c \
	jcapistd.c \
	jcarith.c \
	jccoefct.c \
	jccolor.c \
	jcdctmgr.c \
	jchuff.c \
	jcicc.c \
	jcinit.c \
	jcmainct.c \
	jcmarker.c \
	jcmaster.c \
	jcomapi.c \
	jcparam.c \
	jcphuff.c \
	jcprepct.c \
	jcsample.c \
	jctrans.c \
	jdapimin.c \
	jdapistd.c \
	jdarith.c \
	jdatadst.c \
	jdatasrc.c \
	jdcoefct.c \
	jdcolor.c \
	jddctmgr.c \
	jdhuff.c \
	jdicc.c \
	jdinput.c \
	jdmainct.c \
	jdmarker.c \
	jdmaster.c \
	jdmerge.c \
	jdphuff.c \
	jdpostct.c \
	jdsample.c \
	jdtrans.c \
	jerror.c \
	jfdctflt.c \
	jfdctfst.c \
	jfdctint.c \
	jidctflt.c \
	jidctfst.c \
	jidctint.c \
	jidctred.c \
	jmemmgr.c \
	jmemnobs.c \
	jquant1.c \
	jquant2.c \
	jutils.c \
	transupp.c

# switch between SIMD supported and non supported architectures
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
JPEGTURBO_SRC_FILES += \
	simd/arm/aarch32/jchuff-neon.c \
	simd/arm/aarch32/jsimd.c \
	simd/arm/jccolor-neon.c \
	simd/arm/jcgray-neon.c \
	simd/arm/jcphuff-neon.c \
	simd/arm/jcsample-neon.c \
	simd/arm/jdcolor-neon.c \
	simd/arm/jdmerge-neon.c \
	simd/arm/jdsample-neon.c \
	simd/arm/jfdctfst-neon.c \
	simd/arm/jfdctint-neon.c \
	simd/arm/jidctfst-neon.c \
	simd/arm/jidctint-neon.c \
	simd/arm/jidctred-neon.c \
	simd/arm/jquanti-neon.c
JPEGTURBO_CFLAGS += \
	-DNEON_INTRINSICS=1
else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
JPEGTURBO_SRC_FILES += \
	simd/arm/aarch64/jchuff-neon.c \
	simd/arm/aarch64/jsimd.c \
	simd/arm/jccolor-neon.c \
	simd/arm/jcgray-neon.c \
	simd/arm/jcphuff-neon.c \
	simd/arm/jcsample-neon.c \
	simd/arm/jdcolor-neon.c \
	simd/arm/jdmerge-neon.c \
	simd/arm/jdsample-neon.c \
	simd/arm/jfdctfst-neon.c \
	simd/arm/jfdctint-neon.c \
	simd/arm/jidctfst-neon.c \
	simd/arm/jidctint-neon.c \
	simd/arm/jidctred-neon.c \
	simd/arm/jquanti-neon.c
JPEGTURBO_CFLAGS += \
	-DNEON_INTRINSICS=1
else
JPEGTURBO_SRC_FILES += jsimd_none.c
endif

# fb_jpegturbo module
include $(CLEAR_VARS)
LOCAL_MODULE:= fb_jpegturbo
LOCAL_SRC_FILES := $(JPEGTURBO_SRC_FILES)
LOCAL_CFLAGS := $(JPEGTURBO_CFLAGS)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_STATIC_LIBRARY)
