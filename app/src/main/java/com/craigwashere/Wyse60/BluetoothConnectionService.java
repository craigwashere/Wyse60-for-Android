package com.craigwashere.Wyse60;

//import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.craigwashere.Wyse60.MainActivity.TAG;

public class BluetoothConnectionService
{
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private int mState;
    private int mNewState;

    //    private static final String TAG = "BluetoothConnService";
    private static final String appName = "MYAPP";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;

    public BluetoothConnectionService(Context context)
    {
        Log.d(TAG, "BluetoothConnectionService: constructor");
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start()
    {
        Log.d(TAG, "BluetoothConnectionService: start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mInsecureAcceptThread == null)
        {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread
    {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure)
        {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try
            {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        appName, MY_UUID_INSECURE);
            } catch (IOException e)
            {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run()
        {
            Log.d(TAG, "AcceptThread: run: ");
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            try
            {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.d(TAG, "run: before accept()");
                    socket = mmServerSocket.accept();
                Log.d(TAG, "run: after accept();");
            } catch (IOException e)
            {
                Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
            }

            // If a connection was accepted
            if (socket != null)
            {
                connected(socket, mmDevice);
            }
            Log.d(TAG, "END mAcceptThread, socket Type: " + mSocketType);
        }

        public void cancel()
        {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try
            {
                mmServerSocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid)
    {
        Log.d(TAG, "startClient: ");
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectThread extends Thread
    {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid)
        {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run()
        {
            Log.d(TAG, "run: mConnectThread");
            BluetoothSocket tmp = null;

            //get a bluetooth socket for a connection with the given bluetooth device
            try
            {
                Log.d(TAG, "ConnectThread: trying to create InsecureRFcommSocket using UUID:"+ MY_UUID_INSECURE);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            }
            catch (IOException e)
            {
                Log.d(TAG, "ConnectThread: Could not create InsecureRFcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            //make a connection to the BluetoothSocket

            //This is a blocking call and will only return on
            //a successful connection or an exception
            try
            {
                mmSocket.connect();
                Log.d(TAG, "run: ConnectThread connected");
            }
            catch (IOException e)
            {
                //close socket
                try
                {
                    mmSocket.close();
                    Log.d(TAG, "run: closed socket");
                }
                catch (IOException e1)
                {
                    Log.e(TAG, "run: unable to close the connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: could not connect to UUID " + MY_UUID_INSECURE);
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel()
        {
            Log.d(TAG, "cancel: Closing Client Socket");
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "cancel: Close() of mmSocket in ConnectThread failed. " + e.getMessage());
            }
        }

    }

    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG, "ConnectedThread: starting...");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            if (tmpIn == null)
                Log.d(TAG, "ConnectedThread: tmpIn is null");
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            Log.d(TAG, "ConnectedThread: run: ");

            int bytes; // bytes returned from read()

            //keep listening to the InputStream until an exception occurs
            while (true)
            {
                try
                {
                    byte[] buffer = new byte[mmInStream.available()];  //buffer store for the stream

                    bytes = mmInStream.read(buffer);
                    if (bytes != 0)
                    {
                        StringBuilder debug_string = new StringBuilder();
                        for (int i = 0; i < bytes; i++) {
                            debug_string.append((int) buffer[i]);
                            debug_string.append(' ');
                        }

                        String incomingMessage = null; // for UTF-8 encoding Integer.toString(buffer);
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            incomingMessage = new String(buffer, StandardCharsets.UTF_8);
                        }
                        Intent incoming_message_intent = new Intent("Incoming_Message");
                        incoming_message_intent.putExtra("theMessage", incomingMessage);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(incoming_message_intent);
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    break;
                }
            }
        }

        //call this from the main activity to send  data to the remote device
        public void write(String bytes)
        {
            //String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: writing to output stream: " + bytes);
            try
            {
                for (int i = 0; i < bytes.length(); i++)
                    mmOutStream.write(bytes.charAt(i));
            }
            catch (IOException e)
            {
                Log.e(TAG, "write: error writing to output stream. " + e.getMessage());
            }
        }
        /*call this from the main activity to shutdown the connection */
        public void cancel()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice)
    {
        Log.d(TAG, "connected: starting");

        //start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(String)
     */
    public void write(String out)
    {
        // Create temporary object
        ConnectedThread temporary_mConnectedThread =  mConnectedThread;
        // Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: write called");
        // Perform the write unsynchronized
        if (temporary_mConnectedThread != null)
            temporary_mConnectedThread.write(out);
        else
            Log.d(TAG, "write: mConnectedThread is null");
    }
}
