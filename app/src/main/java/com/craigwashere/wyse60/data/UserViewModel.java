package com.craigwashere.wyse60.data;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UserViewModel extends ViewModel
{
    final static String TAG = "UserViewModel";
    public MutableLiveData<String> name;

    public UserViewModel()
    {
        Log.d(TAG, "UserViewModel(): ");
        name = new MutableLiveData<>();
    }

    public MutableLiveData<String> getName()
    {
        Log.d(TAG, "getUser: ");
        if (name == null)
            name = new MutableLiveData<>();

        return name;
    }
}
