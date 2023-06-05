package com.craigwashere.wyse60.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Iterator;

public class TextArea
{
    private static final String TAG = "TextArea";
    private final short  T_NORMAL       = 0x30,
                         T_BLANK        = 0x01,
                         T_BLINK        = 0x02,
                         T_REVERSE      = 0x04,
                         T_UNDERSCORE   = 0x08,
                         T_DIM          = 0x40,
                         T_BOTH         = 0x44,
                         T_ALL          = 0x7f,
                         T_PROTECTED    = 0x100,
                         T_GRAPHICS     = 0x1000;

    private final short single_high__single_wide = 0x40,
                        single_high__double_wide = 0x41,
                        top_half_double_high__single_wide = 0x42,
                        bottom_half_double_high_single_wide = 0x43,
                        top_half_double_high__double_wide = 0x44,
                        bottom_half_double_high__double_wide = 0x45,
                        normal_background = 0x47,
                        bold_background = 0x48,
                        invisible_background = 0x49,
                        dim_background = 0x4a;

    private float m_single_font_width = 11.0F, m_single_font_height = 20.0F;

    int m_current_attribute;

    ArrayList<char_text> text;
    float m_write_pos_x, m_write_pos_y, m_text_size, m_current_font_width = 11.0f,
            m_text_spacing = 0F, m_current_font_height = 20.0F;

    int m_char_pos_x, m_char_pos_y;
    int text_color = Color.GREEN, background_color = Color.BLACK, dim_text_color = Color.BLUE;

    public TextArea(float text_size)
    {
        m_text_size = text_size;
        m_char_pos_x = 0;
        m_write_pos_x = 0;

        text = new ArrayList<char_text>();

        m_current_attribute = T_NORMAL;
        m_current_font_width = m_single_font_width;
    }

    public void draw_text(Canvas canvas, Paint mPaintText)
    {
//        int local_attribute = m_current_attribute;
        if (text.size() > 0)
        {
            for (char_text c : text)
            {
                // I'm sure we don't have to set attributes for every character, but I haven't
                // figured out a decent algorithm, yet.
                change_attributes(c, canvas, mPaintText);
                canvas.drawText(String.valueOf(c.character_to_print), c.m_write_pos_x, c.m_write_pos_y, mPaintText);
            }
        }
    }

    private void change_attributes(char_text c, Canvas canvas, Paint mPaintText)
    {
        switch (c.m_attribute)
        {
            case single_high__single_wide: /* 0x40 '@' */
                mPaintText.setTextScaleX(1.0F);
                break;
            case single_high__double_wide: /* 0x41 'A' */
                mPaintText.setTextScaleX(2.0F);
                break;
                /* I don't know what any of these mean. hopefully I won't encounter them*/
            case top_half_double_high__single_wide:
            case bottom_half_double_high_single_wide:
            case top_half_double_high__double_wide:
            case bottom_half_double_high__double_wide:
            case normal_background:
            case bold_background:
            case invisible_background:
            case dim_background:
                break;
            default:
                mPaintText.setTextScaleX(1.0F);
                if (c.m_attribute == T_NORMAL)
                {
                    mPaintText.setColor(Color.GREEN);
                    mPaintText.setFlags(0);
                    m_current_font_height = m_single_font_height;
                    m_current_font_width  = m_single_font_width;
                    mPaintText.setTextScaleX(1.0F);
                }

                if((c.m_attribute & T_DIM) != 0)
                    mPaintText.setColor(dim_text_color);

                if((c.m_attribute & T_UNDERSCORE) != 0)
                    mPaintText.setFlags(Paint.UNDERLINE_TEXT_FLAG);

                if((c.m_attribute & T_REVERSE) != 0)
                {
                    mPaintText.setColor(text_color);
                    canvas.drawRect(c.m_write_pos_x, c.m_write_pos_y-m_current_font_height,
                            c.m_write_pos_x+m_current_font_width, c.m_write_pos_y, mPaintText);
                    mPaintText.setColor(background_color);
                }
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
        m_write_pos_x += m_current_font_width;
    }

    public void clear_rectangle(int targetRow, int targetColumn, char fill_char)
    {
        Log.d(TAG, "clear_rectangle: (" + targetRow + ", " + targetColumn + "): " + fill_char);
        int     temp_char_pos_x  = m_char_pos_x,    temp_char_pos_y  = m_char_pos_y;
        float   temp_write_pos_x = m_write_pos_x,   temp_write_pos_y = m_write_pos_y;

        /* We could call add_char(fill_char) but we risk creating extra char_texts needlessly */
        for (m_write_pos_y = temp_write_pos_y; m_write_pos_y <= targetRow * m_current_font_height; m_write_pos_y += m_current_font_height)
            for (m_write_pos_x = temp_write_pos_x; m_write_pos_x <= targetColumn * m_current_font_width; m_write_pos_x += m_current_font_width)
            {
                char_text temp_char = new char_text(m_write_pos_x, m_write_pos_y, fill_char, m_current_attribute);
                int temp_char_location = text.indexOf(temp_char);

                if (temp_char_location != -1)
                    text.set(temp_char_location, temp_char);
            }

        m_write_pos_y = temp_write_pos_y;
        m_write_pos_x = temp_write_pos_x;
        m_char_pos_x = temp_char_pos_x;
        m_char_pos_y = temp_char_pos_y;
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
        return m_current_font_width;
    }

    public float get_font_height()
    {
        return m_current_font_height;
    }

    public void change_attribute(int attributes)
    {
        switch (attributes) {
            case single_high__single_wide: /* 0x40 '@' */
                m_current_font_height = m_single_font_height;
                m_current_font_width = m_single_font_width;
                break;
            case single_high__double_wide: /* 0x41 'A' */
                m_current_font_width = 2.0f * m_single_font_width;
                break;
            /* I don't know what any of these mean. hopefully I won't encounter them*/
            case top_half_double_high__single_wide:
            case bottom_half_double_high_single_wide:
            case top_half_double_high__double_wide:
            case bottom_half_double_high__double_wide:
            case normal_background:
            case bold_background:
            case invisible_background:
            case dim_background:
            default:
                m_current_font_height = m_single_font_height;
                m_current_font_width  = m_single_font_width;
                break;
        }
        m_current_attribute = attributes;
    }

    public void gotoXY(int x, int y)
    {
        m_char_pos_x = x;
        m_char_pos_y = y;
        m_write_pos_x = x * m_current_font_width;
        m_write_pos_y = y * m_current_font_height;
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
