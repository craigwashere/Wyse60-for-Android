package com.craigwashere.wyse60.data;

import androidx.lifecycle.ViewModel;

public class User extends ViewModel
{
    public String name;
    public String getName()
    {
        return name;
    }
    public User(String name)
    {
        this.name = name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

}
