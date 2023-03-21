package com.craigwashere.Wyse60;

import android.text.SpannableStringBuilder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;


class ScreenBuffer
{
    private static final String TAG = "ScreenBuffer";

    ArrayList<_attributes> attributes;

    public ArrayList<SpannableStringBuilder> lineBuffer;

    int attribute_index;

    int cursorX;
    int cursorY;
    int maximumWidth;
    int maximumHeight;

    ScreenBuffer()
    {
        maximumWidth = 80;  maximumHeight = 42;
        cursorX = 0;        cursorY = 0;

        lineBuffer = new ArrayList<SpannableStringBuilder>(maximumHeight);

        set_new_geometry(maximumWidth, maximumHeight);

        attributes = new ArrayList<_attributes>();
        attribute_index = -1;
    }

    public ArrayList<SpannableStringBuilder> getLineBuffer() {
        return lineBuffer;
    }

    public void setLineBuffer(ArrayList<SpannableStringBuilder> text){
        lineBuffer = text;
    }

    public int getCursorX()         {   return cursorX; }
    public int getCursorY()         {   return cursorY; }
    public int getMaximumWidth()    {   return maximumWidth;    }
    public int getMaximumHeight()   {   return maximumHeight;   }

    public void setCursorX(int x)               { cursorX = x;  }
    public void setCursorY(int y)               { cursorY = y;  }
    public void setMaximumWidth(int width)      { maximumWidth = width;  }
    public void setMaximumHeight(int height)    { maximumHeight = height;   }

    public void clear_screen(int attributes, char fillChar)
    {
        char[] temp_char = new char[maximumWidth];
        Arrays.fill(temp_char, fillChar);

        for (int i = 0; i < maximumHeight; i++)
            lineBuffer.get(i).replace(0, maximumWidth, new String(temp_char));

        this.attributes.clear();
    }

    public int find_attribute(int x, int y)
    {
        for (int i = 0; i < attributes.size(); i++)
        {
            _attributes temp = attributes.get(i);
            if (temp.startX == x && temp.startY == y)
                return i;
        }
        return -1;
    }

    public void set_new_geometry(int new_width, int new_height)
    {
        this.maximumWidth = new_width;
        this.maximumHeight = new_height;
        lineBuffer.clear();
        setup_ssb_buffer();
    }

    private void setup_ssb_buffer()
    {
        Log.d(TAG, "setup_ssb_buffer: ");
        char[] temp_char = new char[maximumWidth];
        for (int i = 0; i < maximumHeight; i++)
        {
            SpannableStringBuilder temp = new SpannableStringBuilder(new String(temp_char));
            lineBuffer.add(temp);
        }
    }
}
