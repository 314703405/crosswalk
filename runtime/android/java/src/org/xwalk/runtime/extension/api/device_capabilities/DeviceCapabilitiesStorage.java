// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.extension.api.device_capabilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import org.xwalk.runtime.extension.XWalkExtensionContext;

public class DeviceCapabilitiesStorage {
    private static final String TAG = "DeviceCapabilitiesStorage";
    private DeviceCapabilities mDeviceCapabilities;
    private XWalkExtensionContext mExtensionContext;

    private static int mStorageCount = 0;

    private boolean mIsListening = false;

    private IntentFilter mIntentFilter = new IntentFilter();

    // Holds all available storages.
    private final SparseArray<StorageUnit> mStorageList = new SparseArray<StorageUnit>();

    class StorageUnit {
        private int mId;
        private String mName;
        private String mType;
        private long mCapacity;
        private long mAvailCapacity;
        private String mPath;

        public StorageUnit(int id, String name, String type) {
            mId = id;
            mName = name;
            mType = type;
            mPath = "";
            mCapacity = 0;
            mAvailCapacity = 0;
        }

        public int getId() { return mId; }
        public String getName() { return mName; }
        public String getType() { return mType; }
        public String getPath() { return mPath; }
        public long getCapacity() { return mCapacity; }
        public long getAvailCapacity() { return mAvailCapacity; }

        public void setType(String s) { mType = s;}
        public void setPath(String s) {
          mPath = s;
          updateCapacity();
        }

        public boolean isSame(StorageUnit s) {
            return mPath == s.getPath();
        }

        public boolean isValid() {
            if (mPath == null || mPath.isEmpty()) {
                mCapacity = 0;
                mAvailCapacity = 0;
                return false;
            }

            File f = new File(mPath);
            return f.canRead();
        }

        public void updateCapacity() {
            if (!isValid()) {
                return;
            }

            StatFs stat = new StatFs(mPath);
            // FIXME(halton): After API level 18, use getTotalBytes() and
            // getAvailableBytes() instead
            long blockSize = stat.getBlockSize();
            mCapacity = blockSize * stat.getBlockCount() / 1024;
            mAvailCapacity = blockSize * stat.getAvailableBlocks() / 1024;
        }

        public JSONObject convertToJSON() {
            JSONObject o = new JSONObject();

            try {
                o.put("id", mId + 1); // Display from 1
                o.put("name", mName);
                o.put("type", mType);
                o.put("capacity", mCapacity);
                o.put("availCapacity", mAvailCapacity);
            } catch (JSONException e) {
                return setErrorMessage(e.toString());
            }

            return o;
        }
    }

    private final BroadcastReceiver mStorageListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                notifyAndSaveAttachedStorage();
            }

            if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
                || Intent.ACTION_MEDIA_REMOVED.equals(action)
                || Intent.ACTION_MEDIA_BAD_REMOVAL.equals(action)) {
                notifyAndRemoveDetachedStorage();
            }
        }
    };

    public DeviceCapabilitiesStorage(DeviceCapabilities instance,
                                     XWalkExtensionContext context) {
        mDeviceCapabilities = instance;
        mExtensionContext = context;

        // Fetch the original storage list
        initStorageList();
        registerIntentFilter();
    }

    public JSONObject getInfo() {
        JSONObject o = new JSONObject();
        JSONArray arr = new JSONArray();
        try {
            for(int i = 0; i < mStorageList.size(); i++) {
                arr.put(mStorageList.valueAt(i).convertToJSON());
            }
            o.put("storages", arr);
        } catch (JSONException e) {
            return setErrorMessage(e.toString());
        }

        return o;
    }

    private void initStorageList() {
        mStorageList.clear();
        mStorageCount = 0;

        StorageUnit s = new StorageUnit(mStorageCount, "Internal", "fixed");
        s.setPath(Environment.getRootDirectory().getAbsolutePath());
        mStorageList.put(mStorageCount, s);
        ++mStorageCount;

        // Attempt to add emulated stroage first
        int sdcardNum = mStorageCount - 1; // sdcard count from 0
        s = new StorageUnit(mStorageCount, new String("sdcard" + Integer.toString(sdcardNum)), "fixed");
        if (Environment.isExternalStorageRemovable()) {
            s.setType("removable");
        }
        s.setPath(Environment.getExternalStorageDirectory().getAbsolutePath());

        if (s.isValid()) {
            mStorageList.put(mStorageCount, s);
            ++mStorageCount;
        }

        // Then attempt to add real removable storage
        attemptAddExternalStorage();
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

    private boolean attemptAddExternalStorage() {
        int sdcardNum = mStorageCount - 1;
        StorageUnit s = new StorageUnit(mStorageCount, new String("sdcard" + Integer.toString(sdcardNum)), "removable");
        s.setPath("/storage/sdcard" + Integer.toString(sdcardNum));

        if (!s.isValid()) {
            return false;
        }

        for(int i = 0; i < mStorageList.size(); i++) {
            if (s.isSame(mStorageList.valueAt(i))) {
                return false;
            }
        }

        mStorageList.put(mStorageCount, s);
        ++mStorageCount;
        return true;
    }

    public void registerListener() {
        if (mIsListening) {
            return;
        }

        mIsListening = true;
        mExtensionContext.getActivity().registerReceiver(mStorageListener, mIntentFilter);
    }

    public void unregisterListener() {
        if (!mIsListening) {
            return;
        }

        mIsListening = false;
        mExtensionContext.getActivity().unregisterReceiver(mStorageListener);
    }

    private void notifyAndSaveAttachedStorage() {
        if(!attemptAddExternalStorage()) {
            return;
        }

        StorageUnit s = mStorageList.valueAt(mStorageList.size() - 1);
        JSONObject o = new JSONObject();
        try {
            o.put("reply", "attachStorage");
            o.put("eventName", "onattach");
            o.put("data", s.convertToJSON());

            mDeviceCapabilities.broadcastMessage(o.toString());
        } catch (JSONException e) {
            mDeviceCapabilities.printErrorMessage(e);
        }

    }

    private void notifyAndRemoveDetachedStorage() {
        StorageUnit s = mStorageList.valueAt(mStorageList.size() - 1);

        if(s.getType() != "removable") {
            return;
        }

        JSONObject o = new JSONObject();
        try {
            o.put("reply", "detachStorage");
            o.put("eventName", "ondetach");
            o.put("data", s.convertToJSON());

            mDeviceCapabilities.broadcastMessage(o.toString());
            mStorageList.remove(s.getId());
            --mStorageCount;
        } catch (JSONException e) {
            mDeviceCapabilities.printErrorMessage(e);
        }
    }

    public void onResume() {
        // Fistly, check the lasted external storage is valid.
        // If not, remove it and send "ondetached" event.
        StorageUnit lastUnit = mStorageList.valueAt(mStorageList.size() - 1);
        if(!lastUnit.isValid()) {
            notifyAndRemoveDetachedStorage();
        }

        // Secondly, attmpt to add a possible external storage and send "onattached" event.
        notifyAndSaveAttachedStorage();
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
