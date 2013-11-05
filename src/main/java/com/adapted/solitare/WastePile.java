package com.adapted.solitare;

/**
 * Created by mark on 6/18/13.
 */
class WastePile extends PlayableComposite
{
   
   private float posX, posY;
   private int baseZorder = 0;
   private int moveZorder = 1000;

   WastePile(Mediator _mediator, GraphicsInterface _graphics)
   {
      super(_mediator, _graphics);

      id = new CardComponentId(Const.MediatorType.WASTE, (byte) 0, (byte) 0);
   }

   @Override
   public void moveTo(float _x, float _y)
   {
      posX = _x;
      posY = _y;
      
      super.moveTo(_x,_y);
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

   @Override
   public Playable findTouched(float _posX, float _posY)
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

   @Override
   public void processUserInput(int _type, int _param, float _posX, float _posY)
   {
      if (_type == Const.InputType.TOUCH && components.size() > 0)
      {
         MMsg response;
         MMsg msg = acquireMsg();
         CardComponentId card_id = components.get(components.size() - 1).id;

         MMsg.addField(msg, Const.MsgType.GET_PARAM);
         MMsg.addField(msg, Const.Fld.CARD_POSSIBLE_PLAYS);
         MMsg.addField(msg, Const.Fld.SRC, id.bytes);
         MMsg.addField(msg, card_id.bytes);
         response = sendMsg(msg);

         if (response.fieldCount > 0)
         {
            //always play the card when there is somewhere to play to
            //usually this deafult is used when the card cant be played to a
            //tableau and can only play to a foundation
            int move_to_fld_tab = 0;
            int move_to_fld_foun = 0;
            int move_to_fld = 0;
            int highest_tab_index = -1;
            int lowest_foundation = 1000;

            for (int idx = 1; idx < response.fieldCount; idx += 2)
            {
               if (response.subfieldByte(response, idx, 0) == Const.MediatorType.TABLEAU)
               {
                  int tindex = response.subfieldByte(response, idx, 1);
                  if (tindex > highest_tab_index)
                  {
                     highest_tab_index = tindex;
                     move_to_fld_tab = idx;
                  }
               }
               else if (response.subfieldByte(response, idx, 0) == Const.MediatorType.FOUNDATION)
               {
                  int findex = response.subfieldByte(response, idx, 1);
                  if (findex < lowest_foundation)
                  {
                     lowest_foundation = findex;
                     move_to_fld_foun = idx;
                  }
               }
            }

            if (move_to_fld_tab > 0)
               move_to_fld = move_to_fld_tab;
            else if (move_to_fld_foun > 0)
               move_to_fld = move_to_fld_foun;

            MMsg.clear(msg);
            CardComponentId move_to_id = new CardComponentId(response.bytes, response.fieldStartIdx[move_to_fld]);
            MMsg.addField(msg, Const.MsgType.MOVE)
                    .addField(msg, Const.Fld.SRC).addToField(msg, id.bytes)
                    .addField(msg, Const.Fld.DEST).addToField(msg, move_to_id.bytes)
                    .addField(msg, card_id.bytes);
            releaseMsg(response);
            response = sendMsg(msg);
         }
         releaseMsg(msg);
         releaseMsg(response);
      }
   }

   float step_x;
   float step_y;
   float step_rotation;
   float step_scale;
   int num_steps = 10;
   int step_delay = 32;
   Card moveCard = null;
   int step = 0;

   @Override
   public int executeCommand(CardCommand _cmd)
   {
      int ret_val = -1;

      if (_cmd.type == CardCommand.TYPE_MOVE)
      {
         if (_cmd.dest == this)
         {
            if (_cmd.step == 0)
            {
               Card new_card = new Card(this, _cmd.getSuit(), _cmd.getRank());
               new_card.positionInPile = components.size();
               _cmd.order = new_card.positionInPile;
               components.add(new_card);

               if (_cmd.immediate || _cmd.undo || moveCard != null)
               {
                  if (components.size() > 1)
                     components.get(components.size() - 2).show(false);
                  new_card.moveTo(posX, posY);
                  new_card.flip(Const.Angle.FACE_UP, GraphicsInterface.AXIS_JUSTIFY_NONE);
                  new_card.scale(1f);
                  new_card.setZorder(baseZorder + components.size());
                  if (_cmd.immediate || _cmd.undo)
                     ret_val = -1;
                  else
                  {
                     moveCard.moveTo(posX, posY);
                     moveCard.flip(Const.Angle.FACE_UP, GraphicsInterface.AXIS_JUSTIFY_NONE);
                     moveCard.scale(1f);
                     moveCard.setZorder(baseZorder + components.size() - 1);
                     moveCard = null;
                     ret_val = step_delay;
                  }
               }
               else
                  ret_val = step_delay;

               if (moveCard == null && ret_val != -1)
               {
                  new_card.setZorder(moveZorder);

                  moveCard = new_card;
                  step_scale = .3f / (float) (num_steps / 2);
                  step_x = (posX - moveCard.posX()) / (float) (num_steps - (num_steps / 2));
                  step_y = (posY - moveCard.posY()) / (float) (num_steps - (num_steps / 2));
                  step_rotation = 180f / (float) num_steps;
                  step = 0;
                  ret_val = step_delay;
               }
            }
            else if (moveCard != null && moveCard.positionInPile == _cmd.order)
            {
               step++;
               if (step < num_steps / 2)
                  moveCard.scale(1f + (step_scale * (float) step));
               else
                  moveCard.scale((1f + (step_scale * (float) num_steps)) - (step_scale * (float) step));
               moveCard.flip(step * step_rotation, GraphicsInterface.AXIS_JUSTIFY_LEFT);
               if (step > (num_steps / 2) - 1)
                  moveCard.moveTo(posX, posY);

               if (step < num_steps)
                  ret_val = step_delay;
               else
               {
                  if (components.size() > 1)
                     components.get(components.size() - 2).show(false);
                  moveCard.moveTo(posX, posY);
                  moveCard.flip(Const.Angle.FACE_UP, GraphicsInterface.AXIS_JUSTIFY_NONE);
                  moveCard.scale(1f);
                  moveCard.setZorder(baseZorder + components.size());
                  if (moveCard.positionInPile < components.size() - 1)
                  {
                     moveCard = (Card)components.get(moveCard.positionInPile + 1);
                     moveCard.setZorder(moveZorder);
                     step_scale = .3f / (float) (num_steps / 2);
                     step_x = (posX - moveCard.posX()) / (float) (num_steps - (num_steps / 2));
                     step_y = (posY - moveCard.posY()) / (float) (num_steps - (num_steps / 2));
                     step_rotation = 180f / (float) num_steps;
                     step = 0;
                  }
                  else
                  {
                     moveCard = null;
                  }
                  ret_val = -1;
               }
            }
         }
         else if (_cmd.src == this)
         {
            for (int idx = 0; idx < components.size(); idx++)
            {
               if (((Card)components.get(idx)).same((byte) _cmd.getSuit(), (byte) _cmd.getRank()))
               {
                  components.remove(idx);
                  break;
               }
            }
            if (components.size() > 0)
            {
               components.get(components.size() - 1).show(true);
               ((Card)components.get(components.size() - 1)).flip(Const.Angle.FACE_UP, GraphicsInterface.AXIS_JUSTIFY_NONE);
            }
         }
      }
      else if (_cmd.type == CardCommand.TYPE_RECYCLE_WASTE && _cmd.step == 0)
      {
         if (_cmd.src == this)
            components.clear();
         else
         {
            for (int i = 0; i < _cmd.multicardCount(); i++)
            {
               Card c = new Card(this, _cmd.multicardSuitIdx(i), _cmd.multicardRankIdx(i));
               components.add(c);
               c.show(false);
               c.moveTo(posX, posY);
               c.setZorder(1);
            }
            ((Card)components.get(components.size() - 1)).flip(Const.Angle.FACE_UP, GraphicsInterface.AXIS_JUSTIFY_NONE);
            components.get(components.size() - 1).show(true);
            ret_val = -1;
         }
      }

      return ret_val;
   }

   ////message RECYCLE_CARDS
   //message
   //field 1 - <byte>GET_PARAM
   //field 2 - <byte>RECYCLE_CARDS
   //field 3 - <byte[3]>id of wastepile
   //response
   //field 1 - <byte>RECYCLE_CARDS
   //field 2..n - <byte[3]>id of cards to recycle
   @Override
   public int receiveMsg(MMsg msg, MMsg response)
   {
      if (MMsg.subfieldByte(msg, 0, 0) == Const.MsgType.GET_PARAM && MMsg.subfieldByte(msg, 1, 0) == Const.Fld.RECYCLE_CARDS)
      {
         if (id.equals(msg.bytes, msg.fieldStartIdx[2]) && components.size() > 0)
         {
            response.addField(response, Const.Fld.RECYCLE_CARDS);
            for (Playable c : components)
               response.addField(response, c.id.bytes);
         }
      }
      return 0;
   }
}
