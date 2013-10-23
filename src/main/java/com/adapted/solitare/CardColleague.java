package com.adapted.solitare;

import java.util.ArrayList;

/**
 * Created by mark on 6/8/13.
 */
public abstract class CardColleague extends Colleague implements CardComponent, CardCommandReceiver, CardColleagueVistable, PlayerClient
{
   public GraphicsInterface graphics;
   public CardComponentId id;

   CardColleague (Mediator _mediator, GraphicsInterface _graphics)
   {
      super (_mediator);
      graphics = _graphics;
   }

   CardColleague (CardColleague _colleague)
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
   public void setZorderOffset(int _offset)
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
   public CardColleague find (CardComponentId _id)
   {
      CardColleague found = null;

      if (id.equals(_id))
         found = this;

      return found;
   }

   @Override
   public CardColleague findTouched (float _posX, float _posY)
   {
      return null;
   }

   @Override
   public int GetPlaylist (Playlist list, PlaylistFilter filter)
   {
      return 0;
   }
}


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
abstract class CardColleagueComposite extends CardColleague
{
   ArrayList<CardColleague> components;

   CardColleagueComposite (Mediator _mediator, GraphicsInterface _graphics)
   {
      super (_mediator, _graphics);
      components = new ArrayList<CardColleague>();
   }

   CardColleagueComposite (CardColleague _colleague)
   {
      super (_colleague);
      components = new ArrayList<CardColleague>();
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
   public CardColleague find (CardComponentId _id)
   {
      CardColleague found = null;

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
   public CardColleague findTouched (float _posX, float _posY)
   {
      CardColleague found = null;
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
      for (CardColleague comp : components)
         comp.setOrientationLandscape();
   }

   @Override
   public void setOrientationPortrait()
   {
      for (CardColleague comp : components)
         comp.setOrientationPortrait();
   }

   @Override
   public void moveTo (float _x, float _y)
   {
      for (CardColleague cc : components)
         cc.moveTo(_x, _y);
   }

   @Override
   public void move(float _dx, float _dy)
   {
      for (CardColleague cc : components)
         cc.move(_dx, _dy);
   }

   @Override
   public void show (boolean _show)
   {
      for (CardColleague cc : components)
        cc.show(_show);
   }

   @Override
   public int GetPlaylist (Playlist list, PlaylistFilter filter)
   {
      for (CardColleague cc : components)
         cc.GetPlaylist(list, filter);
      return 1;
   }
}
