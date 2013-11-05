package com.adapted.solitare;

import android.os.Bundle;
import android.util.Log;

import java.util.Random;

/**
 * Created by mark on 6/8/13.
 */

public class PlayableMediator implements Mediator
{
   private Playable components;
   private CardCommandInvoker invoker = null;
   private GraphicsInterface graphics;
   private CardComponent dragComp = null;
   private CardMediatorMsgPool msgPool;

   PlayableMediator(GraphicsInterface _graphics, CardCommandInvoker _invoker)
   {
      components = new RootPile(this, _graphics);
      invoker = _invoker;
      graphics = _graphics;
      msgPool = new CardMediatorMsgPool();

      float ar = graphics.screenAspectRatio();
      if (ar < 1f)
      {
         components.setOrientationPortrait();
         createPortraitLayout();
      }
      else
      {
         components.setOrientationLandscape();
         createLandscapeLayout();
      }
   }

   public MMsg acquireMsg()
   {
      return msgPool.acquire();
   }

   public void releaseMsg (MMsg msg)
   {
      msgPool.release(msg);
   }

   public MMsg sendMsg (MMsg msg)
   {
      MMsg response = acquireMsg();
      CardComponent src = null, dest = null;

      MMsg.clear(response);

      if (MMsg.subfieldByte(msg, 0, 0) == Const.MsgType.GET_PARAM)
      {
         components.receiveMsg(msg, response);
      }
      else if (MMsg.subfieldByte(msg, 0, 0) == Const.MsgType.DEAL_CARD)
      {
         MMsg cmd_msg = acquireMsg();
         MMsg.addField(cmd_msg, Const.Cmd.DEAL_CARD);
         for (short i=1; i < msg.fieldCount; i++)
            MMsg.copyField(cmd_msg, msg, i); //cmd_msg = Colleague.makeMsg(cmd_msg, fieldStartIdx[i]);

         CardCommand cc = new CardCommand(this, cmd_msg);
         invoker.queueCommand(cc);
      }
      else if (MMsg.subfieldByte(msg, 0, 0) == Const.MsgType.MOVE) //else if (fieldStartIdx[0].equals(Const.Msg.MOVE))
      {
         MMsg cmd_msg = acquireMsg();
         MMsg.addField(cmd_msg, Const.Cmd.MOVE); //cmd_msg = Colleague.makeMsg(Const.Cmd.MOVE);
         for (short i=1; i < msg.fieldCount; i++)
            MMsg.copyField(cmd_msg, msg, i); //cmd_msg = Colleague.makeMsg(cmd_msg, fieldStartIdx[i]);

         CardCommand cc = new CardCommand(this, cmd_msg);
         invoker.queueCommand(cc);
      }
      else if (MMsg.subfieldByte(msg, 0, 0) == Const.MsgType.MULTIMOVE)
      {
         MMsg cmd_msg = acquireMsg();
         MMsg.addField(cmd_msg, Const.Cmd.MULTIMOVE); //cmd_msg = Colleague.makeMsg(Const.Cmd.MOVE);
         for (short i=1; i < msg.fieldCount; i++)
            MMsg.copyField(cmd_msg, msg, i); //cmd_msg = Colleague.makeMsg(cmd_msg, fieldStartIdx[i]);

         CardCommand cc = new CardCommand(this, cmd_msg);
         invoker.queueCommand(cc);
      }
      else if (MMsg.subfieldByte(msg, 0, 0) == Const.MsgType.RECYCLE_WASTE)
      {
         MMsg cmd_msg = acquireMsg();
         MMsg.addField(cmd_msg, Const.Cmd.RECYCLE_WASTE); //cmd_msg = Colleague.makeMsg(Const.Cmd.MOVE);
         for (short i=1; i < msg.fieldCount; i++)
            MMsg.copyField(cmd_msg, msg, i); //cmd_msg = Colleague.makeMsg(cmd_msg, fieldStartIdx[i]);

         CardCommand cc = new CardCommand(this, cmd_msg);
         invoker.queueCommand(cc);
      }

      return response;
   }

   public boolean processTouch (int _type, int _param, float _posX, float _posY)
   {
      CardComponent found = null;

      if (dragComp == null)
         found = components.findTouched(_posX, _posY);
      else
         found = dragComp;

      if (found != null || dragComp != null)
      {

         if (_type == Const.InputType.DRAG_START)
         {
            found.processUserInput(_type, _param, _posX, _posY);
            dragComp = found;
         }
         else if (dragComp != null && _type == Const.InputType.DRAG_END)
         {
            dragComp.processUserInput(_type, _param, _posX, _posY);
            dragComp = null;
         }
         else
            found.processUserInput(_type, _param, _posX, _posY);

         return true;
      }
      else
         return false;
   }

   public Playable find (CardComponentId _id)
   {
      return (Playable)components.find(_id);
   }

   private void createPortraitLayout ()
   {
      CardComponent cc;
      float pos_x, pos_y;
      float space;

      //stock pile 1st row right side of screen
      cc = components.find ( Const.PILE_ID_STOCK);
      pos_x = 2.0f  - graphics.cardWidth();
      pos_y = 2.0f - graphics.cardHeight();
      cc.moveTo(pos_x, pos_y);

      //waste pile -- adjacent to stock pile
      cc = components.find ( Const.PILE_ID_WASTE);
      space = .2f;
      float waste_pos_x = 2.0f  - ((graphics.cardWidth()*2f) + (graphics.cardWidth()*space));
      pos_y = 2.0f - graphics.cardHeight();
      cc.moveTo(waste_pos_x, pos_y);

      //foundation piles
      //before tableau so touch will play card to foundation before tableau
      cc = components.find ( Const.PILE_ID_FOUNDATIONS);
      pos_x = (waste_pos_x - cc.width()) / 2f;
      pos_y = 2.0f - graphics.cardHeight();
      cc.moveTo(pos_x, pos_y);

      //tableau piles second row centered horizontally
      cc = components.find ( Const.PILE_ID_TABLEAUS);
      pos_x = (2.0f  - cc.width()) / 2f;
      pos_y = 2f - ((graphics.cardHeight() *.1f) + (graphics.cardHeight() * 2f));
      cc.moveTo(pos_x, pos_y);
   }

   private void createLandscapeLayout ()
   {
      CardComponent cc;
      float pos_x, pos_y;
      float space;

      //stock pile 1st row right side of screen
      cc = components.find ( Const.PILE_ID_STOCK);
      pos_x = 2.0f  - graphics.cardWidth();
      pos_y = 2.0f - graphics.cardHeight();
      cc.moveTo(pos_x, pos_y);

      //waste pile -- adjacent to stock pile
      cc = components.find ( Const.PILE_ID_WASTE);
      space = .2f;
      pos_x = 2.0f  - ((graphics.cardWidth()*2f) + (graphics.cardWidth()*space));
      pos_y = 2.0f - graphics.cardHeight();
      cc.moveTo(pos_x, pos_y);
      float waste_x = pos_x;

      //foundation piles
      //before tableau so touch will play card to foundation before tableau
      cc = components.find ( Const.PILE_ID_FOUNDATIONS);
      pos_x = 0;
      pos_y = 2.0f - cc.height();
      cc.moveTo(pos_x, pos_y);

      //tableau piles second row centered horizontally
      cc = components.find ( Const.PILE_ID_TABLEAUS);
      float avail_width = waste_x - graphics.cardWidth();
      float tabs_width = cc.width();
      pos_x = graphics.cardWidth() + ((avail_width-tabs_width) / 2f);
      pos_y = 2f - graphics.cardHeight();
      cc.moveTo(pos_x, pos_y);
   }

   public void newGame (CardCommandInvoker _invoker)
   {
      invoker = _invoker;
      components = new RootPile(this, graphics);
      msgPool.clear();

      float ar = graphics.screenAspectRatio();
      if (ar < 1f)
      {
         components.setOrientationPortrait();
         createPortraitLayout();
      }
      else
      {
         components.setOrientationLandscape();
         createLandscapeLayout();
      }

      deal ();
   }

   public void saveState (Bundle _stateDataBuffer)
   {
      //invoker.saveState(_stateDataBuffer);
   }

   public void deal ()
   {
      MMsg msg = acquireMsg();
      Random rand = new Random();
      int seed = rand.nextInt();

      MMsg.addField(msg, Const.Cmd.DEAL)
          .addField(msg, Const.Fld.DEST).addToField(msg,  Const.PILE_ID_STOCK.bytes)
          .addField(msg, Const.Fld.DEAL_SEED, seed);
      CardCommand cc = new CardCommand(this, msg);
      invoker.queueCommand(cc);
   }

   public Playable GetRoot ()
   {
      return components;
   }
}


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class CardMediatorMsgPool
{
   private static final int CAPACITY = 1024;
   private int numUsed;
   private int next;
   private MMsg [] pool = new MMsg[CAPACITY];

   public CardMediatorMsgPool ()
   {
      numUsed = 0;
      next = 0;
      for (int i=0; i < CAPACITY; i++)
      {
         pool[i] = new MMsg();
         pool[i].poolId = -1;
      }
   }

   public void clear ()
   {
      numUsed = 0;
      next = 0;
      for (int i=0; i < CAPACITY; i++)
      {
         MMsg.clear (pool[i]);
         pool[i].poolId = -1;
      }
   }

   public MMsg acquire ()
   {
      MMsg newm = null;
      if (numUsed < CAPACITY)
      {
         while (pool[next].poolId != -1)
         {
            next++;
            if (next >= CAPACITY)
               next=0;
         }
         newm = pool[next];
         newm.poolId = next;
         numUsed++;
      }
      else
      {
         Log.e("solitare", "MMsg Pool exhasted");
      }
      return newm;
   }

   public void release (MMsg msg)
   {
      MMsg.clear(msg);
      msg.poolId = -1;
      numUsed--;
   }
}


