package com.craigwashere.wyse60.data;

public class Constants
{
    static public class characterAttributes {
        public static final short T_NORMAL       =0x30,
        T_BLANK        =0x01,
        T_BLINK        =0x02,
        T_REVERSE      =0x04,
        T_UNDERSCORE   =0x08,
        T_DIM          =0x40,
        T_BOTH         =0x44,
        T_ALL          =0x7f,
        T_PROTECTED    =0x100,
        T_GRAPHICS     =0x1000;
    }

    static public final String graphicsCharacters = "┬└┌┐├┘│█┼┤─▓═┴║▒";
}
