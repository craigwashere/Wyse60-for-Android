package com.craigwashere.wyse60.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.craigwashere.wyse60.data.User;
import com.craigwashere.wyse60.data.UserViewModel;

public class CustomReceiver extends BroadcastReceiver
{
    final static String TAG = "CustomReceiver";
    private final UserViewModel mViewModel;
    public CustomReceiver(UserViewModel viewModel)
    {
        this.mViewModel = viewModel;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        char[] text = intent.getStringExtra("theMessage").toCharArray();

        String debug_string = "";
        for (char c: text)
            debug_string += Integer.toHexString(c) + ' ';

        mViewModel.getName().postValue("theMessage: " + debug_string + '.');
        Log.d(TAG, "onReceive: " + debug_string);
    }
}
