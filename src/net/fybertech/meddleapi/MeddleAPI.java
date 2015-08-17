package net.fybertech.meddleapi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.itemtest.ItemTest;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeddleAPI
{

	public static final Logger LOGGER = LogManager.getLogger("MeddleAPI");
	public static List<String> apiMods = new ArrayList<String>();

	public static Object mainObject = null;


	public static String getVersion()
	{
		return "1.0-alpha";
	}


	public static void preInit(Object obj)
	{
		LOGGER.info("[MeddleAPI] PreInit: " + obj.getClass().getName());
		mainObject = obj;
	}

	public static void init()
	{
		LOGGER.info("[MeddleAPI] Init");

		// Note: This is currently just for testing purposes
		for (String className : apiMods) {
			try {
				Object c = Class.forName(className, true, MeddleAPI.class.getClassLoader()).newInstance();
				LOGGER.info("[MeddleAPI] Initialized mod " + c);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Minecraft.getMinecraft().refreshResources();
	}


	// Don't use this for anything yet, just for testing purposes
	public static void registerMod(String string)
	{
		apiMods.add(string);
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
