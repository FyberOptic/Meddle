package net.fybertech.dynamicmappings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.MeddleUtil;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;



public class DynamicClientMappings
{
	static ClassNode cn_Main = null;
	static ClassNode cn_Minecraft = null;
	static ClassNode cn_GameConfiguration = null;
	static ClassNode cn_RenderItem = null;
	static ClassNode cn_ItemModelMesher = null;
	static ClassNode cn_GuiMainMenu = null;
	static ClassNode cn_ModelResourceLocation = null;


	public static ClassNode getMainClass()
	{
		if (cn_Main != null) return cn_Main;
		cn_Main = DynamicMappings.getClassNode("net/minecraft/client/main/Main");
		return cn_Main;
	}


	public static ClassNode getMinecraftClass()
	{
		if (cn_Minecraft != null) return cn_Minecraft;

		ClassNode main = getMainClass();
		if (main == null) return null;

		List<MethodNode> methods = DynamicMappings.getMatchingMethods(main, "main", "([Ljava/lang/String;)V");
		if (methods.size() != 1) return null;
		MethodNode mainMethod = methods.get(0);

		String minecraftClassName = null;
		String gameConfigClassName = null;
		boolean confirmed = false;

		// We're looking for these instructions:
		// NEW net/minecraft/client/Minecraft
		// INVOKESPECIAL net/minecraft/client/Minecraft.<init> (Lnet/minecraft/client/main/GameConfiguration;)V
		// INVOKEVIRTUAL net/minecraft/client/Minecraft.run ()V
		for (AbstractInsnNode insn = mainMethod.instructions.getLast(); insn != null; insn = insn.getPrevious())
		{
			if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				MethodInsnNode mn = (MethodInsnNode)insn;
				minecraftClassName = mn.owner;
			}

			else if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
				MethodInsnNode mn = (MethodInsnNode)insn;

				// Check for something wrong
				if (minecraftClassName == null || !mn.owner.equals(minecraftClassName)) return null;

				Type t = Type.getMethodType(mn.desc);
				Type[] args = t.getArgumentTypes();
				if (args.length != 1) return null;

				// Get this while we're here
				gameConfigClassName = args[0].getClassName();
			}

			else if (insn.getOpcode() == Opcodes.NEW) {
				TypeInsnNode vn = (TypeInsnNode)insn;
				if (minecraftClassName != null && vn.desc.equals(minecraftClassName)) {
					confirmed = true;
					break;
				}
			}
		}

		if (confirmed) {
			cn_Minecraft = DynamicMappings.getClassNode(minecraftClassName);
			cn_GameConfiguration = DynamicMappings.getClassNode(gameConfigClassName);
		}

		return cn_Minecraft;
	}


	public static ClassNode getGameConfigurationClass() {
		if (cn_GameConfiguration != null) return cn_GameConfiguration;
		getMinecraftClass();
		return cn_GameConfiguration;
	}


	public static ClassNode getRenderItemClass() {
		if (cn_RenderItem != null) return cn_RenderItem;

		ClassNode minecraft = getMinecraftClass();
		if (minecraft == null) return null;

		for (MethodNode method : (List<MethodNode>)minecraft.methods) {
			Type t = Type.getMethodType(method.desc);
			if (t.getArgumentTypes().length != 0) continue;
			if (t.getReturnType().getSort() != Type.OBJECT) continue;

			String className = t.getReturnType().getClassName();
			if (className.contains(".")) continue;

			if (DynamicMappings.searchConstantPoolForStrings(className, "textures/misc/enchanted_item_glint.png", "Rendering item")) {
				cn_RenderItem = DynamicMappings.getClassNode(className);
				break;
			}

			// TODO - Use this to process other getters from Minecraft class

		}

		return cn_RenderItem;
	}


	public static ClassNode getItemModelMesherClass() {
		if (cn_ItemModelMesher != null) return cn_ItemModelMesher;

		ClassNode renderItem = getRenderItemClass();
		if (renderItem == null) return null;

		// Find constructor RenderItem(TextureManager, ModelManager)
		MethodNode initMethod = null;
		int count = 0;
		for (MethodNode method : (List<MethodNode>)renderItem.methods) {
			if (!method.name.equals("<init>")) continue;
			if (!DynamicMappings.checkMethodParameters(method, Type.OBJECT, Type.OBJECT)) continue;
			count++;
			initMethod = method;
		}
		if (count != 1) return null;

		Type t = Type.getMethodType(initMethod.desc);
		Type[] args = t.getArgumentTypes();
		// TODO: Get TextureManager and ModelManager from args


		String className = null;

		count = 0;
		for (AbstractInsnNode insn = initMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
			if (insn.getOpcode() == Opcodes.NEW) {
				TypeInsnNode tn = (TypeInsnNode)insn;
				className = tn.desc;
				count++;
			}
		}
		if (count != 1 || className == null) return null;

		// We'll assume this is it, might do more detailed confirmations later if necessary
		cn_ItemModelMesher = DynamicMappings.getClassNode(className);

		return cn_ItemModelMesher;
	}


	public static ClassNode getGuiMainMenuClass()
	{
		if (cn_GuiMainMenu != null) return cn_GuiMainMenu;

		ClassNode minecraft = getMinecraftClass();
		if (minecraft == null) return null;

		List<String> possibleClasses = null;

		for (MethodNode method : (List<MethodNode>)minecraft.methods) {
			//if (!DynamicMappings.checkMethodParameters(method, Type.OBJECT)) continue;
			Type t = Type.getMethodType(method.desc);
			if (t.getReturnType().getSort() != Type.VOID) continue;
			if (t.getArgumentTypes().length != 0) continue;

			boolean foundFirst = false;
			boolean foundSecond = false;
			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext())
			{
				if (!foundFirst && !DynamicMappings.isLdcWithString(insn, "LWJGL Version: ")) continue;
				foundFirst = true;
				if (!foundSecond && !DynamicMappings.isLdcWithString(insn, "Post startup")) continue;
				foundSecond = true;

				if (possibleClasses == null) possibleClasses = new ArrayList<String>();

				if (insn.getOpcode() == Opcodes.NEW) {
					TypeInsnNode tn = (TypeInsnNode)insn;
					possibleClasses.add(tn.desc);
				}
			}

			if (possibleClasses != null) break;
		}

		String guiInGame = null;
		String guiConnecting = null;
		String guiMainMenu = null;
		String loadingScreenRenderer = null;

		for (String className : possibleClasses) {

			if (guiInGame == null && DynamicMappings.searchConstantPoolForStrings(className, "textures/misc/vignette.png", "bossHealth")) {
				guiInGame = className;
				continue;
			}

			if (guiConnecting == null && DynamicMappings.searchConstantPoolForStrings(className, "Connecting to", "connect.connecting")) {
				guiConnecting = className;
				continue;
			}

			if (guiMainMenu == null && DynamicMappings.searchConstantPoolForStrings(className, "texts/splashes.txt", "Merry X-mas!")) {
				guiMainMenu = className;
				continue;
			}

			// TODO - Figure out a way to scan for the class
			//if (loadingScreenRenderer == null
		}

		// TODO - Process the rest
		//System.out.println(guiMainMenu + " " + guiInGame + " " + guiConnecting);

		if (guiMainMenu != null) cn_GuiMainMenu = DynamicMappings.getClassNode(guiMainMenu);

		return cn_GuiMainMenu;
	}


	// Gets net.minecraft.client.resources.model.ModelResourceLocation
	public static ClassNode getModelResourceLocationClass()
	{
		if (cn_ModelResourceLocation != null) return cn_ModelResourceLocation;

		ClassNode item = DynamicMappings.getItemClass();
		ClassNode itemModelMesher = getItemModelMesherClass();
		if (!MeddleUtil.notNull(item, itemModelMesher)) return null;

		for (MethodNode method : (List<MethodNode>)itemModelMesher.methods) {
			if (!DynamicMappings.checkMethodParameters(method, Type.OBJECT, Type.INT, Type.OBJECT)) continue;
			Type t = Type.getMethodType(method.desc);
			if (!t.getArgumentTypes()[0].getClassName().equals(item.name)) continue;
			cn_ModelResourceLocation = DynamicMappings.getClassNode(t.getArgumentTypes()[2].getClassName());
		}

		return cn_ModelResourceLocation;
	}




	public static void generateClassMappings()
	{
		if (!MeddleUtil.isClientJar()) return;

		Map<String, ClassNode> tempMappings = new HashMap<String, ClassNode>();

		tempMappings.put("net/minecraft/client/main/Main", getMainClass());
		tempMappings.put("net/minecraft/client/Minecraft", getMinecraftClass());
		tempMappings.put("net/minecraft/client/main/GameConfiguration", getGameConfigurationClass());
		tempMappings.put("net/minecraft/client/renderer/entity/RenderItem", getRenderItemClass());
		tempMappings.put("net/minecraft/client/renderer/ItemModelMesher", getItemModelMesherClass());
		tempMappings.put("net/minecraft/client/resources/model/ModelResourceLocation", getModelResourceLocationClass());
		tempMappings.put("net/minecraft/client/gui/GuiMainMenu", getGuiMainMenuClass());

		for (String key : tempMappings.keySet())
		{
			ClassNode node = tempMappings.get(key);
			if (node == null) Meddle.LOGGER.error("[Meddle] Couldn't resolve dynamic client mapping for " + key);
			else DynamicMappings.addClassMapping(key, node);
		}
	}



	public static void main(String[] args)
	{
		DynamicMappings.generateClassMappings();
		generateClassMappings();

		String[] sortedKeys = DynamicMappings.classMappings.keySet().toArray(new String[0]);
		Arrays.sort(sortedKeys);
		for (String key : sortedKeys) {
			String className = DynamicMappings.getClassMapping(key);
			System.out.println(key + " -> " + (className != null ? className : "[MISSING]"));
		}
	}


}

