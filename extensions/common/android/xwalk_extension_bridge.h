// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef XWALK_EXTENSIONS_COMMON_ANDROID_XWALK_EXTENSION_BRIDGE_H_
#define XWALK_EXTENSIONS_COMMON_ANDROID_XWALK_EXTENSION_BRIDGE_H_

#include <string>

#include "base/android/jni_helper.h"
#include "base/android/scoped_java_ref.h"
#include "base/logging.h"
#include "base/memory/scoped_ptr.h"
#include "xwalk/extensions/browser/xwalk_extension_service.h"
#include "xwalk/extensions/common/xwalk_extension.h"

namespace xwalk {
namespace extensions {

class XWalkExtensionBridge : public XWalkExtension {
 public:
  XWalkExtensionBridge(JNIEnv* env, jobject obj,
                       jint api_version, jstring name, jstring js_api);
  virtual ~XWalkExtensionBridge();

  void PostMessage(JNIEnv* env, jobject obj, jstring msg);

  virtual const char* GetJavaScriptAPI() OVERRIDE;
  virtual XWalkExtension::Context* CreateContext(
      const XWalkExtension::PostMessageCallback& post_message) OVERRIDE;
  void RegisterExtensions(XWalkExtensionService* extension_service);

 private:
  class Context : public XWalkExtension::Context {
   public:
    Context(JNIEnv* env, jobject obj,
        const XWalkExtension::PostMessageCallback& post_message);
    ~Context();

    inline void PostMessageWrapper(const char* msg) {
      PostMessage(scoped_ptr<base::Value>(new base::StringValue(msg)));
    }

   private:
    virtual void HandleMessage(scoped_ptr<base::Value> msg) OVERRIDE;
    virtual scoped_ptr<base::Value> HandleSyncMessage(
        scoped_ptr<base::Value> msg) OVERRIDE;

    JNIEnv* jni_env_;
    jobject jni_obj_;

    DISALLOW_COPY_AND_ASSIGN(Context);
  };

  bool is_valid();

  Context* context_;
  JNIEnv* jni_env_;
  jobject jni_obj_;
  std::string js_api_;

  DISALLOW_COPY_AND_ASSIGN(XWalkExtensionBridge);
};

bool RegisterXWalkExtensionBridge(JNIEnv* env);

}  // namespace extensions
}  // namespace xwalk

#endif  // XWALK_EXTENSIONS_COMMON_ANDROID_XWALK_EXTENSION_BRIDGE_H_
