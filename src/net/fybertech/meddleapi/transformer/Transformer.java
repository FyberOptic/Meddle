package net.fybertech.meddleapi.transformer;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.minecraft.launchwrapper.IClassTransformer;

public class Transformer implements IClassTransformer 
{

	String minecraftServer = DynamicMappings.getClassMapping("net/minecraft/server/MinecraftServer");
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) 
	{	
		if (name.equals(minecraftServer)) return transformMinecraftServer(basicClass);
		return basicClass;
	}

	
	private byte[] transformMinecraftServer(byte[] basicClass)
	{
		// TODO - Hook for mod initialization		
		
		return basicClass;
	}
	
	
}
