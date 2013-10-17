// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.extension.api.device_capabilities;

import org.xwalk.runtime.extension.XWalkExtensionContext;

import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DeviceCapabilitiesDisplay {
    private static final String TAG = "DeviceCapabilitiesDisplay";

    private DeviceCapabilities mDeviceCapabilities;

    private DisplayManager mDisplayManager;

    private boolean mIsListening = false;
    private final Handler mHandler = new Handler();

    // Holds all available displays connected to the system.
    private final SparseArray<Display> mDisplayList = new SparseArray<Display>();

    /**
     * Listens for the display arrival and removal.
     *
     * We rely on onDisplayAdded/onDisplayRemoved callback to trigger the display
     * availability change event.
     *
     * Note the display id is a system-wide unique number for each physical connection.
     * It means that for the same display device, the display id assigned by the system
     * would be different if it is re-connected again.
     */
    private final DisplayListener mDisplayListener = new DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            // Broadcast and add the added display to JavaScript
            notifyAndSaveConnectedDisplay(mDisplayManager.getDisplay(displayId));
        }

        @Override
        public void onDisplayChanged(int arg0) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            Display d = mDisplayList.get(displayId);

            // Do nothing if the display does not exsit on cache.
            if (d == null) {
                return;
            }

            // Broadcast and remove the added display to JavaScript
            notifyAndRemoveDisconnectedDisplay(d);
        }
    };

    public DeviceCapabilitiesDisplay(DeviceCapabilities instance,
                                     XWalkExtensionContext context) {
        mDeviceCapabilities = instance;
        mDisplayManager =
                (DisplayManager) context.getContext().getSystemService(Context.DISPLAY_SERVICE);

        // Fetch the original display list
        initDisplayList();
    }

    public JSONObject getInfo() {
        JSONObject outputObject = new JSONObject();
        JSONArray outputArray = new JSONArray();

        try {
            for(int i = 0; i < mDisplayList.size(); i++) {
                outputArray.put(convertDisplayToJSON(mDisplayList.valueAt(i)));
            }
            outputObject.put("displays", outputArray);
        } catch (JSONException e) {
            return setErrorMessage(e.toString());
        }

        return outputObject;
    }

    public JSONObject convertDisplayToJSON(Display d) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getRealMetrics(displayMetrics);

        Point realSize = new Point();
        d.getRealSize(realSize);

        Point availSize = new Point();
        d.getSize(availSize);

        JSONObject o = new JSONObject();
        try {
            o.put("id", d.getDisplayId());
            o.put("name", d.getName());
            o.put("isPrimary", d.getDisplayId() == d.DEFAULT_DISPLAY);
            o.put("isInternal", d.getDisplayId() == d.DEFAULT_DISPLAY);
            o.put("dpiX", (int) displayMetrics.xdpi);
            o.put("dpiY", (int) displayMetrics.ydpi);
            o.put("width", realSize.x);
            o.put("height", realSize.y);
            o.put("availWidth", availSize.x);
            o.put("availHeight", availSize.y);
        } catch (JSONException e) {
            return setErrorMessage(e.toString());
        }
        return o;
    }

    private void initDisplayList() {
        Display[] displays = mDisplayManager.getDisplays();

        for (Display d : displays) {
            mDisplayList.put(d.getDisplayId(), d);
        }
    }

    private void notifyAndSaveConnectedDisplay(Display d) {
        JSONObject o = new JSONObject();
        try {
            o.put("reply", "connectDisplay");
            o.put("eventName", "onconnect");
            o.put("data", convertDisplayToJSON(d));
        } catch (JSONException e) {
            mDeviceCapabilities.printErrorMessage(e);
        }

        mDeviceCapabilities.broadcastMessage(o.toString());
        mDisplayList.put(d.getDisplayId(), d);
    }

    private void notifyAndRemoveDisconnectedDisplay(Display d) {
        JSONObject o = new JSONObject();
        try {
            o.put("reply", "disconnectDisplay");
            o.put("eventName", "ondisconnect");
            o.put("data", convertDisplayToJSON(d));
        } catch (JSONException e) {
            mDeviceCapabilities.printErrorMessage(e);
        }

        mDeviceCapabilities.broadcastMessage(o.toString());
        mDisplayList.remove(d.getDisplayId());
    }

    public void registerListener() {
        if(mIsListening) {
            return;
        }

        mIsListening = true;
        mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);
    }

    public void unregisterListener() {
        if(!mIsListening) {
            return;
        }

        mIsListening = false;
        mDisplayManager.unregisterDisplayListener(mDisplayListener);
    }

    public void onResume() {
        Display[] displays = mDisplayManager.getDisplays();

        // Firstly, check whether display in latest list is in cached display list.
        // If not found, then send out "onconnect" message and insert to cache.
        // If found, only update the display object without sending message.
        for (Display d : displays) {
            Display foundDisplay = mDisplayList.get(d.getDisplayId());
            if (foundDisplay == null) {
                notifyAndSaveConnectedDisplay(d);
            } else {
                mDisplayList.put(d.getDisplayId(), d);
            }
        }

        // Secondly, remove those displays that only in cache.
        for(int i = 0; i < mDisplayList.size(); i++) {
            boolean found = false;
            for (Display d : displays) {
                if (mDisplayList.valueAt(i).getDisplayId() == d.getDisplayId()) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                notifyAndRemoveDisconnectedDisplay(mDisplayList.valueAt(i));
            }
        }
    }

    private JSONObject setErrorMessage(String error) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("error", error);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
        return jsonObject;
    }
}
