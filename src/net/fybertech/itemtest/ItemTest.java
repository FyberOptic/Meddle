package net.fybertech.itemtest;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
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
		System.out.println("onRightClick " + world.isRemote);
		return new ObjectActionHolder<ItemStack>(ItemUseResult.a, stack);
	}
	
	@Override
	public int getColorFromItemStack(ItemStack stack, int renderPass)
    {
        return 0x955E3B;
    }
	
}
