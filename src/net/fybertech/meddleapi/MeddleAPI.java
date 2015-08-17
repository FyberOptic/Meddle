package net.fybertech.meddleapi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.itemtest.ItemTest;
import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.Meddle.ModContainer;
import net.minecraft.client.Minecraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeddleAPI
{

	public static final Logger LOGGER = LogManager.getLogger("MeddleAPI");
	public static List<Object> apiMods = new ArrayList<Object>();

	public static Object mainObject = null;


	public static String getVersion()
	{
		return "1.0-alpha";
	}


	public static void preInit(Object obj)
	{
		LOGGER.info("[MeddleAPI] PreInit: " + obj.getClass().getName());
		mainObject = obj;
		
		
		for (ModContainer meddleMod : Meddle.discoveredModsList) 
		{
			Manifest manifest = null;
			try {
				manifest = new JarFile(meddleMod.jar).getManifest();
			} catch (IOException e) {}
			if (manifest == null) continue;

			Attributes attr = manifest.getMainAttributes();
			if (attr == null) continue;

			String apiModsAttr = attr.getValue("MeddleAPI-Mods");
			if (apiModsAttr == null || apiModsAttr.length() < 1) continue;
			
			String[] mods = apiModsAttr.split(" ");
			for (String className : mods) 
			{
				try {
					LOGGER.info("[MeddleAPI] Initializing mod " + className);
					Object c = Class.forName(className, true, MeddleAPI.class.getClassLoader()).newInstance();					
					apiMods.add(c);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			
		}
		
	}

	public static void init()
	{
		LOGGER.info("[MeddleAPI] Init");

		for (Object modClass : apiMods) {		
			try {
				Method init = modClass.getClass().getMethod("init");
				init.invoke(modClass);
			} catch (Exception e) {}
		}

		Minecraft.getMinecraft().refreshResources();
	}



	private static Method registerItemMethod = null;

	public static void registerItem(int itemID, String itemName, ItemTest item)
	{
		if (registerItemMethod == null)
		{
			String itemClassName = DynamicMappings.getClassMapping("net.minecraft.item.Item");

			Class itemClass = null;
			try {
				itemClass = Class.forName(itemClassName);
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			Method[] methods = itemClass.getDeclaredMethods();
			for (Method method : methods) {
				Class[] types = method.getParameterTypes();
				if (types.length != 3) continue;
				if (types[0] != int.class) continue;
				if (types[1] != String.class) continue;
				if (types[2] != itemClass) continue;
				method.setAccessible(true);
				registerItemMethod = method;
			}
		}

		try {
			registerItemMethod.invoke(null, itemID, itemName, item);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
