package com.adapted.solitare;

/**
 * Created by mark on 6/8/13.
 */
public final class Const
{
   public static class Rank
   {
      static final byte TWO    = 0;
      static final byte THREE  = 1;
      static final byte FOUR   = 2;
      static final byte FIVE   = 3;
      static final byte SIX    = 4;
      static final byte SEVEN  = 5;
      static final byte EIGHT  = 6;
      static final byte NINE   = 7;
      static final byte TEN    = 8;
      static final byte JACK   = 9;
      static final byte QUEEN  = 10;
      static final byte KING   = 11;
      static final byte ACE    = 12;
   }

   public static class Suit
   {
      static final byte SPADE    = 1;
      static final byte CLUB     = 2;
      static final byte HEART    = 3;
      static final byte DIAMOND  = 4;
   }

   public static final int NUM_SUITS = 4;
   public static final int NUM_RANKS = 13;
   public static final int INT_BYTES = 4;

   public static final String resourceSuits [] =
           {
                   "spade",
                   "clb",
                   "hrt",
                   "dmd",
           };

   public static final String resourceRanks [] =
           {
                   "_2",
                   "_3",
                   "_4",
                   "_5",
                   "_6",
                   "_7",
                   "_8",
                   "_9",
                   "_10",
                   "_j",
                   "_q",
                   "_k",
                   "_a"
           };

   public static class MsgType
   {
      static final byte DEAL           = 1;
      static final byte DEAL_CARD      = 2;
      static final byte MOVE           = 3;
      static final byte GET_PARAM      = 4;
      static final byte RECYCLE_WASTE  = 5;
      static final byte MULTIMOVE      = 6;
   };

   public static class Cmd
   {
      static final byte DEAL           = 1;
      static final byte DEAL_CARD      = 2;
      static final byte MOVE           = 3;
      static final byte RECYCLE_WASTE  = 4;
      static final byte MULTIMOVE      = 5;
   };

   public static class MediatorType
   {
      static final byte CARD           = 100;
      static final byte TABLEAU        = 101;
      static final byte WASTE          = 103;
      static final byte FOUNDATION     = 104;
      static final byte STOCK          = 105;
      static final byte ROOT           = 106;
   }

   public static class Fld
   {
      static final byte SRC                              = 1;
      static final byte DEST                             = 2;
      static final byte INITIAL_DELAY                    = 3;
      static final byte ORDER                            = 4;
      static final byte STEP                             = 5;
      static final byte UNDO                             = 6;
      static final byte DELAY                            = 7;
      static final byte TERMINATE                        = 8;
      //static final byte CARD                             = 9;
      static final byte RECYCLE_CARDS                    = 10;
      static final byte CARD_POSSIBLE_PLAYS              = 11;
      static final byte CARD_POSSIBLE_MULTIPLAY          = 12;
      static final byte CARD_FLIP                        = 13;
      static final byte CARD_POSSIBLE_PLAY_OVERLAP       = 14;
      static final byte CARD_OVERLAP_PERCENT             = 15;


      static final byte S_POSITION                       = 16;
      static final byte DEAL_SEED                        = 17;


      static final byte T_NUM_DEAL_CARDS                 = 18;
      static final byte T_NUM_TABLEAU                    = 19;
      static final byte T_DEAL_INIT_DELAY                = 20;
      static final byte T_DEAL_INIT_DELAY_TAB            = 21;
      static final byte T_DEAL_INIT_DELAY_CARD           = 22;

      static final byte X_COORD                          = 23;
      static final byte Y_COORD                          = 24;
   };

   public static class InputType
   {
      static final int TOUCH = 1;
      static final int DRAG_START   = 2;
      static final int DRAG         = 3;
      static final int DRAG_END     = 4;

   }

   public static class Angle
   {
      static final float FACE_UP = 180;
      static final float FACE_DOWN = 0;

      static final int AXIS_X = 1;
      static final int AXIS_Y = 2;
      static final int AXIS_Z = 3;
   }

   public static class PlayFilterType
   {
      static final byte PFT_UNDEFINED = 0;
      static final byte PFT_TOUCH_PLAYABLE = 1;
      static final byte PFT_TOUCH = 2;
   }

   public static final CardAnimStrategyNull NullAnimStrategy = new CardAnimStrategyNull();

   public static final CardComponentId PILE_ID_STOCK = new CardComponentId(MediatorType.STOCK, (byte)0, (byte)0);
   public static final CardComponentId PILE_ID_WASTE = new CardComponentId(MediatorType.WASTE, (byte)0, (byte)0);
   public static final CardComponentId PILE_ID_TABLEAUS = new CardComponentId(MediatorType.TABLEAU, (byte)0, (byte)0);
   public static final CardComponentId PILE_ID_FOUNDATIONS = new CardComponentId(MediatorType.FOUNDATION, (byte)0, (byte)0);

   private Const () {};
}
