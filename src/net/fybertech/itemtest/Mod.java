package net.fybertech.itemtest;

import net.fybertech.meddleapi.MeddleAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;


public class Mod
{

	ItemTest item = new ItemTest();

	public Mod()
	{
		MeddleAPI.registerItem(5000, "testItem", item);

		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, new ModelResourceLocation("testItem", "inventory"));

	}

}

