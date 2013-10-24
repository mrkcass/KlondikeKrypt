package com.adapted.solitare;

import java.util.ArrayList;

/**
 * Created by mark on 6/8/13.
 */
public class FoundationPiles extends CardColleagueComposite
{
   private final int numFoundations = 4;
   private boolean portrait = true;

   FoundationPiles(Mediator _mediator, GraphicsInterface _graphics)
   {
      super (_mediator, _graphics);

      id = new CardComponentId(Const.MediatorType.FOUNDATION, (byte)0, (byte)0);

      for (int i = 0; i < numFoundations; i++)
         components.add(new Foundation(_mediator, _graphics, i+1));
   }

   public int receiveMsg(MMsg msg, MMsg response)
   {
      int error = 0;
      int child_error = 0;

      int len = components.size();
      for (int i=0; i < len; i++)
         child_error = components.get(i).receiveMsg(msg, response);

      if (child_error != 0)
         return child_error;
      else
         return error;
   }

   @Override
   public void moveTo(float _x, float _y)
   {
      if (portrait)
      {
         float dist_to_next_card = graphics.cardWidth() + (graphics.cardWidth()*.1f);
         float x = _x;

         for (CardColleague cc : components)
         {
            cc.moveTo(x, _y);
            x += dist_to_next_card;
         }
      }
      else
      {
         float dist_to_next_card = graphics.cardHeight() + (graphics.cardHeight()*.03f);
         float y = _y;

         for (int i = components.size()-1; i >= 0; i--)
         {
            components.get(i).moveTo(_x, y);
            y += dist_to_next_card;
         }
      }
   }

   @Override 
   public float width ()
   {
      if (!portrait)
         return graphics.cardWidth();
      else
      {
         float w;

         w = graphics.cardWidth() * numFoundations;
         w += (graphics.cardWidth() * .1) * (numFoundations - 1);

         return w;
      }
   }

   @Override 
   public float height ()
   {
      if (portrait)
         return graphics.cardHeight();
      else
      {
         float h;

         h = graphics.cardHeight() * numFoundations;
         h += (graphics.cardHeight() * .03) * (numFoundations - 1);

         return h;
      }
   }

   @Override
   public void setOrientationLandscape()
   {
      portrait = false;
   }

   @Override
   public void setOrientationPortrait()
   {
      portrait = true;
   }
}


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class Foundation extends CardColleagueComposite
{
   private static final String baseName = "foundation";
   private float posX, posY;
   private int cardsOnPile;
   private Card movingCard;
   private long moveDelayMS = 32;
   private final int zorderBase = 100;

   Foundation(Mediator _mediator, GraphicsInterface _graphics, int _index)
   {
      super (_mediator, _graphics);

      id = new CardComponentId(Const.MediatorType.FOUNDATION, (byte)_index, (byte)0);
      posX = posY = 0;
      cardsOnPile = 0;
   }

   @Override
   public int receiveMsg(MMsg msg, MMsg response)
   {
      if (MMsg.subfieldByte(msg, 0, 0) == Const.MsgType.GET_PARAM)
      {
         if (MMsg.subfieldByte(msg, 1, 0) == Const.Fld.CARD_POSSIBLE_PLAYS)
         {
            boolean src_is_foundation = false;
            boolean can_play_card = false;

            if (MMsg.subfieldByte(msg, 2, 1) == Const.MediatorType.FOUNDATION)
               src_is_foundation = true;
            else if (MMsg.subfieldByte(msg, 3, 0) == Const.MediatorType.CARD)
            {
               if (components.size() == 0)
               {
                  if (MMsg.subfieldByte(msg, 3, 2) == Const.Rank.ACE)
                     can_play_card = true;
               }
               else if (components.size() == 1)
               {
                  Card c = (Card) components.get(components.size()-1);

                  if (c.sameSuit(MMsg.subfieldByte(msg, 3, 1)) && MMsg.subfieldByte(msg, 3, 2) == Const.Rank.TWO)
                  {
                     can_play_card = true;
                  }
               }
               else
               {
                  Card c = (Card) components.get(components.size()-1);

                  if (c.sameSuit(MMsg.subfieldByte(msg, 3, 1)) && c.precedesRank(MMsg.subfieldByte(msg, 3, 2)))
                  {
                     can_play_card = true;
                  }
               }
            }

            if (!src_is_foundation && can_play_card)
            {
               MMsg.addField(response, Const.Fld.CARD_POSSIBLE_PLAYS);
               MMsg.addField(response, id.bytes);
            }
         }
      }
      return 0;
   }

   @Override
   public void moveTo(float _x, float _y)
   {
      posX = _x;
      posY = _y;

      super.moveTo(_x,_y);
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
   public int executeCommand(CardCommand _cmd)
   {
      if (_cmd.type == CardCommand.TYPE_MOVE)
      {
         if (_cmd.dest == this)
         {
            if (_cmd.immediate)
               return moveCardImmediate(_cmd);
            else
               return moveCardToPile (_cmd);
         }
         else
         {
            CardComponent cc = find(new CardComponentId(Const.MediatorType.CARD, (byte) _cmd.getSuit(), (byte) _cmd.getRank()));
            if (cc != null)
               components.remove(cc);
            cardsOnPile--;
            return  -1;
         }
      }
      else
         return -1;
   }

   private int moveCardToPile (CardCommand _cmd)
   {
      long delay = moveDelayMS;

      if (_cmd.step == 0)
      {
         if (_cmd.order == -1)
            _cmd.order = cardsOnPile;
         Card new_card = addCard (_cmd.getSuit(), _cmd.getRank(), _cmd.order);
         cardsOnPile++;

         if (movingCard == null)
         {
            movingCard = new_card;
            movingCard.setAnimator(new CardAnimStrategyTabToTab(movingCard, posX, posY));
         }
      }
      else if (movingCard != null && movingCard.positionInPile == _cmd.order)
      {
         delay = movingCard.animator().step();
         _cmd.posX = movingCard.posX(); _cmd.posY = movingCard.posY();
         _cmd.sizeX = movingCard.width(); _cmd.sizeY = movingCard.height();
         if (delay == 0)
         {
            if (movingCard.positionInPile < cardsOnPile-1)
            {
               movingCard = (Card)components.get(movingCard.positionInPile+1);
               movingCard.setAnimator(new CardAnimStrategyTabToTab(movingCard, posX, posY));
            }
            else
            {
               movingCard.setAnimator(null);
               movingCard = null;
            }
         }
      }

      if (delay != 0)
         return (int)delay;
      else
         return -1;
   }

   public int moveCardImmediate (CardCommand _cmd)
   {
      Card new_card = new Card(this, _cmd.getSuit(), _cmd.getRank());

      if (_cmd.order == -1)
         _cmd.order = cardsOnPile;
      new_card.positionInPile = _cmd.order;
      components.add(new_card);

      new_card.moveTo(posX, posY );
      new_card.setZorder(zorderBase + new_card.positionInPile);
      //_cmd.posX = new_card.posX(); _cmd.posY = new_card.posY();
      //_cmd.sizeX = new_card.width(); _cmd.sizeY = new_card.height();
      new_card.spin(0);
      cardsOnPile++;
      return -1;
   }

   private Card addCard (int _suitIdx, int _rankIdx, int _order)
   {
      Card new_card = new Card(this, _suitIdx, _rankIdx);

      new_card.positionInPile = _order;
      new_card.setZorder(zorderBase+_order);
      components.add(new_card);

      return new_card;
   }

   @Override
   public int GetPlaylist (Playlist list, PlaylistFilter filter)
   {
      PlaylistFilterTouch tf;

      if (filter.type == Const.PlayFilterType.PFT_TOUCH_PLAYABLE)
      {
         tf = (PlaylistFilterTouch)filter;
         CardColleague cc = findTouched(tf.x, tf.y);
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

   @Override
   public CardColleague findTouched (float _posX, float _posY)
   {
      boolean touched = false;

      if (components.size() > 0)
      {
         //todo findTouched
         float left = posX;
         float right = posX + graphics.cardWidth();
         float bottom = posY;
         float top = posY + graphics.cardHeight();

         if (_posX >= left && _posX <= right)
            if (_posY >= bottom && _posY <= top)
               touched = true;
      }
      if (touched)
         return this;
      else
         return null;
   }

   @Override
   public void processUserInput (int _type, int _param, float _posX, float _posY)
   {
      if (_type == Const.InputType.TOUCH)
         processTouch ();
   }

   private void processTouch ()
   {
      MMsg response;
      MMsg msg = acquireMsg();
      CardComponentId card_id = components.get(components.size()-1).id;

      MMsg.addField(msg, Const.MsgType.GET_PARAM);
      MMsg.addField(msg, Const.Fld.CARD_POSSIBLE_PLAYS);
      MMsg.addField(msg, Const.Fld.SRC, id.bytes);
      MMsg.addField(msg, card_id.bytes);
      response = sendMsg(msg);

      if (response.fieldCount > 0)
      {
         int move_to_fld = 0;
         int highest_tab_index = -1;

         for (int idx=1; idx < response.fieldCount; idx+=2)
         {
            if (MMsg.subfieldByte(response, idx, 0) == Const.MediatorType.TABLEAU)
            {
               int tindex = MMsg.subfieldByte(response, idx, 1);
               if (tindex > highest_tab_index)
               {
                  highest_tab_index = tindex;
                  move_to_fld = idx;
               }
            }
         }

         MMsg.clear(msg);
         MMsg.addField(msg, Const.MsgType.MOVE);
         MMsg.addField(msg, Const.Fld.DEST).addToField(msg, response.bytes, response.fieldStartIdx[move_to_fld], 3);
         MMsg.addField(msg, Const.Fld.SRC, id.bytes);
         MMsg.addField(msg, card_id.bytes);

         releaseMsg (response);
         response = sendMsg(msg);
      }
      releaseMsg(msg);
      releaseMsg(response);
   }
}
