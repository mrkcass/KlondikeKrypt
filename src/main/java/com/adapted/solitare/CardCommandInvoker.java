package com.adapted.solitare;

import android.os.SystemClock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * Created by mark on 6/8/13.
 */
public class CardCommandInvoker implements GraphicsExecCommandsListener
{
   private ArrayList<CardCommand> history;
   private int historyCount = 0;
   private int [] runningCmdsIdx = new int [500];
   private int runningCmdsIdxCount = 0;
   private int [] newCmdsIdx = new int [100];
   private int newCmdsIdxCount = 0;
   private GraphicsInterface graphics;
   private String gameStateFileName;
   private int undoCmdIdx = -1;
   public boolean stop = false;

   public CardCommandInvoker (GraphicsInterface _graphics, String _gameStateFileName)
   {
      graphics = _graphics;
      history = new ArrayList<CardCommand>();
      gameStateFileName = _gameStateFileName;
      graphics.addExecGraphicsCommandListener(this);
   }

   public void queueCommand (CardCommand _cmd)
   {
      synchronized (this)
      {
         int index = saveCommand(_cmd);
         newCmdsIdx[newCmdsIdxCount] = index;
         newCmdsIdxCount++;
      }
   }

   public void undo ()
   {
      CardCommand cc = history.get(history.size()-1);
      if (cc.type != CardCommand.TYPE_DEAL_CARD)
      {
         cc.convertToUndo();
         synchronized (this)
         {
            undoCmdIdx = history.size()-1;
            newCmdsIdx[newCmdsIdxCount] = undoCmdIdx;
            newCmdsIdxCount++;
         }
      }
   }

   public boolean restoreState (PlayableMediator _mediator)
   {
      return restoreGame(_mediator);
   }

   long time_start, time_done, pass_time, last_start_time, num_exec;
   long next_exec_delay = 50;
   boolean refresh = false;
   int [] removes = new int [100];
   int removesCount = 0;

   public boolean ExecGraphicsCommands ()
   {
      next_exec_delay = 50;

      last_start_time = time_start;
      time_start = SystemClock.uptimeMillis();
      num_exec = 0;

      boolean dirty = false;
      for (int cmdIdx=0; cmdIdx < runningCmdsIdxCount; cmdIdx++)
      {
         CardCommand cmd = history.get(runningCmdsIdx[cmdIdx]);
         if (cmd.initial_delay == 0 && (cmd.next_exec_time == 0 || cmd.next_exec_time <= time_start))
         {
            long delay;

            delay = cmd.execute ();
            num_exec++;
            dirty = true;

            if (delay == -1)
            {
               removes[removesCount] = cmdIdx;
               removesCount++;
            }
            else
            {
               if (delay < next_exec_delay)
                  next_exec_delay = delay;
               cmd.next_exec_time = time_start + delay;
            }
         }
         else if (cmd.initial_delay > 0)
         {
            long delay;

            delay = cmd.initial_delay;
            cmd.initial_delay = 0;
            cmd.next_exec_time = time_start + delay;
            if (delay < next_exec_delay)
               next_exec_delay = delay;
         }
         else if (cmd.next_exec_time - time_start < next_exec_delay)
         {
            next_exec_delay = cmd.next_exec_time - time_start;
         }
      }

      if (newCmdsIdxCount > 0)
      {
         synchronized (this)
         {
            for (int i=0; i < newCmdsIdxCount; i++)
            {
               runningCmdsIdx[runningCmdsIdxCount] = newCmdsIdx[i];
               runningCmdsIdxCount++;
            }
            newCmdsIdxCount = 0;
         }
      }

      if (removesCount > 0)
      {
         int offset = 0;
         for (int remIdx=0; remIdx < removesCount; remIdx++)
         {
            if (undoCmdIdx != -1 && undoCmdIdx == runningCmdsIdx[removes[remIdx]])
            {
               history.get(undoCmdIdx).release();
               history.remove(undoCmdIdx);
               historyCount--;
               saveHistory();
               undoCmdIdx = -1;
            }
            for (int runIdx = removes[remIdx]+1+offset; runIdx < runningCmdsIdxCount; runIdx++)
            {
               runningCmdsIdx[runIdx-1] = runningCmdsIdx[runIdx];
            }
            runningCmdsIdxCount--;
            offset--;
         }
         removesCount = 0;
      }

      time_done = SystemClock.uptimeMillis();
      pass_time = time_done - time_start;

      return dirty;
   }

   void terminate ()
   {
      stop = true;
      while (stop)
         sleepEx(1);
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

   private boolean restoreGame (PlayableMediator _mediator)
   {
      boolean fileRead = false;
      File f = new File(gameStateFileName);

      if (f.exists() && f.canRead())
      {
         try
         {
            RandomAccessFile fr = new RandomAccessFile(gameStateFileName, "r");
            int filelen = (int)f.length();
            byte [] fb = new byte[filelen];
            fr.read(fb);

            int msgstart=0, msglen = 0;
            boolean  found_delimiter = false;
            for (int i = 0; i < filelen; i++)
            {
               if (fb[i] == MMsg.MSG_FIELD_DELIMITER)
               {
                  if (found_delimiter)
                  {
                     MMsg msg = _mediator.acquireMsg();
                     MMsg.load (msg, fb, msgstart, msglen-1);
                     CardCommand cc = new CardCommand(_mediator, msg);
                     cc.immediate = true;
                     cc.execute();
                     history.add(historyCount, cc);
                     historyCount++;
                     cc.immediate = false;
                     found_delimiter = false;
                     msgstart = i+1;
                     msglen = -1;
                  }
                  else
                     found_delimiter = true;
               }
               else
                  found_delimiter = false;
               msglen++;
            }
            fileRead = true;
            fr.close();
            graphics.forceRedraw();
         }
         catch (Exception e)
         {

         }
      }
      return fileRead;
   }

   private int saveCommand (CardCommand _cmd)
   {
      boolean first_command;
      File f = new File(gameStateFileName);

      if (history.size() == 0)
         first_command = true;
      else
         first_command = false;

      history.add(historyCount, _cmd);
      historyCount++;

      try
      {
         FileOutputStream fw;
         if (first_command)
            fw = new FileOutputStream(f, false);
         else
            fw = new FileOutputStream(f, true);
         fw.write(_cmd.getMsg().bytes, 0, _cmd.getMsg().len);
         fw.write(MMsg.MSG_TERMINATOR);
         fw.close();
      }
      catch (Exception e)
      {

      }

      return historyCount-1;
   }

   private void saveHistory ()
   {
      File f = new File(gameStateFileName);

      try
      {
         FileWriter fw;
         fw = new FileWriter(f, false);
         BufferedWriter bw = new BufferedWriter(fw);
         for (CardCommand cmd : history)
            bw.append(cmd.getMsg().bytes + System.getProperty("line.separator"));
         bw.close();
      }
      catch (Exception e)
      {

      }
   }
}