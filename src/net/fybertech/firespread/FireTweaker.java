package net.fybertech.firespread;

import java.io.File;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.MeddleMod;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

@MeddleMod(id="firespread", name="FireSpread", author="FyberOptic", version="1.2.1", depends={"dynamicmappings"})
public class FireTweaker implements ITweaker, IClassTransformer
{
	String blockFireClass = DynamicMappings.getClassMapping("net/minecraft/block/BlockFire");
	String worldClass = DynamicMappings.getClassMapping("net/minecraft/world/World");
	String blockPosClass = DynamicMappings.getClassMapping("net/minecraft/util/BlockPos");
	String iBlockStateClass = DynamicMappings.getClassMapping("net/minecraft/block/state/IBlockState");

	// public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
	String updateTickDesc = "(L" + worldClass + ";L" + blockPosClass + ";L" + iBlockStateClass + ";Ljava/util/Random;)V";

	// public boolean setBlockState(BlockPos pos, IBlockState newState, int flags)
	String setBlockStateDesc = "(L" + blockPosClass + ";L" + iBlockStateClass + ";I)Z";


	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (name.equals(blockFireClass)) return patchBlockFire(basicClass);
		else return basicClass;
	}


	private byte[] failGracefully(String error, byte[] bytes)
	{
		Meddle.LOGGER.error("[Meddle/FireTweaker] " + error);
		return bytes;
	}


	private byte[] patchBlockFire(byte[] basicClass)
	{
		// We need World.setBlockState as a reference later
		ClassNode world = DynamicMappings.getClassNode(worldClass);
		if (world == null) return failGracefully("Unable to find World class!", basicClass);

		int count = 0;
		MethodNode setBlockStateNode = null;
		for (MethodNode method : (List<MethodNode>)world.methods)
		{
			if (!method.desc.equals(setBlockStateDesc)) continue;
			count++;
			setBlockStateNode = method;
		}

		if (count != 1) return failGracefully("Unable to find World.setBlockState!", basicClass);

		String setBlockStateMethod = setBlockStateNode.name;



		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn, 0);

		MethodNode updateTickMethod = null;

		// Find the method we're patching
		for (MethodNode method : cn.methods)
		{
			if (!method.desc.equals(updateTickDesc)) continue;

			// Look for LDC "doFireTick"
			boolean foundMethod = false;
			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
				if (DynamicMappings.isLdcWithString(insn, "doFireTick")) { updateTickMethod = method; break; }
			}
		}

		if (updateTickMethod == null) return failGracefully("Couldn't find BlockFire.updateTick!", basicClass);


		AbstractInsnNode node = null;
		boolean foundFirst = false;

		// We've found updateTick, now to patch it
		for (node = updateTickMethod.instructions.getLast(); node != null; node = node.getPrevious())
		{
			if (!foundFirst && node instanceof MethodInsnNode)
			{
				MethodInsnNode mnode = (MethodInsnNode)node;
				if (mnode.owner.equals(worldClass) && mnode.name.equals(setBlockStateMethod) && mnode.desc.equals(setBlockStateDesc))
					foundFirst = true;
			}
			else if (foundFirst && node.getOpcode() == Opcodes.ILOAD) break;
		}

		if (node != null) {
			updateTickMethod.instructions.set(node, new InsnNode(Opcodes.ICONST_0));
			Meddle.LOGGER.info("[Meddle/FireTweaker] BlockFire.updateTick patched");
		}
		else return failGracefully("Couldn't patch BlockFire.updateTick!", basicClass);

		ClassWriter writer = new ClassWriter(Opcodes.ASM5);
		cn.accept(writer);
		return writer.toByteArray();
	}


	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {}


	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		classLoader.registerTransformer(FireTweaker.class.getName());
	}


	@Override
	public String getLaunchTarget() {
		return null;
	}


	@Override
	public String[] getLaunchArguments()
	{
		return new String[0];
	}


}
