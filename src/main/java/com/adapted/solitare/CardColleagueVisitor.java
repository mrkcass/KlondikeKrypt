package com.adapted.solitare;

/**
 * Created by mcass on 10/17/13.
 */
public interface CardColleagueVisitor
{
   public void visit (Tableau _pile);
   public void visit (Foundation _pile);
   public void visit (StockPile _pile);
   public void visit (WastePile _pile);
}

interface CardColleagueVistable
{
   public void AcceptVisitor (CardColleagueVisitor _visitor);
}
