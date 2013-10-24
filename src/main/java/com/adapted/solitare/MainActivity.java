package com.adapted.solitare;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;

public class MainActivity extends Activity implements GraphicsSurfaceChangedListener
{
   private GameGLSurface mGLView;
   private CardMediator mediator = null;
   private Bundle savedState = null;
   private CardCommandInvoker invoker = null;
   private  String gameStateFileName;
   private Player player = null;

   @Override
   protected void onCreate (Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      mGLView = new GameGLSurface(this);
      mGLView.getGraphicsInterface().addSurfaceChangedListener(this);
      setContentView(mGLView);
      if (savedInstanceState != null)
         savedState = (Bundle)savedInstanceState.clone();
      gameStateFileName = getFilesDir() + File.separator + "gamestate.dat";
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState)
   {
      super.onSaveInstanceState(savedInstanceState);
      if (mediator != null)
      {
         mediator.saveState(savedInstanceState);
      }
   }

   @Override
   public void onRestoreInstanceState(Bundle savedInstanceState)
   {
      super.onRestoreInstanceState(savedInstanceState);
      if (invoker != null && mediator != null)
         invoker.restoreState(mediator);
   }


   @Override
   public boolean onCreateOptionsMenu (Menu menu)
   {
      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.main, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected (MenuItem item)
   {
      // Handle item selection
      switch (item.getItemId())
      {
         case R.id.action_deal:
            if (mediator != null)
            {
               if (invoker != null)
                  mGLView.getGraphicsInterface().delExecGraphicsCommandListener(invoker);
               invoker = new CardCommandInvoker(mGLView.getGraphicsInterface(), gameStateFileName);
               mediator.newGame(invoker);
               if (player != null)
                  mGLView.InputListenerRemove((PlayerReal)player);
               PlayerReal realPlayer = new PlayerReal(mediator.GetRoot(), invoker);
               mGLView.InputListenerAdd(realPlayer);
               player = realPlayer;
            }
            return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   @Override
   public void surfaceChanged ()
   {
      if (invoker != null)
         mGLView.getGraphicsInterface().delExecGraphicsCommandListener(invoker);
      invoker = new CardCommandInvoker(mGLView.getGraphicsInterface(), gameStateFileName);
      mediator = new CardMediator(mGLView.getGraphicsInterface(), invoker);

      if (player != null)
         mGLView.InputListenerRemove((PlayerReal)player);
      PlayerReal realPlayer = new PlayerReal(mediator.GetRoot(), invoker);
      mGLView.InputListenerAdd(realPlayer);
      player = realPlayer;

      if (!invoker.restoreState(mediator))
         mediator.deal();
   }

   @Override
   public void onBackPressed()
   {
      if (invoker != null)
         invoker.undo();
   }
}

