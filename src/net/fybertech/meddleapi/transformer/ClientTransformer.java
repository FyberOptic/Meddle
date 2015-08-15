package net.fybertech.meddleapi.transformer;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.Meddle;
import net.fybertech.meddle.MeddleUtil;
import net.fybertech.meddleapi.MeddleAPI;
import net.minecraft.launchwrapper.IClassTransformer;

public class ClientTransformer implements IClassTransformer
{
	
	String minecraftClass = DynamicMappings.getClassMapping("net/minecraft/client/Minecraft");
	String guiMainMenu = DynamicMappings.getClassMapping("net/minecraft/client/gui/GuiMainMenu");
	
	
	public ClientTransformer()
	{		
		if (!MeddleUtil.notNull(minecraftClass, guiMainMenu)) 
			MeddleAPI.LOGGER.error("[MeddleAPI] Error obtaining dynamic mappings for client transformer");
	}
	
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) 
	{
		//System.out.println(getClass().getClassLoader() + " " + DynamicMappings.classMappings.size());
		if (name.equals(minecraftClass)) return transformMinecraft(basicClass);
		if (name.equals(guiMainMenu)) return transformGuiMainMenu(basicClass);
		
		return basicClass;
	}
	
	
	private byte[] transformGuiMainMenu(byte[] basicClass)
	{		
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);
		
		//System.out.println("GuiMainMenu: " + cn.name);
		
		for (MethodNode method : (List<MethodNode>)cn.methods) {
			if (!DynamicMappings.checkMethodParameters(method, Type.INT, Type.INT, Type.FLOAT)) continue;
			if ((method.access & Opcodes.ACC_PUBLIC) != Opcodes.ACC_PUBLIC) continue;			
						
			FieldInsnNode fontRendererVar = null;
			FieldInsnNode heightVar = null;
			MethodInsnNode invokeDrawString = null;			
			boolean foundFirst = false;
			boolean foundSecond = false;
			
			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) 
			{
				if (foundFirst && !foundSecond && DynamicMappings.isLdcWithString(insn, " Demo")) {
					foundSecond = true;
					continue;
				}
				
				if (foundSecond) 
				{					
					// First we want GuiMainMenu.fontRendererObj
					if (fontRendererVar == null && insn.getOpcode() == Opcodes.GETFIELD) {
						FieldInsnNode fn = (FieldInsnNode)insn;
						if (fn.owner.equals(cn.name)) fontRendererVar = fn;
						continue;
					}
					// Next we want GuiMainMenu.height
					if (heightVar == null && insn.getOpcode() == Opcodes.GETFIELD) {
						FieldInsnNode fn = (FieldInsnNode)insn;						
						if (fn.owner.equals(cn.name)) heightVar = fn;
						continue;
					}
					// Finally we want GuiMainMenu.drawString
					if (invokeDrawString == null && insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
						MethodInsnNode min = (MethodInsnNode)insn;
						if (min.owner.equals(cn.name)) { invokeDrawString = min; break; }
					}
					continue;
				}					
				
				String string = DynamicMappings.getLdcString(insn);
				if (string != null && string.startsWith("Minecraft")) foundFirst = true;	
			}
			
			// If everything was found, insert code to draw branding to the main menu
			if (MeddleUtil.notNull(fontRendererVar, heightVar, invokeDrawString)) 
			{
				// TODO - Confirm invokeDrawString's types
				
				InsnList il = new InsnList();
				il.add(new VarInsnNode(Opcodes.ALOAD, 0));
				il.add(new VarInsnNode(Opcodes.ALOAD, 0));
				il.add(new FieldInsnNode(Opcodes.GETFIELD, fontRendererVar.owner, fontRendererVar.name, fontRendererVar.desc));
				il.add(new LdcInsnNode("MeddleAPI v" + MeddleAPI.getVersion()));
				il.add(new LdcInsnNode(new Integer(2))); // X
				//il.add(new LdcInsnNode(new Integer(2))); // Y
				il.add(new VarInsnNode(Opcodes.ALOAD, 0));
				il.add(new FieldInsnNode(Opcodes.GETFIELD, heightVar.owner, heightVar.name, heightVar.desc));
				il.add(new LdcInsnNode(new Integer(20)));
				il.add(new InsnNode(Opcodes.ISUB)); // substract 20 from height
				il.add(new LdcInsnNode(new Integer(0xFFFFFF))); // Color
				il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, invokeDrawString.owner, invokeDrawString.name, invokeDrawString.desc, false));
				
				method.instructions.insert(invokeDrawString, il);
				
				ClassWriter writer = new ClassWriter(0);
				cn.accept(writer);
				return writer.toByteArray();
			}
		}
		
		
		return basicClass;
	}
	
	
	
	
	private byte[] transformMinecraft(byte[] basicClass)
	{
		ClassReader reader = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		reader.accept(cn,  0);
		
		for (MethodNode method : (List<MethodNode>)cn.methods) {
			//if (!DynamicMappings.checkMethodParameters(method, Type.OBJECT)) continue;
			Type t = Type.getMethodType(method.desc);
			if (t.getReturnType().getSort() != Type.VOID) continue;			
			if (t.getArgumentTypes().length != 0) continue;
			
			boolean foundFirst = false;
			boolean foundSecond = false;			
			for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) 
			{
				// TODO: Add a default resource pack while here
				
				if (!foundFirst && DynamicMappings.isLdcWithString(insn, "Startup")) 
				{
					foundFirst = true;					
					if (insn.getNext() instanceof MethodInsnNode) {
						insn = insn.getNext();
						insn = insn.getNext();
						InsnList list = new InsnList();
						list.add(new VarInsnNode(Opcodes.ALOAD, 0));
						list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleAPI", "preInit", "(Ljava/lang/Object;)V", false));
						method.instructions.insert(insn, list);
					}
					else { /* TODO: Error */ }
					continue;
				}
				
				if (foundFirst && DynamicMappings.isLdcWithString(insn, "Post startup"))
				{
					if (insn.getNext() instanceof MethodInsnNode) {
						insn = insn.getNext();
						// Just use a list here too in case we want to add more later
						InsnList list = new InsnList();						
						list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/meddleapi/MeddleAPI", "init", "()V", false));
						method.instructions.insert(insn, list);
					}
					else { /* TODO: Error */ }
					break;
				}
								
				//if (!foundSecond && !DynamicMappings.isLdcWithString(insn, "Post startup")) continue;
				//foundSecond = true;				
								
			}		
		}		
		
		ClassWriter writer = new ClassWriter(0); 
		cn.accept(writer);
		return writer.toByteArray();	
	}
	
	
	

}
