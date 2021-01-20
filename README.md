# Chesslink

This is the start of a project to communicate with the chess link.

For inspiration, see

https://github.com/domschl/python-mchess

which is a Python library for communication.

Now that JS has Bluetooth and USB APIs, it is relevant to make a JS
implementation of the communication protocol.

## Usage

1. Turn on your chess link, connect it with your laptop/computer/phone
using bluetooth.

2. Open bluetooth.html in Chrome or Firefox or another browser, which
implements the Bluetooth API.

3. Open the JS development console. Ctrl+shift+j in Chrome.

4. Click Connect. It will scan for bluetooth devices, and your millenium
   chess device should appear. Pair with that, and you will get output
   about a connection, a service, and three different characteristics.

5. TODO: Figure out how to initialize the board state, & get moves.

## Protocol documentation

https://github.com/domschl/python-mchess/blob/master/mchess/magic-board.md

has documentation on the communication protocol.

## Gweneral bluetooth documentation

Introduction to bluetooth GATT
https://www.youtube.com/watch?v=eHqtiCMe4NA&list=PLYj4Cw17Aw7ypuXt7mDFWAyy6P661TD48&index=5

## Device specific characteristics

List of characteristics: (https://www.microchip.com/forums/m893253.aspx)

	// Write Notify
	"49535343-026e-3a9b-954c-97daef17e26e"

	// Notify. Called TX in Python mchess. Read on this gives 0, 0, 0, 0, 0.
	"49535343-1e4d-4bd9-ba61-23c647249616"

	// Read. Initial read gives:
	// 136, 102, 0, 0, 0, 0, 0, 0, 0   	^B
	"49535343-6daa-4d02-abf6-19569aca69fe"

	// Write. Called RX in Python mchess
	"49535343-8841-43f4-a8d4-ecbe34729bb3"

	// Write Notify
	"49535343-aca3-481c-91ec-d85e28a60318"


    public static String UUIDSTR_ISSC_TRANS_TX = "49535343-1e4d-4bd9-ba61-23c647249616";
    public static String UUIDSTR_ISSC_TRANS_RX = "49535343-8841-43f4-a8d4-ecbe34729bb3";


        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (!mNotificationEnabled) {
            mNotificationEnabled = true;
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); { 0x01 0x00}
        }

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";


    private byte addParity(byte C) {
        int     i;
        C |= 0x80;

        for (i=0;i < 7;i++) {
        if ((C & (1 << i)) != 0) {
                C ^= 0x80;
            }
        }
        return (C);
    }

    private void sendString(String S) {
        int     fragment = 20;
        int     i,j;
        byte    sTx[] = new String(S).getBytes();
        byte    bTx[] = new byte[fragment];

        j = 0;
        for (i = 0;i < S.length();i++) {
            sTx[i] = addParity(sTx[i]);
            if (fragment > 0) {
                bTx[j] = sTx[i];
                j += 1;
                if (j >= fragment) {
                    // Send the fragment
                    mBluetoothLeService.writeCharacteristic(mRxCharacteristic,bTx);
                    j = 0;
                    bTx = new byte[fragment];
                }
            }
        }
        if (fragment > 0) {
            if (j > 0) {
                // Send any remaining bytes
                byte bTx1[] = new byte[j];
                for (i=0;i < j;i++) {
                    bTx1[i] = bTx[i];
                }
                mBluetoothLeService.writeCharacteristic(mRxCharacteristic,bTx1);
            }
        } else {
            // Send the whole string
            mBluetoothLeService.writeCharacteristic(mRxCharacteristic,sTx);
        }
    }

