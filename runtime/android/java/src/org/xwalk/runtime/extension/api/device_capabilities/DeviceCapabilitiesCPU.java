// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.extension.api.device_capabilities;

import android.util.Log;

import java.io.RandomAccessFile;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceCapabilitiesCPU {
    public static final String SYSTEM_INFO_STAT_FILE = "/proc/stat";

    private static final String TAG = "DeviceCapabilitiesCPU";

    private int mCoreNum = 0;
    private String mCPUArch = "Unknown";
    private double mCPULoad = 0.0;

    public DeviceCapabilitiesCPU() {
        getCPUArch();
        getCPUCoreNumber();
    }

    public JSONObject getInfo() {
        JSONObject o = new JSONObject();
        if (getCPULoad()) {
            try {
                o.put("numOfProcessors", mCoreNum);
                o.put("archName", mCPUArch);
                o.put("load", mCPULoad);
            } catch (JSONException e) {
                return setErrorMessage(e.toString());
            }
        } else {
            try {
                o.put("numOfProcessors", 0);
                o.put("archName", "Unknown");
                o.put("load", 0.0);
            } catch (JSONException e) {
                return setErrorMessage(e.toString());
            }
        }
        return o;
    }

    private void getCPUCoreNumber() {
        mCoreNum = Runtime.getRuntime().availableProcessors();
    }

    private void getCPUArch() {
        mCPUArch = System.getProperty("os.arch");
    }

    /**
     * The algorithm here can be found at:
     * http://stackoverflow.com/questions/3017162/how-to-get-total-cpu-usage-in-linux-c
     */
    private boolean getCPULoad() {
        try {
            RandomAccessFile r = new RandomAccessFile(SYSTEM_INFO_STAT_FILE, "r");
            String l = r.readLine();

            String[] arrs = l.split(" ");
            long used1 = Long.parseLong(arrs[2]) + Long.parseLong(arrs[3])
                         + Long.parseLong(arrs[4]);
            long total1 = used1
                          + Long.parseLong(arrs[5]) + Long.parseLong(arrs[6])
                          + Long.parseLong(arrs[7]) + Long.parseLong(arrs[8]);
            try {
                Thread.sleep(360);
            } catch (Exception e) {
                mCPULoad = 0.0;
                return false;
            }

            r.seek(0);
            l = r.readLine();
            r.close();

            arrs = l.split(" ");
            long used2 = Long.parseLong(arrs[2]) + Long.parseLong(arrs[3])
                         + Long.parseLong(arrs[4]);
            long total2 = used1
                          + Long.parseLong(arrs[5]) + Long.parseLong(arrs[6])
                          + Long.parseLong(arrs[7]) + Long.parseLong(arrs[8]);

            mCPULoad = (double) (used2 - used1) / (total2 - total1);
        } catch (IOException e) {
            mCPULoad = 0.0;
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
