package net.fybertech.alltheitems;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.MeddleMod;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

@MeddleMod(id="alltheitems", name="AllTheItems", author="FyberOptic", version="1.2", depends={"dynamicmappings"})
public class AllTheItems implements ITweaker, IClassTransformer
{
	String entityItemClass = DynamicMappings.getClassMapping("net.minecraft.entity.item.EntityItem");

	// private void searchForOtherItemsNearby()
	String searchItemsDesc = "()V";


	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (name.equals(entityItemClass)) return transformEntityItem(basicClass);
		else return basicClass;
	}

	private byte[] transformEntityItem(byte[] classbytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(classbytes);
		classReader.accept(classNode, 0);

		Iterator<MethodNode> methods = classNode.methods.iterator();
		while (methods.hasNext())
		{
			MethodNode m = methods.next();

			boolean foundFirstClue = false;
			boolean foundSecondClue = false;

			// Try to find 'private void searchForOtherItemsNearby()'
			if (!m.desc.equals("()V") || m.name.equals("<clinit>")) continue;

			for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext())
			{
				// LDC Lnet/minecraft/entity/item/EntityItem;.class
				if (!foundFirstClue && insn instanceof LdcInsnNode)
				{
					LdcInsnNode ldc = (LdcInsnNode)insn;
					if (ldc.cst instanceof Type)
					{
						if (((Type)ldc.cst).getClassName().equals(entityItemClass)) foundFirstClue = true;
					}
				}

				// INVOKEINTERFACE java/util/List.iterator ()Ljava/util/Iterator;
				if (!foundSecondClue && insn instanceof MethodInsnNode)
				{
					MethodInsnNode mn = (MethodInsnNode)insn;
					if (mn.owner.equals("java/util/List") && mn.name.equals("iterator")) foundSecondClue = true;
				}
			}

			// High confidence in a match, patch it
			if (foundFirstClue && foundSecondClue)
			{
				m.instructions.insertBefore(m.instructions.getFirst(), new InsnNode(Opcodes.RETURN));
				ClassWriter writer = new ClassWriter(0); //ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
				classNode.accept(writer);

				Meddle.LOGGER.info("[Meddle/AllTheItems] EntityItem.searchForOtherItemsNearby patched");
				return writer.toByteArray();
			}
		}

		Meddle.LOGGER.error("[Meddle/AllTheItems] Couldn't patch EntityItem.searchForOtherItemsNearby!");
		return classbytes;

	}


	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {}


	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		classLoader.registerTransformer(AllTheItems.class.getName());
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

