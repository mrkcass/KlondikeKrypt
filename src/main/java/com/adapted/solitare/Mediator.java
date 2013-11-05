package com.adapted.solitare;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by mark on 6/8/13.
 */
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
interface Mediator
{
   public MMsg sendMsg (MMsg msg);
   public MMsg acquireMsg();
   public void releaseMsg (MMsg msg);
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
abstract class Colleague
{
   private Mediator mediator;

   public Colleague (Mediator _mediator)
   {
      mediator = _mediator;
   }

   public Colleague (Colleague _colleague)
   {
      mediator = _colleague.mediator;
   }

   public MMsg sendMsg (MMsg msg)
   {
      return mediator.sendMsg(msg);
   }

   public int receiveMsg (MMsg _msg, MMsg response)
   {
      return 0;
   }

   public MMsg acquireMsg()
   {
      return mediator.acquireMsg();
   }

   public void releaseMsg (MMsg msg)
   {
      mediator.releaseMsg(msg);
   }
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class MMsg
{
   public byte bytes[] = new byte[MAX_MSG_LEN];
   public int len = 0;
   public int fieldStartIdx[] = new int [MAX_FIELDS];
   public int fieldCount = 0;
   public int poolId = -1;

   public static final int MAX_FIELDS = 64;
   public static final int MAX_MSG_LEN = 256;
   public static final byte MSG_FIELD_DELIMITER = Byte.MAX_VALUE;
   public static final byte [] MSG_TERMINATOR = {Byte.MAX_VALUE, Byte.MAX_VALUE};

   public MMsg ()
   {
   }

   public static boolean load (MMsg _msg, byte [] bytes, int start, int len)
   {
      for (int i=0; i < len; i++)
         _msg.bytes[i] = bytes[start+i];

      _msg.len = _msg.bytes.length;
      _msg.fieldCount = 0;
      _msg.fieldStartIdx[_msg.fieldCount++] = 0;
      for (int i=0; i < _msg.len; i++)
      {
         if (_msg.bytes[i] == MMsg.MSG_FIELD_DELIMITER)
            _msg.fieldStartIdx[_msg.fieldCount++] = i+1;
      }

      return true;
   }

   public static void copy (MMsg _dst, MMsg _src)
   {
      for (int i=0; i < _src.len; i++)
         _dst.bytes[i] = _src.bytes[i];
      _dst.len = _src.len;
      for (int i=0; i < _src.fieldCount; i++)
         _dst.fieldStartIdx[i] = _src.fieldStartIdx[i];
      _dst.fieldCount = _src.fieldCount;
   }

   public static MMsg clear (MMsg _msg)
   {
      for (int i=0; i < _msg.len; i++)
         _msg.bytes[i] = 0;
      _msg.len = 0;
      _msg.fieldCount = 0;
      return _msg;
   }

   public static MMsg addField (MMsg _msg, byte [] _data)
   {
      if (_msg.len > 0)
         _msg.bytes[_msg.len++] = MSG_FIELD_DELIMITER;
      _msg.fieldStartIdx[_msg.fieldCount++] = _msg.len;

      for (int i = 0; i < _data.length; i++)
      {
         _msg.bytes[_msg.len++] = _data[i];
      }
      return _msg;
   }

   public static MMsg addField (MMsg _msg, byte _type, byte [] _data)
   {
      if (_msg.len > 0)
         _msg.bytes[_msg.len++] = MSG_FIELD_DELIMITER;
      _msg.fieldStartIdx[_msg.fieldCount++] = _msg.len;

      _msg.bytes[_msg.len++] = _type;
      int dlen = _data.length;
      for (int i = 0; i < dlen; i++)
      {
         _msg.bytes[_msg.len++] = _data[i];
      }
      return _msg;
   }

   public static MMsg addField (MMsg _msg, byte _type, int _val)
   {
      if (_msg.len > 0)
         _msg.bytes[_msg.len++] = MSG_FIELD_DELIMITER;
      _msg.fieldStartIdx[_msg.fieldCount++] = _msg.len;

      _msg.bytes[_msg.len++] = _type;

      byte[] val_bytes;
      //here is where I need to convert the int length to a byte array
      //todo : write directly to bytes instead of temp array then bytes
      val_bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(_val).array();
      for (int i = 0; i < 4; i++)
      {
         _msg.bytes[_msg.len++] = val_bytes[i];
      }
      return _msg;
   }

   public static MMsg addField (MMsg _msg, byte _type)
   {
      if (_msg.len > 0)
         _msg.bytes[_msg.len++] = MSG_FIELD_DELIMITER;
      _msg.fieldStartIdx[_msg.fieldCount++] = _msg.len;

      _msg.bytes[_msg.len++] = _type;
      return _msg;
   }

   public static MMsg addToField(MMsg _msg, byte _data)
   {
      _msg.bytes[_msg.len++] = _data;
      return _msg;
   }

   public static MMsg addToField(MMsg _msg, byte[] _data)
   {
      int dlen = _data.length;
      for (int i = 0; i < dlen; i++)
      {
         _msg.bytes[_msg.len++] = _data[i];
      }
      return _msg;
   }

   public static MMsg addToField(MMsg _msg, byte[] _data, int _offset, int _len)
   {
      for (int i = 0; i < _len; i++)
      {
         _msg.bytes[_msg.len++] = _data[_offset+i];
      }
      return _msg;
   }

   public static MMsg addToField(MMsg _msg, int _data)
   {
      byte[] val_bytes;
      //here is where I need to convert the int length to a byte array
      //todo : write directly to bytes instead of temp array then bytes
      val_bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(_data).array();
      for (int i = 0; i < 4; i++)
      {
         _msg.bytes[_msg.len++] = val_bytes[i];
      }
      return _msg;
   }

   public static MMsg addToField(MMsg _msg, float _data)
   {
      int len = Float.SIZE / Byte.SIZE;
      byte[] val_bytes;
      //here is where I need to convert the int length to a byte array
      //todo : write directly to bytes instead of temp array then bytes
      val_bytes = ByteBuffer.allocate(len).order(ByteOrder.LITTLE_ENDIAN).putFloat(_data).array();
      for (int i = 0; i < len; i++)
      {
         _msg.bytes[_msg.len++] = val_bytes[i];
      }
      return _msg;
   }

   public static MMsg copyField (MMsg _dst, MMsg _src, int _srcField)
   {
      if (_dst.len > 0)
         _dst.bytes[_dst.len++] = MSG_FIELD_DELIMITER;
      _dst.fieldStartIdx[_dst.fieldCount++] = _dst.len;

      for (int i = _src.fieldStartIdx[_srcField]; i < _src.len && _src.bytes[i] != MSG_FIELD_DELIMITER; i++)
      {
         _dst.bytes[_dst.len++] = _src.bytes[i];
      }
      return _dst;
   }

   public static MMsg copyFields (MMsg _dst, MMsg _src, int _srcFieldFirst, int _srcFieldLast)
   {
      for (int i = _srcFieldFirst; i < _srcFieldLast && i < _src.fieldCount-1; i++)
         MMsg.copyField(_dst, _src, i);
      return _dst;
   }

   public static byte subfieldByte (MMsg _msg, int _fieldIdx, int _subfieldIdx)
   {
      boolean found = false;
      byte sfByte = 0;

      if (_fieldIdx < _msg.fieldCount)
      {
         int idx = _msg.fieldStartIdx[_fieldIdx] + _subfieldIdx;
         int fieldEndIdx;
         if (_fieldIdx+1 < _msg.fieldCount)
            fieldEndIdx = _msg.fieldStartIdx[_fieldIdx+1];
         else
            fieldEndIdx = _msg.len;

         if (idx <= fieldEndIdx)
         {
            sfByte = _msg.bytes[idx];
            found = true;
         }
      }

      assert (found);

      return sfByte;
   }

   public static int subfieldInt (MMsg _msg, int _fieldIdx, int _subfieldOffset)
   {
      int ret = 0;
      if (_fieldIdx < _msg.fieldCount)
      {
         int start = _msg.fieldStartIdx[_fieldIdx] + _subfieldOffset;
         int fieldEndIdx;
         int size_of_int = 4;

         if (_fieldIdx+1 < _msg.fieldCount)
            fieldEndIdx = _msg.fieldStartIdx[_fieldIdx+1];
         else
            fieldEndIdx = _msg.len;

         if (start+size_of_int <= fieldEndIdx)
         {
            byte [] ibytes = new byte[4];
            for (int i=0; i < size_of_int; i++)
               ibytes[i] = _msg.bytes[start+i];
            final ByteBuffer bb = ByteBuffer.wrap(ibytes);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            ret = bb.getInt();
         }
      }
      return ret;
   }

   public static float subfieldFloat (MMsg _msg, int _fieldIdx, int _subfieldOffset)
   {
      float ret = 0;
      if (_fieldIdx < _msg.fieldCount)
      {
         int start = _msg.fieldStartIdx[_fieldIdx] + _subfieldOffset;
         int fieldEndIdx;
         int size_of_float = Float.SIZE / Byte.SIZE;

         if (_fieldIdx+1 < _msg.fieldCount)
            fieldEndIdx = _msg.fieldStartIdx[_fieldIdx+1];
         else
            fieldEndIdx = _msg.len;

         if (start+size_of_float <= fieldEndIdx)
         {
            byte [] ibytes = new byte[size_of_float];
            for (int i=0; i < size_of_float; i++)
               ibytes[i] = _msg.bytes[start+i];
            final ByteBuffer bb = ByteBuffer.wrap(ibytes);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            ret = bb.getFloat();
         }
      }
      return ret;
   }
}

