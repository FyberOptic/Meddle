package net.fybertech.itemtest;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class InventoryBackpack extends InventoryBasic
{

	final NBTTagCompound backpackTag;


	public InventoryBackpack(String title, boolean customName, int slotCount, NBTTagCompound tag)
	{
		super(title, customName, slotCount);
		backpackTag = tag;
		this.loadInventoryFromNBT(tag.getTagList("inventory", 10));
	}


	public void loadInventoryFromNBT(NBTTagList tag)
	{
		int inventorySize = this.getSizeInventory();
		for (int n = 0; n < inventorySize; n++)
		{
			this.setInventorySlotContents(n, null);
		}

		int tagCount = tag.tagCount();
		for (int n = 0; n < tagCount; n++)
		{
			NBTTagCompound slotTag = tag.getCompoundTagAt(n);

			int slotNum = slotTag.getByte("Slot") & 255;
			if (slotNum < 0 || slotNum >= inventorySize) continue;

			this.setInventorySlotContents(slotNum, ItemStack.loadItemStackFromNBT(slotTag));
		}
	}


	public NBTTagList saveInventoryToNBT()
	{
		NBTTagList list = new NBTTagList();

		int inventorySize = this.getSizeInventory();
		for (int n = 0; n < inventorySize; n++)
		{
			ItemStack stack = this.getStackInSlot(n);
			if (stack == null) continue;

			NBTTagCompound slotTag = new NBTTagCompound();
			slotTag.setByte("Slot", (byte)n);
			stack.writeToNBT(slotTag);
			list.appendTag(slotTag);
		}

		return list;
	}


	@Override
	public void markDirty() {
		super.markDirty();

		NBTTagList inventory = this.saveInventoryToNBT();
		backpackTag.setTag("inventory", inventory);
	}

}
