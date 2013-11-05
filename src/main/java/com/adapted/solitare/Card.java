package com.adapted.solitare;

import android.graphics.RectF;

import java.util.Comparator;

/**
 * Created by mark on 6/6/13.
 */
public class Card extends Playable
{
   int gid = 0;
   public int positionInPile;
   int dx, dy;
   private CardAnimStrategy animator;
   int suit, rank;

   Card (Mediator _mediator, GraphicsInterface _graphics, int _suit, int _rank)
   {
      super (_mediator, _graphics);

      init ((byte)_suit, (byte)_rank);
   }

   Card (Mediator _mediator, GraphicsInterface _graphics, byte _suit, byte _rank)
   {
      super (_mediator, _graphics);

      init (_suit,_rank);
   }

   Card (Playable _colleague, byte _suit, byte _rank)
   {
      super (_colleague);

      init (_suit,_rank);
   }

   Card (Playable _colleague, int _suit, int _rank)
   {
      super (_colleague);

      init ((byte)_suit, (byte)_rank);
   }

   public static CardComponentId makeId (int _suit, int _rank)
   {
      return new CardComponentId(Const.MediatorType.CARD, (byte)_suit, (byte)_rank);
   }

   private void init (byte _suit, byte _rank)
   {
      id = new CardComponentId(Const.MediatorType.CARD, _suit, _rank);
      gid = graphics.getCardId(_suit, _rank);
      positionInPile = -1;
      animator = null;
      suit = _suit;
      rank = _rank;
   }

   @Override
   public void moveTo(float _x, float _y)
   {
      graphics.cardMoveTo(gid, _x, _y);
   }

   @Override
   public void move(float _dx, float _dy)
   {
      graphics.cardMove(gid, _dx, _dy);
   }

   public void flip (float _angle, int _axisJustification)
   {
      graphics.cardRotate(gid, Const.Angle.AXIS_Y, _axisJustification, _angle);
   }

   public void spin (float _angle)
   {
      graphics.cardRotate(gid, Const.Angle.AXIS_Z, GraphicsInterface.AXIS_JUSTIFY_CENTER, _angle);
   }

   public void scale(float _scale)
   {
      graphics.cardScale(gid, _scale);
   }

   @Override public float width ()
   {
      return graphics.cardWidth();
   }

   @Override public float height ()
   {
      return graphics.cardHeight();
   }

   @Override
   public void setZorder(int _zorder)
   {
      graphics.cardSetZorder(gid, _zorder);
   }

   public int getZorder ()
   {
      return graphics.getZorder(gid);
   }

   @Override
   public void show(boolean _show)
   {
      graphics.cardShow(gid, _show);
   }

   @Override public Playable findTouched (float _posX, float _posY)
   {
      //todo findTouched
      return null;
   }

   public boolean same (byte _suit, byte _rank)
   {
      if (id.bytes[1] == _suit && id.bytes[2] == _rank)
         return true;
      else
         return false;
   }

   public float posX ()
   {
      return graphics.cardX(gid);
   }

   public float posY ()
   {
      return graphics.cardY(gid);
   }

   public boolean oppositeSuit (byte _compSuit)
   {
      boolean opposite = false;
      boolean comp_suit_red = _compSuit == Const.Suit.HEART || _compSuit == Const.Suit.DIAMOND;
      boolean this_suit_red = id.bytes[1] == Const.Suit.HEART || id.bytes[1] == Const.Suit.DIAMOND;

      if (comp_suit_red && !this_suit_red)
         opposite = true;
      else if (!comp_suit_red && this_suit_red)
         opposite = true;

      return opposite;
   }

   public boolean sameSuit (byte _compSuit)
   {
      boolean same = false;

      if (id.bytes[1] == _compSuit)
         same = true;

      return same;
   }

   public boolean precedesRank (byte _compRank)
   {
      boolean precedes = false;

      if (_compRank-1 == id.bytes[2])
         precedes = true;

      return precedes;
   }

   public boolean succeedsRank (byte _compRank)
   {
      boolean succeeds = false;

      if (_compRank+1 == id.bytes[2])
         succeeds = true;

      return succeeds;
   }

   public float flipAngle ()
   {
      return graphics.cardRotationAngle(gid, Const.Angle.AXIS_Y);
   }

   public float spinAngle ()
   {
      return graphics.cardRotationAngle(gid, Const.Angle.AXIS_Z);
   }

   public static class PositionInPileComparator implements Comparator<Playable>
   {
      public int compare (Playable _c1, Playable _c2)
      {
         if (((Card)_c1).positionInPile < ((Card)_c2).positionInPile)
            return -1;
         else if (((Card)_c1).positionInPile > ((Card)_c2).positionInPile)
            return 1;
         else
            return 0;
      }
   }

   public void setAnimator (CardAnimStrategy _animator)
   {
      animator = _animator;
   }

   public CardAnimStrategy animator ()
   {
      if (animator != null)
         return animator;
      else
         return Const.NullAnimStrategy;
   }

   public boolean intersecting (float _minX, float _minY, float _width, float _height)
   {
      RectF this_rect;

      this_rect = new RectF(posX(), posY(), posX()+width(), posY()+height());

      return this_rect.intersect(_minX, _minY, _minX+_width, _minY+_height);
   }

   public boolean pointInCard (float _x, float _y)
   {
      if (_x >= posX() && _y >= posY() && _x <= posX()+width() && _y < posY()+height())
         return true;
      else
         return false;
   }

   public boolean pointInCardExOverlap (float _x, float _y, Card _overlappingCard)
   {
      float bot = posY();
      float h = height();
      if (_overlappingCard != null)
      {
         bot = _overlappingCard.posY()+_overlappingCard.height();
         h = (posY()+height())-bot;
      }
      if (_x >= posX() && _y >= bot && _x <= posX()+width() && _y < bot+h)
         return true;
      else
         return false;
   }

   public RectF getRect ()
   {
      RectF this_rect;

      return new RectF(posX(), posY(), posX()+width(), posY()+height());
   }
}
