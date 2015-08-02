package net.fybertech.physicsfix;

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

import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.mappings.DynamicMappings;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class PhysicsTweaker implements ITweaker, IClassTransformer
{
	String entityLivingBaseClass = DynamicMappings.getClassMapping("net/minecraft/entity/EntityLivingBase");
	String entityClass = DynamicMappings.getClassMapping("net/minecraft/entity/Entity");

	// public void moveEntityWithHeading(float strafe, float forward)
	String moveEntityWithHeadingDesc = "(FF)V";


	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (name.equals(entityLivingBaseClass)) return patchEntityLivingBase(basicClass);
		else return basicClass;
	}


	private byte[] failGracefully(String error, byte[] bytes)
	{
		Meddle.LOGGER.error("[Meddle/PhysicsTweaker] " + error);
		return bytes;
	}


	private byte[] patchEntityLivingBase(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn, 0);

		int count = 0;
		MethodNode moveMethod = null;

		// Find moveEntityWithHeading
		for (MethodNode method : cn.methods)
		{
			if (!method.desc.equals(moveEntityWithHeadingDesc)) continue;

			boolean skip = false;
			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
				//INVOKESPECIAL net/minecraft/entity/Entity.fall (FF)V
				if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
					MethodInsnNode mn = (MethodInsnNode)insn;
					if (mn.owner.equals(entityClass) && mn.desc.equals(moveEntityWithHeadingDesc))
					{
						skip = true;
						break;
					}
				}
			}
			if (skip) continue;

			count++;
			moveMethod = method;
		}

		if (count != 1 || moveMethod == null)
			return failGracefully("Unable to find EntityLivingBase.moveEntityWithHeading", basicClass);


		// Found the method, now find the patch location
		AbstractInsnNode node = moveMethod.instructions.getFirst();
		while (node != null)
		{
			if (node.getOpcode() == Opcodes.IFEQ) break;
			node = node.getNext();
		}

		if (node == null)
			return failGracefully("Couldn't patch EntityLivingBase.moveEntityWithHeading!", basicClass);

		moveMethod.instructions.set(node, new InsnNode(Opcodes.POP));

		Meddle.LOGGER.info("[Meddle/PhysicsTweaker] EntityLivingBase.moveEntityWithHeading patched");

		ClassWriter writer = new ClassWriter(0);
		cn.accept(writer);
		return writer.toByteArray();
	}


	@Override
	public void acceptOptions(List<String> inArgs, File gameDir, File assetsDir, String profile) {}


	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		classLoader.registerTransformer(PhysicsTweaker.class.getName());
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
