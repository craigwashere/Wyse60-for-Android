package com.craigwashere.wyse60.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.craigwashere.wyse60.util.DeviceListAdapter;
import com.craigwashere.wyse60.R;

import java.util.ArrayList;
import java.util.Set;

public class SetupActivity extends AppCompatActivity
{
    private static final String TAG = "SetupActivity";
    CheckBox enable_bt, visible_bt;
    TextView name_bt, lbl_discovering;
    ListView lv_bluetooth_devices;

    private BluetoothAdapter bluetooth_adapter;
    private Set<BluetoothDevice> pairedDevices;
  //  private BroadcastReceiver state_changed_broadcast_receiver;
    Button btn_Discover;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        enable_bt   = findViewById(R.id.enable_bt);
        visible_bt  = findViewById(R.id.visible_bt);
        name_bt     = findViewById(R.id.name_bt);
        lv_bluetooth_devices    = findViewById(R.id.list_view);
        lbl_discovering = findViewById(R.id.lbl_discovering);

        name_bt.setText(getLocalBluetoothName());

        bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetooth_adapter == null)
        {
            Toast.makeText( this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (bluetooth_adapter.isEnabled())
            enable_bt.setChecked(true);

        enable_bt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    bluetooth_adapter.disable();
                    Toast.makeText(getApplicationContext() , "Turned off", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intentOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intentOn, 0);
                    Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_SHORT).show();
                }
            }
        });

        visible_bt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
                if (!isChecked)
                {
                    Intent getVisible = new Intent (BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(getVisible, 0);
                    Toast.makeText(getApplicationContext(), "Visible for 2 min", Toast.LENGTH_SHORT).show();
                }
            }
        });

  /*      IntentFilter state_changed_filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        Log.d(TAG, "onCreate: " + state_changed_filter.toString());
        Log.d(TAG, "onCreate: " + state_changed_broadcast_receiver.toString());

        if (registerReceiver(state_changed_broadcast_receiver, state_changed_filter) == null)
            Log.d(TAG, "state_changed_broadcast_receiver failed");
*/


        lv_bluetooth_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public  void onItemClick(AdapterView<?> parent, View view, int position, long id){
                //Toast.makeText(MainActivity.this, "Click ListItem Number " + position, Toast.LENGTH_SHORT).show();
                bluetooth_adapter.cancelDiscovery();

                String deviceName = mBTDevices.get(position).getName();

                lv_bluetooth_devices.setSelection(position);

                Log.d(TAG, "setOnItemClickListener: deviceName: " + mBTDevices.get(position).getName());
                Log.d(TAG, "setOnItemClickListener: deviceAddress: " + mBTDevices.get(position).getAddress());

                Intent return_intent = SetupActivity.this.getIntent();
                Log.d(TAG, "onReceive: return_intent = " + return_intent.toString());
                return_intent.putExtra("DEVICE_NAME", mBTDevices.get(position).getName());                                  //Add BLE device name to the intent
                return_intent.putExtra("DEVICE_ADDRESS", mBTDevices.get(position).getAddress());                             //Add BLE device address to the intent

                SetupActivity.this.setResult(RESULT_OK, return_intent);

                finish();
            }
        });

      /*  state_changed_broadcast_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "state_changed_broadcast_receiver");
                final String action = intent.getAction();

                if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                {
                    BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (mDevice.getBondState())
                    {
                        case BluetoothDevice.BOND_BONDED:   Log.d(TAG, "state_changed_broadcast_receiver: BOND_BONDED");
                            bluetooth_adapter.cancelDiscovery();
                            lbl_discovering.setText("Connected to: " + mDevice.getName());
                            Intent return_intent = SetupActivity.this.getIntent();
                            Log.d(TAG, "onReceive: return_intent = " + return_intent.toString());
                            return_intent.putExtra("DEVICE_NAME", mDevice.getName());                                  //Add BLE device name to the intent
                            return_intent.putExtra("DEVICE_ADDRESS", mDevice.getAddress());                             //Add BLE device address to the intent


                            SetupActivity.this.setResult(RESULT_OK, return_intent);

                            finish();
                            break;
                        case BluetoothDevice.BOND_BONDING:  Log.d(TAG, "state_changed_broadcast_receiver: BOND_BONDING");   break;
                        case BluetoothDevice.BOND_NONE:     Log.d(TAG, "state_changed_broadcast_receiver: BOND_NONE");      break;
                        default: Log.d(TAG, "Shouldn't get here"); break;
                    }
                }
            }
        };*/
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
//        unregisterReceiver(state_changed_broadcast_receiver);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
//        unregisterReceiver(state_changed_broadcast_receiver);
    }

    public String getLocalBluetoothName()
    {
        if (bluetooth_adapter == null)
            bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();

        String name = bluetooth_adapter.getName();
        if (name == null)
            name = bluetooth_adapter.getAddress();

        return name;
    }

    private BroadcastReceiver state_changed_broadcast_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "state_changed_broadcast_receiver");
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (mDevice.getBondState())
                {
                    case BluetoothDevice.BOND_BONDED:   Log.d(TAG, "state_changed_broadcast_receiver: BOND_BONDED");
                        bluetooth_adapter.cancelDiscovery();
                        lbl_discovering.setText("Connected to: " + mDevice.getName());
                        Intent return_intent = new Intent();

                        return_intent.putExtra("DEVICE_NAME", mDevice.getName());                                  //Add BLE device name to the intent
                        return_intent.putExtra("DEVICE_ADDRESS", mDevice.getAddress());                             //Add BLE device address to the intent


                        SetupActivity.this.setResult(1, return_intent);

                        finish();
                        break;
                    case BluetoothDevice.BOND_BONDING:  Log.d(TAG, "state_changed_broadcast_receiver: BOND_BONDING");   break;
                    case BluetoothDevice.BOND_NONE:     Log.d(TAG, "state_changed_broadcast_receiver: BOND_NONE");      break;
                    default: Log.d(TAG, "Shouldn't get here"); break;
                }
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.d(TAG, "onReceive: " + action);

            if (action.equals(BluetoothDevice.ACTION_FOUND))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                /*Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                for (int i=0; i<uuidExtra.length; i++)
                {
                    Log.d(TAG, "Device: " + device.getName() + ", " + device + ", Service: " + uuidExtra[i].toString());
                }*/
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());

                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lv_bluetooth_devices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    public void btnDiscover(View view)
    {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if (bluetooth_adapter.isDiscovering())
            bluetooth_adapter.cancelDiscovery();

        bluetooth_adapter.startDiscovery();

        check_bluetooth_permissions();

        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        if (discoverDevicesIntent == null) Log.d(TAG, "discoverDevicesIntent = null");

        if (registerReceiver(mBroadcastReceiver, discoverDevicesIntent) == null)
            Log.d(TAG, "registerReceiver failed");

        lbl_discovering.setVisibility(View.VISIBLE);
    }


    private void check_bluetooth_permissions()
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            int permissionCheck  = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0)
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},1001);
        }
    }
}
