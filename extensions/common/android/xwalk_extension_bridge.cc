// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "xwalk/extensions/common/android/xwalk_extension_bridge.h"

#include "base/bind.h"
#include "base/logging.h"
#include "xwalk/extensions/common/xwalk_extension.h"
#include "jni/XWalkExtensionBridge_jni.h"

namespace xwalk {
namespace extensions {

XWalkExtensionBridge::XWalkExtensionBridge(JNIEnv* env, jobject obj,
                                           jint api_version,
                                           jstring name, jstring js_api)
    : XWalkExtension(),
      context_(NULL),
      jni_env_(env),
      jni_obj_(obj) {
  const char *str = env->GetStringUTFChars(name, 0);
  set_name(str);
  env->ReleaseStringUTFChars(name, str);

  str = env->GetStringUTFChars(js_api, 0);
  js_api_ = str;
  env->ReleaseStringUTFChars(js_api, str);
}

XWalkExtensionBridge::~XWalkExtensionBridge() {
  Java_XWalkExtensionBridge_onDestroy(jni_env_, jni_obj_);
}

bool XWalkExtensionBridge::is_valid() {
  if (!context_)
    return false;
  return true;
}

void XWalkExtensionBridge::PostMessage(JNIEnv* env, jobject obj, jstring msg) {
  DCHECK(is_valid());

  const char* str = env->GetStringUTFChars(msg, 0);
  context_->PostMessageWrapper(str);
  env->ReleaseStringUTFChars(msg, str);
}

const char* XWalkExtensionBridge::GetJavaScriptAPI() {
  return js_api_.c_str();
}

XWalkExtension::Context* XWalkExtensionBridge::CreateContext(
    const XWalkExtension::PostMessageCallback& post_message) {
  if (!context_) {
    context_ = new XWalkExtensionBridge::Context(jni_env_,
                                                 jni_obj_,
                                                 post_message);
  }

  return context_;
}

XWalkExtensionBridge::Context::Context(JNIEnv* env, jobject obj,
    const XWalkExtension::PostMessageCallback& post_message)
    : XWalkExtension::Context(post_message),
      jni_env_(env),
      jni_obj_(obj) {
}

XWalkExtensionBridge::Context::~Context() {
}

void XWalkExtensionBridge::Context::HandleMessage(
    scoped_ptr<base::Value> msg) {
  std::string value;

  if (!msg->GetAsString(&value))
    return;

  jstring buffer = jni_env_->NewStringUTF(value.c_str());
  Java_XWalkExtensionBridge_handleMessage(jni_env_, jni_obj_, buffer);
}

scoped_ptr<base::Value>
XWalkExtensionBridge::Context::HandleSyncMessage(
    scoped_ptr<base::Value> msg) {
  base::StringValue* ret_val = base::Value::CreateStringValue("");

  std::string value;
  if (!msg->GetAsString(&value)) {
    return scoped_ptr<base::Value>(ret_val);
  }

  jstring buffer = jni_env_->NewStringUTF(value.c_str());
  ScopedJavaLocalRef<jstring> ret =
      Java_XWalkExtensionBridge_handleSyncMessage(jni_env_, jni_obj_, buffer);

  const char *str = jni_env_->GetStringUTFChars(ret.obj(), 0);
  ret_val = base::Value::CreateStringValue(str);
  jni_env_->ReleaseStringUTFChars(ret.obj(), str);

  return scoped_ptr<base::Value>(ret_val);
}

void XWalkExtensionBridge::RegisterExtensions(XWalkExtensionService* extension_service) {
  extension_service->RegisterExtension(this);
}

static jint Init(JNIEnv* env, jobject obj,
    jint api_version, jstring name, jstring js_api) {
  XWalkExtensionBridge* extension =
    new XWalkExtensionBridge(env, obj, api_version, name, js_api);
  XWalkExtensionService::SetRegisterExtensionsCallbackForAndroid(
      base::Bind(&XWalkExtensionBridge::RegisterExtensions,
                 base::Unretained(extension)));
  return reinterpret_cast<jint>(extension);
}

bool RegisterXWalkExtensionBridge(JNIEnv* env) {
  return RegisterNativesImpl(env) >= 0;
}

}  // namespace extensions
}  // namespace xwalk
