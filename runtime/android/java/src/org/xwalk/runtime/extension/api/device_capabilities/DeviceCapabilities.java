// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.extension.api;

import android.util.Log;

import org.xwalk.runtime.extension.XWalkExtension;
import org.xwalk.runtime.extension.XWalkExtensionContext;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceCapabilities extends XWalkExtension {
    public static final String NAME = "navigator.system";
    public static final String JS_API_PATH = "js_api/device_capabilities.js";

    private static final String TAG = "DeviceCapabilities";

    private DeviceCapabilitiesCPU mCPU = new DeviceCapabilitiesCPU();
    private DeviceCapabilitiesDisplay mDisplay = new DeviceCapabilitiesDisplay(this);
    private DeviceCapabilitiesMemory mMemory = new DeviceCapabilitiesMemory();
    private DeviceCapabilitiesStorage mStorage = new DeviceCapabilitiesStorage(this);

    public DeviceCapabilities(String jsApiContent, XWalkExtensionContext context) {
        super(NAME, jsApiContent, context);
        mDisplay.setExtensionContext(context);
        mStorage.setExtensionContext(context);
    }

    private void handleMessage(int instanceID, String message) {
        try {
            JSONObject jsonInput = new JSONObject(message);
            String cmd = jsonInput.getString("cmd");
            if (cmd.equals("addEventListener")) {
                String eventName = jsonInput.getString("eventName");
                handleAddEventListener(eventName);
            } else {
                String promiseId = jsonInput.getString("_promise_id");
                handleGetDeviceInfo(instanceID, promiseId, cmd);
            }
        } catch (JSONException e) {
            sendErrorMessage(instanceID, e);
        }
    }

    private void handleGetDeviceInfo(int instanceID, String promiseId, String cmd) {
        try {
            JSONObject jsonOutput = new JSONObject();
            if (cmd.equals("getCPUInfo")) {
                jsonOutput.put("data", mCPU.getInfo());
            } else if (cmd.equals("getDisplayInfo")) {
                jsonOutput.put("data", mDisplay.getInfo());
            } else if (cmd.equals("getMemoryInfo")) {
                jsonOutput.put("data", mMemory.getInfo());
            } else if (cmd.equals("getStorageInfo")) {
                jsonOutput.put("data", mStorage.getInfo());
            }
            jsonOutput.put("_promise_id", promiseId);
            this.postMessage(instanceID, jsonOutput.toString());
        } catch (JSONException e) {
            sendErrorMessage(instanceID, e);
        }
    }

    private void handleAddEventListener(String eventName) {
        if (eventName.equals("onattach")) {
            mStorage.registerOnAttachListener();
        } else if (eventName.equals("ondetach")) {
            mStorage.registerOnDetachListener();
        } else if (eventName.equals("onconnect")) {
            mDisplay.registerOnConnectListener();
        } else if (eventName.equals("ondisconnect")) {
            mDisplay.registerOnDisonnectListener();
        }
    }

    protected void sendErrorMessage(int instanceID, JSONException jsonException) {
        try {
            JSONObject jsonError = new JSONObject();
            jsonError.put("error", jsonException.toString());
            this.postMessage(instanceID, jsonError.toString());
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    protected void broadcastErrorMessage(JSONException jsonException) {
        try {
            JSONObject jsonError = new JSONObject();
            jsonError.put("error", jsonException.toString());
            this.broadcastMessage(jsonError.toString());
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onMessage(int instanceID, String message) {
        if (!message.isEmpty()) {
            handleMessage(instanceID, message);
        }
    }

    @Override
    public void onDestroy() {
        mDisplay.unregisterListener();
        mStorage.unregisterListeners();
    }
}
