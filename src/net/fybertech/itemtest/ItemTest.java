package net.fybertech.itemtest;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ItemUseResult;
import net.minecraft.util.MainOrOffHand;
import net.minecraft.util.ObjectActionHolder;
import net.minecraft.world.World;

public class ItemTest extends Item 
{

	public ItemTest()
	{
		this.setUnlocalizedName("testItem");
		this.setMaxStackSize(1);
		this.setMaxDamage(0);
		this.setCreativeTab(CreativeTabs.tabTools);
	}
	
	@Override
	public ItemUseResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, MainOrOffHand hand,
									EnumFacing facing, float x, float y, float z) 
	{		
		System.out.println("onItemUse " + x + " " + y + " " + z + " " + world.isRemote);		
		//return ItemUseResult.a;
		
		return super.onItemUse(stack,  player,  world,  pos,  hand,  facing,  x,  y,  z);
	}
	
	@Override
	public ObjectActionHolder<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, MainOrOffHand hand) 
	{	
		player.displayGUIChest(new InventoryBasic("Backpack", true, 9 * 3));
		/*player.displayGUIChest(new IInventory() {

			  // Method descriptor #9 ()I
			  // getSizeInventory
			  public int n_() { return 9 * 3; }
			  
			  // Method descriptor #11 (I)Lnet/minecraft/item/ItemStack;
			  // getStackInSlot
			  public net.minecraft.item.ItemStack a(int arg0) { return null; }
			  
			  // Method descriptor #12 (II)Lnet/minecraft/item/ItemStack;
			  // decrStackSize
			  public  net.minecraft.item.ItemStack a(int arg0, int arg1) { return null; }
			  
			  // Method descriptor #11 (I)Lnet/minecraft/item/ItemStack;
			  // getStackInSlotOnClosing
			  public  net.minecraft.item.ItemStack b(int arg0) { return null; }
			  
			  // Method descriptor #14 (ILnet/minecraft/item/ItemStack;)V
			  // setInventorySlotContents
			  public  void a(int arg0, net.minecraft.item.ItemStack arg1) {}
			  
			  // Method descriptor #9 ()I
			  // getInventoryStackLimit
			  public  int p_() { return 64; }
			  
			  // Method descriptor #17 ()V
			  // markDirty
			  public  void o_() {}
			  
			  // Method descriptor #18 (Lnet/minecraft/entity/player/EntityPlayer;)Z
			  // isUseableByPlayer
			  public  boolean a(net.minecraft.entity.player.EntityPlayer arg0) { return true; }
			  
			  // Method descriptor #19 (Lnet/minecraft/entity/player/EntityPlayer;)V
			  // openInventory
			  public  void b(net.minecraft.entity.player.EntityPlayer arg0) {}
			  
			  // Method descriptor #19 (Lnet/minecraft/entity/player/EntityPlayer;)V
			  // closeInventory
			  public  void c(net.minecraft.entity.player.EntityPlayer arg0) {}
			  
			  // Method descriptor #21 (ILnet/minecraft/item/ItemStack;)Z
			  // isItemValidForSlot
			  public  boolean b(int arg0, net.minecraft.item.ItemStack arg1) { return true; }
			  
			  // Method descriptor #22 (I)I
			  // getField
			  public  int c(int arg0) { return 0; }
			  
			  // Method descriptor #23 (II)V
			  // setField
			  public  void b(int arg0, int arg1) {}
			  
			  // Method descriptor #9 ()I
			  // getFieldCount
			  public  int g() { return 0; }
			  
			  // Method descriptor #17 ()V
			  // clear
			  public  void l() {}

			@Override
			public IChatComponent getDisplayName() {
				return new ChatComponentText("Backpack");
			}

			@Override
			public String getName() { return "Backpack"; }

			@Override
			public boolean hasCustomName() {
				return false;
			}

			
			
		});*/
		System.out.println("onRightClick " + world.isRemote);
		return new ObjectActionHolder<ItemStack>(ItemUseResult.a, stack);
	}
	
	@Override
	public int getColorFromItemStack(ItemStack stack, int renderPass)
    {
        return 0x955E3B;
    }
	
}
