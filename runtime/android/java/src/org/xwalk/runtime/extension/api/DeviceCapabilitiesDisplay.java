// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.extension.api;

import org.xwalk.runtime.extension.XWalkExtensionContext;

import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DeviceCapabilitiesDisplay {
    private static final String TAG = "DeviceCapabilitiesDisplay";

    private DeviceCapabilities mDeviceCapabilities;

    private Context mContext;
    private DisplayManager mDisplayManager;
    private XWalkExtensionContext mExtensionContext;

    private boolean mIsRegisteredOnConnect = false;
    private boolean mIsRegisteredOnDisconnect = false;

    class DisplayInfo {
        private boolean mIsInternal;
        private boolean mIsPrimary;
        private int mDpiX;
        private int mDpiY;
        private long mAvailableHeight;
        private long mAvailableWidth;
        private long mHeight;
        private long mWidth;
        private String mDisplayId;
        private String mDisplayName;

        public DisplayInfo(boolean isInternal, boolean isPrimary, int dpiX, int dpiY,
                           long availableHeight, long availableWidth, long height,
                           long width, String displayId, String displayName) {
            mIsInternal = isInternal;
            mIsPrimary = isPrimary;
            mDpiX = dpiX;
            mDpiY = dpiY;
            mAvailableHeight = availableHeight;
            mAvailableWidth = availableWidth;
            mHeight = height;
            mWidth = width;
            mDisplayId = displayId;
            mDisplayName = displayName;
        }
    }

    private Map<String, DisplayInfo> mDisplays = new HashMap<String, DisplayInfo>();
    private final Handler mHandler = new Handler();

    private final DisplayListener mOnConnectListener = new DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            updateInfo();
            JSONObject outputObject = new JSONObject();
            JSONObject displayObject = setDisplayById(String.valueOf(displayId));
            try {
                outputObject.put("reply", "connectDisplay");
                outputObject.put("eventName", "onconnect");
                outputObject.put("data", displayObject);
            } catch (JSONException e) {
                mDeviceCapabilities.sendErrorMessage(e);
            }
            mDeviceCapabilities.postMessage(outputObject.toString());
        }
        @Override
        public void onDisplayChanged(int arg0) {
        }
        @Override
        public void onDisplayRemoved(int displayId) {
            if (!mIsRegisteredOnDisconnect)
                updateInfo();
        }
    };

    private final DisplayListener mOnDisconnectListener = new DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            if (!mIsRegisteredOnConnect)
                updateInfo();
        }
        @Override
        public void onDisplayChanged(int arg0) {
        }
        @Override
        public void onDisplayRemoved(int displayId) {
            JSONObject outputObject = new JSONObject();
            JSONObject displayObject = setDisplayById(String.valueOf(displayId));
            updateInfo();
            try {
                outputObject.put("reply", "disconnectDisplay");
                outputObject.put("eventName", "ondisconnect");
                outputObject.put("data", displayObject);
            } catch (JSONException e) {
                mDeviceCapabilities.sendErrorMessage(e);
            }
            mDeviceCapabilities.postMessage(outputObject.toString());
        }
    };

    public DeviceCapabilitiesDisplay(DeviceCapabilities instance) {
        mDeviceCapabilities = instance;
    }

    public void setExtensionContext(XWalkExtensionContext context) {
        mExtensionContext = context;
    }

    public JSONObject getInfo() {
        JSONObject outputObject = new JSONObject();
        JSONArray outputArray = new JSONArray();

        updateInfo();
        try {
            Iterator<String> iter = mDisplays.keySet().iterator();
            while (iter.hasNext()) {
                JSONObject displayObject = setDisplayById(iter.next());
                outputArray.put(displayObject);
            }
            outputObject.put("displays", outputArray);
        } catch (JSONException e) {
            return setErrorMessage(e.toString());
        }
        return outputObject;
    }

    public JSONObject setDisplayById(String displayId) {
        DisplayInfo displayInfo = mDisplays.get(displayId);
        JSONObject retObject = new JSONObject();

        try {
            retObject.put("id", displayInfo.mDisplayId);
            retObject.put("name", displayInfo.mDisplayName);
            retObject.put("isPrimary", displayInfo.mIsPrimary);
            retObject.put("isInternal", displayInfo.mIsInternal);
            retObject.put("dpiX", displayInfo.mDpiX);
            retObject.put("dpiY", displayInfo.mDpiY);
            retObject.put("width", displayInfo.mWidth);
            retObject.put("height", displayInfo.mHeight);
            retObject.put("availWidth", displayInfo.mAvailableWidth);
            retObject.put("availHeight", displayInfo.mAvailableHeight);
        } catch (JSONException e) {
            return setErrorMessage(e.toString());
        }
        return retObject;
    }

    private void updateInfo() {
        mContext = mExtensionContext.getContext();
        mDisplayManager =
                (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        Display[] dispArr = mDisplayManager.getDisplays();

        mDisplays.clear();
        for (Display display : dispArr) {
            String displayId = String.valueOf(display.getDisplayId());
            String displayName = display.getName();
            boolean isPrimary;
            boolean isInternal;
            if (displayId.equals(String.valueOf(display.DEFAULT_DISPLAY))) {
                isPrimary = true;
                isInternal = true;
            } else {
                isPrimary = false;
                isInternal = false;
            }
            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getRealMetrics(displayMetrics);
            int dpiX = (int) displayMetrics.xdpi;
            int dpiY = (int) displayMetrics.ydpi;
            Point outSize = new Point();
            display.getRealSize(outSize);
            int width = outSize.x;
            int height = outSize.y;
            display.getSize(outSize);
            int availableWidth = outSize.x;
            int availableHeight = outSize.y;
            DisplayInfo newDisplay = new DisplayInfo(isInternal, isPrimary, dpiX,
                    dpiY, availableHeight, availableWidth, height, width, displayId,
                    displayName);
            mDisplays.put(displayId, newDisplay);
        }
    }

    public void registerOnConnectListener() {
        mContext = mExtensionContext.getContext();
        mDisplayManager =
                (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        if (!mIsRegisteredOnDisconnect)
            updateInfo();
        if (!mIsRegisteredOnConnect) {
            mDisplayManager.registerDisplayListener(mOnConnectListener, mHandler);
            mIsRegisteredOnConnect = true;
        }
    }

    public void registerOnDisonnectListener() {
        mContext = mExtensionContext.getContext();
        mDisplayManager =
                (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        if (!mIsRegisteredOnConnect)
            updateInfo();
        if (!mIsRegisteredOnDisconnect) {
            mDisplayManager.registerDisplayListener(mOnDisconnectListener, mHandler);
            mIsRegisteredOnDisconnect = true;
        }
    }

    public void unregisterListener() {
        if (mIsRegisteredOnConnect) {
            mDisplayManager.unregisterDisplayListener(mOnConnectListener);
        }
        if (mIsRegisteredOnDisconnect) {
            mDisplayManager.unregisterDisplayListener(mOnDisconnectListener);
        }
        mIsRegisteredOnConnect = false;
        mIsRegisteredOnDisconnect = false;
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
