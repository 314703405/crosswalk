// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.extension.api.device_capabilities;

import java.util.HashSet;

public class MediaCodecNull extends XWalkMediaCodec {
    public MediaCodecNull(DeviceCapabilities instance) {
        mDeviceCapabilities = instance;

        mAudioCodecsSet = new HashSet<AudioCodecElement>();
        mVideoCodecsSet = new HashSet<VideoCodecElement>();
    }

    @Override
    protected void getCodecsList() {
        return;
    }
}
