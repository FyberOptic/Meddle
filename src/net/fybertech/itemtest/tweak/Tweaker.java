package net.fybertech.itemtest.tweak;

import java.io.File;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.meddle.MeddleMod;
import net.fybertech.meddleapi.MeddleAPI;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

@MeddleMod(id="itemtest", depends={"meddleapi"})
public class Tweaker implements ITweaker {

	// Static because the class will be inited again for the transformer
	//static String setUnlocalizedNameMethod = null;
	//static String getColorFromItemStackMethod = null;
	
		
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) 
	{
		MeddleAPI.registerMod("net.fybertech.itemtest.Mod");
		
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {		
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
