// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "xwalk/runtime/renderer/pepper/xwalk_renderer_pepper_host_factory.h"

#include "base/logging.h"
#include "content/public/renderer/renderer_ppapi_host.h"
#include "ppapi/host/ppapi_host.h"
#include "ppapi/host/resource_host.h"
#include "ppapi/proxy/ppapi_messages.h"
#include "ppapi/proxy/ppapi_message_utils.h"
#include "ppapi/shared_impl/ppapi_permissions.h"
#include "xwalk/runtime/renderer/pepper/pepper_uma_host.h"

using ppapi::host::ResourceHost;

XWalkRendererPepperHostFactory::XWalkRendererPepperHostFactory(
    content::RendererPpapiHost* host)
    : host_(host) {
}

XWalkRendererPepperHostFactory::~XWalkRendererPepperHostFactory() {
}

scoped_ptr<ResourceHost>
XWalkRendererPepperHostFactory::CreateResourceHost(
    ppapi::host::PpapiHost* host,
    const ppapi::proxy::ResourceMessageCallParams& params,
    PP_Instance instance,
    const IPC::Message& message) {
  DCHECK(host == host_->GetPpapiHost());

  // Make sure the plugin is giving us a valid instance for this resource.
  if (!host_->IsValidInstance(instance))
    return scoped_ptr<ResourceHost>();

  // Permissions for the following interfaces will be checked at the
  // time of the corresponding instance's method calls.  Currently these
  // interfaces are available only for whitelisted apps which may not have
  // access to the other private interfaces.
  switch (message.type()) {
    case PpapiHostMsg_UMA_Create::ID: {
      return scoped_ptr<ResourceHost>(new PepperUMAHost(
          host_, instance, params.pp_resource()));
    }
  }

  return scoped_ptr<ResourceHost>();
}
