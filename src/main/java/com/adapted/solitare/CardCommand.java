package com.adapted.solitare;

import java.util.ArrayList;

/**
 * Created by mark on 6/8/13.
 */
public class CardCommand
{
   private Mediator mediator;
   private MMsg command;
   public int step = 0;
   public long next_exec_time;
   public long initial_delay;
   public CardCommandReceiver src;
   public CardCommandReceiver dest;
   public int type = 1;
   public int order = -1;
   public boolean undo = false;
   public boolean immediate = false;
   public int seed = -1;
   public float posX, posY, sizeX, sizeY;
   public boolean stepSrc, stepDest;
   public boolean flip = false;

   public static final int TYPE_DEAL_CARD       = 1;
   public static final int TYPE_MOVE            = 2;
   public static final int TYPE_RECYCLE_WASTE   = 3;
   public static final int TYPE_MULTIMOVE       = 4;
   public static final int TYPE_INIT_DEAL       = 5;

   ArrayList<CardComponentId> multicards = null;


   public CardCommand (CardMediator _mediator, MMsg _cmd)
   {
      mediator = _mediator;
      command = _cmd;
      next_exec_time = 0;
      initial_delay = 0;
      order = -1;
      posX = posY = sizeX = sizeY = 0;
      stepSrc = stepDest = true;
      CardComponentId ccid = new CardComponentId();

      src = null;
      dest = null;

      if (MMsg.subfieldByte(_cmd, 0, 0) == Const.Cmd.DEAL)
         type = TYPE_INIT_DEAL;
      else if (MMsg.subfieldByte(_cmd, 0, 0) == Const.Cmd.DEAL_CARD)
         type = TYPE_DEAL_CARD;
      else if (MMsg.subfieldByte(_cmd, 0, 0) == Const.Cmd.MOVE)
         type = TYPE_MOVE;
      else if (MMsg.subfieldByte(_cmd, 0, 0) == Const.Cmd.RECYCLE_WASTE)
         type = TYPE_RECYCLE_WASTE;
      else if (MMsg.subfieldByte(_cmd, 0, 0) == Const.Cmd.MULTIMOVE)
         type = TYPE_MULTIMOVE;

      for (int i = 1; i < _cmd.fieldCount; i++)
      {
         if (MMsg.subfieldByte(_cmd, i, 0) == Const.Fld.INITIAL_DELAY)
         {
            initial_delay = MMsg.subfieldInt(_cmd, i, 1);
         }
         else if (MMsg.subfieldByte(_cmd, i, 0) == Const.Fld.ORDER)
         {
            order = MMsg.subfieldInt(_cmd, i, 1);
         }
         else if (MMsg.subfieldByte(_cmd, i, 0) == Const.Fld.SRC)
         {
            ccid.set (_cmd.bytes, _cmd.fieldStartIdx[i]+1);
            src = _mediator.find (ccid);
         }
         else if (MMsg.subfieldByte(_cmd, i, 0) == Const.Fld.DEST)
         {
            ccid.set (_cmd.bytes, _cmd.fieldStartIdx[i]+1);
            dest = _mediator.find (ccid);
         }
         else if (MMsg.subfieldByte(_cmd, i, 0) == Const.Fld.DEAL_SEED)
         {
            seed = MMsg.subfieldInt(_cmd, i, 1);
         }
         else if (MMsg.subfieldByte(_cmd, i, 0) == Const.Fld.CARD_FLIP)
         {
            flip = true;
         }
         else if (MMsg.subfieldByte(_cmd, i, 0) == Const.MediatorType.CARD)
         {
            CardComponentId mc = new CardComponentId();
            mc.set (_cmd.bytes, _cmd.fieldStartIdx[i]);
            if (multicards == null)
               multicards = new ArrayList<CardComponentId>();
            multicards.add (mc);
         }
      }
   }

   public long execute ()
   {
      long src_milliseconds_until_next_exec = 0;
      long dst_milliseconds_until_next_exec = 0;

      if (src != null && stepSrc)
         src_milliseconds_until_next_exec = src.executeCommand(this);
      if (src_milliseconds_until_next_exec == -1 || src == null)
         stepSrc = false;

      if (dest != null && stepDest)
         dst_milliseconds_until_next_exec = dest.executeCommand(this);
      if (dst_milliseconds_until_next_exec == -1 || dest == null)
         stepDest = false;

      step++;

      if (stepDest && stepSrc)
      {
         if (dst_milliseconds_until_next_exec < src_milliseconds_until_next_exec)
            return dst_milliseconds_until_next_exec;
         else
            return src_milliseconds_until_next_exec;
      }
      else if (stepDest)
         return dst_milliseconds_until_next_exec;
      else if (stepSrc)
         return src_milliseconds_until_next_exec;
      else
         return -1;
   }

   public int multicardRankIdx (int idx)
   {
      if (multicards != null && idx >= 0 && idx < multicards.size())
         return multicards.get(idx).bytes[2];
      else
         return -1;
   }

   public int multicardSuitIdx (int idx)
   {
      if (multicards != null && idx >= 0 && idx < multicards.size())
         return multicards.get(idx).bytes[1];
      else
         return -1;
   }

   public int multicardCount ()
   {
      if (multicards != null)
         return multicards.size();
      else
         return 0;
   }


   public int getSuit ()
   {
      if (multicards != null)
         return multicards.get(0).bytes[1];
      else
         return -1;
   }

   public int getRank ()
   {
      if (multicards != null)
         return multicards.get(0).bytes[2];
      else
         return -1;
   }

   public void convertToUndo ()
   {
      undo = true;
      step = 0;
      order = -1;

      CardCommandReceiver tmp;
      tmp = src;
      src = dest;
      dest = tmp;
      stepSrc = stepDest = true;
   }

   public MMsg getMsg ()
   {
      return command;
   }

   public void release ()
   {
      mediator.releaseMsg(command);
   }
}
