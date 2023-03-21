package com.craigwashere.Wyse60;

class _attributes
{
    int startX, startY;
    int effect;
    int effect_length=0;

    void set_effect(int effect)         {   this.effect = effect;   }
    int  get_effect()                   {   return this.effect;     }

    void set_startX(int X)              {   this.startX = X;    }
    int  get_startX()                   {   return this.startX; }

    void set_startY(int Y)              {   this.startY = Y;    }
    int  get_startY()                   {   return this.startY; }

    void set_effect_length(int length)  {   this.effect_length = length;}
    int  get_effect_length()             {  return this.effect_length;   }
}