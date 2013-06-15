LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# OpenCV
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
include /Users/josh/Development/Android/OpenCV-2.4.3.1-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := CalcPad
LOCAL_SRC_FILES := CalcPad.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)