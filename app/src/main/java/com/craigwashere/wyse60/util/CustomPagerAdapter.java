package com.craigwashere.wyse60.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;

import com.craigwashere.wyse60.R;

import java.util.ArrayList;
import java.util.List;

public class CustomPagerAdapter extends PagerAdapter implements View.OnClickListener
{
    private static final String TAG = "CustomPagerAdapter ";
    private Context mContext;
    private List<Button> buttons;

    private static final int[] POSITION_0_BUTTON_IDS =
            {
                    R.id.btn_F1,
                    R.id.btn_F2,
                    R.id.btn_F3,
                    R.id.btn_F4,
                    R.id.btn_F5,
                    R.id.btn_F6,
                    R.id.btn_F7,
                    R.id.btn_F8,
                    R.id.btn_F9,
                    R.id.btn_F10,
                    R.id.btn_F11,
                    R.id.btn_F12,
            };

    private static final int[] POSITION_1_BUTTON_IDS =
            {
                    R.id.btn_1,
                    R.id.btn_2,
                    R.id.btn_3,
                    R.id.btn_4,
                    R.id.btn_5,
                    R.id.btn_6,
                    R.id.btn_7,
                    R.id.btn_8,
                    R.id.btn_9,
                    R.id.btn_0,
                    R.id.btn_space,
                    R.id.btn_enter,
            };

    private static final int[] POSITION_2_BUTTON_IDS =
            {
                    R.id.btn_up,
                    R.id.btn_down,
                    R.id.btn_left,
                    R.id.btn_right,
                    R.id.btn_A,
                    R.id.btn_B,
                    R.id.btn_C,
                    R.id.btn_D,
                    R.id.btn_E,
                    R.id.btn_F,
                    R.id.btn_G,
                    R.id.btn_H,
            };

    private static final int[] POSITION_3_BUTTON_IDS =
            {
                    R.id.btn_I,
                    R.id.btn_J,
                    R.id.btn_K,
                    R.id.btn_L,
                    R.id.btn_M,
                    R.id.btn_N,
                    R.id.btn_O,
                    R.id.btn_P,
                    R.id.btn_Q,
                    R.id.btn_R,
                    R.id.btn_S,
                    R.id.btn_T,
            };

    private static final int[] POSITION_4_BUTTON_IDS =
            {
                    R.id.btn_U,
                    R.id.btn_V,
                    R.id.btn_W,
                    R.id.btn_X,
                    R.id.btn_Y,
                    R.id.btn_Z,
            };

    public CustomPagerAdapter(Context context) {    mContext = context; }

    @Override
    public Object instantiateItem(ViewGroup collection, int position)
    {
        ModelObject modelObject = ModelObject.values()[position];
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewGroup layout = (ViewGroup) inflater.inflate(modelObject.getLayoutResId(), collection, false);
        collection.addView(layout);

        buttons = new ArrayList<Button>();

        int[] button_ids = POSITION_0_BUTTON_IDS;

        switch (position)
        {
            case 0: button_ids = POSITION_0_BUTTON_IDS;
                    break;
            case 1: button_ids = POSITION_1_BUTTON_IDS;
                    break;
            case 2: button_ids = POSITION_2_BUTTON_IDS;
                    break;
            case 3: button_ids = POSITION_3_BUTTON_IDS;
                    break;
            case 4: button_ids = POSITION_4_BUTTON_IDS;
                    break;
            default:
        }

        for(int id : button_ids)
        {
            Button button = (Button)collection.findViewById(id);
            button.setOnClickListener(this); // maybe
            buttons.add(button);
        }

        return layout;
    }


    @Override
    public void onClick(View v)
    {
//        String message = "";
        StringBuilder message = new StringBuilder();
        switch (v.getId())
        {
            case R.id.btn_F1:
                Log.d(TAG, "onClick: F1");message.append((char)0x01); message.append((char)0x40); message.append((char)0x0d);   break;
            case R.id.btn_F2:       message.append((char)0x01); message.append((char)0x41); message.append((char)0x0d);   break;
            case R.id.btn_F3:       message.append((char)0x01); message.append((char)0x42); message.append((char)0x0d);   break;
            case R.id.btn_F4:       message.append((char)0x01); message.append((char)0x43); message.append((char)0x0d);   break;
            case R.id.btn_F5:       message.append((char)0x3e); message.append((char)0x30); message.append((char)0x0d);   break;
            case R.id.btn_F6:       Log.d(TAG, "onClick: F6");message.append((char)0x01); message.append((char)0x45); message.append((char)0x0d);   break;
            case R.id.btn_F7:       message.append((char)0x01); message.append((char)0x46); message.append((char)0x0d);   break;
            case R.id.btn_F8:       message.append((char)0x01); message.append((char)0x47); message.append((char)0x0d);   break;
            case R.id.btn_F9:       message.append((char)0x01); message.append((char)0x48); message.append((char)0x0d);   break;
            case R.id.btn_F10:      message.append((char)0x01); message.append((char)0x49); message.append((char)0x0d);   break;
            case R.id.btn_F11:      message.append((char)0x01); message.append((char)0x4a); message.append((char)0x0d);   break;
            case R.id.btn_F12:      message.append((char)0x01); message.append((char)0x4b); message.append((char)0x0d);   break;
            case R.id.btn_1:        message.append('1');
                Log.d(TAG, "onClick: 1"); break;
            case R.id.btn_2:        message.append('2'); break;
            case R.id.btn_3:        message.append('3'); break;
            case R.id.btn_4:        message.append('4'); break;
            case R.id.btn_5:        message.append('5'); break;
            case R.id.btn_6:        message.append('6'); break;
            case R.id.btn_7:        message.append('7'); break;
            case R.id.btn_8:        message.append('8'); break;
            case R.id.btn_9:        message.append('9'); break;
            case R.id.btn_0:        message.append('0'); break;
            case R.id.btn_space:    message.append(' '); break;
            case R.id.btn_enter:    message.append((char)0x0d); break;
            case R.id.btn_up:       message.append((char)0x0b); break;
            case R.id.btn_down:     message.append((char)0x0a); break;
            case R.id.btn_left:     message.append((char)0x08); break;
            case R.id.btn_right:    message.append((char)0x0c); break;
            case R.id.btn_A:        message.append('a'); break;
            case R.id.btn_B:        message.append('b'); break;
            case R.id.btn_C:        message.append('c'); break;
            case R.id.btn_D:        message.append('d'); break;
            case R.id.btn_E:        message.append('e'); break;
            case R.id.btn_F:        message.append('f'); break;
            case R.id.btn_G:        message.append('g'); break;
            case R.id.btn_H:        message.append('h'); break;
            case R.id.btn_I:        message.append('i'); break;
            case R.id.btn_J:        message.append('j'); break;
            case R.id.btn_K:        message.append('k'); break;
            case R.id.btn_L:        message.append('l'); break;
            case R.id.btn_M:        message.append('m'); break;
            case R.id.btn_N:        message.append('n'); break;
            case R.id.btn_O:        message.append('o'); break;
            case R.id.btn_P:        message.append('p'); break;
            case R.id.btn_Q:        message.append('q'); break;
            case R.id.btn_R:        message.append('r'); break;
            case R.id.btn_S:        message.append('s'); break;
            case R.id.btn_T:        message.append('t'); break;
            case R.id.btn_U:        message.append('u'); break;
            case R.id.btn_V:        message.append('v'); break;
            case R.id.btn_W:        message.append('w'); break;
            case R.id.btn_X:        message.append('x'); break;
            case R.id.btn_Y:        message.append('y'); break;
            case R.id.btn_Z:        message.append('z'); break;

            default: message.append('a'); break;
        }

        Log.d(TAG, "onClick: char_to_send: "+ message + '.');
        Log.d(TAG, "onClick: message_length: " + message.length());
        Intent intent = new Intent("send_character");
        intent.putExtra("message", message.toString());
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view)
    {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() { return ModelObject.values().length; }

    @Override
    public boolean isViewFromObject(View view, Object object)
    {   return view == object;  }

    @Override
    public CharSequence getPageTitle(int position)
    {
        ModelObject customPagerEnum = ModelObject.values()[position];
        return mContext.getString(customPagerEnum.getTitleResId());
    }
}
