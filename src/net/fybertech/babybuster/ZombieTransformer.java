package net.fybertech.babybuster;

import java.util.ArrayList;
import java.util.List;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ZombieTransformer implements IClassTransformer
{
	String zombieClass = DynamicMappings.getClassMapping("net/minecraft/entity/monster/EntityZombie");


	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (name.equals(zombieClass)) return transformZombie(basicClass);
		else return basicClass;
	}


	private byte[] failGracefully(String msg, byte[] b)
	{
		Meddle.LOGGER.error("[Meddle/BabyBuster] " + msg);
		return b;
	}


	private byte[] transformZombie(byte[] classbytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(classbytes);
		classReader.accept(classNode, 0);

		for (MethodNode method : (List<MethodNode>)classNode.methods)
		{
			if (!DynamicMappings.checkMethodParameters(method, Type.OBJECT, Type.OBJECT)) continue;
			Type t = Type.getMethodType(method.desc);
			Type[] args = t.getArgumentTypes();
			Type returnType = t.getReturnType();

			// If not returning an object, forget it
			if (returnType.getSort() != Type.OBJECT) continue;
			// If second parameter isn't the same as the return class, forget it
			if (!returnType.getClassName().equals(args[1].getClassName())) continue;

			boolean doRemove = false;
			List<AbstractInsnNode> remove = new ArrayList<AbstractInsnNode>();

			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext())
			{
				if (doRemove) {
					if (insn.getOpcode() == Opcodes.ICONST_0) break;
					remove.add(insn);
					continue;
				}

				if (insn.getOpcode() == Opcodes.NEW) {
					insn = insn.getNext();
					if (insn.getOpcode() == Opcodes.DUP)
					{
						insn = insn.getNext();
						// Opcodes class doesn't contain ALOAD_0, so match to class
						if (!(insn instanceof VarInsnNode)) return failGracefully("Couldn't find patch location in EntityZombie!", classbytes);
						doRemove = true;
					}
				}
			}

			for (AbstractInsnNode insn : remove) method.instructions.remove(insn);

			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			classNode.accept(writer);
			Meddle.LOGGER.info("[Meddle/BabyBuster] EntityZombie.func_180482_a patched");
			return writer.toByteArray();

		}

		return failGracefully("Couldn't patch EntityZombie!", classbytes);

	}


}
