// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "xwalk/extensions/common/android/xwalk_extension_bridge.h"

#include "base/android/jni_android.h"
#include "base/bind.h"
#include "base/logging.h"
#include "content/public/browser/browser_thread.h"
#include "xwalk/extensions/common/xwalk_extension.h"
#include "xwalk/runtime/browser/xwalk_browser_main_parts.h"
#include "jni/XWalkExtensionBridge_jni.h"

using content::BrowserThread;

namespace xwalk {
namespace extensions {

XWalkExtensionBridge::XWalkExtensionBridge(JNIEnv* env, jobject obj,
                                           jint api_version,
                                           jstring name, jstring js_api)
    : XWalkExtension(),
      context_(NULL),
      java_ref_(env, obj) {
  const char *str = env->GetStringUTFChars(name, 0);
  set_name(str);
  env->ReleaseStringUTFChars(name, str);

  str = env->GetStringUTFChars(js_api, 0);
  js_api_ = str;
  env->ReleaseStringUTFChars(js_api, str);
}

XWalkExtensionBridge::~XWalkExtensionBridge() {
  JNIEnv* env = base::android::AttachCurrentThread();

  ScopedJavaLocalRef<jobject> obj = java_ref_.get(env);
  if (obj.is_null())
    return;

  Java_XWalkExtensionBridge_onDestroy(env, obj.obj());
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
    context_ = new XWalkExtensionBridge::Context(java_ref_,
                                                 post_message);
  }

  return context_;
}

XWalkExtensionBridge::Context::Context(
    const JavaObjectWeakGlobalRef& java_ref,
    const XWalkExtension::PostMessageCallback& post_message)
    : XWalkExtension::Context(post_message),
      java_ref_(java_ref) {
}

XWalkExtensionBridge::Context::~Context() {
}

void XWalkExtensionBridge::Context::HandleMessage(
    scoped_ptr<base::Value> msg) {
  std::string value;

  if (!msg->GetAsString(&value))
    return;

  BrowserThread::PostTask(
      BrowserThread::UI,
      FROM_HERE,
      base::Bind(
          &XWalkExtensionBridge::Context::HandleMessageToJNI,
          this,
          value));
}

scoped_ptr<base::Value>
XWalkExtensionBridge::Context::HandleSyncMessage(
    scoped_ptr<base::Value> msg) {
  base::StringValue* ret_val = base::Value::CreateStringValue("");

  std::string value;
  if (!msg->GetAsString(&value)) {
    return scoped_ptr<base::Value>(ret_val);
  }

  JNIEnv* env = base::android::AttachCurrentThread();
  ScopedJavaLocalRef<jobject> obj = java_ref_.get(env);
  if (obj.is_null())
    return scoped_ptr<base::Value>(ret_val);

  jstring buffer = env->NewStringUTF(value.c_str());
  ScopedJavaLocalRef<jstring> ret =
      Java_XWalkExtensionBridge_handleSyncMessage(env, obj.obj(), buffer);

  const char *str = env->GetStringUTFChars(ret.obj(), 0);
  ret_val = base::Value::CreateStringValue(str);
  env->ReleaseStringUTFChars(ret.obj(), str);

  return scoped_ptr<base::Value>(ret_val);
}

void XWalkExtensionBridge::Context::HandleMessageToJNI(const std::string& msg) {
  JNIEnv* env = base::android::AttachCurrentThread();

  jstring buffer = env->NewStringUTF(msg.c_str());
  ScopedJavaLocalRef<jobject> obj = java_ref_.get(env);
  if (obj.is_null())
    return;

  Java_XWalkExtensionBridge_handleMessage(env, obj.obj(), buffer);
}

static jint Init(JNIEnv* env, jobject obj,
    jint api_version, jstring name, jstring js_api) {
  XWalkExtensionBridge* extension =
    new XWalkExtensionBridge(env, obj, api_version, name, js_api);
  XWalkBrowserMainParts::extension_service()->RegisterExtension(extension);
  return reinterpret_cast<jint>(extension);
}

bool RegisterXWalkExtensionBridge(JNIEnv* env) {
  return RegisterNativesImpl(env) >= 0;
}

}  // namespace extensions
}  // namespace xwalk
