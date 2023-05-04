package com.craigwashere.wyse60.util;

class char_text
{
    float m_write_pos_x, m_write_pos_y;
    char character_to_print;
    int m_attribute;

    public char_text(float write_pos_x, float write_pos_y, char replacing_char, int attribute)
    {
        m_write_pos_x = write_pos_x;
        m_write_pos_y = write_pos_y;
        character_to_print = replacing_char;
        m_attribute = attribute;
    }
    public boolean equals(Object o)
    {
        if (o instanceof char_text)
        {
            //id comparison
            char_text mo = (char_text)o;
            return ((m_write_pos_y == ((char_text) o).m_write_pos_y) && (m_write_pos_x == ((char_text) o).m_write_pos_x));
        }
        return false;
    }

    public int hashCode()
    {   return java.util.Objects.hash(m_write_pos_x, m_write_pos_y);  }

    public void change_attributes(int attributes)
    {   m_attribute = attributes; }

}
