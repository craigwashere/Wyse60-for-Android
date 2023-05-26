package com.craigwashere.wyse60.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.craigwashere.wyse60.util.BluetoothConnectionService;
import com.craigwashere.wyse60.R;
import com.craigwashere.wyse60.util.Wyse60view;
//import com.craigwashere.wyse60.util.components.keyboard.CustomKeyboardView;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    final static String TAG = "MainActivity";

    float font_size = 8.5f;

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private static final UUID HC_05_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothDevice m_bluetooth_device;
    BluetoothConnectionService m_bluetooth_connection;
    SharedPreferences sharedPreferences;

    Wyse60view main_text;
//    CustomKeyboardView keyboard;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        main_text = findViewById(R.id.main_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        m_bluetooth_connection = new BluetoothConnectionService(this);
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        SharedPreferences.Editor shared_preferences_editor = sharedPreferences.edit();
        shared_preferences_editor.putString(getString(R.string.font_size_key), Float.toString(font_size));
        shared_preferences_editor.commit();

        show_keyboard();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("Incoming_Message"));

        //register receiver to send messages through bluetooth
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("send_character"));

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

/*---------------Following lines are debug shortcut -----------------------*/
        BluetoothAdapter m_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice m_bluetooth_device = m_bluetooth_adapter.getRemoteDevice("00:21:79:DF:0A:7C");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "trying to pair with IOGEAR GBC232A Serial Adapter");
            m_bluetooth_device.createBond();
            m_bluetooth_connection.startClient(m_bluetooth_device, HC_05_UUID_INSECURE);
        }
    }

/*---------------show keyboard selection dialog and force keyboard to show -----------------------*/
    private void show_keyboard()
    {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        Log.d(TAG, "show_keyboard: " + imm);

        imm.showInputMethodPicker();
        imm.showSoftInput(main_text, InputMethodManager.SHOW_IMPLICIT);
        //force keyboard to show
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
/*---------------keyboard-----------------------*/

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        Log.d(TAG, "dispatchKeyEvent: " + event.getKeyCode());
        int key_code = event.getKeyCode();
        StringBuilder message = new StringBuilder();

        //check for function keys
        if ((key_code >= 131) && (key_code <= 142))
        {
            //for some reason 'F5' is different
            if (key_code == 135) {
                message.append((char) 0x3e);
                message.append((char) 0x30);
            } else {
                message.append((char) 0x01);
                message.append((char) (0x40 + (key_code - 131)));
                Log.d(TAG, "dispatchKeyEvent: " + Integer.toHexString((char) 0x40 + (key_code - 131)));
            }

            if (event.isShiftPressed())
            {
                Log.d(TAG, "dispatchKeyEvent: SHIFT + F-Key pressed");
                message.setCharAt(1, (char) (message.charAt(1) & 0x2f));
            }

            message.append((char) 0x0d);
        }
        else if (key_code == 127) //delete key pressed
        {   //sending delete code (0x7f) doesn't command Wyse60 to delete a character, we have to
            //send ESC W
            message.append((char) 0x1b); //ESC
            message.append((char) 0x87); //'W'
        }
        else if (key_code == 4) //KEYCODE_BACK
        {
            //if we don't intercept the back key code, '4' is sent to the bluetooth stream
            //let's send ESC to go back a page
            message.append((char)0x1b);
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

        if (message.length() > 0)
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

                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
                {
                    Log.d(TAG, "trying to pair with " + device_name);
                    m_bluetooth_device.createBond();
                    m_bluetooth_connection.startClient(m_bluetooth_device, HC_05_UUID_INSECURE);
                }
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