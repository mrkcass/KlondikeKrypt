package com.adapted.solitare;

/**
 * Created by mcass on 6/10/13.
 */
public interface GraphicsInterface
{
   static final int AXIS_JUSTIFY_NONE =   0;
   static final int AXIS_JUSTIFY_LEFT =   1;
   static final int AXIS_JUSTIFY_CENTER = 2;
   static final int AXIS_JUSTIFY_RIGHT =  3;

   int getCardId (byte suit, byte rank);
   void cardShow(int _id, boolean show);
   void cardMove(int _id, float _dx, float _dy);
   void cardMoveTo(int _id, float _x, float _y);
   void cardRotate(int _id, int _axis, int _axisJustification, float angle);
   float cardRotationAngle (int _id, int _axis);
   int cardSetZorder(int _id, int _zorder);

   void cardScale (int _id, float scale);

   float cardHeight ();
   float cardWidth ();
   float cardX (int _id);
   float cardY (int _id);

   int getZorder (int _id);

   float screenAspectRatio ();
   void draw ();
   void addSurfaceChangedListener(GraphicsSurfaceChangedListener _gl);
   void addExecGraphicsCommandListener (GraphicsExecCommandsListener _listner);
   void delExecGraphicsCommandListener (GraphicsExecCommandsListener _listner);

   boolean drawComplete();
   void forceRedraw ();

   float cardScaleValue (int _id);
}

interface GraphicsSurfaceChangedListener
{
   void surfaceChanged ();
}

interface GraphicsExecCommandsListener
{
   boolean ExecGraphicsCommands ();
}

