package net.fybertech.meddleapi.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.dynamicmappings.DynamicRemap;
import net.fybertech.dynamicmappings.InheritanceMap;
import net.minecraft.launchwrapper.IClassTransformer;

public class ReobfTransformer implements IClassTransformer
{

	DynamicRemap toObfRemapper = null;

	String[] exclusions = new String[] {
			"com.jcraft.",
			"net.fybertech.meddle.",
			"net.fybertech.dynamicmappings.",
			//"net.fybertech.meddleapi.",
			"org.slf4j.",
			"org.apache.",
			"io.netty.",
			"com.google",
			"paulscode.",
			"joptsimple.",
			"com.mojang.",
			"net.minecraft.",
			"oshi.",
			"com.ibm."
	};



	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (!name.contains(".")) return basicClass;
		for (String ex : exclusions) { if (name.startsWith(ex)) return basicClass; }

		return toObfRemapper.remapClass(basicClass);
	}




	public ReobfTransformer()
	{
		final DynamicRemap toDeobfRemapper = new DynamicRemap(
				DynamicMappings.reverseClassMappings,
				DynamicMappings.reverseFieldMappings,
				DynamicMappings.reverseMethodMappings);

		toDeobfRemapper.unpackagedPrefix = null;


		toObfRemapper = new DynamicRemap(
				DynamicMappings.classMappings,
				DynamicMappings.fieldMappings,
				DynamicMappings.methodMappings) {

			@Override
			protected ClassNode getClassNode(String className) {
				if (className == null) return null;

				className = className.replace(".", "/");

				if (DynamicMappings.classMappings.containsKey(className)) {
					return toDeobfRemapper.remapClass(DynamicMappings.classMappings.get(className));
				}

				File f = new File(className + ".class");
				if (!f.exists()) return super.getClassNode(className);

				InputStream stream = null;
				try {
					stream = new FileInputStream(f);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				ClassReader reader = null;
				try {
					reader = new ClassReader(stream);
				} catch (IOException e) { return null; }

				ClassNode cn = new ClassNode();
				reader.accept(cn, 0);

				return cn;
			}

		};

		toObfRemapper.unpackagedPrefix = null;
		toObfRemapper.unpackagedInnerPrefix = null;

		toObfRemapper.inheritanceMapper = new InheritanceMap() {
			@Override
			public ClassNode locateClass(String classname) throws IOException
			{
				String out = DynamicMappings.getClassMapping(classname);
				if (out != null) classname = out;
				return toDeobfRemapper.remapClass(classname);
			}
		};
	}




}
