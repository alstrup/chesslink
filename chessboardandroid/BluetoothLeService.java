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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import java.util.Queue;
import java.util.LinkedList;
/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static int mBoardIndex = 100;
    private static int mRxParity;
    private static int mBoardParity;
    private static byte mRxBoard[] = new byte[64];

    private boolean mNotificationEnabled = false;
    private int mCounter = 0;

    private class writeCharacteristicRecord {
        BluetoothGattCharacteristic characteristic;
        byte[]                      data;
    }
    private class writeDescriptorRecord {
        BluetoothGattDescriptor descriptor;
        byte[]                      data;
    }
    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<BluetoothGattCharacteristic>();
    private Queue<writeCharacteristicRecord> characteristicWriteQueue = new LinkedList<writeCharacteristicRecord>();
    private Queue<writeDescriptorRecord> descriptorWriteQueue = new LinkedList<writeDescriptorRecord>();

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_TRANSPARENT_TX =
            UUID.fromString(SampleGattAttributes.UUIDSTR_ISSC_TRANS_TX);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        // Implements callback when services are discovered
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        // Callback when characteristic has been read
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            characteristicReadQueue.remove();
             if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
            handleQueues();
         }

        // Handle the queues that are used to sequence reads and writes
        private void handleQueues() {
            if (characteristicWriteQueue.size() > 0)
            {
                writeCharacteristicRecord rec = characteristicWriteQueue.peek();
                rec.characteristic.setValue(rec.data);
                mBluetoothGatt.writeCharacteristic(rec.characteristic);
            }
            else if (descriptorWriteQueue.size() > 0) {
                writeDescriptorRecord rec = descriptorWriteQueue.peek();
                rec.descriptor.setValue(rec.data);
                mBluetoothGatt.writeDescriptor(rec.descriptor);
            }
            else if (characteristicReadQueue.size() > 0) {
                mBluetoothGatt.readCharacteristic(characteristicReadQueue.peek());
            }
        }

        // Callback when characteristic has been written
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            }
            else
            {
                Log.w(TAG, "write Error: " + status);
            }
            characteristicWriteQueue.remove();
            handleQueues();
        }

        // Callback when a characteristic is changed
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        // Callback when a descriptor has been written
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            descriptorWriteQueue.remove();
            handleQueues();
        }
    };

    public void discoverServices() {
        mBluetoothGatt.discoverServices();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Return binary equivalent of hex character
     * @param C Hex character
     * @return Binary equivalent or 0xff
     */

    private int byteHex (final byte C)
    {
        if ((C >= '0') && (C <= '9'))
        {
            return (C-'0');
        }
        else if ((C >= 'A') && (C <= 'F'))
        {
            return (C - 'A' + 10);
        }
        return (0xff);
    }

    /**
     * Handle data as received from chess board
     * @param intent
     * @param data
     */

    private void handleData(final Intent intent,final byte[] data){
        int i;

        for (byte C : data) {
            C &= 0x7f;
            if ((C < 0x20) || (C > 0x7a)) {
            }
            else if (C == 's') {
                mBoardIndex = 0;
                mRxParity = 's';
            } else if (mBoardIndex < 66) {
                if (mBoardIndex == 64) {
                    mBoardParity = byteHex(C);
                    mBoardIndex++;
                } else if (mBoardIndex == 65) {
                    mBoardParity <<= 4;
                    mBoardParity += byteHex(C);
                    if (mRxParity == mBoardParity) {
                        // Valid board received

                        final StringBuilder sBoard = new StringBuilder(72);
                        for (i = 0; i < 64; i++) {
                            sBoard.append(String.format("%c", mRxBoard[i]));
                        }
                        intent.putExtra(EXTRA_DATA, sBoard.toString());
                    } else {
                        mRxParity = mBoardParity;
                    }
                    mBoardIndex++;
                } else {
                    mRxBoard[mBoardIndex] = C;
                    mBoardIndex++;
                    mRxParity ^= C;
                }
            }
        }
    }

    /**
     * Handle a broadcast
     * @param action Broadcasted action
     * @param characteristic
     */

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (action == ACTION_DATA_AVAILABLE) {
            final byte[] data = characteristic.getValue();
            if ((data != null) && (data.length > 0)) {
               if (UUID_TRANSPARENT_TX.equals(characteristic.getUuid())) {
                   mCounter++;
                   if (mCounter == 1) {
                       handleData(intent, data);
                   }
                   else
                   {
                       handleData(intent, data);
                   }
                   mCounter --;
               } else {
                // For all other profiles, writes the data formatted in HEX.
                    if (data != null && data.length > 0) {
                        final StringBuilder stringBuilder = new StringBuilder(data.length);
                        for (byte byteChar : data)
                            stringBuilder.append(String.format("%02X ", byteChar));
                        intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                    }
                }
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback,BluetoothDevice.TRANSPORT_LE);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Get the total number of records in the queues
     * @return Count
     */

    public int queueCount(){
        int     count;

        count = characteristicReadQueue.size();
        count += characteristicWriteQueue.size();
        count += descriptorWriteQueue.size();
        return (count);
    }


    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        characteristicReadQueue.add(characteristic);
        if (queueCount() == 1) {
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }

    /**
     * Write to a given characteristic
     * @param characteristic The characteristic to write
     * @param data The data to be written
     */

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic,byte[] data) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        writeCharacteristicRecord rec = new writeCharacteristicRecord();
        rec.characteristic = characteristic;
        rec.data = data;
        characteristicWriteQueue.add(rec);
        if (queueCount() == 1) {
            characteristic.setValue(data);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    /**
     * Write to a given descriptor
     * @param descriptor Descriptor to be written
     * @param data The data to be written
     */

    public void writeDescriptor(BluetoothGattDescriptor descriptor,byte[] data) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        writeDescriptorRecord rec = new writeDescriptorRecord();
        rec.descriptor = descriptor;
        rec.data = data;
        descriptorWriteQueue.add(rec);
        if (queueCount() == 1) {
            descriptor.setValue(data);
            mBluetoothGatt.writeDescriptor(rec.descriptor);
        }
    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (!mNotificationEnabled) {
            mNotificationEnabled = true;
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); { 0x01 0x00}
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
