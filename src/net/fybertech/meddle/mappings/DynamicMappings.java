package net.fybertech.meddle.mappings;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;











import org.objectweb.asm.tree.TypeInsnNode;

import net.fybertech.meddle.Meddle;
import net.minecraft.launchwrapper.Launch;

public class DynamicMappings
{
	private static ClassNode cn_World = null;
	private static ClassNode cn_Blocks = null;
	private static ClassNode cn_Block = null;
	private static ClassNode cn_IBlockState = null;
	private static ClassNode cn_IBlockAccess = null;
	private static ClassNode cn_BlockPos = null;
	private static ClassNode cn_ItemStack = null;
	private static ClassNode cn_EntityItem = null;
	private static ClassNode cn_BlockFire = null;
	private static ClassNode cn_BlockLeaves = null;
	private static ClassNode cn_BlockLeavesBase = null;
	private static ClassNode cn_Item = null;
	private static ClassNode cn_BlockChest = null;
	private static ClassNode cn_BlockContainer = null;
	private static ClassNode cn_ITileEntityProvider = null;
	private static ClassNode cn_TileEntity = null;
	private static ClassNode cn_EntityLivingBase = null;
	private static ClassNode cn_EntityPlayer = null;
	private static ClassNode cn_Entity = null;

	private static Map<String, FieldNode> blocksClassFields = null;

	private static Map<String, ClassNode> classNodes = new HashMap<String, ClassNode>();
	private static Map<String, ClassNode> classMappings;


	// Load ClassNode from class name
	public static ClassNode getClassNode(String className)
	{
		className = className.replace(".", "/");
		if (classNodes.containsKey(className)) return classNodes.get(className);

		InputStream stream = Launch.classLoader.getResourceAsStream(className + ".class");
		if (stream == null) return null;

		ClassReader reader = null;
		try {
			reader = new ClassReader(stream);
		} catch (IOException e) { return null; }

		ClassNode cn = new ClassNode();
		reader.accept(cn, 0);

		classNodes.put(className, cn);

		return cn;
	}


	// Get constant pool string that an LDC is loading
	public static String getLdcString(AbstractInsnNode node)
	{
		if (!(node instanceof LdcInsnNode)) return null;
		LdcInsnNode ldc = (LdcInsnNode)node;
		if (!(ldc.cst instanceof String)) return null;
		return new String((String)ldc.cst);
	}


	// Check if LDC is loading the specified string
	public static boolean isLdcWithString(AbstractInsnNode node, String string)
	{
		String s = getLdcString(node);
		return (s != null && string.equals(s));
	}


	// Check if LDC is loading specified int
	public static boolean isLdcWithInteger(AbstractInsnNode node, int val)
	{
		if (!(node instanceof LdcInsnNode)) return false;
		LdcInsnNode ldc = (LdcInsnNode)node;
		if (!(ldc.cst instanceof Integer)) return false;
		return ((Integer)ldc.cst) == val;
	}


	// Get the description of the specified field from the class
	public static String getFieldDesc(ClassNode cn, String fieldName)
	{
		for (FieldNode field : (List<FieldNode>)cn.fields)
		{
			if (field.name.equals(fieldName)) return field.desc;
		}
		return null;
	}


	// Get the specified field node from the class
	public static FieldNode getFieldByName(ClassNode cn, String fieldName)
	{
		for (FieldNode field : (List<FieldNode>)cn.fields)
		{
			if (field.name.equals(fieldName)) return field;
		}
		return null;
	}


	// Search a class's constant pool for the list of strings
	public static boolean searchConstantPoolForStrings(String className, String... matchStrings)
	{
		className = className.replace(".", "/");
		InputStream stream = Launch.classLoader.getResourceAsStream(className + ".class");
		if (stream == null) return false;

		ClassReader reader = null;
		try {
			reader = new ClassReader(stream);
		} catch (IOException e) { return false; }

		int itemCount = reader.getItemCount();
		char[] buffer = new char[reader.getMaxStringLength()];

		int matches = 0;

		for (int n = 1; n < itemCount; n++)	{
			int pos = reader.getItem(n);
			if (pos == 0 || reader.b[pos - 1] != 8) continue;

			Arrays.fill(buffer, (char)0);
			reader.readUTF8(pos,  buffer);
			String string = (new String(buffer)).trim();

			for (int n2 = 0; n2 < matchStrings.length; n2++) {
				if (string.equals(matchStrings[n2])) { matches++; break; }
			}
		}

		return (matches == matchStrings.length);
	}


	public static boolean checkMethodParameters(MethodNode method, int ... types)
	{
		Type t = Type.getMethodType(method.desc);
		Type[] args = t.getArgumentTypes();
		if (args.length != types.length) return false;

		int len = args.length;
		for (int n = 0; n < len; n++) {
			if (args[n].getSort() != types[n]) return false;
		}

		return true;
	}






	public static ClassNode getMinecraftServerClass()
	{
		return getClassNode("net/minecraft/server/MinecraftServer");
	}


	public static ClassNode getWorldClass()
	{
		if (cn_World != null) return cn_World;

		ClassNode server = getMinecraftServerClass();
		if (server == null) return null;

		List<String> potentialClasses = new ArrayList<String>();

		// Fetch all obfuscated classes used inside MinecraftServer's interfaces
		for (String interfaceClass : (List<String>)server.interfaces) {
			if (interfaceClass.contains("/")) continue;

			ClassNode node = getClassNode(interfaceClass);
			if (node == null) continue; // TODO - Error message

			for (MethodNode method : node.methods) {
				Type returnType = Type.getReturnType(method.desc);

				if (returnType.getSort() == Type.OBJECT) {
					if (!returnType.getClassName().contains(".")) potentialClasses.add(returnType.getClassName());
				}
			}
		}

		String[] matchStrings = new String[] { "Getting biome", "chunkCheck", "Level name", "Chunk stats" };

		// Search constant pools of potential classes for strings matching the list above
		for (String className : potentialClasses)
		{
			if (searchConstantPoolForStrings(className, matchStrings))
			{
				cn_World = getClassNode(className);
				return cn_World;
			}
		}

		return null;
	}


	public static ClassNode getBlocksClass()
	{
		if (cn_Blocks != null) return cn_Blocks;

		ClassNode worldClass = getWorldClass();
		if (worldClass == null) return null;

		Set<String> potentialClasses = new HashSet<String>();

		for (MethodNode method : (List<MethodNode>)worldClass.methods) {
			for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext()) {
				if (node.getOpcode() != Opcodes.GETSTATIC) continue;
				FieldInsnNode fieldNode = (FieldInsnNode)node;

				if (!fieldNode.desc.startsWith("L") || fieldNode.desc.contains("/")) continue;
				String descClass = Type.getType(fieldNode.desc).getClassName();
				if (fieldNode.owner.equals(descClass)) continue;

				potentialClasses.add(fieldNode.owner);
			}
		}

		String[] matchStrings = new String[] { "Accessed Blocks before Bootstrap!", "air", "stone" };

		for (String className : potentialClasses) {
			if (searchConstantPoolForStrings(className, matchStrings))	{
				cn_Blocks = getClassNode(className);
				return cn_Blocks;
			}
		}

		return null;
	}


	public static ClassNode getBlockClass()
	{
		if (cn_Block != null) return cn_Block;

		ClassNode blocksClass = getBlocksClass();
		if (blocksClass == null) return null;

		Map<String, Integer> classes = new HashMap<String, Integer>();

		for (FieldNode field : (List<FieldNode>)blocksClass.fields) {
			String descClass = Type.getType(field.desc).getClassName();
			int val = (classes.containsKey(descClass) ? classes.get(descClass) : 0);
			val++;
			classes.put(descClass,  val);
		}

		String mostClass = null;
		int mostCount = 0;

		for (String key : classes.keySet()) {
			if (classes.get(key) > mostCount) { mostClass = key; mostCount = classes.get(key); }
		}

		if (mostCount > 100) {
			cn_Block = getClassNode(mostClass);
			return cn_Block;
		}

		return null;

	}


	private static boolean searchedColorClasses = false;
	private static void searchBlockColorClasses()
	{
		if (searchedColorClasses) return;
		searchedColorClasses = true;

		ClassNode blockClass = getBlockClass();
		if (blockClass == null) return;

		// Look for getBlockColor(), getRenderColor(IBlockState), and colorMultiplier(IBlockAccess, BlockPos, int)
		for (MethodNode method : (List<MethodNode>)blockClass.methods) {
			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
				if (!isLdcWithInteger(insn, 16777215)) continue;

				Type methodType = Type.getMethodType(method.desc);
				Type[] types = methodType.getArgumentTypes();

				for (int n = 0; n < types.length; n++) {
					Type t = types[n];
					if (t.getSort() != Type.OBJECT) continue;

					// If there's only one arg, it's getRenderColor(IBlockState)
					if (types.length == 1) cn_IBlockState = getClassNode(t.getClassName());
					// If three, it's colorMultiplier(IBlockAccess, BlockPos, int)
					if (types.length == 3 && n == 0) cn_IBlockAccess = getClassNode(t.getClassName());
					if (types.length == 3 && n == 1) cn_BlockPos = getClassNode(t.getClassName());
				}
			}
		}
	}


	public static ClassNode getIBlockStateClass()
	{
		if (cn_IBlockState != null) return cn_IBlockState;
		searchBlockColorClasses();
		return cn_IBlockState;
	}


	public static ClassNode getIBlockAccessClass()
	{
		if (cn_IBlockAccess != null) return cn_IBlockAccess;
		searchBlockColorClasses();
		return cn_IBlockAccess;
	}


	public static ClassNode getBlockPosClass()
	{
		if (cn_BlockPos != null) return cn_BlockPos;
		searchBlockColorClasses();
		return cn_BlockPos;
	}


	public static ClassNode getEntityItemClass()
	{
		if (cn_EntityItem != null) return cn_EntityItem;

		ClassNode worldClass = getWorldClass();
		ClassNode blockPosClass = getBlockPosClass();
		ClassNode blockClass = getBlockClass();
		if (worldClass == null || blockPosClass == null || blockClass == null) return null;

		// search for Block.spawnAsEntity(World worldIn, BlockPos pos, ItemStack stack)
		for (MethodNode method : (List<MethodNode>)blockClass.methods)
		{
			// We're looking for a static method
			if ((method.access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) continue;

			// Look for methods with three arguments
			Type methodType = Type.getMethodType(method.desc);
			Type[] arguments = methodType.getArgumentTypes();
			if (arguments.length != 3) continue;

			// Make sure all arguments are objects
			if (arguments[0].getSort() != Type.OBJECT || arguments[1].getSort() != Type.OBJECT || arguments[2].getSort() != Type.OBJECT ) continue;
			// Make sure arg0 is World and arg1 is BlockPos
			if (!arguments[0].getClassName().equals(worldClass.name) || !arguments[1].getClassName().equals(blockPosClass.name)) continue;

			boolean foundString = false;
			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext())
			{
				if (!foundString && isLdcWithString(insn, "doTileDrops")) { foundString = true; continue; }
				if (foundString && insn.getOpcode() == Opcodes.NEW)
				{
					TypeInsnNode newNode = (TypeInsnNode)insn;
					cn_EntityItem = getClassNode(newNode.desc);

					// This also means that the last arg for spawnAsEntity is ItemStack
					cn_ItemStack = getClassNode(arguments[2].getClassName());

					return cn_EntityItem;
				}
			}

		}

		return null;
	}


	public static ClassNode getItemStackClass()
	{
		if (cn_ItemStack != null) return cn_ItemStack;
		getEntityItemClass();
		return cn_ItemStack;
	}


	public static void discoverBlocksFields()
	{
		if (blocksClassFields != null) return;
		blocksClassFields = new HashMap<String, FieldNode>();

		ClassNode blocksClass = getBlocksClass();
		if (blocksClass == null) return;

		for (MethodNode method : (List<MethodNode>)blocksClass.methods)
		{
			if (!method.name.equals("<clinit>")) continue;

			String lastString = null;
			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext())
			{
				String s = getLdcString(insn);
				if (s != null) lastString = s;
				// Avoid any strings that definitely aren't block names
				if (lastString == null || lastString.contains(" ")) continue;

				if (insn.getOpcode() != Opcodes.PUTSTATIC) continue;
				FieldInsnNode fieldNode = (FieldInsnNode)insn;
				// Filter out non-objects and packaged classes, just in case
				if (!fieldNode.desc.startsWith("L") || fieldNode.desc.contains("/")) continue;

				blocksClassFields.put(lastString, getFieldByName(blocksClass, fieldNode.name));
			}
		}
	}


	public static String getBlockClassFromBlocks(String name)
	{
		discoverBlocksFields();
		if (blocksClassFields == null) return null;

		FieldNode field = blocksClassFields.get(name);
		if (field == null) return null;

		Type t = Type.getType(field.desc);
		return t.getClassName();
	}


	public static ClassNode getBlockFireClass()
	{
		if (cn_BlockFire != null) return cn_BlockFire;

		String className = getBlockClassFromBlocks("fire");
		if (className == null) return null;

		if (searchConstantPoolForStrings(className, "doFireTick"))
			cn_BlockFire = getClassNode(className);

		return cn_BlockFire;
	}


	public static ClassNode getBlockLeavesClass()
	{
		if (cn_BlockLeaves != null) return cn_BlockLeaves;

		String className = getBlockClassFromBlocks("leaves");
		if (className == null) return null;

		if (searchConstantPoolForStrings(className, "decayable"))
			cn_BlockLeaves = getClassNode(className);

		return cn_BlockLeaves;
	}


	public static ClassNode getBlockLeavesBaseClass()
	{
		if (cn_BlockLeavesBase != null) return cn_BlockLeavesBase;

		// BlockLeaves should extend BlockLeavesBase
		ClassNode blockLeaves = getBlockLeavesClass();
		if (blockLeaves == null || blockLeaves.superName == null) return null;
		ClassNode blockLeavesBase = getClassNode(blockLeaves.superName);
		if (blockLeavesBase == null || blockLeavesBase.superName == null) return null;

		// BlockLeavesBase should extend Block
		ClassNode blockClass = getBlockClass();
		if (!blockClass.name.equals(blockLeavesBase.superName)) return null;

		cn_BlockLeavesBase = blockLeavesBase;

		return cn_BlockLeavesBase;
	}


	public static ClassNode getItemClass()
	{
		if (cn_Item != null) return cn_Item;

		ClassNode blockClass = getBlockClass();
		if (blockClass == null) return null;
		ClassNode itemStackClass = getItemStackClass();
		if (itemStackClass == null) return null;

		List<String> possibleClasses = new ArrayList<String>();

		for (MethodNode method : (List<MethodNode>)itemStackClass.methods)
		{
			if (!method.name.equals("<init>")) continue;
			Type t = Type.getMethodType(method.desc);
			Type[] args = t.getArgumentTypes();
			if (args.length != 1 || args[0].getSort() != Type.OBJECT) continue;

			// Get the class of the first method parameter
			String className = args[0].getClassName();
			// One of them will be the block class, ignore it
			if (className.equals(blockClass.name)) continue;
			possibleClasses.add(className);
		}

		if (possibleClasses.size() == 1)
		{
			String className = possibleClasses.get(0);
			if (searchConstantPoolForStrings(className, "item.", "arrow"))
				cn_Item = getClassNode(possibleClasses.get(0));
		}

		return cn_Item;
	}


	public static ClassNode getBlockChestClass()
	{
		if (cn_BlockChest != null) return cn_BlockChest;

		String className = getBlockClassFromBlocks("chest");
		if (className == null) return null;

		if (!searchConstantPoolForStrings(className, "container.chestDouble")) return null;

		cn_BlockChest = getClassNode(className);
		return cn_BlockChest;
	}


	public static ClassNode getBlockContainerClass()
	{
		if (cn_BlockContainer != null) return cn_BlockContainer;

		ClassNode blockClass = getBlockClass();
		if (blockClass == null) return null;
		ClassNode blockChestClass = getBlockChestClass();
		if (blockChestClass == null || blockChestClass.superName == null) return null;

		ClassNode containerClass = getClassNode(blockChestClass.superName);
		if (!containerClass.superName.equals(blockClass.name)) return null;

		if (containerClass.interfaces.size() != 1) return null;

		cn_BlockContainer = containerClass;
		cn_ITileEntityProvider = getClassNode(containerClass.interfaces.get(0));

		return cn_BlockContainer;
	}


	public static ClassNode getITileEntityProviderClass()
	{
		if (cn_ITileEntityProvider != null) return cn_ITileEntityProvider;
		getBlockContainerClass();
		return cn_ITileEntityProvider;
	}


	public static ClassNode getTileEntityClass()
	{
		if (cn_TileEntity != null) return cn_TileEntity;

		ClassNode teProviderClass = getITileEntityProviderClass();
		if (teProviderClass == null) return null;

		if (teProviderClass.methods.size() != 1) return null;

		MethodNode m = teProviderClass.methods.get(0);

		Type t = Type.getMethodType(m.desc);
		Type returnType = t.getReturnType();
		if (returnType.getSort() != Type.OBJECT) return null;

		String teClassName = returnType.getClassName();
		if (searchConstantPoolForStrings(teClassName, "Furnace", "MobSpawner"))
			cn_TileEntity = getClassNode(teClassName);

		return cn_TileEntity;
	}


	public static ClassNode getEntityPlayerClass()
	{
		if (cn_EntityPlayer != null) return cn_EntityPlayer;

		ClassNode serverClass = getMinecraftServerClass();
		ClassNode worldClass = getWorldClass();
		ClassNode blockPosClass = getBlockPosClass();
		if (serverClass == null || worldClass == null || blockPosClass == null) return null;

		List<String> potentialClasses = new ArrayList<String>();

		// Find isBlockProtected(World, BlockPos, EntityPlayer)Z
		for (MethodNode method : (List<MethodNode>)serverClass.methods)
		{
			Type t = Type.getMethodType(method.desc);
			if (t.getReturnType().getSort() != Type.BOOLEAN) continue;

			if (!checkMethodParameters(method, Type.OBJECT, Type.OBJECT, Type.OBJECT)) continue;
			Type[] args = t.getArgumentTypes();

			if (!args[0].getClassName().equals(worldClass.name) || !args[1].getClassName().equals(blockPosClass.name)) continue;
			potentialClasses.add(args[2].getClassName());
		}

		if (potentialClasses.size() != 1) return null;
		String className = potentialClasses.get(0);
		if (!searchConstantPoolForStrings(className, "Inventory", "Notch", "game.player.die")) return null;

		cn_EntityPlayer = getClassNode(className);
		return cn_EntityPlayer;
	}


	public static ClassNode getEntityLivingBaseClass()
	{
		if (cn_EntityLivingBase != null) return cn_EntityLivingBase;

		ClassNode entityPlayerClass = getEntityPlayerClass();
		if (entityPlayerClass == null || entityPlayerClass.superName == null) return null;

		if (!searchConstantPoolForStrings(entityPlayerClass.superName, "Health", "doMobLoot", "ai")) return null;

		cn_EntityLivingBase = getClassNode(entityPlayerClass.superName);
		return cn_EntityLivingBase;
	}


	public static ClassNode getEntityClass()
	{
		if (cn_Entity != null) return cn_Entity;

		ClassNode entityLivingBase = getEntityLivingBaseClass();
		if (entityLivingBase == null || entityLivingBase.superName == null) return null;

		ClassNode entity = getClassNode(entityLivingBase.superName);
		if (!entity.superName.equals("java/lang/Object")) return null;

		cn_Entity = entity;
		return cn_Entity;
	}




	public static void generateClassMappings()
	{
		classMappings = new HashMap<String, ClassNode>();

		classMappings.put("net/minecraft/block/Block", getBlockClass());
		classMappings.put("net/minecraft/block/BlockChest", getBlockChestClass());
		classMappings.put("net/minecraft/block/BlockContainer", getBlockContainerClass());
		classMappings.put("net/minecraft/block/BlockFire", getBlockFireClass());
		classMappings.put("net/minecraft/block/BlockLeaves", getBlockLeavesClass());
		classMappings.put("net/minecraft/block/BlockLeavesBase", getBlockLeavesBaseClass());
		classMappings.put("net/minecraft/block/ITileEntityProvider", getITileEntityProviderClass());
		classMappings.put("net/minecraft/block/state/IBlockState", getIBlockStateClass());
		classMappings.put("net/minecraft/entity/Entity", getEntityClass());
		classMappings.put("net/minecraft/entity/EntityLivingBase", getEntityLivingBaseClass());
		classMappings.put("net/minecraft/entity/item/EntityItem", getEntityItemClass());
		classMappings.put("net/minecraft/entity/player/EntityPlayer", getEntityPlayerClass());
		classMappings.put("net/minecraft/init/Blocks", getBlocksClass());
		classMappings.put("net/minecraft/item/Item", getItemClass());
		classMappings.put("net/minecraft/item/ItemStack", getItemStackClass());
		classMappings.put("net/minecraft/server/MinecraftServer", getMinecraftServerClass());
		classMappings.put("net/minecraft/tileentity/TileEntity", getTileEntityClass());
		classMappings.put("net/minecraft/util/BlockPos", getBlockPosClass());
		classMappings.put("net/minecraft/world/IBlockAccess", getIBlockAccessClass());
		classMappings.put("net/minecraft/world/World", getWorldClass());

		for (String key : classMappings.keySet())
		{
			if (classMappings.get(key) == null) Meddle.LOGGER.error("[Meddle] Couldn't resolve dynamic mapping for " + key);
		}
	}


	public static String getClassMapping(String className)
	{
		if (classMappings == null) generateClassMappings();

		className = className.replace(".",  "/");
		ClassNode cn = classMappings.get(className);

		return cn != null ? cn.name : null;
	}

}
