package net.fybertech.alltheitems;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class AllTheItems implements ITweaker, IClassTransformer	
{
	// net/minecraft/entity/item/EntityItem
	String entityItemClass = "vm";
	
	// private void searchForOtherItemsNearby()
	String searchItemsMethod = "w";
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
        	
        	if (m.name.equals(searchItemsMethod) && m.desc.equals(searchItemsDesc))
        	{	        		
        		m.instructions.insertBefore(m.instructions.getFirst(), new InsnNode(Opcodes.RETURN));        		
        		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classNode.accept(writer);
                
                System.out.println("EntityItem.searchForOtherItemsNearby patched");
                return writer.toByteArray();        		
        	}
        }
        
        System.out.println("Couldn't patch EntityItem.searchForOtherItemsNearby!");
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

