// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.extension.api;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceCapabilitiesMemory {
    public static final int SYSTEM_INFO_MEMORY_FILE_SIZE = 8 * 1024;
    public static final String SYSTEM_INFO_MEMORY_FILE = "/proc/meminfo";

    private static final String TAG = "DeviceCapabilitiesMemory";

    private BufferedReader mBufferedReader;
    private FileReader mFileReader;

    private long mAvailableCapacity = 0;
    private long mCapacity = 0;

    public DeviceCapabilitiesMemory() {
    }

    public JSONObject getInfo() {
        JSONObject outputObject = new JSONObject();
        if (readMemoryInfo()) {
            try {
                outputObject.put("capacity", mCapacity);
                outputObject.put("availCapacity", mAvailableCapacity);
            } catch (JSONException e) {
                return setErrorMessage(e.toString());
            }
        } else {
            try {
                outputObject.put("capacity", 0);
                outputObject.put("availCapacity", 0);
            } catch (JSONException e) {
                return setErrorMessage(e.toString());
            }
        }
        return outputObject;
    }

    private boolean readMemoryInfo() {
        /**
         * free memory calculation:
         * actual free memory = cached > total ? free : free + buffers + cached
         */
        try {
            mFileReader = new FileReader(SYSTEM_INFO_MEMORY_FILE);
            mBufferedReader =
                    new BufferedReader(mFileReader, SYSTEM_INFO_MEMORY_FILE_SIZE);
            String[] sMemTotal = mBufferedReader.readLine().split("\\s+");
            String[] sMemFree = mBufferedReader.readLine().split("\\s+");
            String[] sBuffers = mBufferedReader.readLine().split("\\s+");
            String[] sCached = mBufferedReader.readLine().split("\\s+");

            mCapacity = Integer.valueOf(sMemTotal[1]).intValue();
            long freeMemory = Integer.valueOf(sMemFree[1]).intValue();
            long buffersMemory = Integer.valueOf(sBuffers[1]).intValue();
            long cachedMemory = Integer.valueOf(sCached[1]).intValue();

            if (cachedMemory > mCapacity) {
                mAvailableCapacity = freeMemory;
            } else {
                mAvailableCapacity = freeMemory + buffersMemory + cachedMemory;
            }
            mBufferedReader.close();
        } catch (IOException e) {
            return false;
        }
        return true;
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
