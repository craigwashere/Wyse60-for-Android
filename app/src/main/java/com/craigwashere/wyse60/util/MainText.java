package com.craigwashere.wyse60.util;

import java.util.Iterator;

public class MainText extends TextArea
{
    private static final String TAG = "MainText";
    private final short  T_NORMAL = 0x30;

    public MainText(float text_size)
    {
        super(text_size);
    }

    public void clear_to_end_of_line()
    {
        for (Iterator<char_text> iterator = text.iterator(); iterator.hasNext(); )
        {
            char_text value = iterator.next();
            if ((value.m_write_pos_y == m_write_pos_y) && (value.m_write_pos_x >= m_write_pos_x))
            {
                value.m_attribute = T_NORMAL;
                value.character_to_print = ' ';
            }
        }
    }
}
