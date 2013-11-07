package com.adapted.solitare;

/**
 * Created by mcass on 10/23/13.
 */
public class PlayerReal extends Player implements UserInputListener
{
   PlayerClient dragClient = null;
   Playlist playlist;

   public PlayerReal(PlayerClient _client, CardCommandInvoker _invoker)
   {
      super (_client, _invoker);
      playlist = new Playlist();
   }

   @Override
   public boolean touchEvent(int _type, int _param, float _posX, float _posY)
   {
      boolean ret_val = false;
      PlayerClient found = null;
      PlaylistFilterTouch filter = new PlaylistFilterTouch ();

      filter.touchType = _type;
      filter.touchParam = _param;
      filter.x = _posX;
      filter.y = _posY;

      if (dragClient == null)
      {
         filter.type = Const.PlayFilterType.PFT_TOUCH_PLAYABLE;
         playlist.Clear();
         client.GetPlaylist(playlist, filter);
         found = playlist.plays[0].origin;
      }
      else
         found = dragClient;

      if (found != null)
      {
         filter.type = Const.PlayFilterType.PFT_TOUCH;
         playlist.Clear();
         found.GetPlaylist(playlist, filter);

         if (_type == Const.InputType.DRAG_START)
            dragClient = found;
         else if (dragClient != null && _type == Const.InputType.DRAG_END)
            dragClient = null;
         ret_val = true;

         if (playlist.numPlays > 0)
         {
            processPlaylist ();
         }
      }

      return ret_val;
   }

   private void processPlaylist ()
   {
      if (playlist.numPlays > 0)
      {
         CardComponentId play_src_id = playlist.plays[0].srcId;
         if (Tableau.isClassId(play_src_id))
            playFromTableau();
         else if (WastePile.isClassId(play_src_id))
            playFromWaste ();
         else if (StockPile.isClassId(play_src_id))
            playFromStock ();
      }
   }

   private void playFromTableau ()
   {
      int highest_tab_fld = -1;
      int highest_tab_index = -1;
      int this_tab_index = Tableau.indexFromId(playlist.plays[0].srcId);
      int next_least_tab_idx = -1;
      int next_least_tab_fld = -1;
      int least_foundation_idx = -1;
      int least_tab_idx = -1;
      int least_tab_fld = -1;
      int foundation_fld = -1;

      for (int play_idx = 0; play_idx < playlist.numPlays; play_idx++)
      {
         Play play = playlist.plays[play_idx];
         for (int idx=0; idx < play.numDest; idx++)
         {
            if (Tableau.isClassId(play.destId[idx]))
            {
               int tindex = Tableau.indexFromId(play.destId[idx]);
               if (tindex < this_tab_index && tindex > next_least_tab_idx)
               {
                  next_least_tab_idx = tindex;
                  next_least_tab_fld = idx;
               }

               if (tindex > highest_tab_index)
               {
                  highest_tab_index = tindex;
                  highest_tab_fld = idx;
               }

               if (least_tab_idx == -1 || tindex < least_tab_idx)
               {
                  least_tab_idx = tindex;
                  least_tab_fld = idx;
               }
            }
            else if (Foundation.isFoundationId(play.destId[idx]))
            {
               int findex = Foundation.indexFromId(play.destId[idx]);
               if (least_foundation_idx == -1 || findex < least_foundation_idx)
               {
                  least_foundation_idx = findex;
                  foundation_fld = idx;
               }
            }
         }

         int move_to_fld = -1;
         if (play.cards[0].rank == Const.Rank.KING && foundation_fld == -1)
         {
            if (least_tab_idx < this_tab_index)
               move_to_fld = least_tab_fld;
         }
         else if (next_least_tab_fld != -1)
            move_to_fld = next_least_tab_fld;
         else if (foundation_fld != -1)
            move_to_fld = foundation_fld;
         else if (highest_tab_fld != -1)
            move_to_fld = highest_tab_fld;

         if (move_to_fld != -1)
         {
            playlist.selectPlay(play_idx);
            play.selectDest(move_to_fld);
            CardCommand cc = playlist.createCommand();
            cc.setSrc(client.GetCommandReceiver(play.srcId));
            cc.setDest(client.GetCommandReceiver(play.destId[move_to_fld]));
            invoker.queueCommand(cc);
            break;
         }
      }
   }
}
