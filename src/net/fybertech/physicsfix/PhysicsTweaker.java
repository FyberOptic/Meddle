package net.fybertech.physicsfix;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class PhysicsTweaker  implements ITweaker, IClassTransformer 
{
	
	// net/minecraft/entity/EntityLivingBase
	String entityLivingBaseClass = "qa";
	
	// public void moveEntityWithHeading(float strafe, float forward)
	String moveEntityWithHeadingMethod = "g";
	String moveEntityWithHeadingDesc = "(FF)V";
	
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) 
	{
		if (name.equals(entityLivingBaseClass))
		{			
			ClassReader reader = new ClassReader(basicClass);
			ClassNode cn = new ClassNode();
			reader.accept(cn, 0);			
			
			for (MethodNode method : cn.methods)
			{
				if (method.name.equals(moveEntityWithHeadingMethod) && method.desc.equals(moveEntityWithHeadingDesc))
				{
					AbstractInsnNode node = method.instructions.getFirst();
					while (node != null)
					{
						if (node.getOpcode() == Opcodes.IFEQ) break;
						node = node.getNext();
					}
					
					if (node == null)
					{ 
						System.out.println("Couldn't patch EntityLivingBase.moveEntityWithHeading!"); 
						return basicClass; 
					}
					
					method.instructions.set(node, new InsnNode(Opcodes.POP));
				}
			}
			
			System.out.println("EntityLivingBase.moveEntityWithHeading patched");
			
			ClassWriter writer = new ClassWriter(Opcodes.ASM4);
			cn.accept(writer);
			return writer.toByteArray();			
		}
		
		return basicClass;
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
