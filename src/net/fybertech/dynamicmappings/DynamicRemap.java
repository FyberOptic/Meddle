package net.fybertech.dynamicmappings;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import net.fybertech.dynamicmappings.DynamicClientMappings;
import net.fybertech.dynamicmappings.DynamicMappings;
import net.fybertech.dynamicmappings.InheritanceMap;
import net.fybertech.dynamicmappings.InheritanceMap.FieldHolder;
import net.fybertech.dynamicmappings.InheritanceMap.MethodHolder;


public class DynamicRemap 
{
	
	
	private static class MyRemapper extends Remapper
	{
		@Override
		public String map(String typeName) 
		{
			String startName = typeName;
			if (DynamicMappings.reverseClassMappings.containsKey(typeName)) typeName = DynamicMappings.reverseClassMappings.get(typeName);
			
			// Remap the parent class in the case of an inner class
			String[] split = typeName.split("\\$");
			if (DynamicMappings.reverseClassMappings.containsKey(split[0])) split[0] = DynamicMappings.reverseClassMappings.get(split[0]);
			typeName = split[0];
			for (int n = 1; n < split.length; n++) typeName += "$" + split[n];
				
			if (!typeName.contains("/")) typeName = "net/minecraft/class_" + typeName;
			return super.map(typeName);
		}
		
		
		@Override
		public String mapFieldName(String owner, String name, String desc)
		{
			ClassNode cn = DynamicMappings.getClassNode(owner);
			if (cn == null) return super.mapFieldName(owner, name, desc);
			
			InheritanceMap map = null;
			try {
				map = InheritanceMap.buildMap(cn);
			} catch (IOException e) {
				e.printStackTrace();
			}			
			Set<FieldHolder> fields = map.fields.get(name + " " + desc);
			
			if (fields == null) return super.mapFieldName(owner, name, desc);
			
			for (FieldHolder holder : fields) {			
				String key = holder.cn.name + " " + holder.fn.name + " " + holder.fn.desc;
				if (DynamicMappings.reverseFieldMappings.containsKey(key)) {
					String mapping = DynamicMappings.reverseFieldMappings.get(key);
					String[] split = mapping.split(" ");
					return super.mapFieldName(owner, split[1], desc);
				}
			}
			
			return super.mapFieldName(owner,  name,  desc);
		}		
		
		
		@Override
		public String mapMethodName(String owner, String name, String desc) 
		{		
			if (owner.startsWith("[") || name.startsWith("<")) return super.mapMethodName(owner, name, desc);
			
			ClassNode cn = DynamicMappings.getClassNode(owner);
			if (cn == null) return super.mapMethodName(owner, name, desc);
				
			InheritanceMap map = null;
			try {
				map = InheritanceMap.buildMap(cn);
			} catch (IOException e) {
				e.printStackTrace();
			}			
			Set<MethodHolder> methods = map.methods.get(name + " " + desc);
			
			if (methods == null) return super.mapMethodName(owner, name, desc);			
			
			for (MethodHolder holder : methods) {			
				String key = holder.cn.name + " " + holder.mn.name + " " + holder.mn.desc;
				if (DynamicMappings.reverseMethodMappings.containsKey(key)) {
					String mapping = DynamicMappings.reverseMethodMappings.get(key);
					//System.out.println(mapping);
					String[] split = mapping.split(" ");
					return super.mapMethodName(owner, split[1], desc);
				}
			}
			
			return super.mapMethodName(owner, name, desc);
		}
	}
	
	
	public static ClassNode remapClass(String className)
	{
		InputStream stream = DynamicRemap.class.getClassLoader().getResourceAsStream(className + ".class");
		ClassReader reader = null;
		try {
			reader = new ClassReader(stream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ClassNode cn = new ClassNode();
		reader.accept(new RemappingClassAdapter(cn, new MyRemapper()), ClassReader.EXPAND_FRAMES);
		
		for (MethodNode method : cn.methods)
		{
			int paramCount = 0;
			int varCount = 0;
			
			if (method.localVariables != null)
				for (LocalVariableNode lvn : method.localVariables)
				{
					if (!lvn.name.equals("\u2603")) continue;
					if (lvn.start == method.instructions.getFirst()) lvn.name = "param" + paramCount++;
					else lvn.name = "var" + varCount++;
				}
		}
		
		
		return cn;
	}
	

	public static byte[] getFileFromZip(ZipEntry entry, ZipFile zipFile)
	{
		byte[] buffer = null;		

		if (entry != null)
		{			
			try {
				InputStream stream = zipFile.getInputStream(entry);				
				int pos = 0;
				buffer = new byte[(int)entry.getSize()];
				while (true)
				{
					int read = stream.read(buffer, pos, Math.min(1024, (int)entry.getSize() - pos));					
					pos += read;					
					if (read < 1) break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return buffer;
	}
	
	
	public static void main(String[] args)
	{	
				
		DynamicMappings.generateClassMappings();
		DynamicClientMappings.generateClassMappings();
		DynamicMappings.generateMethodMappings();
		
		URL url = DynamicRemap.class.getClassLoader().getResource("net/minecraft/server/MinecraftServer.class");	
		if (url == null) { System.out.println("Couldn't locate server class!"); return; }
		
		JarFile jar = null;
		if ("jar".equals(url.getProtocol())) {
			JarURLConnection connection = null;
			try {
				connection = (JarURLConnection) url.openConnection();
				jar = connection.getJarFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (jar == null) return;
		
		
		JarOutputStream outJar = null;
		try {
			outJar = new JarOutputStream(new FileOutputStream("mcremapped.jar"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for (Enumeration<JarEntry> enumerator = jar.entries(); enumerator.hasMoreElements();)
		{
			JarEntry entry = enumerator.nextElement();
			String name = entry.getName();
			byte[] bytes = null;
			
			if (name.endsWith(".class")) {
				name = name.substring(0, name.length() - 6);			
				ClassNode mapped = remapClass(name);
				
				ClassWriter writer = new ClassWriter(0);
				mapped.accept(writer);
				name = mapped.name + ".class";
				bytes = writer.toByteArray();
			}
			else bytes = getFileFromZip(entry, jar);
			
			ZipEntry ze = new ZipEntry(name);
			try {
				outJar.putNextEntry(ze);				
				outJar.write(bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}
		
		try {
			outJar.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		
	}
	
}
