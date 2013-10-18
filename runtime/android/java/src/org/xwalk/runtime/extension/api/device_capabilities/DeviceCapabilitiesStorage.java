// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.extension.api;

import org.xwalk.runtime.extension.XWalkExtensionContext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class DeviceCapabilitiesStorage {
    private static final String TAG = "DeviceCapabilitiesStorage";

    private DeviceCapabilities mDeviceCapabilities;

    private XWalkExtensionContext mExtensionContext;

    private boolean mIsRegisteredOnAttach = false;
    private boolean mIsRegisteredOnDetach = false;
    private long[] mInternalInfo = new long[2];
    // FIXME(guanxian): Assuming there is only one SDCard.
    private long[] mSDCardInfo = new long[2];

    private IntentFilter mIntentFilter = new IntentFilter();

    // FIXME(guanxian): Listeners only return the event message signals.
    private final BroadcastReceiver mOnAttachListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                JSONObject sdCardObject = new JSONObject();
                try {
                    sdCardObject.put("reply", "attachStorage");
                    sdCardObject.put("eventName", "onattach");
                    sdCardObject.put("data", "attach"); // Attach signal.
                } catch (JSONException e) {
                    mDeviceCapabilities.sendErrorMessage(e);
                }
                mDeviceCapabilities.postMessage(sdCardObject.toString());
            }
        }
    };

    private final BroadcastReceiver mOnDetachListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
                || Intent.ACTION_MEDIA_REMOVED.equals(action)
                || Intent.ACTION_MEDIA_BAD_REMOVAL.equals(action)) {
                JSONObject sdCardObject = new JSONObject();
                try {
                    sdCardObject.put("reply", "detachStorage");
                    sdCardObject.put("eventName", "ondetach");
                    sdCardObject.put("data", "detach"); // Detach signal.
                } catch (JSONException e) {
                    mDeviceCapabilities.sendErrorMessage(e);
                }
                mDeviceCapabilities.postMessage(sdCardObject.toString());
            }
        }
    };

    public DeviceCapabilitiesStorage(DeviceCapabilities instance) {
        mDeviceCapabilities = instance;
        registerIntentFilter();
    }

    public void setExtensionContext(XWalkExtensionContext context) {
        mExtensionContext = context;
    }

    public JSONObject getInfo() {
        JSONObject outputObject = new JSONObject();
        JSONArray outputArray = new JSONArray();
        try {
            if (getStorageInfo()) {
                JSONObject internalObject = new JSONObject();
                internalObject.put("id", 1);
                internalObject.put("name", "Internal");
                internalObject.put("type", "fixed");
                internalObject.put("capacity", mInternalInfo[0]);
                internalObject.put("availCapacity", mInternalInfo[1]);
                outputArray.put(internalObject);

                // FIXME(guanxian): Assuming there is only one SDCard.
                JSONObject sdCardObject = new JSONObject();
                sdCardObject.put("id", 2);
                sdCardObject.put("name", "SDCard");
                if (Environment.isExternalStorageRemovable()) {
                    sdCardObject.put("type", "removable");
                } else {
                    sdCardObject.put("type", "fixed");
                }
                sdCardObject.put("capacity", mSDCardInfo[0]);
                sdCardObject.put("availCapacity", mSDCardInfo[1]);
                outputArray.put(sdCardObject);
            }
            outputObject.put("storages", outputArray);
        } catch (JSONException e) {
            return setErrorMessage(e.toString());
        }
        return outputObject;
    }

    public void registerOnAttachListener() {
        if (!mIsRegisteredOnAttach) {
            mExtensionContext.getActivity().registerReceiver(mOnAttachListener,
                    mIntentFilter);
            mIsRegisteredOnAttach = true;
        }
    }

    public void registerOnDetachListener() {
        if (!mIsRegisteredOnDetach) {
            mExtensionContext.getActivity().registerReceiver(mOnDetachListener,
                    mIntentFilter);
            mIsRegisteredOnDetach = true;
        }
    }

    public void unregisterListeners() {
        if (mIsRegisteredOnAttach) {
            mExtensionContext.getActivity().unregisterReceiver(mOnAttachListener);
        }
        if (mIsRegisteredOnDetach) {
            mExtensionContext.getActivity().unregisterReceiver(mOnDetachListener);
        }
        mIsRegisteredOnAttach = false;
        mIsRegisteredOnDetach = false;
    }

    private boolean getStorageInfo() {
        File internalDir = Environment.getDataDirectory();
        StatFs internalStatFs = new StatFs(internalDir.getPath());
        long bSizeInternal = internalStatFs.getBlockSize();

        mInternalInfo[0] = bSizeInternal * internalStatFs.getBlockCount() / 1024;
        mInternalInfo[1] =
                bSizeInternal * internalStatFs.getAvailableBlocks() / 1024;

        // FIXME(guanxian): This Environment API can't figure out whether the 
        //                  external directory is emulated or physical.
        String externalStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
            File sdCardDir = Environment.getExternalStorageDirectory();
            StatFs sdCardStatfs = new StatFs(sdCardDir.getPath());
            long bSizeSDCard = sdCardStatfs.getBlockSize();

            mSDCardInfo[0] = bSizeSDCard * sdCardStatfs.getBlockCount() / 1024;
            mSDCardInfo[1] = bSizeSDCard * sdCardStatfs.getAvailableBlocks() / 1024;
        }
        return true;
    }

    private void registerIntentFilter() {
        mIntentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        mIntentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mIntentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        mIntentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        mIntentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        mIntentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mIntentFilter.addDataScheme("file");
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
