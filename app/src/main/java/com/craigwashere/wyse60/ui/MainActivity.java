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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.craigwashere.wyse60.util.BluetoothConnectionService;
import com.craigwashere.wyse60.util.CustomPagerAdapter;
import com.craigwashere.wyse60.R;
import com.craigwashere.wyse60.data.User;
import com.craigwashere.wyse60.databinding.ActivityMainBinding;
import com.craigwashere.wyse60.util.wyse60_TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    final static String TAG = "MainActivity";
   // wyse60_TextView main_text;

    //simple Data binding to textView
    User user;

    // ScreenBuffer main_text;
    float font_size = 8.5f;
    BluetoothConnectionService m_bluetooth_connection;
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private static final UUID HC_05_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothDevice m_bluetooth_device;

    SharedPreferences sharedPreferences;
    private List<ToggleButton> toggleButtons;
    private boolean CTRL_is_checked = false,
                    SHIFT_is_checked = false,
                    ALT_is_checked = false;

    private static final int[] TOGGLE_BUTTON_IDS =
            {
                    R.id.btn_SHIFT,
                    R.id.btn_CTRL,
                    R.id.btn_ALT
            };
    private Button escape_button;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user = new User("JJJJ");
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.setViewmodel(user);

        m_bluetooth_connection = new BluetoothConnectionService(this);
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        SharedPreferences.Editor shared_preferences_editor = sharedPreferences.edit();
        shared_preferences_editor.putString(getString(R.string.font_size_key), Float.toString(font_size));
        shared_preferences_editor.commit();

        toggleButtons = new ArrayList<ToggleButton>();

        for (int id : TOGGLE_BUTTON_IDS) {
            ToggleButton toggle_button = findViewById(id);
            toggle_button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    switch (buttonView.getId()) {
                        case R.id.btn_ALT:
                            ALT_is_checked = isChecked;
                            break;
                        case R.id.btn_CTRL:
                            CTRL_is_checked = isChecked;
                            break;
                        case R.id.btn_SHIFT:
                            SHIFT_is_checked = isChecked;
                            break;
                    }
                }
            });
            toggleButtons.add(toggle_button);
        }

        escape_button = (Button) findViewById(R.id.btn_escape);
        escape_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Escape button");
                String message = Character.toString((char) 0x1b);
                m_bluetooth_connection.write(message);
            }
        });

//        main_text = this.<wyse60_TextView>findViewById(R.id.main_view);



//        main_text.setSpannableFactory(new Spannable.Factory(){
//            @Override
//            public Spannable newSpannable(CharSequence source) {
//                return (Spannable) source;
//            }
//        });
//        Keep in mind, it should be set from ViewHolder's constructor, not onBindViewHolder.
//        When you get reference to it by findViewById.


        ViewPager vp_keyboard_pager = (ViewPager) findViewById(R.id.vp_keyboard_area);
        vp_keyboard_pager.setAdapter(new CustomPagerAdapter(this));

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("Incoming_Message"));

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

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.hasExtra("message"))    return;

            String message_to_send  = intent.getStringExtra("message");

            Log.d(TAG, "onReceive: message_to_send length: "+ message_to_send.length() );

            /*if (message_to_send.length() == 1)
            {
                Log.d(TAG, "onReceive: length = 1");
                if (message_to_send.charAt(0) > 0x0c)
                {
                    char char_to_send = (char)message_to_send.charAt(0);
                    if (CTRL_is_checked)
                        char_to_send &= 0x1F;

                    if (SHIFT_is_checked)
                        char_to_send &= 0x5F;

                    if (ALT_is_checked)
                        char_to_send &= 0x3F;

                    message_to_send = Character.toString(char_to_send);
                }
            }
            else if (message_to_send.length() == 3)
            {
                Log.d(TAG, "onReceive: " + message_to_send);
                char char_to_send = message_to_send.charAt(1);
                if (SHIFT_is_checked)
                    char_to_send |= 0x2F;

                //hopefully CTRL and ALT are never used with function keys

                message_to_send = (char)0x01 + Character.toString(char_to_send) + (char) 0x0d;
            }*/

            m_bluetooth_connection.write(message_to_send);
        }
    };

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            char[] text = intent.getStringExtra("theMessage").toCharArray();
            //main_text.setText(text);
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    m_bluetooth_device = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
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

        //wyse60_TextView.fcs.animator.end();
    }
}