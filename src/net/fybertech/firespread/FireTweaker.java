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
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class FireTweaker  implements ITweaker, IClassTransformer 
{
	String blockFireClass = "aic"; 		// net/minecraft/block/BlockFire	
	String worldClass = "aeo"; 			// net/minecraft/world/World
	String blockPosClass = "cj"; 		// net/minecraft/util/BlockPos    
    String iBlockStateClass = "anm"; 	// net/minecraft/block/state/IBlockState	
	
	// public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
	String updateTickMethod = "b";
	String updateTickDesc = "(L" + worldClass + ";L" + blockPosClass + ";L" + iBlockStateClass + ";Ljava/util/Random;)V";
	
	// public boolean setBlockState(BlockPos pos, IBlockState newState, int flags)
	String setBlockStateMethod = "a";
	String setBlockStateDesc = "(L" + blockPosClass + ";L" + iBlockStateClass + ";I)Z";
	
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) 
	{
		if (name.equals(blockFireClass)) 
		{			
			ClassReader reader = new ClassReader(basicClass);
			ClassNode cn = new ClassNode();
			reader.accept(cn, 0);			
			
			for (MethodNode method : cn.methods)
			{				
				if ((method.name.equals(updateTickMethod) && method.desc.equals(updateTickDesc)))					
				{
					AbstractInsnNode node = method.instructions.getLast();
					
					boolean foundFirst = false;
					
					while (node != null)
					{						
						if (!foundFirst && node instanceof MethodInsnNode)
						{							
							MethodInsnNode mnode = (MethodInsnNode)node;
							if (mnode.owner.equals(worldClass) && 
								mnode.name.equals(setBlockStateMethod) && 
								mnode.desc.equals(setBlockStateDesc)) 
							{ foundFirst = true; }							
						}
						else if (foundFirst && node.getOpcode() == Opcodes.ILOAD) break;
						
						node = node.getPrevious();
					}
					
					if (node == null)
					{ 
						System.out.println("Couldn't patch BlockFire.updateTick!"); 
						return basicClass; 
					}
					
					method.instructions.set(node, new InsnNode(Opcodes.ICONST_0));
				}
			}
			
			System.out.println("BlockFire.updateTick patched");
			
			ClassWriter writer = new ClassWriter(Opcodes.ASM5);
			cn.accept(writer);
			return writer.toByteArray();			
		}
		
		return basicClass;
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
