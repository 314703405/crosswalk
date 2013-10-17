package org.xwalk.runtime.extension.api;

import org.xwalk.runtime.extension.XWalkExtensionContext;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DeviceCapabilitiesCodecs {
    private static final String TAG = "DeviceCapabilitiesCodecs";
    private boolean mEncode;
    private boolean mHwAccel;
    private String mFormat;

    public DeviceCapabilitiesCodecs() {
    }

    public JSONObject getInfo() {
        JSONObject outputObject = new JSONObject();

        JSONArray audioCodecsArray = new JSONArray();
        JSONArray videoCodecsArray = new JSONArray();

        int numCodecs = MediaCodecList.getCodecCount();
        try {
            for (int i = 0; i < numCodecs; i++) {
                MediaCodecInfo codeInfo = MediaCodecList.getCodecInfoAt(i);
                String[] formats = codeInfo.getSupportedTypes();

                for (String format : formats) {
                    JSONObject codecsObject = new JSONObject();

                    String[] msgArray = format.split("/", 2);
                    // FIXME get the value of hardware-accelerate
                    codecsObject.put("format", codeInfo.getName());
                    if (msgArray[0].equals("audio")) {
                        audioCodecsArray.put(codecsObject);
                    } else if (msgArray[0].equals("video")) {
                        codecsObject.put("encode", codeInfo.isEncoder());
                        codecsObject.put("hwAccel", false);
                        videoCodecsArray.put(codecsObject);
                    }
                }
            }

            outputObject.put("audioCodecs", audioCodecsArray);
            outputObject.put("videoCodecs", videoCodecsArray);
        } catch (JSONException e) {
            return setErrorMessage(e.toString());
        }

        return outputObject;
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
