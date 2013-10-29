// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.extension.api.device_capabilities;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.runtime.extension.XWalkExtensionContext;

public class DeviceCapabilitiesMemory {
    private static final String TAG = "DeviceCapabilitiesMemory";

    private DeviceCapabilities mDeviceCapabilities;
    private Context mContext;

    private long mAvailableCapacity = 0;
    private long mCapacity = 0;

    public DeviceCapabilitiesMemory(DeviceCapabilities instance,
                                    XWalkExtensionContext context) {
        mDeviceCapabilities = instance;
        mContext = context.getContext();
    }

    public JSONObject getInfo() {
        readMemoryInfo();

        JSONObject o = new JSONObject();
        try {
            o.put("capacity", mCapacity);
            o.put("availCapacity", mAvailableCapacity);
        } catch (JSONException e) {
            return mDeviceCapabilities.setErrorMessage(e.toString());
        }

        return o;
    }

    private void readMemoryInfo() {
        MemoryInfo mi = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        mCapacity = mi.totalMem / 1024;
        mAvailableCapacity = mi.availMem / 1024;
    }
}
