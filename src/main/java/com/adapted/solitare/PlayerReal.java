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
      }

      return ret_val;
   }
}
