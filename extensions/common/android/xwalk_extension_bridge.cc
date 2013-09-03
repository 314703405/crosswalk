// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "xwalk/extensions/common/android/xwalk_extension_bridge.h"

#include "base/android/jni_android.h"
#include "base/bind.h"
#include "base/logging.h"
#include "content/public/browser/browser_thread.h"
#include "jni/XWalkExtensionBridge_jni.h"
#include "xwalk/extensions/common/xwalk_extension.h"
#include "xwalk/runtime/browser/xwalk_browser_main_parts.h"
#include "xwalk/runtime/browser/xwalk_content_browser_client.h"

using content::BrowserThread;

namespace xwalk {
namespace extensions {

XWalkExtensionBridge::XWalkExtensionBridge(JNIEnv* env, jobject obj,
                                           jstring name, jstring js_api)
    : XWalkExtension(),
      instance_(NULL),
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
  if (!instance_)
    return false;
  return true;
}

void XWalkExtensionBridge::PostMessage(JNIEnv* env, jobject obj, jstring msg) {
  DCHECK(is_valid());

  const char* str = env->GetStringUTFChars(msg, 0);
  instance_->PostMessageWrapper(str);
  env->ReleaseStringUTFChars(msg, str);
}

const char* XWalkExtensionBridge::GetJavaScriptAPI() {
  return js_api_.c_str();
}

XWalkExtensionInstance* XWalkExtensionBridge::CreateInstance(
    const XWalkExtension::PostMessageCallback& post_message) {
  if (!instance_) {
    instance_ = new XWalkExtensionBridgeInstance(java_ref_);
    instance_->SetPostMessageCallback(post_message);
  }

  return instance_;
}

XWalkExtensionBridgeInstance::XWalkExtensionBridgeInstance(
    const JavaObjectWeakGlobalRef& java_ref)
    : java_ref_(java_ref) {
}

XWalkExtensionBridgeInstance::~XWalkExtensionBridgeInstance() {
}

void XWalkExtensionBridgeInstance::HandleMessage(
    scoped_ptr<base::Value> msg) {
  std::string value;

  if (!msg->GetAsString(&value))
    return;

  BrowserThread::PostTask(
      BrowserThread::UI,
      FROM_HERE,
      base::Bind(
          &XWalkExtensionBridgeInstance::HandleMessageToJNI,
          this,
          value));
}

scoped_ptr<base::Value>
XWalkExtensionBridgeInstance::HandleSyncMessage(
    scoped_ptr<base::Value> msg) {

  std::string value;
  if (!msg->GetAsString(&value)) {
    return scoped_ptr<base::Value>(base::Value::CreateStringValue(""));
  }

  std::string ret_value;
  BrowserThread::PostTaskAndReplyWithResult(
      BrowserThread::UI,
      FROM_HERE,
      base::Bind(
          &XWalkExtensionBridgeInstance::HandleSyncMessageToJNI,
          this, value),
      base::Bind(
          &XWalkExtensionBridgeInstance::ReturnSyncMessageToJS,
          this, &ret_value));

  return scoped_ptr<base::Value>(
      base::Value::CreateStringValue(ret_value.c_str()));
}

void XWalkExtensionBridgeInstance::HandleMessageToJNI(const std::string& msg) {
  JNIEnv* env = base::android::AttachCurrentThread();

  jstring buffer = env->NewStringUTF(msg.c_str());
  ScopedJavaLocalRef<jobject> obj = java_ref_.get(env);
  if (obj.is_null())
    return;

  Java_XWalkExtensionBridge_handleMessage(env, obj.obj(), buffer);
}

std::string XWalkExtensionBridgeInstance::HandleSyncMessageToJNI(
    const std::string& msg) {
  JNIEnv* env = base::android::AttachCurrentThread();
  ScopedJavaLocalRef<jobject> obj = java_ref_.get(env);
  if (obj.is_null())
    return "";

  jstring buffer = env->NewStringUTF(msg.c_str());
  ScopedJavaLocalRef<jstring> ret =
      Java_XWalkExtensionBridge_handleSyncMessage(env, obj.obj(), buffer);

  const char *str = env->GetStringUTFChars(ret.obj(), 0);
  std::string ret_val = str;
  env->ReleaseStringUTFChars(ret.obj(), str);

  return ret_val;
}

void XWalkExtensionBridgeInstance::ReturnSyncMessageToJS(
    std::string* ret_val, const std::string& msg) {
  *ret_val = msg;
}

static jint Init(JNIEnv* env, jobject obj, jstring name, jstring js_api) {
  XWalkExtensionBridge* extension =
      new XWalkExtensionBridge(env, obj, name, js_api);
  XWalkExtensionService* service =
      XWalkContentBrowserClient::Get()->main_parts()->extension_service();
  service->RegisterExtension(scoped_ptr<XWalkExtension>(extension));
  return reinterpret_cast<jint>(extension);
}

bool RegisterXWalkExtensionBridge(JNIEnv* env) {
  return RegisterNativesImpl(env) >= 0;
}

}  // namespace extensions
}  // namespace xwalk
