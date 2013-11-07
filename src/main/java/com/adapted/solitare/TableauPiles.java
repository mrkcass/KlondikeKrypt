package com.adapted.solitare;

import java.util.Collections;

/**
 * Created by mark on 6/6/13.
 */
public class TableauPiles extends PlayableComposite
{
   private final int num_tableau = 7;
   private final int tableau_sizes[] = {1, 2, 3, 4, 5, 6, 7};
   private int dealInitialDelayTable[][];

   TableauPiles (Mediator _mediator, GraphicsInterface _graphics)
   {
      super(_mediator, _graphics);
      id = new CardComponentId(Const.MediatorType.TABLEAU, (byte)0, (byte)0);
      for (int i = 0; i < num_tableau; i++)
         components.add(new Tableau(_mediator, _graphics, i+1, tableau_sizes[i]));
      dealInitialDelayTable = null;
   }

   ////message T_NUM_DEAL_CARDS
      //message
         //field 1 - <byte>GET_PARAM
         //field 2 - <byte>T_NUM_TABLEAU
      //repsonse
         //field 1 - <byte>T_NUM_TABLEAU<int>>number of tableaus
   ////message T_DEAL_INIT_DELAY
      //message
         //field 1 - <byte>GET_PARAM
         //field 2 - <byte>T_DEAL_INIT_DELAY
         //field 3 - <byte>T_DEAL_INIT_DELAY_TAB<int>tableau to query
         //field 3 - <byte>T_DEAL_INIT_DELAY_CARD<int>card to query
      //repsonse
         //field 1 - <byte>T_DEAL_INIT_DELAY<int>delay in ms.
   public int receiveMsg (MMsg msg, MMsg response)
   {
      int error = 0;
      int child_error = 0;

      if (MMsg.subfieldByte(msg, 0, 0) == Const.MsgType.GET_PARAM)
      {
         if (MMsg.subfieldByte(msg, 1, 0) == Const.Fld.T_NUM_TABLEAU)
         {
            MMsg.addField(response, Const.Fld.T_NUM_TABLEAU).addToField(response, components.size());
         }
         else if (MMsg.subfieldByte(msg, 1, 0) == Const.Fld.T_DEAL_INIT_DELAY)
         {
            if (dealInitialDelayTable == null)
               buildDealTimeTable();
            int tab = MMsg.subfieldInt(msg, 2, 1);
            int card = MMsg.subfieldInt(msg, 3, 1);
            MMsg.addField(response, Const.Fld.T_DEAL_INIT_DELAY, dealInitialDelayTable[tab][card]);
         }
      }

      int len = components.size();
      for (int i=0; i < len; i++)
         child_error = components.get(i).receiveMsg(msg, response);

      if (child_error != 0)
         return child_error;
      else
         return error;
   }

   @Override
   public void moveTo (float _x, float _y)
   {
      float dist_to_next_card = graphics.cardWidth() + (graphics.cardWidth()*.1f);
      float x = _x;

      for (Playable cc : components)
      {
         cc.moveTo(x, _y);
         x += dist_to_next_card;
      }
   }

   @Override 
   public float width ()
   {
      float w;

      w = graphics.cardWidth() * num_tableau;
      w += (graphics.cardWidth() * .1) * (num_tableau - 1);

      return w;
   }

   @Override 
   public float height ()
   {
      float max_height = 0;
      float h;

      for (Playable tab : components)
      {
         h = tab.height();
         if (h > max_height)
            max_height = h;
      }

      return max_height;
   }

   private void buildDealTimeTable ()
   {
      dealInitialDelayTable = new int [num_tableau][num_tableau];
      float stock_pile_x = 0, stock_pile_y = 0;

      //String msg = Colleague.makeMsg(Const.Msg.GET_PARAM, Const.Fld.S_POSITION, StockPile.name);
      MMsg msg = acquireMsg();
      MMsg.addField(msg, Const.MsgType.GET_PARAM).addField(msg, Const.Fld.S_POSITION).addField(msg, id.bytes);
      MMsg response = sendMsg(msg);
      if (response.fieldCount >= 1)
      {
         stock_pile_x = MMsg.subfieldFloat(response, 1, 1);
         stock_pile_y = MMsg.subfieldFloat(response, 2, 1);
      }
      releaseMsg(msg);
      releaseMsg(response);

      int total_time = 0;
      int tab_times[] = new int [num_tableau];
      int time;
      for (int card=0; card < num_tableau; card++)
      {
         for (int tab = 0; tab < num_tableau; tab++)
         {
            if (card < tableau_sizes[tab])
            {
               if (card == 0)
               {
                  //time = components.get(tab).getDealFlightTime(card, stock_pile_x, stock_pile_y) / 2;
                  //tab_times[tab] = time * 2;
                  time = ((Tableau)components.get(tab)).getDealFlightTime(card, stock_pile_x, stock_pile_y) / 2;
                  tab_times[tab] = time * 2;
                  dealInitialDelayTable[tab][card] = total_time + time;
               }
               else
               {
                  time = ((Tableau)components.get(tab)).getDealFlightTime(card, stock_pile_x, stock_pile_y) / 2;
                  //dealInitialDelayTable[tab][card] = tab_times[tab];
                  dealInitialDelayTable[tab][card] = total_time + time;
                  tab_times[tab] += time;
               }
               total_time += time;
            }
         }
         //if (card == 0)
            //total_time *= 2;
      }
   }
}


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class Tableau extends PlayableComposite
{
   byte index;
   int maxCards;
   Card movingCard = null;
   Card flipCard = null;
   Card dragCard = null;
   float posX, posY;
   float cardOffset;
   int cardsOnPile = 0;
   private int dealFlightDelayMsPerStep = 32;
   private static final String baseName = "tableau";
   private final int zorderBase = 100;


   Tableau (Mediator _mediator, GraphicsInterface _graphics, int _index, int _maxCards)
   {
      super(_mediator, _graphics);
      id = new CardComponentId(Const.MediatorType.TABLEAU, (byte)_index, (byte)0);
      index = (byte)_index;
      maxCards = _maxCards;
      posX = posY = 0;
      cardOffset = graphics.cardHeight() * -.2f;
   }

   ////message T_NUM_DEAL_CARDS
      //message
         //field 1 - <byte>GET_PARAM
         //field 2 - <byte>T_NUM_DEAL_CARDS
         //field 3 - <byte[3]>id for intended component
      //repsonse
         //field 1 - <byte>T_NUM_DEAL_CARDS<int>num of deal cards
   ////message CARD_POSSIBLE_PLAYS
      //message
         //field 1 - <byte>GET_PARAM
         //field 2 - <byte>CARD_POSSIBLE_PLAYS
         //field 3 - <byte>SRC<byte[3]>source id
         //field 4 - <byte[3]>id of card requesting to play
      //response
         //field 1 - <byte> CARD_POSSIBLE_PLAYS
         //field 2 - <byte[3]>id of tableau that is possible play
   ////message CARD_POSSIBLE_MULTIPLAY
      //message
         //field 1 - <byte>GET_PARAM
         //field 2 - <byte>CARD_POSSIBLE_MULTIPLAY
         //field 3 - <byte>SRC<byte[3]>source id
         //field 4 - <byte[3]>id of first card requesting to play
      //response
         //field 1 - <byte>CARD_POSSIBLE_MULTIPLAY
         //field 2 - <byte[3]>id of tableau that is possible play
   ////message CARD_POSSIBLE_PLAY_OVERLAP
      //message
         //field 1 - <byte>GET_PARAM
         //field 2 - <byte>CARD_POSSIBLE_PLAY_OVERLAP
         //field 3 - <byte>SRC<byte[3]>source id
         //field 4 - <byte[3]>id of first card requesting to play
      //response
         //field 1 - <byte>CARD_POSSIBLE_PLAY_OVERLAP
         //field 2 - <byte[3]>id of tableau that is possible play
         //field 3 - <byte>CARD_OVERLAP_PERCENT<int>percentage of overlap normalized to 10,000
   @Override
   public int receiveMsg (MMsg msg, MMsg response)
   {
      if (MMsg.subfieldByte(msg, 0, 0) == Const.MsgType.GET_PARAM)
      {
         if (MMsg.subfieldByte(msg, 1, 0) == Const.Fld.T_NUM_DEAL_CARDS && id.equals(msg.bytes, msg.fieldStartIdx[2]))
         {
            MMsg.addField(response, Const.Fld.T_NUM_DEAL_CARDS).addToField(response, maxCards);
         }
         else if (MMsg.subfieldByte(msg,1,0) ==  Const.Fld.CARD_POSSIBLE_PLAYS || MMsg.subfieldByte(msg,1,0) == Const.Fld.CARD_POSSIBLE_MULTIPLAY || MMsg.subfieldByte(msg,1,0) == Const.Fld.CARD_POSSIBLE_PLAY_OVERLAP)
         {
            boolean possible = false;
            boolean overlap_msg = MMsg.subfieldByte(msg, 1, 0) == Const.Fld.CARD_POSSIBLE_PLAY_OVERLAP;
            byte mtype = MMsg.subfieldByte(msg,1,0);
            if (components.size() > 0)
            {
               Card c = (Card)components.get(components.size()-1);

               if (MMsg.subfieldByte(msg, 3, 2) != Const.Rank.ACE && c.oppositeSuit (MMsg.subfieldByte(msg, 3, 1)) && c.succeedsRank(MMsg.subfieldByte(msg, 3, 2)))
               {
                  if (!overlap_msg)
                     MMsg.addField(response, mtype).addField(response, id.bytes);
                  possible = true;
               }
            }
            else if (MMsg.subfieldByte(msg, 3, 2) == Const.Rank.KING)
            {
               if (!overlap_msg)
                  MMsg.addField(response, mtype).addField(response, id.bytes);
               possible = true;
            }
            if (overlap_msg && possible)
            {
               float cwidth = graphics.cardWidth();
               float src_x = graphics.cardX(graphics.getCardId(MMsg.subfieldByte(msg, 3, 1), MMsg.subfieldByte(msg, 3, 2)));
               float overlap = 0;

               if (src_x < posX+cwidth && src_x + cwidth > posX)
               {
                  if (src_x < posX)
                     overlap = ((src_x+cwidth)-posX) / cwidth;
                  else
                     overlap = ((posX+cwidth)-src_x) / cwidth;
                  MMsg.addField(response, Const.Fld.CARD_POSSIBLE_PLAY_OVERLAP).addField(response, id.bytes).addField(response, Const.Fld.CARD_OVERLAP_PERCENT).addToField(response, (int) (overlap * 10000));
               }
               else
                  MMsg.addField(response, Const.Fld.CARD_POSSIBLE_PLAY_OVERLAP).addField(response, id.bytes).addField(response, Const.Fld.CARD_OVERLAP_PERCENT).addToField(response, 0);
            }
         }

      }

      return 0;
   }

   @Override
   public void moveTo (float _x, float _y)
   {
      float offset = 0;

      posX = _x;
      posY = _y;

      for (Playable crd : components)
      {
         crd.moveTo(posX, posY + offset);
         offset += cardOffset;
      }

   }

   @Override
   public float width ()
   {
      return graphics.cardWidth();
   }

   @Override
   public float height ()
   {
      return graphics.cardHeight() + (cardOffset * components.size());
   }

   public static int indexFromId (CardComponentId id)
   {
      return id.bytes[1];
   }

   public static boolean isClassId(CardComponentId id)
   {
      if (id.bytes[0] == Const.MediatorType.TABLEAU)
         return true;
      else
         return false;
   }

   @Override
   public int executeCommand(CardCommand _cmd)
   {
      if (_cmd.type == CardCommand.TYPE_DEAL_CARD || _cmd.type == CardCommand.TYPE_MOVE || _cmd.type == CardCommand.TYPE_MULTIMOVE)
      {
         if (_cmd.dest == this)
         {
            if (_cmd.type == CardCommand.TYPE_MULTIMOVE)
            {
               if (_cmd.immediate)
                  return moveMulticardToPileImmediate (_cmd);
               else
                  return moveMulticardToPile (_cmd);
            }
            else
            {
               if (_cmd.immediate)
                  return moveCardImmediate(_cmd);
               else
                  return moveCardToPile (_cmd);
            }
         }
         else
         {
            if (_cmd.step == 0)
            {
               if (_cmd.type == CardCommand.TYPE_MULTIMOVE)
               {
                  for (int i=0; i < _cmd.multicardCount(); i++)
                  {
                     CardComponent cc = find (new CardComponentId(Const.MediatorType.CARD, (byte)_cmd.multicardSuitIdx(i), (byte)_cmd.multicardRankIdx(i)));

                     if (cc != null)
                     {
                        components.remove(cc);
                        cardsOnPile--;
                     }
                  }
               }
               else
               {
                  CardComponent cc = find (new CardComponentId(Const.MediatorType.CARD, (byte)_cmd.getSuit(), (byte)_cmd.getRank()));

                  if (cc != null)
                  {
                     components.remove(cc);
                    cardsOnPile--;
                  }
               }
            }

            if (_cmd.flip)
               return flipSrcCard(_cmd);
            else
               return -1;
         }
      }
      else
         return -1;
   }

   public int getDealFlightTime (int _card, float _currentX, float _currentY)
   {
      float y = posY + (_card * cardOffset);
      float dist = (float) Math.sqrt(Math.pow(posX - _currentX, 2) + Math.pow(y - _currentY, 2));
      float steps = (dist / graphics.cardWidth()) * 1.5f;

      return (int)(steps < 4 ? 3 : (int)steps-1) * dealFlightDelayMsPerStep;
   }

   private int moveMulticardToPile (CardCommand _cmd)
   {
      long delay = dealFlightDelayMsPerStep;

      if (_cmd.step == 0)
      {
         for (int i = 0; i < _cmd.multicardCount(); i++)
         {
            Card new_card = addCard (_cmd.multicardSuitIdx(i), _cmd.multicardRankIdx(i), cardsOnPile);
            CardAnimStrategy animator = new CardAnimStrategyTabToTab(new_card, posX, posY + (new_card.positionInPile * cardOffset));
            new_card.setAnimator(animator);

            if (movingCard == null)
            {
               movingCard = new_card;
               if (_cmd.undo && cardsOnPile > 0 && ((Card)components.get(cardsOnPile-1)).flipAngle() == Const.Angle.FACE_UP)
               {
                  if (cardsOnPile > 1)
                  {
                     if (((Card)components.get(cardsOnPile-2)).flipAngle() == Const.Angle.FACE_DOWN)
                        ((Card)components.get(cardsOnPile-1)).flip(Const.Angle.FACE_DOWN, GraphicsInterface.AXIS_JUSTIFY_NONE);
                  }
                  else
                     ((Card)components.get(cardsOnPile-1)).flip(Const.Angle.FACE_DOWN, GraphicsInterface.AXIS_JUSTIFY_NONE);
               }
            }
            cardsOnPile++;
         }
      }
      else if (movingCard != null)
      {
         float minX, minY, maxX, maxY;
         minX = minY = Float.MAX_VALUE;
         maxX = maxY = Float.MIN_VALUE;

         for (int i = components.size()-1; i >= 0; i--)
         {
            Card c = (Card)components.get(i);
            delay = c.animator().step();
            if (c.posX() < minX) minX = c.posX();
            if (c.posY() < minY) minY = c.posY();
            if (c.posX() > maxX) maxX = c.posX();
            if (c.posY() > maxY) maxY = c.posY();
            if (delay == 0)
               ((Card)components.get(i)).setAnimator(null);
            if (components.get(i) == movingCard)
            {
               if (delay == 0)
                  movingCard = null;
               break;
            }
         }
         _cmd.posX = minX;
         _cmd.posY = minY;
         _cmd.sizeX = (maxX+graphics.cardWidth()) - minX;
         _cmd.sizeY = (maxY+graphics.cardHeight()) - minY;
      }

      if (delay != 0)
         return (int)delay;
      else
         return -1;
   }

   private int moveMulticardToPileImmediate (CardCommand _cmd)
   {
      long delay = dealFlightDelayMsPerStep;
      float minX, minY, maxX, maxY;

      if (_cmd.step == 0)
      {
         minX = minY = Float.MAX_VALUE;
         maxX = maxY = Float.MIN_VALUE;
         for (int i = 0; i < _cmd.multicardCount(); i++)
         {
            Card new_card = addCard (_cmd.multicardSuitIdx(i), _cmd.multicardRankIdx(i), cardsOnPile);

            if (_cmd.immediate)
            {
               new_card.moveTo(posX, posY + (cardOffset * new_card.positionInPile));
               new_card.setZorder(zorderBase+new_card.positionInPile);
               delay = 0;
               if (new_card.posX() < minX) minX = new_card.posX();
               if (new_card.posY() < minY) minY = new_card.posY();
               if (new_card.posX() > maxX) maxX = new_card.posX();
               if (new_card.posY() > maxY) maxY = new_card.posY();
            }
            cardsOnPile++;
         }
         _cmd.posX = minX; _cmd.posY = minY;
         _cmd.sizeX = (maxX+graphics.cardWidth()) - minX; _cmd.sizeY = (maxY+graphics.cardHeight()) - minY;
      }
      return -1;
   }

   private int moveCardToPile (CardCommand _cmd)
   {
      long delay = dealFlightDelayMsPerStep;

      if (_cmd.step == 0)
      {
         _cmd.order = cardsOnPile;
         Card new_card = addCard (_cmd.getSuit(), _cmd.getRank(), _cmd.order);
         cardsOnPile++;

         if (movingCard == null)
         {
            movingCard = new_card;
            movingCard.setAnimator(new CardAnimStrategyTabToTab(movingCard, posX, posY + (movingCard.positionInPile * cardOffset)));
            if (_cmd.undo && _cmd.flip)
               ((Card)components.get(movingCard.positionInPile-1)).flip(Const.Angle.FACE_DOWN, GraphicsInterface.AXIS_JUSTIFY_NONE);
         }
      }
      else if (movingCard != null && movingCard.positionInPile == _cmd.order)
      {
         delay = movingCard.animator().step();
         _cmd.posX = movingCard.posX(); _cmd.posY = movingCard.posY();
         _cmd.sizeX = movingCard.width(); _cmd.sizeY = movingCard.height();
         if (delay == 0)
         {
            if (movingCard.positionInPile == maxCards-1 && _cmd.type == CardCommand.TYPE_DEAL_CARD)
            {
               movingCard.flip(Const.Angle.FACE_UP, GraphicsInterface.AXIS_JUSTIFY_NONE);
               Collections.sort (components, new Card.PositionInPileComparator());
            }

            if (movingCard.positionInPile < cardsOnPile-1)
            {
               movingCard = (Card)components.get(movingCard.positionInPile+1);
               movingCard.setAnimator(new CardAnimStrategyTabToTab(movingCard, posX, posY + (movingCard.positionInPile * cardOffset)));
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

      new_card.moveTo(posX, posY + (cardOffset * new_card.positionInPile));
      new_card.setZorder(zorderBase + new_card.positionInPile);
      new_card.spin(0);
      _cmd.posX = new_card.posX(); _cmd.posY = new_card.posY();
      _cmd.sizeX = new_card.width(); _cmd.sizeY = new_card.height();
      if (cardsOnPile == maxCards-1 && _cmd.type == CardCommand.TYPE_DEAL_CARD)
      {
         new_card.flip(Const.Angle.FACE_UP, GraphicsInterface.AXIS_JUSTIFY_NONE);
         Collections.sort (components, new Card.PositionInPileComparator());
      }
      cardsOnPile++;
      return -1;
   }

   private int flipSrcCard (CardCommand _cmd)
   {
       long delay = dealFlightDelayMsPerStep;

       if (_cmd.step == 0 && components.size() > 0)
       {
            if (flipCard != null)
            {
               flipCard.animator().finishNow();
               flipCard.setAnimator(null);
               flipCard = null;
            }

          flipCard = (Card)components.get(components.size()-1);
            if (_cmd.undo)
            {
               flipCard = null;
               delay = 0;
            }
            else
            {
               flipCard.setAnimator(new CardAnimStrategyFlipTab(flipCard));
               if (_cmd.immediate)
               {
                  flipCard.animator().finishNow();
                  flipCard.setAnimator(null);
                  flipCard = null;
                  delay = 0;
               }
            }
       }
       else if (flipCard != null)
       {
           if (_cmd.sizeX != _cmd.sizeY && !flipCard.intersecting(_cmd.posX, _cmd.posY, _cmd.sizeX, _cmd.sizeY))
           {
               delay = flipCard.animator().step();
               if (delay == 0)
               {
                  flipCard.setAnimator(null);
                  flipCard = null;
               }
           }
       }
       else
           delay = 0;

       if (delay != 0)
           return (int)delay;
       else
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
         Playable cc = findTouched(tf.x, tf.y);
         if (cc != null)
            list.AddPlay(cc, Const.PlayFilterType.PFT_TOUCH_PLAYABLE, id, id);
      }
      else if (filter.type == Const.PlayFilterType.PFT_TOUCH)
      {
         tf = (PlaylistFilterTouch)filter;
         if (components.size() > 0)
         {
            if (tf.touchType == Const.InputType.TOUCH)
               processTouch (list);
            else if (tf.touchType == Const.InputType.DRAG_START && dragCard == null)
               processDragStart (tf.touchParam, tf.x, tf.y);
            else if (tf.touchType == Const.InputType.DRAG && dragCard != null)
               processDrag (tf.touchParam, tf.x, tf.y);
            else if (tf.touchType == Const.InputType.DRAG_END && dragCard != null)
               processDragEnd (tf.touchParam, tf.x, tf.y, list);
         }
      }

      return 1;
   }

   @Override
   public Playable findTouched (float _posX, float _posY)
   {
      boolean touched = false;

      if (components.size() > 0)
      {
         float left = posX;
         float right = posX + graphics.cardWidth();
         float bottom = ((Card)components.get(components.size()-1)).posY();
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

   private void processTouch (Playlist playlist)
   {
      MMsg possible_msg = acquireMsg();

      for (int pile_idx = components.size()-1; pile_idx >= 0; pile_idx--)
      {
         MMsg rsp;
         if (((Card)components.get(pile_idx)).flipAngle() == Const.Angle.FACE_DOWN)
            break;

         CardComponentId card_id = components.get(pile_idx).id;
         int card_rank_idx = ((Card)components.get(pile_idx)).rank;

         MMsg.clear(possible_msg);
         MMsg.addField(possible_msg, Const.MsgType.GET_PARAM);
         if (pile_idx == components.size()-1)
            MMsg.addField(possible_msg, Const.Fld.CARD_POSSIBLE_PLAYS);
         else
            MMsg.addField(possible_msg, Const.Fld.CARD_POSSIBLE_MULTIPLAY);
         MMsg.addField(possible_msg, Const.Fld.SRC, id.bytes);
         MMsg.addField(possible_msg, card_id.bytes);

         rsp = sendMsg(possible_msg);
         if (rsp.fieldCount > 0)
         {
            for (int rsp_idx=1; rsp_idx < rsp.fieldCount; rsp_idx+=2)
            {
               if (rsp_idx == 1)
               {
                  byte command;
                  if (pile_idx+1 == components.size())
                     command = Const.Cmd.MOVE;
                  else
                     command = Const.Cmd.MULTIMOVE;
                  playlist.AddPlay(this, command, id,  new CardComponentId(rsp.bytes, rsp.fieldStartIdx[rsp_idx]));
                  for (int card_idx = pile_idx; card_idx < components.size(); card_idx++)
                  {
                     Card card = (Card)components.get(card_idx);
                     playlist.AddCard(card.suit, card.rank);
                  }
                  if (pile_idx > 0 && ((Card)components.get(pile_idx-1)).flipAngle() == Const.Angle.FACE_DOWN)
                     playlist.addSubcommand(Const.Fld.CARD_FLIP);
               }
               else
                  playlist.addDest(new CardComponentId(rsp.bytes, rsp.fieldStartIdx[rsp_idx]));
            }
         }
         releaseMsg(rsp);
      }
      releaseMsg(possible_msg);
   }


   float dragStartY = -100f;
   float dragStartX = -100f;
   void processDragStart (int _param, float _posX, float _posY)
   {
      int size = components.size();
      dragStartX = dragStartY = -100;
      for (int i = size-1; i >= 0; i--)
      {
         Card c = (Card)components.get(i);
         if (c.flipAngle() == Const.Angle.FACE_UP && c.pointInCard(_posX, _posY))
         {
            dragCard = c;
            dragCard.scale(2.0f);
            dragCard.setZorder(zorderBase*2);
            graphics.forceRedraw();
            break;
         }
      }
   }

   void sleepEx (long _timeMilli)
   {
      try
      {
         Thread.sleep(_timeMilli);
      }
      catch (InterruptedException e)
      {
         e.printStackTrace();
      }
   }

   void processDrag (int _param, float _posX, float _posY)
   {
      Card card_overlapping_dragcard;

      if (dragCard.positionInPile+1 < components.size())
         card_overlapping_dragcard = (Card)components.get(dragCard.positionInPile+1);
      else
         card_overlapping_dragcard = null;

      if ((!dragCard.pointInCardExOverlap(_posX, _posY, card_overlapping_dragcard) || dragStartX > -99f))
      {
         if (_posX < dragCard.posX() || _posX > dragCard.posX()+dragCard.width() || dragStartX > -99f)
         {
            //drag is to the right or left of the drag card
            int size = components.size();
            float dx, dy=0;
            dragCard.scale(1.0f);
            if (dragStartX < -99f)
            {
               dragStartX = dragCard.posX();
               dragStartY = dragCard.posY();
            }
            dx = _posX - (dragCard.posX()+(dragCard.width()/2f));
            dy = _posY - (dragCard.posY()+(dragCard.height()/2f));
            dragCard.move(dx, dy);
            for (int i = dragCard.positionInPile+1; i < size; i++)
            {
               Card c = (Card)components.get(i);
               c.move (dx, dy);
               c.setZorder(dragCard.getZorder()+i);
            }
            while (!graphics.drawComplete())
               sleepEx (5);
            graphics.forceRedraw();
         }
         else
         {
            //drag is up or down so still selecting drag card
            int size = components.size();
            Card overlapper = null;
            for (int i = size-1; i >= 0; i--)
            {
               Card c = (Card)components.get(i);
               if (c.flipAngle() == Const.Angle.FACE_UP && c.pointInCardExOverlap(_posX, _posY, overlapper))
               {
                  dragCard.scale(1.0f);
                  dragCard.setZorder(zorderBase+dragCard.positionInPile);
                  dragCard = c;
                  dragCard.scale(2.0f);
                  dragCard.setZorder(zorderBase*2);
                  break;
               }
               overlapper = c;
            }
            graphics.forceRedraw();
         }
      }
   }

   void processDragEnd (int _param, float _posX, float _posY, Playlist playlist)
   {
      boolean cancel = true;

      if (dragStartX > -100)
      {
         //see if cards have been dragged to a playable pile
         MMsg poss_msg = acquireMsg();
         MMsg.addField(poss_msg, Const.MsgType.GET_PARAM);
         MMsg.addField(poss_msg, Const.Fld.CARD_POSSIBLE_PLAY_OVERLAP);
         MMsg.addField(poss_msg, Const.Fld.SRC, id.bytes);
         MMsg.addField(poss_msg, dragCard.id.bytes);
         MMsg resp = sendMsg(poss_msg);
         releaseMsg(poss_msg);

         if (resp.fieldCount > 0)
         {
            //playable pile found is a pile that dragged cards overlap?
            int max_overlap = 0;
            CardComponentId max_overlap_id = new CardComponentId();
            //may be over to piles that can play the components. choose the most overlapped pile
            for (int fld_idx=2; fld_idx < resp.fieldCount; fld_idx+=3)
            {
               int overlap = MMsg.subfieldInt(resp, fld_idx, 1);
               if (overlap > max_overlap)
               {
                  max_overlap = overlap;
                  max_overlap_id.set(resp.bytes, resp.fieldStartIdx[fld_idx-1]);
               }
            }
            if (max_overlap > 0)
            {
               playlist.AddPlay(this, Const.Cmd.MULTIMOVE, id, max_overlap_id);
               for (int card_idx=dragCard.positionInPile; card_idx < components.size(); card_idx++)
               {
                  Card card = (Card)components.get(card_idx);
                  playlist.AddCard(card.suit, card.rank);
               }
               if (dragCard.positionInPile > 0 && ((Card)components.get(dragCard.positionInPile-1)).flipAngle() == Const.Angle.FACE_DOWN)
                  playlist.addSubcommand(Const.Fld.CARD_FLIP);

               cancel = false;
            }
         }
         releaseMsg(resp);
      }

      if (cancel)
      {
         //drag is not to a playable pile. put the cards back to pre drag state
         int size = components.size();
         float dx = dragStartX - dragCard.posX();
         float dy = dragStartY - dragCard.posY();
         if (dragStartX < -99)
            dx = dy = 0;
         for (int i = dragCard.positionInPile; i < size; i++)
         {
            Card c = (Card)components.get(i);
            c.move (dx, dy);
            c.setZorder(zorderBase+c.positionInPile);
         }
         dragCard.scale(1.0f);
         graphics.forceRedraw();
      }

      dragCard = null;
      dragStartX = dragStartY = -100;
   }
}


