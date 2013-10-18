// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.extension.api;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceCapabilitiesCPU {
    public static final int SYSTEM_INFO_STAT_FILE_SIZE = 1024;
    public static final String SYSTEM_INFO_CPU_DIR = "/sys/devices/system/cpu/";
    public static final String SYSTEM_INFO_CPUINFO_FILE = "/proc/cpuinfo";
    public static final String SYSTEM_INFO_STAT_FILE = "/proc/stat";

    private static final String TAG = "DeviceCapabilitiesCPU";

    private BufferedReader mBufferedReader;
    private FileReader mFileReader;
    private FileInputStream mFileInputStream;
    private InputStreamReader mInputStreamReader;

    private double mCPULoad = 0.0;
    private int mCoreNum = 0;
    private long mOldCPUTotal = 0;
    private long mOldCPUUsed = 0;
    private String mCPUArch = "";

    public DeviceCapabilitiesCPU() {
        getCPULoad();
    }

    public JSONObject getInfo() {
        JSONObject outputObject = new JSONObject();
        if (getCPUCoreNumber() && getCPUArch() && getCPULoad()) {
            try {
                outputObject.put("numOfProcessors", mCoreNum);
                outputObject.put("archName", mCPUArch);
                outputObject.put("load", mCPULoad);
            } catch (JSONException e) {
                return setErrorMessage(e.toString());
            }
        } else {
            try {
                outputObject.put("numOfProcessors", 0);
                outputObject.put("archName", "");
                outputObject.put("load", 0.0);
            } catch (JSONException e) {
                return setErrorMessage(e.toString());
            }
        }
        return outputObject;
    }

    private boolean getCPUCoreNumber() {
        class CoreFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }
        File cpuInfoDir = new File(SYSTEM_INFO_CPU_DIR);
        File[] coreFiles = cpuInfoDir.listFiles(new CoreFilter());
        mCoreNum = coreFiles.length;
        return true;
    }

    private boolean getCPUArch() {
        try {
            mFileReader = new FileReader(SYSTEM_INFO_CPUINFO_FILE);
            mBufferedReader = new BufferedReader(mFileReader);

            mCPUArch = "";
            String readLine = "";
            while ((readLine = mBufferedReader.readLine()) != null) {
                String[] lineArray = readLine.split(":\\s+", 2);
                if (lineArray[0].contains("model name")) {
                    mCPUArch = lineArray[1];
                    break;
                }
            }
            if (mCPUArch.isEmpty()) {
                mCPUArch = "Unknown";
            }
        } catch (IOException e) {
            mCPUArch = "Unknown";
            return false;
        }
        return true;
    }

    /**
     * The algorithm here can be found at:
     * http://stackoverflow.com/questions/3017162
     * /how-to-get-total-cpu-usage-in-linux-c
     *
     * The process is:
     * work_over_period = work_jiffies_2 - work_jiffies_1
     * total_over_period = total_jiffies_2 - total_jiffies_1
     * cpu_load = work_over_period / total_over_period
     */
    private boolean getCPULoad() {
        try {
            mFileInputStream = new FileInputStream(SYSTEM_INFO_STAT_FILE);
            mInputStreamReader = new InputStreamReader(mFileInputStream);
            mBufferedReader = new BufferedReader(mInputStreamReader,
                    SYSTEM_INFO_STAT_FILE_SIZE);
            String[] cpuInfoArray = mBufferedReader.readLine().split(" ");
            mBufferedReader.close();

            long curCPUUsed = Long.parseLong(cpuInfoArray[2])
                              + Long.parseLong(cpuInfoArray[3])
                              + Long.parseLong(cpuInfoArray[4]);
            long curCPUTotal = curCPUUsed
                               + Long.parseLong(cpuInfoArray[5])
                               + Long.parseLong(cpuInfoArray[6])
                               + Long.parseLong(cpuInfoArray[7])
                               + Long.parseLong(cpuInfoArray[8]);
            mCPULoad = (double) (curCPUUsed - mOldCPUUsed)
                       / (curCPUTotal- mOldCPUTotal);
            mOldCPUUsed = curCPUUsed;
            mOldCPUTotal = curCPUTotal;
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
