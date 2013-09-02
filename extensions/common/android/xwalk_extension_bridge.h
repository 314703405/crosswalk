// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef XWALK_EXTENSIONS_COMMON_ANDROID_XWALK_EXTENSION_BRIDGE_H_
#define XWALK_EXTENSIONS_COMMON_ANDROID_XWALK_EXTENSION_BRIDGE_H_

#include <string>

#include "base/android/jni_helper.h"
#include "base/android/scoped_java_ref.h"
#include "base/callback.h"
#include "base/logging.h"
#include "base/memory/scoped_ptr.h"
#include "xwalk/extensions/browser/xwalk_extension_service.h"
#include "xwalk/extensions/common/xwalk_extension.h"

namespace xwalk {
namespace extensions {

class XWalkExtensionBridgeInstance;

class XWalkExtensionBridge : public XWalkExtension {
 public:
  XWalkExtensionBridge(JNIEnv* env, jobject obj,
                       jint api_version, jstring name, jstring js_api);
  virtual ~XWalkExtensionBridge();

  void PostMessage(JNIEnv* env, jobject obj, jstring msg);

  virtual const char* GetJavaScriptAPI() OVERRIDE;
  virtual XWalkExtensionInstance* CreateInstance(
      const XWalkExtension::PostMessageCallback& post_message) OVERRIDE;

 private:
  bool is_valid();

  XWalkExtensionBridgeInstance* instance_;
  JavaObjectWeakGlobalRef java_ref_;
  std::string js_api_;

  DISALLOW_COPY_AND_ASSIGN(XWalkExtensionBridge);
};

class XWalkExtensionBridgeInstance
      : public XWalkExtensionInstance,
        public base::RefCountedThreadSafe<XWalkExtensionBridgeInstance> {
 public:
  explicit XWalkExtensionBridgeInstance(
      const JavaObjectWeakGlobalRef& java_ref);
  ~XWalkExtensionBridgeInstance();

  void PostMessageWrapper(const char* msg) {
    PostMessageToJS(scoped_ptr<base::Value>(new base::StringValue(msg)));
  }

 private:
  virtual void HandleMessage(scoped_ptr<base::Value> msg) OVERRIDE;
  virtual scoped_ptr<base::Value> HandleSyncMessage(
      scoped_ptr<base::Value> msg) OVERRIDE;

  void HandleMessageToJNI(const std::string& msg);
  std::string HandleSyncMessageToJNI(const std::string& msg);
  void ReturnSyncMessageToJS(std::string* ret_val, const std::string& msg);

  JavaObjectWeakGlobalRef java_ref_;

  DISALLOW_COPY_AND_ASSIGN(XWalkExtensionBridgeInstance);
};

bool RegisterXWalkExtensionBridge(JNIEnv* env);

}  // namespace extensions
}  // namespace xwalk

#endif  // XWALK_EXTENSIONS_COMMON_ANDROID_XWALK_EXTENSION_BRIDGE_H_
