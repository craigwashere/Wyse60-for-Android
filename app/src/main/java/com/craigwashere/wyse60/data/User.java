package com.craigwashere.wyse60.data;


import android.util.Log;

public class User
{
    final static String TAG = "User";
    public String name;

    public String getName()
    {
        return name;
    }

    public User()
    {
        Log.d(TAG, "User(): ");
        this.name = "";
    }
    public User(String name)
    {
        Log.d(TAG, "User(String name): ");
        this.name = name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
}
