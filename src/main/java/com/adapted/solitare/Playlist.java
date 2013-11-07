package com.adapted.solitare;

/**
 * Created by mark on 10/23/13.
 */
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
public class Playlist
{
   static final int MAX_PLAYS = 26;
   Play plays [];
   int numPlays;
   int selectedPlay = -1;

   Playlist ()
   {
      plays = new Play[MAX_PLAYS];
      for (int i=0; i < MAX_PLAYS; i++)
         plays[i] = new Play();
      Clear (MAX_PLAYS);
   }

   void Clear ()
   {
      Clear (numPlays);
   }

   void AddPlay (PlayerClient origin, byte type, CardComponentId srcId, CardComponentId destId)
   {
      numPlays = 0;
      if (numPlays < MAX_PLAYS)
      {
         plays[numPlays].origin = origin;
         plays[numPlays].srcId.set(srcId);
         plays[numPlays].addDest(destId);
         plays[numPlays].type = type;
         numPlays++;
      }
      //todo: error checking
   }

   void AddCard (int suit, int rank)
   {
      if (numPlays > 0 && numPlays <= MAX_PLAYS)
      {
         plays[numPlays-1].AddCard(suit, rank);
      }
   }

   void addDest (CardComponentId id)
   {
      if (numPlays > 0 && numPlays <= MAX_PLAYS)
         plays[numPlays-1].addDest(id);
   }

   void addSubcommand(byte cmd)
   {
      if (numPlays > 0 && numPlays <= MAX_PLAYS)
      {
         plays[numPlays-1].addSubcommand(cmd);
      }
   }

   void addSubcommand(byte [] cmd)
   {
      if (numPlays > 0 && numPlays <= MAX_PLAYS)
      {
         plays[numPlays-1].addSubcommand(cmd);
      }
   }

   private void Clear (int range)
   {
      for (int i=0; i < range; i++)
      {
         plays[i].Clear();
      }
      numPlays = 0;
      selectedPlay = -1;
   }

   public void selectPlay (int index)
   {
      if (index >= 0 && index < MAX_PLAYS)
         selectedPlay = index;
   }

   public CardCommand createCommand ()
   {
      if (selectedPlay != -1)
         return plays[selectedPlay].CreateCommand();
      else
         return null;
   }
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
class Play
{
   static final int MAX_CARDS = 52;
   static final int MAX_DEST = 15;
   byte type;
   PlayerClient origin;
   CardComponentId srcId;
   CardComponentId destId [];
   Playcard cards [];
   int numCards;
   int numDest;
   MMsg subcommands;
   int selectedDest;

   Play ()
   {
      cards = new Playcard[MAX_CARDS];
      for (int i=0; i < MAX_CARDS; i++)
         cards[i] = new Playcard();
      srcId = new CardComponentId();
      destId = new CardComponentId [MAX_DEST];
      for (int i=0; i < MAX_DEST; i++)
         destId[i] = new CardComponentId();
      subcommands = new MMsg();
      Clear(MAX_CARDS);
   }

   void Clear ()
   {
      Clear(numCards);
   }

   void AddCard (int suit, int rank)
   {
      cards[numCards].suit = suit;
      cards[numCards].rank = rank;
      numCards++;
      //todo: error checking
   }

   void addDest (CardComponentId id)
   {
      destId[numDest].set (id);
      numDest++;
   }

   void addSubcommand(byte cmd)
   {
      MMsg.addField(subcommands, cmd);
   }

   void addSubcommand(byte [] cmd)
   {
      MMsg.addField(subcommands, cmd);
   }

   private void Clear (int range)
   {
      for (int i=0; i < range; i++)
         cards[i].Clear();
      origin = null;
      srcId.set ((byte)0,(byte)0,(byte)0);
      for (int i=0; i < numDest; i++)
         destId[i].set ((byte)0,(byte)0,(byte)0);
      MMsg.clear(subcommands);
      numCards = 0;
      numDest = 0;
      type = 0;
      selectedDest = 0;
   }

   public void selectDest (int index)
   {
      if (index > 0 && index < numDest)
         selectedDest = index;
   }

   public CardCommand CreateCommand ()
   {
      MMsg cmd_msg = new MMsg();
      MMsg.addField(cmd_msg, type);
      MMsg.addField(cmd_msg, Const.Fld.SRC).addToField(cmd_msg, srcId.bytes);
      MMsg.addField(cmd_msg, Const.Fld.DEST).addToField(cmd_msg, destId[selectedDest].bytes);
      if (subcommands.fieldCount > 0)
         MMsg.copyFields(cmd_msg, subcommands, 0, subcommands.fieldCount-1);

      CardCommand cc = new CardCommand(cmd_msg);
      return cc;
   }
}

class Playcard
{
   int rank, suit;

   Playcard ()
   {
      Clear ();
   }

   void Clear ()
   {
      rank = suit = -1;
   }
}
