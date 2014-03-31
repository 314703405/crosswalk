// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "xwalk/runtime/browser/nacl_host/nacl_browser_delegate_impl.h"

#include "base/path_service.h"
#include "base/strings/string_split.h"
#include "base/strings/string_util.h"
#include "content/public/browser/browser_thread.h"
#include "content/public/browser/render_frame_host.h"
#include "content/public/browser/site_instance.h"
#include "ppapi/c/private/ppb_nacl_private.h"
#include "xwalk/runtime/browser/renderer_host/pepper/xwalk_browser_pepper_host_factory.h"
#include "xwalk/runtime/common/xwalk_paths.h"

namespace {

// Handles an extension's NaCl process transitioning in or out of idle state by
// relaying the state to the extension's process manager.
//
// A NaCl instance, when active (making PPAPI calls or receiving callbacks),
// sends keepalive IPCs to the browser process BrowserPpapiHost at a throttled
// rate. The content::BrowserPpapiHost passes context information up to the
// chrome level NaClProcessHost where we use the instance's context to find the
// associated extension process manager.
//
// There is a 1:many relationship for extension:nacl-embeds, but only a
// 1:1 relationship for NaClProcessHost:PP_Instance. The content layer doesn't
// rely on this knowledge because it routes messages for ppapi non-nacl
// instances as well, though they won't have callbacks set. Here the 1:1
// assumption is made and DCHECKed.
void OnKeepaliveOnUIThread(
    const content::BrowserPpapiHost::OnKeepaliveInstanceData& instance_data,
    const base::FilePath& profile_data_directory) {
  /*
  DCHECK(content::BrowserThread::CurrentlyOn(content::BrowserThread::UI));

  // Only one instance will exist for NaCl embeds, even when more than one
  // embed of the same plugin exists on the same page.
  DCHECK(instance_data.size() == 1);
  if (instance_data.size() < 1)
    return;

  content::RenderFrameHost* render_frame_host =
      content::RenderFrameHost::FromID(
          instance_data[0].render_process_id, instance_data[0].render_frame_id);
  if (!render_frame_host)
    return;

  content::SiteInstance* site_instance = render_frame_host->GetSiteInstance();
  if (!site_instance)
    return;

  extensions::ExtensionSystem* extension_system =
      extensions::ExtensionSystem::Get(site_instance->GetBrowserContext());
  if (!extension_system)
    return;

  const ExtensionService* extension_service =
      extension_system->extension_service();
  if (!extension_service)
    return;

  const extensions::Extension* extension = extension_service->GetExtensionById(
      instance_data[0].document_url.host(), false);
  if (!extension)
    return;

  extensions::ProcessManager* pm = extension_system->process_manager();
  if (!pm)
    return;

  pm->KeepaliveImpulse(extension);
  */
}

// Calls OnKeepaliveOnUIThread on UI thread.
void OnKeepalive(
    const content::BrowserPpapiHost::OnKeepaliveInstanceData& instance_data,
    const base::FilePath& profile_data_directory) {
  DCHECK(!content::BrowserThread::CurrentlyOn(content::BrowserThread::UI));
  /*
  content::BrowserThread::PostTask(content::BrowserThread::UI, FROM_HERE,
                                   base::Bind(&OnKeepaliveOnUIThread,
                                              instance_data,
                                              profile_data_directory));
                                              */
}

}  // namespace

NaClBrowserDelegateImpl::NaClBrowserDelegateImpl() {
}

NaClBrowserDelegateImpl::~NaClBrowserDelegateImpl() {
}

void NaClBrowserDelegateImpl::ShowNaClInfobar(int render_process_id,
                                              int render_view_id,
                                              int error_id) {
  DCHECK_EQ(PP_NACL_MANIFEST_MISSING_ARCH, error_id);
  // TODO(halton): Add infobar support
}

bool NaClBrowserDelegateImpl::DialogsAreSuppressed() {
  return true;
  //return logging::DialogsAreSuppressed();
}

bool NaClBrowserDelegateImpl::GetCacheDirectory(base::FilePath* cache_dir) {
  // TODO(halton): cros support.
  return PathService::Get(xwalk::DIR_DATA_PATH, cache_dir);
}

bool NaClBrowserDelegateImpl::GetPluginDirectory(base::FilePath* plugin_dir) {
  return PathService::Get(xwalk::DIR_INTERNAL_PLUGINS, plugin_dir);
}

bool NaClBrowserDelegateImpl::GetPnaclDirectory(base::FilePath* pnacl_dir) {
  return PathService::Get(xwalk::DIR_PNACL_COMPONENT, pnacl_dir);
}

bool NaClBrowserDelegateImpl::GetUserDirectory(base::FilePath* user_dir) {
  return PathService::Get(xwalk::DIR_DATA_PATH, user_dir);
}

std::string NaClBrowserDelegateImpl::GetVersionString() const {
  // TODO(halton): return real version
  return "35.0.1903.0 (Developer Build 258503 Linux) custom aura";
}

ppapi::host::HostFactory* NaClBrowserDelegateImpl::CreatePpapiHostFactory(
    content::BrowserPpapiHost* ppapi_host) {
  return new xwalk::XWalkBrowserPepperHostFactory(ppapi_host);
}

void NaClBrowserDelegateImpl::SetDebugPatterns(std::string debug_patterns) {
}

bool NaClBrowserDelegateImpl::URLMatchesDebugPatterns(
    const GURL& manifest_url) {
  return false;
}

// This function is security sensitive.  Be sure to check with a security
// person before you modify it.
bool NaClBrowserDelegateImpl::MapUrlToLocalFilePath(
    const GURL& file_url, bool use_blocking_api, base::FilePath* file_path) {
  return false;
}

content::BrowserPpapiHost::OnKeepaliveCallback
NaClBrowserDelegateImpl::GetOnKeepaliveCallback() {
  return base::Bind(&OnKeepalive);
}
