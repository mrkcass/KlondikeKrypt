package com.adapted.solitare;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================

public class GameGLSurface extends GLSurfaceView
{
   private GameSurfaceRenderer renderer;
   private ArrayList<UserInputListener> inputListeners;
   private float dragX, dragY;
   private boolean dragging;

   private class DragTask extends Thread
   {
      private volatile boolean paused = true;
      private final Object signal = new Object();


      DragTask (String name)
      {
         super (name);
      }

      @Override
      public void run()
      {
         while (true)
         {
            while(paused)
            {
               synchronized(signal)
               {
                  try
                     { signal.wait();}
                  catch (InterruptedException e)
                     {e.printStackTrace();}
               }
            }

            try
               {sleep (500);}
            catch (InterruptedException e)
               {e.printStackTrace();}

            if (!paused)
               timerCallback();
         }
      }

      public void go ()
      {
         paused = false;
         synchronized(signal)
         {
            signal.notify();
         }
      }

      public void pause ()
      {
         paused = true;
         interrupt();
      }
   }
   private DragTask dragTask;

   @TargetApi(Build.VERSION_CODES.FROYO)
   public GameGLSurface(Context context)
   {
      super(context);

      inputListeners = new ArrayList<UserInputListener>();

      // Create an OpenGL ES 2.0 context.
      setEGLContextClientVersion(2);

      // Set the Renderer for drawing on the GLSurfaceView
      renderer = new GameSurfaceRenderer(this);
      renderer.setContext(context);
      setRenderer(renderer);

      // Render the view only when there is a change in the drawing data
      setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

      dragTask = new DragTask("DragTask");
      dragTask.start();
      dragging = false;
   }

   public GraphicsInterface getGraphicsInterface ()
   {
      return renderer;
   }

   @Override
   public boolean onTouchEvent (MotionEvent e)
   {
      int len = inputListeners.size();
      for (int i=0; i < len; i++)
      {
         float x = (float)(((float)e.getX() / (float)renderer.getScreenWidth()) * 2.0);
         float y = 2.0f - (float)(((float)e.getY() / (float)renderer.getScreenHeight()) * 2.0);
         if (e.getAction() == MotionEvent.ACTION_UP)
         {
            dragTask.pause();

            if (!dragging)
            {
               inputListeners.get(i).touchEvent(Const.InputType.TOUCH, 0, x, y);
            }
            else
            {
               inputListeners.get(i).touchEvent(Const.InputType.DRAG_END, 0, x, y);
            }

            dragging = false;
         }
         else if (e.getAction() == MotionEvent.ACTION_DOWN)
         {
               timerStart ();
               dragX = x;
               dragY = y;
         }
         else if (e.getAction() == MotionEvent.ACTION_MOVE)
         {
            if (dragging)
            {
               dragX = x;
               dragY = y;
               inputListeners.get(i).touchEvent(Const.InputType.DRAG, 0, x, y);
            }
         }
      }
      return true;
   }

   private void timerStart ()
   {
      dragTask.go();
   }

   private void timerCallback ()
   {
      int len = inputListeners.size();
      for (int i=0; i < len; i++)
         dragging = inputListeners.get(i).touchEvent(Const.InputType.DRAG_START, 0, dragX, dragY);
      dragTask.pause();
   }

   public void addInputListener (UserInputListener _listener)
   {
      inputListeners.add (_listener);
   }
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class GameSurfaceRenderer implements GLSurfaceView.Renderer, GraphicsInterface
{
   private Context context;
   private float cardWidth, cardHeight;
   private int screenWidth, screenHeight;
   private float screenAspectRatio;
   private GLSurfaceView parent;
   private ArrayList<GraphicsSurfaceChangedListener> SurfaceChangedListeners;
   private ArrayList<GraphicsExecCommandsListener> ExecListeners;
   private float projectionM[] = new float[16];
   private float viewM[] = new float[16];
   private float viewProjectionM[] = new float[16];
   private final Object signalDrawComplete = new Object();
   private boolean drawComplete, waitingOnDrawComplete;
   private GLCardList cardlist;
   private boolean forceRedrawRequested = false;


   public GameSurfaceRenderer (GLSurfaceView _parent)
   {
      super ();

      parent = _parent;
      SurfaceChangedListeners = new ArrayList<GraphicsSurfaceChangedListener>();
      ExecListeners = new ArrayList<GraphicsExecCommandsListener>();
      drawComplete = true;
      waitingOnDrawComplete = false;
   }

   public void setContext (Context _context)
   {
      context = _context;
   }

   @Override
   public void onSurfaceCreated(GL10 gl, EGLConfig config)
   {
      // Set the background frame color
      GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
      gl.glEnable(GL10.GL_TEXTURE_2D); //Enable Texture Mapping ( NEW )
      gl.glDisable(GL10.GL_DEPTH_TEST);

   }

   boolean inside = false;

   @Override
   public void onDrawFrame(GL10 unused)
   {

      if (inside)
         Log.d("solitare", "inside is true on entrance");
      int numListeners = ExecListeners.size();
      boolean dirty = false;
      for (int i=0; i < numListeners; i++)
      {
         boolean listenerDirty = ExecListeners.get(i).ExecGraphicsCommands();
         if (listenerDirty)
            dirty = true;
      }

      if (dirty || forceRedrawRequested)
      {
         GLES20.glClear( GLES20.GL_COLOR_BUFFER_BIT);
         // Draw background color
         cardlist.draw(viewProjectionM);
         forceRedrawRequested = false;
      }
      else
         sleepEx(5);
   }

   @Override
   public void forceRedraw ()
   {
      forceRedrawRequested = true;
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

   @Override
   public boolean drawComplete()
   {
      if (waitingOnDrawComplete)
      {
         synchronized(signalDrawComplete)
         {
            waitingOnDrawComplete = false;
            drawComplete = true;
            signalDrawComplete.notify();
         }

      }
      return drawComplete;
   }

   @Override
   public void draw()
   {
      synchronized(signalDrawComplete)
      {drawComplete = false;
      if (!drawComplete)
      {
            try
            {signalDrawComplete.wait();}
            catch (InterruptedException e)
            {e.printStackTrace();}
      }
      }
   }

   public int getScreenWidth ()
   {
      return screenWidth;
   }

   public int getScreenHeight ()
   {
      return screenHeight;
   }

   @Override
   public void onSurfaceChanged(GL10 unused, int width, int height)
   {
      float cardw, cardh;
      // Adjust the viewport based on geometry changes,
      // such as screen rotation
      GLES20.glViewport(0, 0, width, height);

      float ratio = (float)width / (float)height;

      if (width > height)
      {
         float fh = (float)height;
         float card_aspect = 2.5f / 3.75f;

         float ch = fh / 4.5f;

         cardh =  (ch / fh) * ratio;
         cardw = (cardh  * card_aspect);
      }
      else
      {
         float fw = (float)width;
         float card_aspect = 3.75f / 2.5f;

         float cw = fw / 10f;

         cardw = (cw / fw) / ratio;
         cardh = cardw  * card_aspect;
      }

      screenAspectRatio = ratio;
      cardWidth = cardw;
      cardHeight = cardh;
      screenWidth = width;
      screenHeight = height;

      Matrix.setIdentityM(projectionM, 0);

      Matrix.setIdentityM(viewProjectionM, 0);

      // Set the camera position (View matrix)
      Matrix.setLookAtM(viewM, 0,
              0f, 0f, 5f,
              0f, 0f, 0f,
              0f, 1.0f, 0.0f);

      Matrix.orthoM(projectionM, 0, -screenAspectRatio, screenAspectRatio, -1, 1, 0, 10);
      //Matrix.perspectiveM(projectionM, 0, 5.75f, screenAspectRatio, 0, 20);

      // Calculate the projection and view transformation
      Matrix.multiplyMM(viewProjectionM, 0, projectionM, 0, viewM, 0);

      Rectangle.initialised = false;
      cardlist = new GLCardList(screenAspectRatio, cardw, cardh, context);

      for (GraphicsSurfaceChangedListener gl : SurfaceChangedListeners)
         gl.surfaceChanged();
   }

   @Override
   public int getCardId(byte _suit, byte _rank)
   {
      return cardlist.getId(_suit, _rank);
   }

   @Override
   public void cardMove(int _id, float _dx, float _dy)
   {
      GLCard c = cardlist.get(_id);
      c.move(_dx, _dy);
   }

   @Override
   public void cardMoveTo(int _id, float _x, float _y)
   {
      GLCard c = cardlist.get(_id);
      c.moveTo(_x-1f, _y-1f);
   }

   @Override
   public void cardRotate(int _id, int _axis, int _axisJustification, float _angle)
   {
      GLCard c = cardlist.get(_id);
      c.rotate (_axis, _axisJustification, _angle);
   }

   @Override
   public void cardScale (int _id, float scale)
   {
      GLCard c = cardlist.get(_id);
      c.scale(scale);
   }

   @Override
   public float cardHeight()
   {
      return cardHeight;
   }

   @Override
   public float cardWidth()
   {
     return cardWidth / screenAspectRatio;
      //return cardWidth;
   }

   @Override
   public float cardX(int _id)
   {
      GLCard c = cardlist.get(_id);
      return c.posX() + 1f;
   }

   @Override
   public float cardY(int _id)
   {
      GLCard c = cardlist.get(_id);
      return c.posY() + 1f;
   }

   @Override
   public float screenAspectRatio()
   {
      return screenAspectRatio;
   }

   @Override
   public void addSurfaceChangedListener(GraphicsSurfaceChangedListener _gl)
   {
      SurfaceChangedListeners.add(_gl);
   }

   @Override
   public void addExecGraphicsCommandListener (GraphicsExecCommandsListener _listner)
   {
      ExecListeners.add (_listner);
   }

   @Override
   public void delExecGraphicsCommandListener (GraphicsExecCommandsListener _listner)
   {
      ExecListeners.remove(_listner);
   }

   @Override
   public int cardSetZorder(int _id, int _zorder)
   {
      return cardlist.setZorder(_id, _zorder);
   }

   @Override
   public int getZorder (int _id)
   {
      GLCard c;
      //TODO: GameSurfaceRenderer.cardSetZorder
      c = cardlist.get(_id);
      return c.zorder;
   }

   @Override
   public void cardShow(int _id, boolean _show)
   {
      GLCard c = cardlist.get(_id);
      c.show(_show);
   }

   @Override
   public float cardRotationAngle (int _id, int _axis)
   {
      GLCard c = cardlist.get(_id);
      return c.getAngle(_axis);
   }

   @Override
   public float cardScaleValue (int _id)
   {
      GLCard c = cardlist.get(_id);
      return c.getScale();
   }
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class GLCardList
{
   //this class manages a list of cards. for fast operations an array and linked list are maintained
   //this also saves the heap allocations associated with using iterators to traverse a linked list
   //the id of each card is just the (((suit-1) * rank)+1). each card is stored at "array" index [id]. this
   //allows cards to be looked up in constant time by rank and suit or id. the linked list is used
   //to maintain an ascending z order list of the cards. this list is used when drawing the cards, which
   //must be carried out in ascending z-order order.
   final int NUM_CARDS = (Const.NUM_RANKS * Const.NUM_SUITS) + 1;
   private GLCard first, last;
   private GLCard [] cards;


   GLCardList (float _aspectRatio, float _width, float _height, Context _context)
   {
      int card_id = 1;

      cards = new GLCard[NUM_CARDS];
      first = last = null;

      for (int suit=0; suit < Const.NUM_SUITS; suit++)
      {
         for (int rank=0; rank < Const.NUM_RANKS; rank++)
         {
            int id;

            try
            {
               Field idField;
               String name = Const.resourceSuits[suit] + Const.resourceRanks[rank];
               idField = R.drawable.class.getDeclaredField(name);
               id = idField.getInt(idField);
            }
            catch (Exception e)
            {
               e.printStackTrace();
               id = -1;
            }

            GLCard new_card = new GLCard (card_id, _aspectRatio, _width, _height, (byte)(suit+1), (byte)rank, _context, id);
            new_card.moveTo (0,0);
            cards[card_id] = new_card;
            if (card_id == 1)
            {
               first = new_card;
               last = new_card;
            }
            else
            {
               last.next = new_card;
               new_card.prev = last;
               last = new_card;
            }
            card_id++;
         }
      }
   }

   int getId (int _suit, int _rank)
   {
      return ((_suit-1)*Const.NUM_RANKS)+_rank+1;
   }

   public GLCard get (int _id)
   {
      return cards[_id];
   }

   public void draw (float [] _viewProjectionM)
   {
      GLCard c = first;

      while (c != null)
      {
         c.draw(_viewProjectionM);
         c = c.next;
      }
   }

   public int setZorder (int id, int zorder)
   {
      GLCard c = cards[id];
      int old_zorder = c.zorder;

      if (c.zorder != zorder)
      {
         if (c.prev != null)
            c.prev.next = c.next;
         if (c.next != null)
            c.next.prev = c.prev;
         if (c == first)
            first = c.next;
         else if (c == last)
            last = c.prev;

         GLCard ins = c;
         if (zorder > c.zorder)
         {
            while (c.next != null)
            {
               if (zorder <= c.next.zorder)
                  break;
               c = c.next;
            }
            ins.next = c.next;
            if (ins != c)
               ins.prev = c;
         }
         else
         {
            while (c.prev != null)
            {
               if (zorder >= c.prev.zorder)
                  break;
               c = c.prev;
            }
            if (ins != c)
               ins.next = c;
            ins.prev = c.prev;
         }
         ins.zorder = zorder;

         if (ins.prev != null)
            ins.prev.next = ins;
         if (ins.next != null)
            ins.next.prev = ins;
         if (ins.prev == null)
            first = ins;
         else if (ins.next == null)
            last = ins;
      }

      return old_zorder;
   }

   void checklist ()
   {
      int count = 0;
      GLCard c = first;
      while (c.next != null)
      {
         count ++;
         if (c.next.prev != c)
            Log.e ("linked list", "linked list");
         if (c.zorder > c.next.zorder)
            Log.e ("linked list", "linked list");
         c = c.next;

      }

      if (c != last || count != 51)
         Log.e ("linked list", "linked list");
   }
}


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class GLCard extends Rectangle
{
   public byte suit, rank;
   public int id;
   public GLCard next, prev;

   public GLCard (int _id, float _aspectRatio, float _width, float _height, byte _suit, byte _rank, Context _context, int _rid)
   {
      super (_width, _height, _aspectRatio);

      next = prev = null;
      if (_id != 0)
      {
         id = _id;
         suit = _suit;
         rank = _rank;

         loadGLTexture(_context, _rid);
      }
   }


   public boolean isCard (byte _suit, byte _rank)
   {
      if (suit == _suit && rank == _rank)
         return true;
      else
         return false;
   }

   public int getId ()
   {
      return id;
   }
}

class GLCardNULL extends GLCard
{
   public GLCardNULL ()
   {
      super (0, 0, 0, 0, (byte)0, (byte)0, null, -1);
   }

   public GLCardNULL (int _id, float _aspectRatio, float _width, float _height, String _suit, String _rank, Context _context, int _rid)
   {
      super(0, _aspectRatio, 0, 0, (byte)0, (byte)0, _context, -1);
   }

   public boolean isCard (String _suit, String _rank)
   {
      return false;
   }

   public int getId ()
   {
      return 0;
   }

   public void draw(float [] _viewProjectionM)
   {

   }
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class Rectangle
{
   private final String vertexShaderCode =
            "attribute vec4 vPosition;"                                                                     +
            "attribute vec2 a_TexCoordinate;"                                                               +
            "uniform mat4 uMVPMatrix; "                                                                     +
            "varying vec2 v_TexCoordinate;"                                                                 +
            "void main()"                                                                                   +
            "{"                                                                                             +
               "gl_Position = uMVPMatrix * vPosition;"                                                      +
               "v_TexCoordinate = a_TexCoordinate;"                                                         +
            "}";

   private final String fragmentShaderCode =
            "precision mediump float;"                                                                      +
            "uniform sampler2D u_Texture;"                                                                  +
            "varying vec2 v_TexCoordinate;"                                                                 +
            "void main()"                                                                                   +
            "{"                                                                                             +
               "vec2 flipped_texcoord;"                                                                     +
               "if (gl_FrontFacing){"                                                                       +
                  "flipped_texcoord = vec2(v_TexCoordinate.x, 1.0 - v_TexCoordinate.y);"                    +
               "}else{"                                                                                     +
                  "flipped_texcoord = vec2(1.0 - v_TexCoordinate.x, 1.0 - v_TexCoordinate.y);"              +
               "}"                                                                                          +
               "gl_FragColor = texture2D(u_Texture, flipped_texcoord);"                                     +
            "}";

   private FloatBuffer vertexBuffer;
   private ShortBuffer indexBuffer;

   private int[] textureHandles = new int[1];

   // number of coordinates per vertex in this array
   static final int COORDS_PER_VERTEX = 3;
   static final int SIZEOF_FLOAT = 4;
   static final int SIZEOF_SHORT = 2;

   private final int vertexStride = COORDS_PER_VERTEX * SIZEOF_FLOAT; // bytes per vertex
   //lower left corner
   private float pos_x, pos_y;
   private float width, height;
   private boolean visible;

   private float angleX, angleY, angleZ;
   private float scale;
   private float aspectRatio;
   private int axisJustification;
   //apply matrix operations
   private float modelM[] = new float[16];
   private float result[] = new float[16];

   public int zorder;

   private static FloatBuffer textureCoordsBuffer;
   private static int programHandle;
   public static boolean initialised = false;
   static int mPositionHandle = -1;
   static int mTextureCoordinateHandle = -1;
   static int mMVPMatrixHandle = -1;
   private static int [] textureBackHndl = null;


   public Rectangle(float _width, float _height, float _aspectRatio)
   {
      pos_x = pos_y = 0f;
      width = _width;
      height = _height;
      visible = true;
      zorder = 0;
      angleX = angleY = angleZ = 0;
      scale = 1.0f;
      aspectRatio = _aspectRatio;

      createIndexBuffer ();

      if (!initialised)
      {
         createTextureCoordBuffer();
         createVertexBuffer (_width, _height);
         // prepare shaders and OpenGL program
         int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
         int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

         programHandle = GLES20.glCreateProgram();             // create empty OpenGL Program
         GLES20.glAttachShader(programHandle, vertexShader);   // add the vertex shader to program
         GLES20.glAttachShader(programHandle, fragmentShader); // add the fragment shader to program
         GLES20.glBindAttribLocation(programHandle, 0, "vPosition");
         GLES20.glBindAttribLocation(programHandle, 1, "a_TexCoordinate");
         GLES20.glLinkProgram(programHandle);                  // create OpenGL program executables

         final int[] linkStatus = new int[1];
         GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
         // If the link failed, delete the program.
         if (linkStatus[0] == 0)
         {
            Log.e("Rectangle", "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
            GLES20.glDeleteProgram(programHandle);
            programHandle = 0;
         }
         // Add program to OpenGL environment
         GLES20.glUseProgram(programHandle);
         //setup the texture buffer
         mTextureCoordinateHandle = GLES20.glGetAttribLocation(programHandle, "a_TexCoordinate");
         GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, textureCoordsBuffer);
         GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
         // set the vertex buffer
         mPositionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition");
         GLES20.glEnableVertexAttribArray(mPositionHandle);
         GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
         //handle for transform matrix
         mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
         textureBackHndl = null;
         initialised = true;
      }
   }

   private void checkGlError(String op)
   {
      int error;
      while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
      {
         Log.e("solitare", op + ": glError " + error);
         //throw new RuntimeException(op + ": glError " + error);
      }
   }

   public static int loadShader(int type, String shaderCode)
   {
      // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
      // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
      int shader = GLES20.glCreateShader(type);

      // add the source code to the shader and compile it
      GLES20.glShaderSource(shader, shaderCode);
      GLES20.glCompileShader(shader);

      // Get the compilation status.
      final int[] compileStatus = new int[1];
      GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

      // If the compilation failed, delete the shader.
      if (compileStatus[0] == 0)
      {
         String err = GLES20.glGetShaderInfoLog(shader);
         Log.e("LoadShader", "Error compiling shader: " + err);
         GLES20.glDeleteShader(shader);
         shader = 0;
      }

      return shader;
   }

   public float posX ()
   {
      return pos_x;
   }

   public float posY ()
   {
      return pos_y;
   }

   private void createIndexBuffer()
   {
      short indices [] =
      {
         //triangle 1
         0, 1, 2,
         //triangle 2
         0, 2, 3
      };
      ByteBuffer bb = ByteBuffer.allocateDirect(indices.length*SIZEOF_SHORT);
      bb.order(ByteOrder.nativeOrder());
      indexBuffer = bb.asShortBuffer();
      indexBuffer.put(indices);
      indexBuffer.position(0);
   }

   private void createVertexBuffer(float width, float height)
   {
      float vertices[] = new float [12];
      vertices[0] = -width/2;    vertices[1]  =  height/2;  vertices[2]  = 0;
      vertices[3] = -width/2;    vertices[4]  = -height/2;  vertices[5]  = 0;
      vertices[6] =  width/2;    vertices[7]  = -height/2;  vertices[8]  = 0;
      vertices[9] =  width/2;    vertices[10] =  height/2;  vertices[11] = 0;

      // initialize vertex byte buffer for shape coordinates
      // (number of coordinate values * 4 bytes per float)
      ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * SIZEOF_FLOAT);
      // use the device hardware's native byte order
      bb.order(ByteOrder.nativeOrder());

      // create a floating point buffer from the ByteBuffer
      vertexBuffer = bb.asFloatBuffer();
      // add the coordinates to the FloatBuffer
      vertexBuffer.put(vertices);
      // set the buffer to read the first coordinate
      vertexBuffer.position(0);
   }

   private void createTextureCoordBuffer()
   {
      float textureCoords[] =
      {
            // Mapping coordinates for the vertices
            0.0f, 1.0f, // top left (V2)
            0.0f, 0.0f, // bottom left (V1)
            1.0f, 0.0f, // top right (V4)
            1.0f, 1.0f // bottom right (V3)
      };
      ByteBuffer bb = ByteBuffer.allocateDirect(textureCoords.length * SIZEOF_FLOAT);
      // use the device hardware's native byte order
      bb.order(ByteOrder.nativeOrder());

      // create a floating point buffer from the ByteBuffer
      textureCoordsBuffer = bb.asFloatBuffer();
      // add the coordinates to the FloatBuffer
      textureCoordsBuffer.put(textureCoords);
      // set the buffer to read the first coordinate
      textureCoordsBuffer.position(0);
   }

   public void moveTo (float _x, float _y)
   {
      pos_x = _x;
      pos_y = _y;
   }

   public void move (float _dx, float _dy)
   {
      pos_x += _dx;
      pos_y += _dy;
   }

   public void rotate (int _axis, int _axisJustification, float _angle)
   {
      if (_axis == Const.Angle.AXIS_Y)
      {
         angleY = _angle;

         if (angleY < 0)
            angleY = 0;
         if (angleY > 180)
            angleY = 180;
      }
      else if (_axis == Const.Angle.AXIS_Z)
      {
         angleZ = _angle;
      }
      axisJustification = _axisJustification;
   }

   public float getAngle (int _axis)
   {
      switch (_axis)
      {
         case Const.Angle.AXIS_X:
            return angleX;
         case Const.Angle.AXIS_Y:
            return angleY;
         case Const.Angle.AXIS_Z:
            return angleZ;
         default:
            return 0;
      }
   }

   public float getScale ()
   {
      return scale;
   }

   public void scale (float _scale)
   {
      scale = _scale;
   }

   public void show (boolean _show)
   {
      visible = _show;
   }

   public void loadGLTexture(Context context, int _rid)
   {
      loadCardBack(context);
      // loading texture
      Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), _rid);

      // generate one texture pointer
      GLES20.glGenTextures(1, textureHandles, 0);
      // …and bind it to our array
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[0]);

      // create nearest filtered texture
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

      // Use Android GLUtils to specify a two-dimensional texture image from our bitmap
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

      // Clean up
      bitmap.recycle();
   }

   private void loadCardBack (Context context)
   {
      if (textureBackHndl == null)
      {
         textureBackHndl = new int[1];
         // loading texture
         Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.crd_back);

         // generate one texture pointer
         GLES20.glGenTextures(1, textureBackHndl, 0);
         // …and bind it to our array
         GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureBackHndl[0]);

         // create nearest filtered texture
         GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
         GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

         // Use Android GLUtils to specify a two-dimensional texture image from our bitmap
         GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

         // Clean up
         bitmap.recycle();
      }
   }

   float[] temprot = new float[16];

   public void draw(float [] _viewProjectionM)
   {
      if (visible)
      {
         //set a flag for the shader to flip the texture horizontally
         if (angleY > 90f)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[0]);
         else
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureBackHndl[0]);
         checkGlError("bind texture");
         Matrix.setIdentityM(modelM, 0);
         Matrix.setIdentityM(result, 0);

         float x = (aspectRatio * pos_x)+(width/2f);
         if (axisJustification == GraphicsInterface.AXIS_JUSTIFY_LEFT && angleY > 90f)
            x -= width/2f;
         Matrix.translateM(modelM, 0, x, pos_y+(height/2f), 0f);
         if (Math.abs(1.0 - scale) > .001)
            Matrix.scaleM(modelM, 0, scale, scale, 1.0f);

         if (Math.abs(angleZ) > .01)
         {
            Matrix.setRotateM(temprot, 0, angleZ, 0, 0, 1f);
            Matrix.multiplyMM(result, 0, modelM, 0, temprot, 0);
            System.arraycopy(result, 0, modelM, 0, 16);
         }
         if (axisJustification == GraphicsInterface.AXIS_JUSTIFY_LEFT)
         {
            if (angleY < 90f)
               Matrix.translateM(modelM, 0, -width/2f, 0, 0f);
            else
              Matrix.translateM(modelM, 0, width, 0, 0f);
         }
         boolean yrot = true;
         float yrot_angle = 0;
         if (Math.abs(angleY) < .01)
            yrot = false;
         else if (angleY <= 90f)
            yrot_angle = angleY;
         else if (angleY > 90f && angleY < 180f)
            yrot_angle = 360f - angleY;
         else
            yrot = false;
         if (yrot)
         {
            Matrix.setRotateM(temprot, 0, yrot_angle, 0, 1f, 0);
            Matrix.multiplyMM(result, 0, modelM, 0, temprot, 0);
            System.arraycopy(result, 0, modelM, 0, 16);
         }
         if (axisJustification == GraphicsInterface.AXIS_JUSTIFY_LEFT)
            Matrix.translateM(modelM, 0, width/2f, 0, 0f);

         Matrix.multiplyMM(result, 0, _viewProjectionM, 0, modelM, 0);

         GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, result, 0);
         checkGlError("uniform matrix");

         // Draw the rectangle
         GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
         checkGlError("draw elements");
      }
   }
}
