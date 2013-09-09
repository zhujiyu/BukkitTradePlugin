package me.josvth.trade.transaction.inventory.slot;

import me.josvth.trade.transaction.inventory.TransactionHolder;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public abstract class Slot {

	protected final int slot;

	public Slot(int slot) {
		this.slot = slot;
	}

	protected void setSlot(TransactionHolder holder, ItemStack stack) {
		holder.getInventory().setItem(slot, stack);
	}

	protected ItemStack getSlot(TransactionHolder holder) {
		return holder.getInventory().getItem(slot);
	}

	// Event handling
	public boolean onClick(InventoryClickEvent event) {
		return false;
	}

	public boolean onDrag(InventoryDragEvent event) {
		return false;
	}

	public void update(TransactionHolder holder) {

	}

}
