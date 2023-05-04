package com.craigwashere.wyse60.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Iterator;

public class TextArea
{
    private static final String TAG = "TextArea";
    private final short  T_NORMAL = 0x30,
                                T_BLANK = 0x01,
                                T_BLINK = 0x02,
                                T_REVERSE = 0x04,
                                T_UNDERSCORE = 0x08,
                                T_DIM = 0x40,
                                T_BOTH = 68,
                                T_ALL = 0x7f,
                                T_PROTECTED = 256,
                                T_GRAPHICS = 512;

    int m_current_attribute;

    ArrayList<char_text> text;
    float m_write_pos_x, m_write_pos_y, m_text_size, m_font_width = 11.0f,
            m_text_spacing = 0F, m_font_height = 20.0F;

    int m_char_pos_x, m_char_pos_y;
    int text_color = Color.GREEN, background_color = Color.BLACK, dim_text_color = Color.BLUE;
    public TextArea(float text_size)
    {
        m_text_size = text_size;
        m_char_pos_x = 0;
        m_write_pos_x = 0;

        text = new ArrayList<char_text>();

        m_current_attribute = T_NORMAL;
    }

    public void draw_text(Canvas canvas, Paint mPaintText)
    {
        if (text.size() > 0)
        {
            for (char_text c : text)
            {
                change_attributes(c, canvas, mPaintText);
                canvas.drawText(String.valueOf(c.character_to_print), c.m_write_pos_x, c.m_write_pos_y, mPaintText);
            }
        }
    }

    private void change_attributes(char_text c, Canvas canvas, Paint mPaintText)
    {
        if (c.m_attribute == T_NORMAL)
            mPaintText.setColor(Color.GREEN);

        if((c.m_attribute & T_DIM) != 0)
            mPaintText.setColor(dim_text_color);

        if((c.m_attribute & T_UNDERSCORE) != 0)
            mPaintText.setFlags(Paint.UNDERLINE_TEXT_FLAG);

        if((c.m_attribute & T_REVERSE) != 0)
        {
            mPaintText.setColor(text_color);
            canvas.drawRect(c.m_write_pos_x, c.m_write_pos_y-m_font_height,
                    c.m_write_pos_x+m_font_width, c.m_write_pos_y, mPaintText);
            mPaintText.setColor(background_color);
        }
    }

    public void add_char(char c)
    {
        char_text temp_char = new char_text(m_write_pos_x, m_write_pos_y, c, m_current_attribute);
        int temp_char_location = text.indexOf(temp_char);
        if (temp_char_location == -1)
            text.add(temp_char);
        else
            text.set(temp_char_location, temp_char);

        m_char_pos_x++;
        m_write_pos_x += m_font_width;
    }

    public void setTextSize(float value)
    {
        m_text_size = value;
    }

    public void setTextSpacing(float value) {
        m_text_spacing = value;
    }

    public void clear()
    {
        text.clear();
    }

    public float get_font_width()
    {
        return m_font_width;
    }

    public float get_font_height()
    {
        return m_font_height;
    }

    public void change_attribute(int attributes)
    {
        m_current_attribute = attributes;
    }

    public void gotoXY(int x, int y)
    {
        m_char_pos_x = x;
        m_char_pos_y = y;
        m_write_pos_x = x * m_font_width;
        m_write_pos_y = y * m_font_height;
    }

    public void update_attribute(float x_loc, float y_loc, int attribute)
    {
        try
        {
            text.get(text.indexOf(new char_text(x_loc, y_loc, '0', 0)))
                .change_attributes(attribute);
        }
        catch (java.lang.ArrayIndexOutOfBoundsException ignored)
        {

        }
    }
}
