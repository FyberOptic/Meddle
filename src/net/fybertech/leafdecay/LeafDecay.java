package net.fybertech.leafdecay;

import java.io.File;
import java.util.List;
import java.util.Random;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class LeafDecay implements ITweaker, IClassTransformer {
	
    String blockLeavesBaseClass = "alr"; 	// net/minecraft/block/BlockLeavesBase
    String worldClass = "aeo";				// net/minecraft/world/World   
    String blockClass = "agk";				// net/minecraft/block/Block    
    String blockPosClass = "cj"; 			// net/minecraft/util/BlockPos    
    String iBlockStateClass = "anm"; 		// net/minecraft/block/state/IBlockState
    
    // public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    String methodName = "a";
    String methodDesc = "(L" + worldClass + ";L" + blockPosClass + ";L" + iBlockStateClass + ";L" + blockClass + ";)V";    
        
    // func_180497_b in default MCP for 1.8
    // public void scheduleBlockUpdate(BlockPos pos, Block blockIn, int delay, int priority) {}
    String scheduleBlockUpdateMethod = "b";
    String scheduleBlockUpdateDesc = "(L" + blockPosClass + ";L" + blockClass + ";II)V";
      
    
    private byte[] patchLeaveClass(byte[] basicClass)
    {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, 0);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);

        writer.visitField(Opcodes.ACC_PRIVATE, "leafRand", "Ljava/util/Random;", null, null);
        
        MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC, methodName, methodDesc, null, null);
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
        
        // priority = 0
        mv.visitInsn(Opcodes.ICONST_0);        
        
        // public void scheduleBlockUpdate(BlockPos pos, Block blockIn, int delay, int priority)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, worldClass, scheduleBlockUpdateMethod, scheduleBlockUpdateDesc, false);
        mv.visitInsn(Opcodes.RETURN);     
        mv.visitMaxs(5, 0);
        mv.visitEnd();
        
        System.out.println("BlockLeavesBase.onNeighborBlockChange patched");

        return writer.toByteArray();
    }
    
    
    @Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) 
    {
    	if (transformedName.equals(blockLeavesBaseClass)) return patchLeaveClass(basicClass);    
    	else return basicClass;
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
