package com.adapted.solitare;

/**
 * Created by mcass on 10/21/13.
 */

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class PlaylistFilter
{
   public int type;
}

class PlaylistFilterTouch extends PlaylistFilter
{
   public float x, y;
   public int touchType, touchParam;

}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================

class Playlist
{
   PlayerClient client = null;
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================

interface PlayerClient
{
   public int GetPlaylist (Playlist list, PlaylistFilter filter);
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================

abstract class Player
{
   protected PlayerClient client;
   protected CardCommandInvoker invoker;

   public Player ()
   {
      client = null;
      invoker = null;
   }

   public Player (PlayerClient _client, CardCommandInvoker _invoker)
   {
      client = _client;
      invoker = _invoker;
   }

}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class AutoPlayer extends Player
{
   public AutoPlayer(PlayerClient _client, CardCommandInvoker _invoker)
   {
      super (_client, _invoker);
   }
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class RealPlayer extends Player implements UserInputListener
{
   PlayerClient dragClient = null;

   public RealPlayer(PlayerClient _client, CardCommandInvoker _invoker)
   {
      super (_client, _invoker);
   }

   @Override
   public boolean touchEvent(int _type, int _param, float _posX, float _posY)
   {
      PlayerClient found = null;
      PlaylistFilterTouch filter = new PlaylistFilterTouch ();
      Playlist list = new Playlist();

      filter.touchType = _type;
      filter.touchParam = _param;
      filter.x = _posX;
      filter.y = _posY;

      list.client = null;

      if (dragClient == null)
      {
         filter.type = Const.PlayFilterType.PFT_TOUCH_PLAYABLE;
         client.GetPlaylist(list, filter);
         found = list.client;
      }
      else
         found = dragClient;

      if (found != null)
      {
         filter.type = Const.PlayFilterType.PFT_TOUCH;
         found.GetPlaylist(list, filter);

         if (_type == Const.InputType.DRAG_START)
            dragClient = found;
         else if (dragClient != null && _type == Const.InputType.DRAG_END)
            dragClient = null;

         return true;
      }
      else
         return false;
   }
}

