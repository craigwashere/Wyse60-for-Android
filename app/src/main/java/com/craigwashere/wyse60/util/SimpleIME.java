package com.craigwashere.wyse60.util;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

import com.craigwashere.wyse60.R;

import java.util.List;

public class SimpleIME extends InputMethodService implements KeyboardView.OnKeyboardActionListener
{
    final static String TAG = "SimpleIME";
    private KeyboardView m_keyboard_view;
    private Keyboard m_symbols_keyboard, m_qwerty_keyboard;

    private final int KEYCODE_CTRL = -7;

    private boolean CTRL_is_checked = false;
    private boolean SHIFT_is_checked = false;
    private boolean ALT_is_checked = false;

    @Override
    public View onCreateInputView()
    {
        m_keyboard_view = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard, null);
        m_symbols_keyboard = new Keyboard(this, R.xml.symbols);
        m_qwerty_keyboard = new Keyboard(this, R.xml.qwerty);
        m_keyboard_view.setKeyboard(m_symbols_keyboard);
        m_keyboard_view.setOnKeyboardActionListener(this);
        return m_keyboard_view;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes)
    {
        Log.d(TAG, "onKey: " + Integer.toHexString(primaryCode));
        InputConnection ic = getCurrentInputConnection();

        switch(primaryCode)
        {
            case Keyboard.KEYCODE_DELETE:   ic.deleteSurroundingText(1, 0);
                                            break;
            case Keyboard.KEYCODE_SHIFT:    SHIFT_is_checked = !SHIFT_is_checked;
                                            if (m_keyboard_view.getKeyboard() == m_qwerty_keyboard)
                                            {
                                                m_qwerty_keyboard.setShifted(SHIFT_is_checked);
                                                m_keyboard_view.invalidateAllKeys();
                                            }
                                            break;
            case Keyboard.KEYCODE_ALT:      ALT_is_checked = !ALT_is_checked;
                                            break;
            case Keyboard.KEYCODE_DONE:     ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                                            break;
            //for some reason, we don't have have a keycode for CTRL key
            case KEYCODE_CTRL:              CTRL_is_checked = !CTRL_is_checked;
                                            break;
            case Keyboard.KEYCODE_MODE_CHANGE:  Keyboard current = m_keyboard_view.getKeyboard();
                                                if(current == m_qwerty_keyboard) m_keyboard_view.setKeyboard(m_symbols_keyboard);
                                                else                             m_keyboard_view.setKeyboard(m_qwerty_keyboard);

                                                //since we set a new keyboard, we have to get it again
                                                current = m_keyboard_view.getKeyboard();
                                                set_modifier_keys(current.getKeys());
                                                current.setShifted(SHIFT_is_checked);
                                                break;
            default:                        Log.d(TAG, "onKey: " + primaryCode);

                                            int META = 0;
                                            if (SHIFT_is_checked)   META += 1;      //META_SHIFT_ON = 0x00000001
                                            if (CTRL_is_checked)    META += 4096;   //META_CTRL_ON  = 0x00001000
                                            if (ALT_is_checked)     META += 2;      //META_ALT_ON   = 0x00000002

                                            ic.sendKeyEvent(new KeyEvent(0,             //downTime
                                                                         0,                      //eventTime
                                                                         KeyEvent.ACTION_DOWN,   //action
                                                                         primaryCode,            //code
                                                                         0,                      //repeat
                                                                         META));                 //metaState
        }
    }

    private void set_modifier_keys(List<Keyboard.Key> keys)
    {
        //the 'isModifier' doesn't add a key to modifier group, so we have to cycle through all the keys
        for (Keyboard.Key key : keys)
        {
            if (key.sticky)
                switch(key.codes[0])
                {
                    case KEYCODE_CTRL:          key.on = CTRL_is_checked;   break;
                    case Keyboard.KEYCODE_ALT:  key.on = ALT_is_checked;    break;
                    case Keyboard.KEYCODE_SHIFT:key.on = SHIFT_is_checked;  break;
                    default:    break; //don't know how we got here
                }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        getCurrentInputConnection().sendKeyEvent(new KeyEvent(event));
        return true;
    }

    @Override
    public void onPress(int primaryCode)
    {   }

    @Override
    public void onRelease(int primaryCode)
    {   }

    @Override
    public void onText(CharSequence text)
    {   }

    @Override
    public void swipeDown()
    {   }

    @Override
    public void swipeLeft()
    {   }

    @Override
    public void swipeRight()
    {   }

    @Override
    public void swipeUp()
    {   }
}
