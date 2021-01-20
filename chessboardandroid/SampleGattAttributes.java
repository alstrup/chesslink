/*
 * Copyright (C) 2013 The Android Open Source Project
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

package uk.momentumdc.chessboardandroid;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String SYSTEM_ID_CHAR = "00002a23-0000-1000-8000-00805f9b34fb";
    public static String MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
    public static String SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb";
    public static String FIRMWARE_REVISION_CHAR = "00002a26-0000-1000-8000-00805f9b34fb";
    public static String HARDWARE_REVISION_CHAR = "00002a27-0000-1000-8000-00805f9b34fb";
    public static String SOFTWARE_REVISION_CHAR = "00002a28-0000-1000-8000-00805f9b34fb";
    public static String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
    public static String UUIDSTR_IEEE_11073_20601_CHAR = "00002a2a-0000-1000-8000-00805f9b34fb";

    public static String UUIDSTR_ISSC_PROPRIETARY_SERVICE = "49535343-fe7d-4ae5-8fa9-9fafd205e455";
    public static String UUIDSTR_ISSC_TRANS_TX = "49535343-1e4d-4bd9-ba61-23c647249616";
    public static String UUIDSTR_ISSC_TRANS_RX = "49535343-8841-43f4-a8d4-ecbe34729bb3";
    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put(SYSTEM_ID_CHAR, "System ID Number");
        attributes.put(MODEL_NUMBER_STRING, "Model Number String");
        attributes.put(SERIAL_NUMBER_STRING, "Serial Number String");
        attributes.put(FIRMWARE_REVISION_CHAR, "Firmware Revision Number");
        attributes.put(HARDWARE_REVISION_CHAR, "Hardware Revision Number");
        attributes.put(SOFTWARE_REVISION_CHAR, "Software Revision Number");
        attributes.put(MANUFACTURER_NAME_STRING, "Manufacturer Name String");
        attributes.put(UUIDSTR_IEEE_11073_20601_CHAR, "IEEE Number");
        attributes.put(UUIDSTR_ISSC_PROPRIETARY_SERVICE, "ISSC Proprietary Service");
        attributes.put(UUIDSTR_ISSC_TRANS_TX, "Transparent TX");
        attributes.put(UUIDSTR_ISSC_TRANS_RX, "Transparent RX");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
