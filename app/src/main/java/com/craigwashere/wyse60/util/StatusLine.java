package com.craigwashere.wyse60.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

public class StatusLine extends TextArea
{
    private static final String TAG = "StatusLine";

    public StatusLine(int y_loc, float text_size)
    {
        super(text_size);
        m_char_pos_y = y_loc;
        m_write_pos_y = m_char_pos_y * m_text_size;
    }

    public void move_status_line(int new_y_loc)
    {
        m_char_pos_y = new_y_loc;
        m_write_pos_y = m_char_pos_y * m_text_size;
        Log.d(TAG, "move_status_line: " + m_char_pos_y);

        for (Iterator<char_text> iterator = text.iterator(); iterator.hasNext(); )
        {
            char_text value = iterator.next();
            value.m_write_pos_y = m_write_pos_y;
        }
    }

    public void reset_x_pos()
    {
        m_char_pos_x = 0;
        m_write_pos_x = 0;
        text.clear();
    }

    public void clear()
    {
        text.clear();
    }

    public ArrayList<char_text> get_text()
    {
        return text;
    }
}
