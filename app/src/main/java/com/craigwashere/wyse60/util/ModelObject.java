package com.craigwashere.wyse60.util;

import com.craigwashere.wyse60.R;

public enum ModelObject
{
    K1(R.string.K1, R.layout.fragment_keyboard_1),
    K2(R.string.K2, R.layout.fragment_keyboard_2),
    K3(R.string.K3, R.layout.fragment_keyboard_3),
    K4(R.string.K4, R.layout.fragment_keyboard_4),
    K5(R.string.K5, R.layout.fragment_keyboard_5);

    private int mTitleResId;
    private int mLayoutResId;

    ModelObject(int titleResId, int layoutResId)
    {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    public int getTitleResId()  {   return mTitleResId;     }

    public int getLayoutResId() {   return mLayoutResId;    }
}
