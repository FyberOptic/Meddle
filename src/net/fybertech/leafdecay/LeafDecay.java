package net.fybertech.leafdecay;

import java.io.File;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.mappings.DynamicMappings;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class LeafDecay implements ITweaker, IClassTransformer {

	String blockLeavesBaseClass = DynamicMappings.getClassMapping("net/minecraft/block/BlockLeavesBase");
	String worldClass = DynamicMappings.getClassMapping("net/minecraft/world/World");
	String blockClass = DynamicMappings.getClassMapping("net/minecraft/block/Block");
	String blockPosClass = DynamicMappings.getClassMapping("net/minecraft/util/BlockPos");
	String iBlockStateClass = DynamicMappings.getClassMapping("net/minecraft/block/state/IBlockState");

	// public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
	String neighborChangeDesc = "(L" + worldClass + ";L" + blockPosClass + ";L" + iBlockStateClass + ";L" + blockClass + ";)V";

	// public void scheduleUpdate(BlockPos pos, Block blockIn, int delay)
	String scheduleBlockUpdateDesc = "(L" + blockPosClass + ";L" + blockClass + ";I)V";



	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (transformedName.equals(blockLeavesBaseClass)) return patchLeaveClass(basicClass);
		else return basicClass;
	}


	private byte[] failGracefully(String error, byte[] bytes)
	{
		Meddle.LOGGER.error("[Meddle/LeafDecay] " + error);
		return bytes;
	}


	private byte[] patchLeaveClass(byte[] basicClass)
	{
		// First we need to find World.scheduleBlockUpdate
		ClassNode world = DynamicMappings.getClassNode(worldClass);
		if (world == null) return failGracefully("Couldn't find World class!", basicClass);

		int count = 0;
		MethodNode scheduleBlockUpdateNode = null;
		for (MethodNode method : (List<MethodNode>)world.methods) {
			if (!method.desc.equals(scheduleBlockUpdateDesc)) continue;
			count++;
			scheduleBlockUpdateNode = method;
		}
		if (count != 1 || scheduleBlockUpdateNode == null)
			return failGracefully("Couldn't find World.scheduleBlockUpdate!", basicClass);


		// Now we need Block.onNeighborBlockChange
		ClassNode block = DynamicMappings.getClassNode(blockClass);
		if (block == null) return failGracefully("Couldn't find Block class!", basicClass);

		count = 0;
		MethodNode neighborChangeNode = null;
		for (MethodNode method : (List<MethodNode>)block.methods) {
			if (!method.desc.equals(neighborChangeDesc)) continue;
			count++;
			neighborChangeNode = method;
		}
		if (count != 1 || neighborChangeNode == null)
			failGracefully("Couldn't find Block.onNeighborChange!", basicClass);


		// Now we can generate the method
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(basicClass);
		classReader.accept(classNode, 0);

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);

		writer.visitField(Opcodes.ACC_PRIVATE, "leafRand", "Ljava/util/Random;", null, null);

		MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC, neighborChangeNode.name, neighborChangeDesc, null, null);
		mv.visitCode();

		// if (rand == null) rand = new Random();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, blockLeavesBaseClass, "leafRand", "Ljava/util/Random;");
		Label l1 = new Label();
		mv.visitJumpInsn(Opcodes.IFNONNULL, l1);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitTypeInsn(Opcodes.NEW, "java/util/Random");
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/Random", "<init>", "()V", false);
		mv.visitFieldInsn(Opcodes.PUTFIELD, blockLeavesBaseClass, "leafRand", "Ljava/util/Random;");
		mv.visitLabel(l1);

		// World, BlockPos, Block
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitVarInsn(Opcodes.ALOAD, 0);

		// int delay = leafRand.nextInt(5)
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, blockLeavesBaseClass, "leafRand", "Ljava/util/Random;");
		mv.visitInsn(Opcodes.ICONST_5);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Random", "nextInt", "(I)I", false);

		// delay += 5
		mv.visitInsn(Opcodes.ICONST_5);
		mv.visitInsn(Opcodes.IADD);

		// public void scheduleBlockUpdate(BlockPos pos, Block blockIn, int delay, int priority)
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, worldClass, scheduleBlockUpdateNode.name, scheduleBlockUpdateDesc, false);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(5, 0);
		mv.visitEnd();

		Meddle.LOGGER.info("[Meddle/LeafDecay] BlockLeavesBase.onNeighborBlockChange patched");

		return writer.toByteArray();
	}


	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {}


	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		classLoader.registerTransformer(LeafDecay.class.getName());
	}


	@Override
	public String getLaunchTarget() {
		return null;
	}


	@Override
	public String[] getLaunchArguments() {
		return new String[0];
	}


}
