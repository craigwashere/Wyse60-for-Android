package com.craigwashere.wyse60.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import com.craigwashere.wyse60.util.BluetoothConnectionService;
import com.craigwashere.wyse60.R;
import com.craigwashere.wyse60.util.Wyse60view;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    final static String TAG = "MainActivity";
    float font_size = 8.5f;
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final UUID HC_05_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //constant passed to startActivityForResult, The system passes this constant back to you in your
    // onActivityResult() implementation as the requestCode parameter
    private static final int REQUEST_ENABLE_BT = 4;
    BluetoothDevice m_bluetooth_device;
    BluetoothConnectionService m_bluetooth_connection;
    SharedPreferences sharedPreferences;
    Wyse60view main_text;
    KeyboardView customKeyboard;
    Keyboard m_qwerty_keyboard;
    Keyboard m_symbols_keyboard;
    boolean SHIFT_is_checked = false;
    boolean CTRL_is_checked = false;
    boolean ALT_is_checked = false;

    @SuppressLint({"MissingPermission", "NewApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main_text = findViewById(R.id.main_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupCustomKeyboard();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            BluetoothAdapter bluetoothAdapter = check_bluetooth();
            if (bluetoothAdapter != null)
                Log.d(TAG, "onCreate: bluetooth adapter not null");
            m_bluetooth_connection = new BluetoothConnectionService(this, bluetoothAdapter);
//        else
//            main_text.setText2("Bluetooth not Available :(");
        }
//        else
//            m_bluetooth_connection = new BluetoothConnectionService(this);

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        SharedPreferences.Editor shared_preferences_editor = sharedPreferences.edit();
        shared_preferences_editor.putString(getString(R.string.font_size_key), Float.toString(font_size));
        shared_preferences_editor.commit();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("Incoming_Message"));
        //register receiver to send messages through bluetooth
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("send_character"));
        //Broadcasts when bond state changes (ie:pairing)

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

    /*---------------Following lines are debug shortcut -----------------------
            BluetoothAdapter m_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice m_bluetooth_device = m_bluetooth_adapter.getRemoteDevice("00:21:79:DF:0A:7C");

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d(TAG, "trying to pair with IOGEAR GBC232A Serial Adapter");
                m_bluetooth_device.createBond();
                m_bluetooth_connection.startClient(m_bluetooth_device, HC_05_UUID_INSECURE);
            }*/
    }
    @SuppressLint("MissingPermission")
    private BluetoothAdapter check_bluetooth()
    {
        // 6/22/2023 from Android's developer site
        // check if bluetooth is 'supported' and enabled
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter != null)
        {
            Log.d(TAG, "check_bluetooth: bluetooth adapter not null");
            if (!bluetoothAdapter.isEnabled())
            {
                Log.d(TAG, "check_bluetooth: bluetooth not enabled");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        return bluetoothAdapter;
    }

    private ActivityResultLauncher<String> permissionLauncherSingle = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    //here we will check if permission was now (from permission request dialog) or already granted or not. the param isGranted contains value true/false
                    Log.d(TAG, "onActivityResult: isGranted: " + isGranted);

                    if (isGranted) {
                        //Permission granted now do the required task here or call the function for that
//                        singlePermissionGranted();
//                        m_bluetooth_connection = new BluetoothConnectionService(this, bluetoothAdapter);
                        Toast.makeText(MainActivity.this, "Permission GRANTED...", Toast.LENGTH_SHORT).show();
                    } else {
                        //Permission was denied so can't do the task that requires that permission
                        Log.d(TAG, "onActivityResult: Permission denied...");
                        Toast.makeText(MainActivity.this, "Permission denied...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
/*---------------custom keyboard-----------------------*/
/* AI Generated code for custom keyboard */
/*  I had to fix it to send correct commands to the bluetooth stream*/
    private void setupCustomKeyboard() {
        customKeyboard = findViewById(R.id.customKeyboardView);
        if (customKeyboard != null) {
            m_qwerty_keyboard = new Keyboard(this, R.xml.qwerty);
            m_symbols_keyboard = new Keyboard(this, R.xml.symbols);
            customKeyboard.setKeyboard(m_symbols_keyboard);
            customKeyboard.setVisibility(View.VISIBLE);
            customKeyboard.setOnKeyboardActionListener(new KeyboardView.OnKeyboardActionListener() {
                @Override
                public void onPress(int primaryCode) {}

                @Override
                public void onRelease(int primaryCode) {}

                @Override
                public void onKey(int primaryCode, int[] keyCodes) {
                    handleCustomKeyboardKey(primaryCode);
                }

                @Override
                public void onText(CharSequence text) {}

                @Override
                public void swipeLeft() {}

                @Override
                public void swipeRight() {}

                @Override
                public void swipeDown() {}

                @Override
                public void swipeUp() {}
            });
        }
    }

    private void handleCustomKeyboardKey(int primaryCode)
    {
        Log.d(TAG, "handleCustomKeyboardKey: " + primaryCode);
        if (customKeyboard == null) return;
        final int KEYCODE_CTRL = -7;

        switch (primaryCode)
        {
            case Keyboard.KEYCODE_MODE_CHANGE:  Keyboard current = customKeyboard.getKeyboard();
                                                if (current == m_qwerty_keyboard)
                                                    customKeyboard.setKeyboard(m_symbols_keyboard);
                                                else
                                                    customKeyboard.setKeyboard(m_qwerty_keyboard);
                                                current = customKeyboard.getKeyboard();
//                                                current.setShifted(SHIFT_is_checked);
                                                setModifierKeysOnKeyboard(current);
                                                customKeyboard.invalidateAllKeys();
                                                break;

            case Keyboard.KEYCODE_SHIFT:        SHIFT_is_checked = !SHIFT_is_checked;
                                                Keyboard curKeyboard = customKeyboard.getKeyboard();
                                                curKeyboard.setShifted(SHIFT_is_checked);
                                                fix_period(curKeyboard, SHIFT_is_checked);
                                                customKeyboard.invalidateAllKeys();
                                                break;

            case KEYCODE_CTRL:                  CTRL_is_checked = !CTRL_is_checked;
//                                                setModifierKeysOnKeyboard(customKeyboard.getKeyboard());
//                                                customKeyboard.invalidateAllKeys();
                                                break;

            case Keyboard.KEYCODE_ALT:          ALT_is_checked = !ALT_is_checked;
//                                                setModifierKeysOnKeyboard(customKeyboard.getKeyboard());
//                                                customKeyboard.invalidateAllKeys();
                                                break;

            default:                            int META = 0;
                                                if (SHIFT_is_checked)   META += 1;      //META_SHIFT_ON = 0x00000001
                                                if (CTRL_is_checked)    META += 4096;   //META_CTRL_ON  = 0x00001000
                                                if (ALT_is_checked)     META += 2;      //META_ALT_ON   = 0x00000002
                                                dispatchKeyEvent(new KeyEvent(0,             //downTime
                                                        0,                                             //eventTime
                                                        KeyEvent.ACTION_DOWN,                          //action
                                                        primaryCode,                                   //code
                                                        0,                                             //repeat
                                                        META));                                        //metaState
                                                break;
        }
    }

    private void fix_period(Keyboard keyboard, boolean SHIFT)
    {
        class _punct_key
        {
            public CharSequence next;
            public CharSequence current;
            public int codes[];
            _punct_key(boolean shift)
            {
                current = (shift) ? "." : "?";
                next = (shift) ? "?" : ".";
                codes = new int[]{(shift) ? 63 : 46};
            }
        }
        _punct_key punct = new _punct_key(SHIFT);
        for (Keyboard.Key key: m_qwerty_keyboard.getKeys())
        {
            if((key.label != null) && (key.label.toString().equals(punct.current)))
            {
                key.label = punct.next;
                key.codes = punct.codes;
            }
        }
    }

    private void setModifierKeysOnKeyboard(Keyboard keyboard)
    {
        if (keyboard == null || keyboard.getKeys() == null) return;
        for (Keyboard.Key key : keyboard.getKeys())
        {
            if (key.sticky && key.codes != null && key.codes.length > 0)
            {
                switch (key.codes[0])
                {
                    case -7: key.on = CTRL_is_checked; break;
                    case -6: key.on = ALT_is_checked; break;
                    case -1: key.on = SHIFT_is_checked; break;
                }
            }
        }
    }
    /* END:  AI Generated code for custom keyboard */


    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        Log.d(TAG, "dispatchKeyEvent: " + event.getKeyCode());
        int key_code = event.getKeyCode();
        StringBuilder message = new StringBuilder();

        /* Bunch of keycodes added for readability */
        final int HARDWARE_BACK_BUTTON = 4
                , TAB = 9
                , PERIOD = 46
                , QUESTION_MARK = -1
                , DELETE = 127;

        switch (key_code)
        {
            //if we don't intercept the back key code, '4' is sent to the bluetooth stream
            //let's send ESC to go back a page
            case HARDWARE_BACK_BUTTON:  message.append((char)0x1b);
                                        break;
            case TAB:                   if (event.isShiftPressed())
                                        {
                                            message.append((char)0x1b);
                                            message.append((char)0x49);
                                        }
                                        else
                                            message.append((char)key_code);
                                        break;
            case PERIOD:
            case QUESTION_MARK:         message.append((char)key_code);
                                        break;
            case DELETE:   //sending delete code (0x7f) doesn't command Wyse60 to delete a character, we have to
                           //send ESC W
                                        message.append((char) 0x1b); //ESC
                                        message.append((char) 0x87); //'W'
                                        break;
            default:                    if ((key_code >= 131) && (key_code <= 142))
                                        {
                                            if (key_code == 131)
                                                Log.d(TAG, "dispatchKeyEvent: /*---------------keyboard-----------------------*/");

                                            //I don't think Wyse60 understands CTRL-Fx or ALT-Fx keys
                                            if (event.isCtrlPressed() || event.isAltPressed())
                                                return false;

                                            message.append((char) 0x01);
                                            message.append((char) (0x40 + (key_code - 131)));
                                            Log.d(TAG, "dispatchKeyEvent: " + Integer.toHexString((char) 0x40 + (key_code - 131)));

                                            if (event.isShiftPressed())
                                            {
                                                Log.d(TAG, "dispatchKeyEvent: SHIFT + F-Key pressed");
                                                message.setCharAt(1, (char) (message.charAt(1) | 0x20));
                                            }

                                            message.append((char) 0x0d);
                                        }
                                        else
                                        {
                                            Log.d(TAG, "dispatchKeyEvent: unicodeChar: " + event.getUnicodeChar());
                                            if (event.isShiftPressed())
                                            {
                                                Log.d(TAG, "dispatchKeyEvent: SHIFT pressed");
                                                key_code &= 0x5f;
                                            }
                                            if (event.isAltPressed())
                                            {
                                                key_code &= 0x3f;
                                                Log.d(TAG, "dispatchKeyEvent: ALT pressed");
                                            }
                                            if (event.isCtrlPressed())
                                            {
                                                key_code &= 0x1f;
                                                Log.d(TAG, "dispatchKeyEvent: CTRL pressed");
                                            }
                                            message.append((char)key_code);
                                        }
        }

        m_bluetooth_connection.write(message.toString());

        return false;
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.hasExtra("message"))    return;

            String message_to_send  = intent.getStringExtra("message");
            m_bluetooth_connection.write(message_to_send);
        }
    };


    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("theMessage");

        if (!message.isEmpty())
            main_text.setText2(message);
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver()
    {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            Log.d(TAG, "onReceive: BroadcastReceiver");
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    m_bluetooth_device = mDevice;
                }

                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING)
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");

                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE)
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
            }
        }
    };

    public void start_BT_connection(BluetoothDevice device, UUID uuid)
    {
        Log.d(TAG, "start_BT_connection: Initializing RFCOMM Bluetooth Connection");
        m_bluetooth_connection.startClient(device, uuid);
    }

    @Override
    public void onPause() {
//        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();

        SharedPreferences.Editor shared_preferences_editor = sharedPreferences.edit();
        shared_preferences_editor.putString(getString(R.string.font_size_key), Float.toString(font_size));
        shared_preferences_editor.commit();
    }

    @Override
    public void onResume()
    {
        super.onResume();

       float minSize = Float.parseFloat(sharedPreferences.getString(getString(R.string.font_size_key), "6"));

       font_size = minSize;

        Log.d(TAG, "onResume: minsize: " + minSize);

        //main_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, minSize);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        Log.d(TAG, "onSharedPreferenceChanged: " + key);
        Log.d(TAG, "onSharedPreferenceChanged: string:" + getString(R.string.font_size_key));

        float minSize = Float.parseFloat(sharedPreferences.getString(getString(R.string.font_size_key), "6.0"));

        Log.d(TAG, "onSharedPreferenceChanged: minsize: " + minSize);
        //main_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, minSize);

        Log.d(TAG, "onSharedPreferenceChanged: exit");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id)
        {
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                Log.d(TAG, "onOptionsItemSelected: action_settings");
                return true;
            case R.id.action_setup:
                Intent junk_intent = new Intent(this, SetupActivity.class);
                startActivityForResult(junk_intent, 1);
                Log.d(TAG, "onOptionsItemSelected: action_setup");
                return true;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    // Call Back method  to get the Message form other Activity
    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "onActivityResult: ");
        // check if the request code is same as what is passed  here it is 1
        if(requestCode == 1)
        {
            if (data == null)
                Log.d(TAG, "onActivityResult: intent data is NULL");
            else
            {
                String  device_name = data.getStringExtra("DEVICE_NAME"),
                        device_addr = data.getStringExtra("DEVICE_ADDRESS");

                //main_text.append("device name: " + device_name + '\n');
                //main_text.append("device address: " + device_addr + '\n');

                BluetoothAdapter m_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice m_bluetooth_device = m_bluetooth_adapter.getRemoteDevice(data.getStringExtra("DEVICE_ADDRESS"));

                Log.d(TAG, "trying to pair with " + device_name);

                m_bluetooth_device.createBond();
                m_bluetooth_connection.startClient(m_bluetooth_device, HC_05_UUID_INSECURE);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(mBroadcastReceiver4);

        // I think the following was in case there's some flashing animation
        //wyse60_TextView.fcs.animator.end();
    }
}