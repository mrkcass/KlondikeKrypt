package com.adapted.solitare;

/**
 * Created by mark on 7/14/13.
 */
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
public interface CardAnimStrategy
{
   public int step ();
   public void finishNow ();
}



//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class CardAnimStrategyNull implements CardAnimStrategy
{
   public CardAnimStrategyNull () {}
   public int step () {return 0;}
   public void finishNow () {}
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================

class CardAnimStrategyTabToTab implements CardAnimStrategy
{
   private float moveOffsetX;
   private float moveOffsetY;
   private final float moveAngleOffset;
   private final float stepScale;
   private float destX, destY;
   private Card card;
   private int stepsTaken, moveSteps;
   private int stepDelay = 32;
   private int destZOrder;
   private float translateDistance;
   private float translateX, translateY;
   private parabolicFunction function;
   private int totalSteps;
   private float amountMovedX, amountMovedY;

   public CardAnimStrategyTabToTab (Card _card, float _destX, float _destY)
   {
      card = _card;
      destX = _destX;
      destY = _destY;
      moveSteps = -1;
      destZOrder = card.getZorder();

      translateDistance = (float) Math.sqrt(Math.pow(destX - card.posX(), 2) + Math.pow(destY - card.posY(), 2));
      translateX = destX - card.posX();
      translateY = destY - card.posY();
      float steps = (translateDistance / card.width()) * 3.2f;
      moveSteps = (int)steps;
      if (moveSteps % 2f == 1)
         moveSteps--;

      amountMovedX = amountMovedY = 0;

      if (moveSteps < 10)
         moveSteps = 10;
      totalSteps = moveSteps;
      float domain = 10f;
      float domainScale = domain / (((float)totalSteps)*.5f);
      function = new parabolicFunction(-.02f, domain, domainScale);
      moveAngleOffset = 360f / moveSteps;
      stepsTaken = 0;
      float scaler = .5f * (steps / 10f);
      if (scaler > .5f)
         scaler = .5f;
      stepScale = scaler / (float)(moveSteps/2);
      card.setZorder(destZOrder+100);
      card.show(true);
   }

   public int step ()
   {
      int delay = 0;

      if (moveSteps == 1)
      {
         card.moveTo(destX, destY);
         card.setZorder(destZOrder);
         card.scale (1f);
      }
      else
      {
         if (stepsTaken < totalSteps/2)
         {
            float percComplete;
            percComplete = function.rangeRatio(stepsTaken+1);
            moveOffsetX = translateX*percComplete*.5f;
            moveOffsetY = translateY*percComplete*.5f;
         }
         else
         {
            //todo
            float percComplete;
            percComplete = function.rangeRatio(totalSteps - (stepsTaken+1));
            moveOffsetX = translateX*(1f - (percComplete *.5f));
            moveOffsetY = translateY*(1f - (percComplete *.5f));
         }
         moveOffsetX -= amountMovedX;
         moveOffsetY -= amountMovedY;
         card.move(moveOffsetX, moveOffsetY);
         amountMovedX += moveOffsetX;
         amountMovedY += moveOffsetY;
         if (stepsTaken < totalSteps / 2)
            card.scale (1f + (stepScale*(float)stepsTaken));
         else
            card.scale (1f + (stepScale * moveSteps));
         delay = stepDelay;
      }
      moveSteps--;
      stepsTaken++;

      return delay;
   }

   public void finishNow ()
   {

   }
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class CardAnimStrategyFlipTab implements CardAnimStrategy
{
   private Card card;
   private int step;
   private int totalSteps;
   private float scaleMax;

   public CardAnimStrategyFlipTab (Card _card)
   {
      card = _card;
      step = 1;
      totalSteps = 7;
      scaleMax = 2f;
   }

   public int step ()
   {
      int delay = 0;

      if (step <= totalSteps)
      {
         card.flip(Const.Angle.FACE_UP * ((float)step / (float)totalSteps), GraphicsInterface.AXIS_JUSTIFY_NONE);
         if (step < totalSteps/2)
            card.scale((scaleMax-1.0f)*((float)step / ((float)totalSteps/2f))+1f);
         else
            card.scale(scaleMax - ((scaleMax-1.0f)*((float)step / (float)totalSteps)));
         step++;
         delay = 32;
      }

      return delay;
   }

   public void finishNow ()
   {
      card.flip(Const.Angle.FACE_UP, GraphicsInterface.AXIS_JUSTIFY_NONE);
   }
}


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class parabolicFunction
{
   private float scaler;
   private float domain;
   private float range;
   private float domainScale;

   public parabolicFunction (float _scaler, float _domain, float _domainScale)
   {
      scaler = _scaler;
      domain = _domain;
      domainScale = _domainScale;

      range = evaluate(_domain);
   }

   public float rangeRatio (float _domainVal)
   {
      float val;

      if (_domainVal*domainScale < 0)
         val = 0;
      else if (_domainVal*domainScale > domain)
         val = domain;
      else
         val = _domainVal*domainScale;

      return evaluate(val) / range;
   }

   private float evaluate (float _val)
   {
      return scaler * (_val * _val);
   }


}