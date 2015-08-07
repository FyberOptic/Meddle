package net.fybertech.dynamicmappings;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.MeddleUtil;


public class DynamicMappings
{
	public static final Logger LOGGER = LogManager.getLogger("Meddle");

	static ClassNode cn_World = null;
	static ClassNode cn_Blocks = null;
	static ClassNode cn_Block = null;
	static ClassNode cn_IBlockState = null;
	static ClassNode cn_IBlockAccess = null;
	static ClassNode cn_BlockPos = null;
	static ClassNode cn_ItemStack = null;
	static ClassNode cn_EntityItem = null;
	static ClassNode cn_BlockFire = null;
	static ClassNode cn_BlockLeaves = null;
	static ClassNode cn_BlockLeavesBase = null;
	static ClassNode cn_Item = null;
	static ClassNode cn_BlockChest = null;
	static ClassNode cn_BlockContainer = null;
	static ClassNode cn_ITileEntityProvider = null;
	static ClassNode cn_TileEntity = null;
	static ClassNode cn_EntityLivingBase = null;
	static ClassNode cn_EntityPlayer = null;
	static ClassNode cn_Entity = null;
	static ClassNode cn_EntityList = null;
	static ClassNode cn_EntityZombie = null;

	// This is a map of all the fields from net.minecraft.init.Blocks.
	// The key is the string used to retrieve them during class initialization.
	// You can use this to match *some* of the blocks to their respective classes.
	public static Map<String, FieldNode> blocksClassFields = null;

	// A map of entity IDs and their respective classes.
	// Obtained from parsing net.minecraft.entity.EntityList.
	//
	// Note: Minecarts currently aren't here since names weren't used.
	public static Map<String, String> entityListClasses = null;

	// The deobfuscated -> obfuscated name.
	public static final Map<String, String> classMappings = new HashMap<String, String>();
	public static final Map<String, String> methodMappings = new HashMap<String, String>();

	// Used by getClassNode to avoid reloading the classes over and over
	private static Map<String, ClassNode> cachedClassNodes = new HashMap<String, ClassNode>();





	// Load a ClassNode from class name.  This is for loading the original
	// obfuscated classes.
	//
	// Note: *Do not* edit classes you get from this.  They're cached and used by
	// anyone doing analysis of vanilla class files.
	public static ClassNode getClassNode(String className)
	{
		className = className.replace(".", "/");
		if (cachedClassNodes.containsKey(className)) return cachedClassNodes.get(className);

		//InputStream stream = Launch.classLoader.getResourceAsStream(className + ".class");
		InputStream stream = DynamicMappings.class.getClassLoader().getResourceAsStream(className + ".class");
		if (stream == null) return null;

		ClassReader reader = null;
		try {
			reader = new ClassReader(stream);
		} catch (IOException e) { return null; }

		ClassNode cn = new ClassNode();
		reader.accept(cn, 0);

		cachedClassNodes.put(className, cn);

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


	// Get constant pool class that an LDC is referencing
	public static String getLdcClass(AbstractInsnNode node)
	{
		if (!(node instanceof LdcInsnNode)) return null;
		LdcInsnNode ldc = (LdcInsnNode)node;
		if (!(ldc.cst instanceof Type)) return null;
		return ((Type)ldc.cst).getClassName();
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


	// Search a class's constant pool for the list of strings.
	// NOTE: Strings are trimmed!  Take into account when matching.
	public static boolean searchConstantPoolForStrings(String className, String... matchStrings)
	{
		className = className.replace(".", "/");
		//InputStream stream = Launch.classLoader.getResourceAsStream(className + ".class");
		InputStream stream = DynamicMappings.class.getClassLoader().getResourceAsStream(className + ".class");
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


	public static List<String> getConstantPoolStrings(String className)
	{
		List<String> strings = new ArrayList<String>();

		className = className.replace(".", "/");
		//InputStream stream = Launch.classLoader.getResourceAsStream(className + ".class");
		InputStream stream = DynamicMappings.class.getClassLoader().getResourceAsStream(className + ".class");
		if (stream == null) return null;

		ClassReader reader = null;
		try {
			reader = new ClassReader(stream);
		} catch (IOException e) { return null; }

		int itemCount = reader.getItemCount();
		char[] buffer = new char[reader.getMaxStringLength()];

		for (int n = 1; n < itemCount; n++)	{
			int pos = reader.getItem(n);
			if (pos == 0 || reader.b[pos - 1] != 8) continue;

			Arrays.fill(buffer, (char)0);
			reader.readUTF8(pos,  buffer);
			String string = (new String(buffer)).trim();

			strings.add(string);
		}

		return strings;
	}


	// Confirm the parameter types of a method.
	// Uses org.objectweb.asm.Type for values.
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


	// Finds all methods matching the specified name and/or description.
	// Both are optional.
	public static List<MethodNode> getMatchingMethods(ClassNode cn, String name, String desc)
	{
		List<MethodNode> output = new ArrayList<MethodNode>();

		for (MethodNode method : (List<MethodNode>)cn.methods) {
			if ((name == null || (name != null && method.name.equals(name))) &&
					(desc == null || (desc != null && method.desc.equals(desc)))) output.add(method);
		}

		return output;
	}





	// Gets net.minecraft.server.MinecraftServer.
	// This is the starting point of all class discovery.
	public static ClassNode getMinecraftServerClass()
	{
		return getClassNode("net/minecraft/server/MinecraftServer");
	}


	// Gets net.minecraft.world.World
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


	// Gets net.minecraft.init.Blocks
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


	// Gets net.minecraft.block.Block
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


	// Gets net.minecraft.block.state.IBlockState
	public static ClassNode getIBlockStateClass()
	{
		if (cn_IBlockState != null) return cn_IBlockState;
		//searchBlockColorClasses();

		ClassNode block = getBlockClass();
		if (block == null) return null;

		Map<String, Integer> counts = new HashMap<String, Integer>();

		// Count the times a class is used as the third parameter of a method
		for (MethodNode method : (List<MethodNode>)block.methods) {
			Type t = Type.getMethodType(method.desc);
			Type[] args = t.getArgumentTypes();
			if (args.length < 3) continue;

			String name = args[2].getClassName();
			int count = (counts.containsKey(name) ? counts.get(name) : 0);
			count++;
			counts.put(name,  count);
		}

		int max = 0;
		String maxClass = null;

		// Find the one with the most
		for (String key : counts.keySet()) {
			int count = counts.get(key);
			if (count > max) { max = count; maxClass = key; }
		}

		// It should be over 15
		if (max < 10) return null;

		cn_IBlockState = getClassNode(maxClass);

		return cn_IBlockState;
	}


	// Gets net.minecraft.world.IBlockAccess
	public static ClassNode getIBlockAccessClass()
	{
		if (cn_IBlockAccess != null) return cn_IBlockAccess;
		//searchBlockColorClasses();

		ClassNode world = getWorldClass();
		if (world == null) return null;

		if (world.interfaces.size() != 1) return null;

		cn_IBlockAccess = getClassNode(world.interfaces.get(0));

		return cn_IBlockAccess;
	}


	// Gets net.minecraft.util.BlockPos
	public static ClassNode getBlockPosClass()
	{
		if (cn_BlockPos != null) return cn_BlockPos;
		//searchBlockColorClasses();

		ClassNode block = getBlockClass();
		if (block == null) return null;

		Map<String, Integer> counts = new HashMap<String, Integer>();

		// Count the times a class is used as the second parameter of a method
		for (MethodNode method : (List<MethodNode>)block.methods) {
			Type t = Type.getMethodType(method.desc);
			Type[] args = t.getArgumentTypes();
			if (args.length < 2) continue;

			String name = args[1].getClassName();
			int count = (counts.containsKey(name) ? counts.get(name) : 0);
			count++;
			counts.put(name,  count);
		}

		int max = 0;
		String maxClass = null;

		// Find the one with the most
		for (String key : counts.keySet()) {
			int count = counts.get(key);
			if (count > max) { max = count; maxClass = key; }
		}

		// It should be over 30
		if (max < 10) return null;

		cn_BlockPos = getClassNode(maxClass);

		return cn_BlockPos;
	}


	// Gets net.minecraft.entity.item.EntityItem
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


	// Gets net.minecraft.item.ItemStack
	public static ClassNode getItemStackClass()
	{
		if (cn_ItemStack != null) return cn_ItemStack;
		getEntityItemClass();
		return cn_ItemStack;
	}


	// Parses net.minecraft.init.Blocks to generate a map of block names matched to the fields
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


	// Retrieves the associated class of a block by its name, based on the fields
	// from net.minecraft.init.Blocks.
	// Note: This may just give you the base Block class, as that's how many of the
	// fields are specified.
	public static String getBlockClassFromBlocks(String name)
	{
		discoverBlocksFields();
		if (blocksClassFields == null) return null;

		FieldNode field = blocksClassFields.get(name);
		if (field == null) return null;

		Type t = Type.getType(field.desc);
		return t.getClassName();
	}


	// Get net.minecraft.block.BlockFire
	public static ClassNode getBlockFireClass()
	{
		if (cn_BlockFire != null) return cn_BlockFire;

		String className = getBlockClassFromBlocks("fire");
		if (className == null) return null;

		if (searchConstantPoolForStrings(className, "doFireTick"))
			cn_BlockFire = getClassNode(className);

		return cn_BlockFire;
	}


	// Get net.minecraft.block.BlockLeaves
	public static ClassNode getBlockLeavesClass()
	{
		if (cn_BlockLeaves != null) return cn_BlockLeaves;

		String className = getBlockClassFromBlocks("leaves");
		if (className == null) return null;

		if (searchConstantPoolForStrings(className, "decayable"))
			cn_BlockLeaves = getClassNode(className);

		return cn_BlockLeaves;
	}


	// Get net.minecraft.block.BlockLeavesBase
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


	// Get net.minecraft.item.Item
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


	// Get net.minecraft.block.BlockChest
	public static ClassNode getBlockChestClass()
	{
		if (cn_BlockChest != null) return cn_BlockChest;

		String className = getBlockClassFromBlocks("chest");
		if (className == null) return null;

		if (!searchConstantPoolForStrings(className, "container.chestDouble")) return null;

		cn_BlockChest = getClassNode(className);
		return cn_BlockChest;
	}


	// Get net.minecraft.block.BlockContainer
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


	// Get net.minecraft.block.ITileEntityProvider
	public static ClassNode getITileEntityProviderClass()
	{
		if (cn_ITileEntityProvider != null) return cn_ITileEntityProvider;
		getBlockContainerClass();
		return cn_ITileEntityProvider;
	}


	// Get net.minecraft.tileentity.TileEntity
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


	// Get net.minecraft.entity.player.EntityPlayer
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
		if (!searchConstantPoolForStrings(className, "Inventory", "Notch")) return null;

		cn_EntityPlayer = getClassNode(className);
		return cn_EntityPlayer;
	}


	// Gets net.minecraft.entity.EntityLivingBase
	public static ClassNode getEntityLivingBaseClass()
	{
		if (cn_EntityLivingBase != null) return cn_EntityLivingBase;

		ClassNode entityPlayerClass = getEntityPlayerClass();
		if (entityPlayerClass == null || entityPlayerClass.superName == null) return null;

		if (!searchConstantPoolForStrings(entityPlayerClass.superName, "Health", "doMobLoot", "ai")) return null;

		cn_EntityLivingBase = getClassNode(entityPlayerClass.superName);
		return cn_EntityLivingBase;
	}


	// Gets net.minecraft.entity.Entity
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


	// Gets net.minecraft.entity.EntityList
	public static ClassNode getEntityListClass()
	{
		if (cn_EntityList != null) return cn_EntityList;

		ClassNode entity = getEntityClass();
		if (entity == null) return null;

		String className = null;

		for (MethodNode method : (List<MethodNode>)entity.methods)
		{
			if (!method.desc.equals("(I)V")) continue;

			boolean foundFirst = false;
			boolean foundSecond = false;

			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
				if (!foundFirst && !isLdcWithString(insn, "changeDimension")) continue;
				else foundFirst = true;

				if (!foundSecond && !isLdcWithString(insn, "reloading")) continue;
				else foundSecond = true;

				if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
					MethodInsnNode mn = (MethodInsnNode)insn;
					if (className == null) { className = mn.owner; continue; }
					else if (!className.equals(mn.owner)) return null;
				}

				if (className != null) break;
			}
			if (className != null) break;
		}

		if (!searchConstantPoolForStrings(className, "ThrownPotion", "EnderDragon")) return null;

		cn_EntityList = getClassNode(className);

		return cn_EntityList;
	}



	// Parses net.minecraft.entity.EntityList to match entity names to
	// their associated classes.
	// Note: Doesn't handle minecarts, as those aren't registered directly with names.
	public static void parseEntityList()
	{
		if (entityListClasses != null) return;

		entityListClasses = new HashMap<String, String>();

		ClassNode entityList = getEntityListClass();
		if (entityList == null) return;

		List<MethodNode> methods = getMatchingMethods(entityList, "<clinit>", "()V");
		if (methods.size() != 1) return;

		String entityClass = null;
		String entityName = null;

		for (AbstractInsnNode insn = methods.get(0).instructions.getFirst(); insn != null; insn = insn.getNext())
		{
			entityClass = getLdcClass(insn);
			if (entityClass == null) continue;

			insn = insn.getNext();
			if (insn == null) break;

			entityName = getLdcString(insn);
			if (entityName == null) continue;

			entityListClasses.put(entityName, entityClass);
		}
	}


	// Gets net.minecraft.entity.monster.EntityZombie
	public static ClassNode getEntityZombieClass()
	{
		if (cn_EntityZombie != null) return cn_EntityZombie;
		parseEntityList();

		String className = entityListClasses.get("Zombie");
		if (className == null) return null;

		if (!searchConstantPoolForStrings(className,  "zombie.spawnReinforcements", "IsBaby")) return null;

		cn_EntityZombie = getClassNode(className);
		return cn_EntityZombie;
	}



	public static void generateClassMappings()
	{
		Map<String, ClassNode> tempMappings = new HashMap<String, ClassNode>();

		tempMappings.put("net/minecraft/block/Block", getBlockClass());
		tempMappings.put("net/minecraft/block/BlockChest", getBlockChestClass());
		tempMappings.put("net/minecraft/block/BlockContainer", getBlockContainerClass());
		tempMappings.put("net/minecraft/block/BlockFire", getBlockFireClass());
		tempMappings.put("net/minecraft/block/BlockLeaves", getBlockLeavesClass());
		tempMappings.put("net/minecraft/block/BlockLeavesBase", getBlockLeavesBaseClass());
		tempMappings.put("net/minecraft/block/ITileEntityProvider", getITileEntityProviderClass());
		tempMappings.put("net/minecraft/block/state/IBlockState", getIBlockStateClass());
		tempMappings.put("net/minecraft/entity/Entity", getEntityClass());
		tempMappings.put("net/minecraft/entity/EntityList", getEntityListClass());
		tempMappings.put("net/minecraft/entity/EntityLivingBase", getEntityLivingBaseClass());
		tempMappings.put("net/minecraft/entity/item/EntityItem", getEntityItemClass());
		tempMappings.put("net/minecraft/entity/monster/EntityZombie", getEntityZombieClass());
		tempMappings.put("net/minecraft/entity/player/EntityPlayer", getEntityPlayerClass());
		tempMappings.put("net/minecraft/init/Blocks", getBlocksClass());
		tempMappings.put("net/minecraft/item/Item", getItemClass());
		tempMappings.put("net/minecraft/item/ItemStack", getItemStackClass());
		tempMappings.put("net/minecraft/server/MinecraftServer", getMinecraftServerClass());
		tempMappings.put("net/minecraft/tileentity/TileEntity", getTileEntityClass());
		tempMappings.put("net/minecraft/util/BlockPos", getBlockPosClass());
		tempMappings.put("net/minecraft/world/IBlockAccess", getIBlockAccessClass());
		tempMappings.put("net/minecraft/world/World", getWorldClass());

		for (String key : tempMappings.keySet())
		{
			ClassNode node = tempMappings.get(key);
			if (node == null) Meddle.LOGGER.error("[Meddle] Couldn't resolve dynamic mapping for " + key);
			else addClassMapping(key, node);
		}


		generateMethodMappings();
	}


	// Use untyped list in case someone compiles without debug version of ASM.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<MethodNode> getMethodsWithDescriptor(List methods, String desc)
	{
		List<MethodNode> list = new ArrayList<MethodNode>();
		for (MethodNode method : (List<MethodNode>)methods) {
			if (method.desc.equals(desc)) list.add(method);
		}
		return list;
	}

	public static List<MethodNode> removeMethodsWithFlags(List<MethodNode> methods, int accFlags)
	{
		List<MethodNode> outList = new ArrayList<MethodNode>();
		for (MethodNode mn : methods) {
			if ((mn.access & accFlags) == 0) outList.add(mn);
		}
		return outList;
	}

	public static List<MethodNode> removeMethodsWithoutFlags(List<MethodNode> methods, int accFlags)
	{
		List<MethodNode> outList = new ArrayList<MethodNode>();
		for (MethodNode mn : methods) {
			if ((mn.access & accFlags) != 0) outList.add(mn);
		}
		return outList;
	}


	//public Item setUnlocalizedName(String unlocalizedName)
	public static void getItemClassMethods(Map<String, String> map)
	{
		ClassNode item = getItemClass();
		ClassNode block = getBlockClass();
		ClassNode itemStack = getItemStackClass();
		if (!MeddleUtil.notNull(item, block, itemStack)) return;

		// Keep a local list because we'll remove them as we go to improve detection
		List<MethodNode> itemMethods = new ArrayList<MethodNode>();
		itemMethods.addAll(item.methods);

		List<MethodNode> methods;
		String noLocate = "[Meddle] Couldn't locate method net.minecraft.item.Item.";


		//public static int getIdFromItem(Item itemIn)
		methods = getMethodsWithDescriptor(itemMethods, "(L" + item.name + ";)I");
		methods = removeMethodsWithoutFlags(methods, Opcodes.ACC_STATIC);
		//for (MethodNode mn : methods) System.out.println(mn.name + " " + mn.desc);
		if (methods.size() == 1) {
			MethodNode mn = methods.get(0);
			itemMethods.remove(mn);
			map.put("net/minecraft/item/Item getIdFromItem (Lnet/minecraft/item/Item;)I",
					item.name + " " + mn.name + " " + mn.desc);
		}
		else LOGGER.error(noLocate + "getIdFromItem");


		//public static Item getItemById(int id)
		methods = getMethodsWithDescriptor(itemMethods, "(I)L" + item.name + ";");
		methods = removeMethodsWithoutFlags(methods, Opcodes.ACC_STATIC);
		if (methods.size() == 1) {
			MethodNode mn = methods.get(0);
			itemMethods.remove(mn);
			map.put("net/minecraft/item/Item getItemById (I)Lnet/minecraft/item/Item;",
					item.name + " " + mn.name + " " + mn.desc);
		}
		else LOGGER.error(noLocate + "getItemById");


		//public static Item getItemFromBlock(Block blockIn)
		methods = getMethodsWithDescriptor(itemMethods, "(L" + block.name + ";)L" + item.name + ";");
		methods = removeMethodsWithoutFlags(methods, Opcodes.ACC_STATIC);
		if (methods.size() == 1) {
			MethodNode mn = methods.get(0);
			itemMethods.remove(mn);
			map.put("net/minecraft/item/Item getItemFromBlock (Lnet/minecraft/block/Block;)Lnet/minecraft/item/Item;",
					item.name + " " + mn.name + " " + mn.desc);
		}
		else LOGGER.error(noLocate + "getItemFromBlock");


		//public static Item getByNameOrId(String id)
		methods = getMethodsWithDescriptor(itemMethods, "(Ljava/lang/String;)L" + item.name + ";");
		methods = removeMethodsWithoutFlags(methods, Opcodes.ACC_STATIC);
		if (methods.size() == 1) {
			MethodNode mn = methods.get(0);
			itemMethods.remove(mn);
			map.put("net/minecraft/item/Item getByNameOrId (Ljava/lang/String;)Lnet/minecraft/item/Item;",
					item.name + " " + mn.name + " " + mn.desc);
		}
		else LOGGER.error(noLocate + "getByNameOrId");


		// Item setUnlocalizedName(String)
		methods = getMethodsWithDescriptor(itemMethods, "(Ljava/lang/String;)L" + item.name + ";");
		methods = removeMethodsWithFlags(methods, Opcodes.ACC_STATIC);
		if (methods.size() == 1) {
			MethodNode mn = methods.get(0);
			itemMethods.remove(mn);
			map.put("net/minecraft/item/Item setUnlocalizedName (Ljava/lang/String;)Lnet/minecraft/item/Item;",
					item.name + " " + mn.name + " " + mn.desc);
		}
		else LOGGER.error(noLocate + "setUnlocalizedName");


		// public int getColorFromItemStack(ItemStack, int)
		methods = getMethodsWithDescriptor(item.methods, "(L" + itemStack.name + ";I)I");
		methods = removeMethodsWithFlags(methods, Opcodes.ACC_STATIC);
		if (methods.size() == 1) {
			MethodNode mn = methods.get(0);
			itemMethods.remove(mn);
			map.put("net/minecraft/item/Item getColorFromItemStack (Lnet/minecraft/item/ItemStack;I)I",
					item.name + " " + mn.name + " " + mn.desc);
		}
		else LOGGER.error(noLocate + "getColorFromItemStack");





	}



	public static void generateMethodMappings()
	{
		Map<String, String> tempMappings = new HashMap<String, String>();

		getItemClassMethods(tempMappings);


		for (String key : tempMappings.keySet())
		{
			String mapping = tempMappings.get(key);
			if (mapping == null) Meddle.LOGGER.error("[Meddle] Couldn't resolve dynamic method mapping for " + key);
			else addMethodMapping(key, mapping);

			//System.out.println("Method: " + key + " -> " + mapping);
		}
	}


	public static String getClassMapping(String deobfClassName)
	{
		return classMappings.get(deobfClassName.replace(".",  "/"));
	}


	public static void addClassMapping(String deobfClassName, ClassNode node)
	{
		if (deobfClassName == null) return;
		addClassMapping(deobfClassName, node.name);
	}

	public static void addClassMapping(String deobfClassName, String obfClassName)
	{
		classMappings.put(deobfClassName, obfClassName);
	}





	public static String getMethodMapping(String className, String methodName, String methodDesc)
	{
		return methodMappings.get(className + " " + methodName + " " + methodDesc);
	}

	public static String getMethodMappingName(String className, String methodName, String methodDesc)
	{
		String mapping = getMethodMapping(className, methodName, methodDesc);
		if (mapping == null) return null;
		String [] split = mapping.split(" ");
		return split.length >= 3 ? split[1] : null;
	}

	// Both in the format of "classname methodname methoddesc"
	public static void addMethodMapping(String deobfMethodDesc, String obfMethodDesc)
	{
		methodMappings.put(deobfMethodDesc, obfMethodDesc);
	}

}

