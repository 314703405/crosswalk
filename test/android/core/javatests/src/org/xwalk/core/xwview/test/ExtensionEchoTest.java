// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core.xwview.test;

import android.graphics.Bitmap;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import org.chromium.base.test.util.Feature;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkClient;
import org.xwalk.core.xwview.test.ExtensionEcho;

/**
 * Test suite for ExtensionEcho().
 */
public class ExtensionEchoTest extends XWalkViewTestBase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        class TestXWalkClient extends XWalkClient {
            @Override
            public void onPageStarted(XWalkView view, String url, Bitmap favicon) {
                mTestContentsClient.onPageStarted(url);
            }

            @Override
            public void onPageFinished(XWalkView view, String url) {
                mTestContentsClient.didFinishLoad(url);
            }
        }
        getXWalkView().setXWalkClient(new TestXWalkClient());
    }

    @SmallTest
    @Feature({"ExtensionEcho"})
    public void testExtensionEcho() throws Throwable {
        ExtensionEcho echo = new ExtensionEcho();
        final String url = "file:///android_asset/echo.html";
        final String expected_title = "Pass";

        loadUrlSync(url);
        
        // sleep to allow extension to change title
        Thread.sleep(1000);
        assertEquals(expected_title, getTitleOnUiThread());
    }
}
