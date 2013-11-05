package com.adapted.solitare;

/**
 * Created by mark on 6/6/13.
 */

public interface CardComponent
{
   public CardComponent find (CardComponentId _id);
   public CardComponent findTouched (float _posX, float _posY);
   public void processUserInput (int _type, int _param, float _posX, float _posY);
   public void moveTo (float _x, float _y);
   public void move (float _dx, float _dy);
   public float width ();
   public float height ();
   public void setOrientationLandscape ();
   public void setOrientationPortrait();
   public void setZorder (int _zorder);
   //public void setZorderOffset (int _offset);
   public void show (boolean _show);
   //public int executeCommand (CardCommand _cmd);
}

class CardComponentId
{
   public byte [] bytes = new byte[3];

   CardComponentId ()
   {
      bytes[0] = bytes[1] = bytes[2] = 0;
   }
   CardComponentId (byte [] _src, int _offset)
   {
      for (int i=0; i < 3; i++)
         bytes[i] = _src[_offset+i];
   }
   CardComponentId (byte b1, byte b2, byte b3)
   {
      bytes[0] = b1;
      bytes[1] = b2;
      bytes[2] = b3;
   }

   void set (byte [] _src, int _offset)
   {
      for (int i=0; i < 3; i++)
         bytes[i] = _src[_offset+i];
   }

   void set (byte b1, byte b2, byte b3)
   {
      bytes[0] = b1;
      bytes[1] = b2;
      bytes[2] = b3;
   }

   void set (CardComponentId _src)
   {
      bytes[0] = _src.bytes[0];
      bytes[1] = _src.bytes[1];
      bytes[2] = _src.bytes[2];
   }

   boolean equals (CardComponentId _cmp)
   {
      if (_cmp.bytes[0] == bytes[0] && _cmp.bytes[1] == bytes[1] && _cmp.bytes[2] == bytes[2])
         return true;
      else
         return false;
   }

   boolean equals (byte [] _bytes, int _offset)
   {
      if (_bytes[_offset] == bytes[0] && _bytes[_offset+1] == bytes[1] && _bytes[_offset+2] == bytes[2])
         return true;
      else
         return false;
   }
}

