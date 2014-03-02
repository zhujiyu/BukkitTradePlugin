package me.josvth.trade.transaction.action.trader.offer;

import me.josvth.trade.transaction.Trader;
import me.josvth.trade.transaction.offer.Offer;

/**
 * Represents an abstract action in which offers of a trader were changed
 *
 * Handles updating of slots and denying standing accepts
 *
 */
public class SetOfferAction extends OfferAction {

    public SetOfferAction(Trader trader) {
        super(trader);
    }

    public void setOffer(int offerIndex, Offer offer) {
         getChanges().put(offerIndex, offer);
    }

    @Override
    public void execute() {

        if (!getChanges().isEmpty()) {
            updateOffers();
            updateSlots();
        }

        denyAccepts();

    }

}