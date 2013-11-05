package com.adapted.solitare;

import java.util.ArrayList;

/**
 * Created by mark on 6/8/13.
 */
public abstract class Playable extends Colleague implements CardComponent, CardCommandReceiver, CardColleagueVistable, PlayerClient
{
   public GraphicsInterface graphics;
   public CardComponentId id;

   Playable(Mediator _mediator, GraphicsInterface _graphics)
   {
      super (_mediator);
      graphics = _graphics;
   }

   Playable(Playable _colleague)
   {
      super (_colleague);
      graphics = _colleague.graphics;
   }

   @Override
   public int receiveMsg (MMsg msg, MMsg response)
   {
      return 0;
   }

   @Override
   public int executeCommand (CardCommand _cmd)
   {
      return 0;
   }

   @Override
   public void processUserInput (int _type, int _param, float _posX, float _posY)
   {
   }

   @Override
   public void AcceptVisitor (CardColleagueVisitor _visitor)
   {
   }

   @Override
   public void moveTo(float _x, float _y)
   {
   }

   @Override
   public void move(float _dx, float _dy)
   {
   }

   @Override public float width ()
   {
      return 0;
   }

   @Override
   public float height ()
   {
      return 0;
   }

   @Override
   public void setZorder(int _zorder)
   {
   }

   @Override
   public void show(boolean _show)
   {
   }

   @Override
   public void setOrientationLandscape()
   {
   }

   @Override
   public void setOrientationPortrait()
   {
   }

   @Override
   public Playable find (CardComponentId _id)
   {
      Playable found = null;

      if (id.equals(_id))
         found = this;

      return found;
   }

   @Override
   public Playable findTouched (float _posX, float _posY)
   {
      return null;
   }

   @Override
   public int GetPlaylist (Playlist list, PlaylistFilter filter)
   {
      return 0;
   }

   @Override
   public CardCommandReceiver GetCommandReceiver (CardComponentId id)
   {
      return find (id);
   }
}


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
abstract class PlayableComposite extends Playable
{
   ArrayList<Playable> components;

   PlayableComposite(Mediator _mediator, GraphicsInterface _graphics)
   {
      super (_mediator, _graphics);
      components = new ArrayList<Playable>();
   }

   PlayableComposite(Playable _colleague)
   {
      super (_colleague);
      components = new ArrayList<Playable>();
   }

   @Override
   public int receiveMsg (MMsg msg, MMsg response)
   {
      int len = components.size();
      for (int i = 0; i < len; i++)
         components.get(i).receiveMsg(msg, response);
      return 0;
   }

   @Override
   public void AcceptVisitor (CardColleagueVisitor _visitor)
   {
      int len = components.size();
      for (int i = 0; i < len; i++)
         components.get(i).AcceptVisitor(_visitor);
   }

   @Override
   public Playable find (CardComponentId _id)
   {
      Playable found = null;

      if (id.equals(_id))
      {
         found = this;
      }
      else
      {
         int len = components.size();
         for (int i = 0; i < len; i++)
         {
            found = components.get(i).find(_id);
            if (found != null)
               break;
         }
      }

      return found;
   }

   @Override
   public Playable findTouched (float _posX, float _posY)
   {
      Playable found = null;
      int len = components.size();
      for (int i = 0; i < len; i++)
      {
         found = components.get(i).findTouched (_posX, _posY);
         if (found != null)
            break;
      }
      return found;
   }

   @Override
   public void setOrientationLandscape()
   {
      for (Playable comp : components)
         comp.setOrientationLandscape();
   }

   @Override
   public void setOrientationPortrait()
   {
      for (Playable comp : components)
         comp.setOrientationPortrait();
   }

   @Override
   public void moveTo (float _x, float _y)
   {
      for (Playable cc : components)
         cc.moveTo(_x, _y);
   }

   @Override
   public void move(float _dx, float _dy)
   {
      for (Playable cc : components)
         cc.move(_dx, _dy);
   }

   @Override
   public void show (boolean _show)
   {
      for (Playable cc : components)
        cc.show(_show);
   }

   @Override
   public int GetPlaylist (Playlist list, PlaylistFilter filter)
   {
      for (Playable cc : components)
         cc.GetPlaylist(list, filter);
      return 1;
   }
}
