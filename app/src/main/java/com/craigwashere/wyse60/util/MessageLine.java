package com.craigwashere.wyse60.util;

import android.util.Log;

public class MessageLine extends StatusLine
{
    private static final String TAG = "MessageLine";
    String temp_string;

    public MessageLine(int y_loc, float text_size)
    {
        super(y_loc, text_size);

        temp_string = "";
    }

    public void insert_char(char c)
    {
        temp_string += c;
    }

    public void solidify_string()
    {
        if ((temp_string.charAt(0) != '\r') || (temp_string.length() != 1))
            for (char c: temp_string.toCharArray())
                add_char(c);

        temp_string = "";
    }
}
