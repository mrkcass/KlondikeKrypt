package com.adapted.solitare;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.Collections;
import java.util.Random;

/**
 * Created by mark on 6/6/13.
 */

public class StockPile extends PlayableComposite
{

   //top of pile is element 0
   private boolean landscape;
   private float posX, posY;

   StockPile (Mediator _mediator, GraphicsInterface _graphics)
   {
      super(_mediator, _graphics);

      id = new CardComponentId(Const.MediatorType.STOCK, (byte)0, (byte)0);

      landscape = true;
      posX = posY = 0;

      int zorder = 0;
      for (int suit=0; suit < Const.NUM_SUITS; suit++)
      {
         for (int rank=0; rank < Const.NUM_RANKS; rank++)
         {
            Card new_card = new Card(this, suit+1, rank);
            components.add(new_card);
            new_card.setZorder(1);
            new_card.flip(Const.Angle.FACE_DOWN, GraphicsInterface.AXIS_JUSTIFY_NONE);
            zorder++;
         }
      }
   }

   public static boolean isClassId(CardComponentId id)
   {
      if (id.bytes[0] == Const.MediatorType.STOCK)
         return true;
      else
         return false;
   }

   ////message CARD_POSSIBLE_PLAY_OVERLAP
      //message
         //field 1 - <byte>GET_PARAM
         //field 2 - <byte>S_POSITION
         //field 3 - <byte[3]>id of requested object
      //response
         //field 1 - <byte>S_POSITION
         //field 2 - <byte>X_COORD<float>x coordinate of requested object
         //field 3 - <byte>Y_COORD<float>y coordinate of requested object
   @TargetApi(Build.VERSION_CODES.GINGERBREAD)
   public int receiveMsg (MMsg msg, MMsg response)
   {
      if (MMsg.subfieldByte(msg, 0, 0) == Const.MsgType.GET_PARAM)
      {
         if (MMsg.subfieldByte(msg, 1, 0) == Const.Fld.S_POSITION && id.equals(msg.bytes, msg.fieldStartIdx[2]))
         {
            MMsg.addField(response, Const.Fld.S_POSITION);
            MMsg.addField(response, Const.Fld.X_COORD).addToField(response, posX);
            MMsg.addField(response, Const.Fld.Y_COORD).addToField(response, posY);
         }
      }

      return 0;
   }

   @Override
   public void moveTo (float _x, float _y)
   {
      posX = _x;
      posY = _y;

      super.moveTo(_x, _y);
   }

   @Override
   public float width ()
   {
      return graphics.cardWidth();
   }

   @Override
   public float height ()
   {
      return graphics.cardHeight();
   }

   @Override
   public void setOrientationLandscape ()
   {
      landscape = true;
   }

   @Override
   public void setOrientationPortrait ()
   {
      landscape = false;
   }

   @Override
   public void show (boolean _show)
   {
      super.show(_show);
   }

   @Override
   public int GetPlaylist (Playlist list, PlaylistFilter filter)
   {
      PlaylistFilterTouch tf;

      if (filter.type == Const.PlayFilterType.PFT_TOUCH_PLAYABLE)
      {
         tf = (PlaylistFilterTouch)filter;
         Playable cc = findTouched(tf.x, tf.y);
         if (cc != null)
            list.AddPlay(cc, Const.PlayFilterType.PFT_TOUCH_PLAYABLE, id, id);
      }
      else if (filter.type == Const.PlayFilterType.PFT_TOUCH)
      {
         tf = (PlaylistFilterTouch)filter;
         processUserInput(tf.touchType, tf.touchParam, tf.x, tf.y);
      }

      return 1;
   }

   @Override public Playable findTouched (float _posX, float _posY)
   {
      if (_posX >= posX && _posX <= posX + graphics.cardWidth())
      {
         if (_posY >= posY && _posY <= posY + graphics.cardHeight())
         {
            return this;
         }
      }
      return null;
   }

   private void processTouch (int _type, int _param, float _posX, float _posY, Playlist playlist)
   {
      if (components.size() > 0)
         flipToWaste(playlist);
      else
         recycleWaste(playlist);
   }

   private void flipToWaste (Playlist playlist)
   {
      playlist.AddPlay(this, Const.Cmd.MOVE, id, Const.PILE_ID_WASTE);
      Card c = (Card)components.get(0);
      c.addToPlaylist(playlist);
   }

   private void recycleWaste (Playlist playlist)
   {
      MMsg response;
      MMsg msg = acquireMsg();

      MMsg.addField(msg, Const.MsgType.GET_PARAM);
      MMsg.addField(msg, Const.Fld.RECYCLE_CARDS);
      MMsg.addField(msg, Const.PILE_ID_WASTE.bytes);
      response = sendMsg(msg);

      if (response.fieldCount > 0)
      {
         playlist.AddPlay(this, Const.Cmd.RECYCLE_WASTE, Const.PILE_ID_WASTE, id);
         for (int i=1; i < response.fieldCount; i++)
            playlist.AddCard(MMsg.subfieldByte(response,i,1), MMsg.subfieldByte(response,i,2));
      }
      releaseMsg(response);
      releaseMsg(msg);
   }

   @Override
   public int executeCommand (CardCommand _cmd)
   {
      int ret_val = -1;

      if (_cmd.type == CardCommand.TYPE_INIT_DEAL)
      {
         shuffle(_cmd.seed);
         if (!_cmd.immediate)
            deal();
         ret_val = -1;
      }
      else if ((_cmd.type == CardCommand.TYPE_DEAL_CARD || _cmd.type == CardCommand.TYPE_MOVE) && _cmd.step == 0)
      {
         if (_cmd.src == this)
         {
            CardComponent cc = find (new CardComponentId(Const.MediatorType.CARD, (byte)_cmd.getSuit(), (byte)_cmd.getRank()));

            if (cc != null)
               components.remove(cc);
            if (components.size() > 0)
               components.get(0).show(true);
         }
         else if (_cmd.dest == this)
         {
            if (_cmd.step == 0)
            {
               if (components.size() > 0)
                  components.get(components.size() - 1).show(false);
               Card new_card = new Card(this, _cmd.getSuit(), _cmd.getRank());
               new_card.positionInPile = _cmd.order;
               components.add(0, new_card);
               new_card.moveTo(posX, posY);
               new_card.setZorder(1);
               new_card.flip(Const.Angle.FACE_DOWN, GraphicsInterface.AXIS_JUSTIFY_NONE);
               ret_val = -1;
            }
         }
      }
      else if (_cmd.type == CardCommand.TYPE_RECYCLE_WASTE && _cmd.step == 0)
      {
         if (_cmd.src == this)
            components.clear();
         else
         {
            for (int i=0; i < _cmd.multicardCount(); i++)
            {
               Card c = new Card(this, _cmd.multicardSuitIdx(i), _cmd.multicardRankIdx(i));
               components.add(c);
               c.show(false);
               c.moveTo(posX, posY);
               c.setZorder(1);
               c.flip(Const.Angle.FACE_DOWN, GraphicsInterface.AXIS_JUSTIFY_NONE);
            }
            components.get(0).show(true);
         }

         ret_val = -1;
      }
      return ret_val;
   }

   private void shuffle (int _seed)
   {
      Collections.shuffle(components, new Random(_seed));
      Collections.reverse(components);
      for (CardComponent cc : components)
         cc.show(false);
      components.get(0).show(true);
   }

   public void deal ()
   {
      MMsg msg = acquireMsg();

      MMsg.addField(msg, Const.MsgType.GET_PARAM);
      MMsg.addField(msg, Const.Fld.T_NUM_TABLEAU);
      MMsg response = sendMsg(msg);
      int num_tabs = MMsg.subfieldInt(response, 0, 1);
      releaseMsg (response);

      int cards_for_tab[] = new int[num_tabs];
      int max_cards_per_tab = 0;
      for (int tab = 0; tab < num_tabs; tab++)
      {
         MMsg.clear(msg);
         MMsg.addField(msg, Const.MsgType.GET_PARAM);
         MMsg.addField(msg, Const.Fld.T_NUM_DEAL_CARDS);
         MMsg.addField(msg, Const.MediatorType.TABLEAU).addToField(msg, (byte)tab+1).addToField(msg, (byte)0);
         response = sendMsg(msg);
         cards_for_tab[tab] = MMsg.subfieldInt(response, 0, 1);
         releaseMsg (response);
         if (cards_for_tab[tab] > max_cards_per_tab)
         {
            max_cards_per_tab = cards_for_tab[tab];
         }
      }

      int cards_dealt = 0;
      for (int card = 0; card < max_cards_per_tab; card++)
      {
         for (int tab = 0; tab < num_tabs; tab++)
         {
            if (card < cards_for_tab[tab])
            {
               Card c = (Card) components.get(cards_dealt);
               MMsg.clear(msg);
               MMsg.addField(msg, Const.MsgType.GET_PARAM);
               MMsg.addField(msg, Const.Fld.T_DEAL_INIT_DELAY);
               MMsg.addField(msg, Const.Fld.T_DEAL_INIT_DELAY_TAB, tab);
               MMsg.addField(msg, Const.Fld.T_DEAL_INIT_DELAY_CARD, card);
               response = sendMsg(msg);

               MMsg.clear(msg);
               MMsg.addField(msg, Const.MsgType.DEAL_CARD);
               MMsg.addField(msg, Const.Fld.DEST).addToField(msg,Const.MediatorType.TABLEAU).addToField(msg, (byte)tab+1).addToField(msg, (byte)0);
               MMsg.addField(msg, Const.Fld.SRC, id.bytes);
               MMsg.addField(msg, Const.Fld.INITIAL_DELAY, MMsg.subfieldInt(response, 0,1));
               MMsg.addField(msg, Const.Fld.ORDER, card);
               MMsg.addField(msg, c.id.bytes);
               releaseMsg (response);
               response = sendMsg(msg);
               releaseMsg (response);
               cards_dealt++;
            }
         }
      }
   }
}
