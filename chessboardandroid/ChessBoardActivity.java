package uk.momentumdc.chessboardandroid;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import android.view.View;

/**
 * Activity to display chess board
 */

public class ChessBoardActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = ChessBoardActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public final static UUID UUID_TRANSPARENT_TX =
            UUID.fromString(SampleGattAttributes.UUIDSTR_ISSC_TRANS_TX);
    public final static UUID UUID_TRANSPARENT_RX =
            UUID.fromString(SampleGattAttributes.UUIDSTR_ISSC_TRANS_RX);

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private String mRxBoard = null;
    private char[] mBoard = new char[64];
    private byte[][] mLeds = new byte[9][9];

    //private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    //private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
     //       new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private boolean mNotificationEnabled = false;
    private BluetoothGattCharacteristic mRxCharacteristic = null;
    private BluetoothGattCharacteristic mTxCharacteristic = null;
    private BluetoothGattCharacteristic mNotifyCharacteristic = null;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                clearBoard();
                mBluetoothLeService.discoverServices();
             } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
               // updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                //clearUI();
                finish();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
               scanGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //enableNotification(mTxCharacteristic);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chess_board);

         final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        Button send = (Button) findViewById(R.id.offButton);
        send.setOnClickListener(this);
        send = (Button) findViewById(R.id.slowButton);
        send.setOnClickListener(this);
        send = (Button) findViewById(R.id.fastButton);
        send.setOnClickListener(this);
        send = (Button) findViewById(R.id.onButton);
        send.setOnClickListener(this);

        //getActionBar().setTitle(mDeviceName);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    // Handle button being clicked
    @Override
    public void onClick(View v) {
        // handle click
        switch (v.getId()) {
            case R.id.offButton: {
                allLeds((byte) 0);
                break;
            }
            case R.id.slowButton: {
                allLeds((byte) 0x33);
                break;
            }
            case R.id.fastButton: {
                allLeds((byte) 0x55);
                break;
            }
            case R.id.onButton: {
                allLeds((byte) 0xff);
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    /**
     * Copy data from input into board image in memory and update display if changed.
     * @param data
     */
    private void displayData(String data) {
        if (data != null) {
            boolean changed = false;
            int     i;

            for (i=0;i < 64;i++) {
                int     Index;
                Switch cableOnLeft = (Switch) findViewById(R.id.cableOnLeft);
                if (cableOnLeft.isChecked()) {
                    Index = i;
                }
                else {
                    Index = 63-i;
                }
                if (mBoard[Index] != data.charAt(i)) {
                    mBoard[Index] = data.charAt(i);
                    changed = true;
                }
            }
            if (changed) {
                displayBoard();
                Switch autoLeds = (Switch) findViewById(R.id.autoLeds);
                if (autoLeds.isChecked()) {
                    autoLeds();
                }
            }
        }
     }

    /**
     * Add odd parity to a byte
     * @param C Initial value
     * @return Value with parity
     */
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

    /**
     * Send a string to chess board adding parity
     * String will be broken up into pieces if fragment is non-zero
     * @param S String to be sent
     */
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

    /**
     * Send LEDs to chess board
     */
    private void sendLeds() {
        int i, x, y;

        final StringBuilder flashString = new StringBuilder(200);
        flashString.append("L32");  // LED command with default rate
        byte blockParity = 'L' ^ '3' ^ '2';

        try {
            for (x = 0; x < 9; x++) {
                for (y = 0; y < 9; y++) {
                    String sLed;
                    sLed = String.format("%02X", mLeds[8-x][y]);
                    flashString.append(sLed);
                    blockParity ^= sLed.charAt(0);
                    blockParity ^= sLed.charAt(1);
                }
            }
            String sParity = String.format("%02X", blockParity);
            flashString.append(sParity);
            sendString(flashString.toString());
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    /**
     * Set entire LED matrix to a flash pattern
     * @param FlashPattern
     */

    private void setLedMatrix(byte FlashPattern) {
        int     x,y;

        for (y=0;y < 9;y++) {
            for (x = 0; x < 9; x++) {
                mLeds[x][y] = FlashPattern;
            }
        }
    }

    /**
     * Set LEDs to flash around any piece that is present
     */
    private void autoLeds() {
        int     x,y;
        byte    FlashPattern = 0x33;    // Slow flash

        setLedMatrix((byte)0);
        for (y=0;y < 8;y++) {
            for (x=0;x < 8;x++) {
                int     Index;
                Switch cableOnLeft = (Switch) findViewById(R.id.cableOnLeft);
                if (cableOnLeft.isChecked()) {
                    Index = (y*8)+(7-x);
                }
                else {
                    Index =((7-y)*8)+x;
                }

                if (mBoard[Index] != '.') {
                    // Piece detected, flash the LEDs around it
                    mLeds[x][y] = FlashPattern;
                    mLeds[x+1][y] = FlashPattern;
                    mLeds[x][y+1] = FlashPattern;
                    mLeds[x+1][y+1] = FlashPattern;                }
            }
        }
        sendLeds();
    }

    /**
     * Set all LEDs to a flash pattern.
     * @param flashPattern
     */
    private void allLeds(byte flashPattern)
    {
        setLedMatrix(flashPattern);
        sendLeds();
    }

    /**
     * Clear the board
     */
    private void clearBoard() {
        int     i;
        for (i=0;i < 64;i++) {
            mBoard[i] = 0;
        }
    }

    /**
     * Display the board from memory image.
     */
    private void displayBoard() {
        int     i;
        final StringBuilder sBoard = new StringBuilder(72);
        for (i = 0; i < 64; i++) {
            sBoard.append(String.format("%c", mBoard[i]));
            if ((i & 7) == 7) {
                sBoard.append("\n");
            }
        }
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(sBoard);
    }

    /**
     * Enable notification on a characteristic
     * @param characteristic
     */
    private void enableNotification(BluetoothGattCharacteristic characteristic) {
            final int charaProp = characteristic.getProperties();
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mBluetoothLeService.readCharacteristic(mNotifyCharacteristic);
                    mNotifyCharacteristic = null;
                  }
            }
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            }
    }

    /**
     * Scan GATT services for characteristics
     * @param gattServices
     */

    private void scanGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        //String unknownServiceString = getResources().getString(R.string.unknown_service);
        //String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        //ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        //ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
        //       = new ArrayList<ArrayList<HashMap<String, String>>>();
        //mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        BluetoothGattCharacteristic txChar = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
             List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
             // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();

                if (UUID_TRANSPARENT_TX.equals(gattCharacteristic.getUuid())) {
                    if (txChar == null) {
                        txChar = gattCharacteristic;
                    }
                    else
                    {
                        txChar = null;
                    }
                }
                if (UUID_TRANSPARENT_RX.equals(gattCharacteristic.getUuid())) {
                    mRxCharacteristic = gattCharacteristic;
                    mRxCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                }
            }
         }

        boolean autoNotify = true;
        if (autoNotify) {
            mTxCharacteristic = txChar;
            enableNotification(mTxCharacteristic);
        }
        else
        {
            mTxCharacteristic = txChar;
            mBluetoothLeService.readCharacteristic(mTxCharacteristic);
        }
    }

    /**
     * Construct a filter got actions that we are interested in.
     * @return Filter
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
