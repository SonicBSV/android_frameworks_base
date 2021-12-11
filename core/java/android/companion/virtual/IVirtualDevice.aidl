/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.companion.virtual;

import android.graphics.Point;
import android.hardware.input.VirtualKeyEvent;
import android.hardware.input.VirtualMouseButtonEvent;
import android.hardware.input.VirtualMouseRelativeEvent;
import android.hardware.input.VirtualMouseScrollEvent;
import android.hardware.input.VirtualTouchEvent;

/**
 * Interface for a virtual device.
 *
 * @hide
 */
interface IVirtualDevice {

    /**
     * Returns the association ID for this virtual device.
     *
     * @see AssociationInfo#getId()
     */
    int getAssociationId();

    /**
     * Closes the virtual device and frees all associated resources.
     */
    void close();
    void createVirtualKeyboard(
            int displayId,
            String inputDeviceName,
            int vendorId,
            int productId,
            IBinder token);
    void createVirtualMouse(
            int displayId,
            String inputDeviceName,
            int vendorId,
            int productId,
            IBinder token);
    void createVirtualTouchscreen(
            int displayId,
            String inputDeviceName,
            int vendorId,
            int productId,
            IBinder token,
            in Point screenSize);
    void unregisterInputDevice(IBinder token);
    boolean sendKeyEvent(IBinder token, in VirtualKeyEvent event);
    boolean sendButtonEvent(IBinder token, in VirtualMouseButtonEvent event);
    boolean sendRelativeEvent(IBinder token, in VirtualMouseRelativeEvent event);
    boolean sendScrollEvent(IBinder token, in VirtualMouseScrollEvent event);
    boolean sendTouchEvent(IBinder token, in VirtualTouchEvent event);
}
