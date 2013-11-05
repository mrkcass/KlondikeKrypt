package com.adapted.solitare;

/**
 * Created by mark on 6/8/13.
 */
public class RootPile extends PlayableComposite
{
   RootPile(Mediator _mediator, GraphicsInterface _graphics)
   {
      super (_mediator, _graphics);

      id = new CardComponentId(Const.MediatorType.ROOT, (byte)0, (byte)0);

      Playable cc;

      cc = new StockPile(_mediator, _graphics);
      components.add(cc);

      cc = new WastePile(_mediator, _graphics);
      components.add(cc);

      cc = new TableauPiles(_mediator, _graphics);
      components.add(cc);

      cc = new FoundationPiles(_mediator, _graphics);
      components.add(cc);
   }
}
