package com.adapted.solitare;

/**
 * Created by mcass on 10/23/13.
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
class Play
{
   static final int MAX_CARDS = 52;
   int type;
   PlayerClient origin;
   CardComponentId srcId;
   CardComponentId destId;
   int cardSuit [];
   int cardRank [];
   int numCards;
   int specialInstruction;

   Play ()
   {
      cardRank = new int [MAX_CARDS];
      cardSuit = new int [MAX_CARDS];
      srcId = new CardComponentId();
      destId = new CardComponentId();
      Clear(MAX_CARDS);
   }

   void Clear ()
   {
      Clear(numCards);
   }

   void AddCard (int suit, int rank)
   {
      cardSuit[numCards] = suit;
      cardRank[numCards] = rank;
      numCards++;
      //todo: error checking
   }

   void AddSpecialInstruction (int instruction)
   {
      specialInstruction = instruction;
   }

   private void Clear (int range)
   {
      for (int i=0; i < range; i++)
      {
         cardRank[i] = cardSuit[i] = -1;
      }
      origin = null;
      srcId.set ((byte)0,(byte)0,(byte)0);
      destId.set ((byte)0,(byte)0,(byte)0);
      numCards = 0;
      specialInstruction = 0;
      type = 0;
   }
}

//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
//======================================================================================================================

class Playlist
{
   static final int MAX_PLAYS = 26;
   Play plays [];
   int numPlays;

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

   void AddPlay (PlayerClient origin, int type, CardComponentId srcId, CardComponentId destId)
   {
      numPlays = 0;
      if (numPlays < MAX_PLAYS)
      {
         plays[numPlays].origin = origin;
         plays[numPlays].srcId.set(srcId);
         plays[numPlays].destId.set(destId);
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

   void SetSpecialInstruction (int instruction)
   {
      if (numPlays > 0 && numPlays <= MAX_PLAYS)
      {
         plays[numPlays-1].AddSpecialInstruction(instruction);
      }
   }

   void SetType (int type)
   {
      if (numPlays > 0 && numPlays <= MAX_PLAYS)
      {
         plays[numPlays-1].AddSpecialInstruction(type);
      }
   }

   private void Clear (int range)
   {
      for (int i=0; i < range; i++)
      {
         plays[i].Clear();
      }
      numPlays = 0;
   }
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
