package net.fybertech.dynamicmappings;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class InheritanceMap
{
	public static List<String> libraries = new ArrayList<String>();
	public static Map<String, InheritanceMap> mapCache = new HashMap<String, InheritanceMap>();
	public static Map<String, ClassNode> classCache = new HashMap<String, ClassNode>();
	
	public List<String> privatefields = new ArrayList<String>();
	public List<String> privatemethods = new ArrayList<String>();
	
	
	public static class FieldHolder
	{
		public ClassNode cn;
		public FieldNode fn;
		public FieldHolder(ClassNode c, FieldNode f)
		{
			cn = c;
			fn = f;
		}
	}
	
	public static class MethodHolder
	{
		public ClassNode cn;
		public MethodNode mn;
		public MethodHolder(ClassNode c, MethodNode m)
		{
			cn = c;
			mn = m;
		}
	}
	
	public String className;
	public Map<String, HashSet<FieldHolder>> fields = new HashMap<String,HashSet<FieldHolder>>();		
	public Map<String, HashSet<MethodHolder>> methods = new HashMap<String,HashSet<MethodHolder>>();
	
	
	public InheritanceMap()
	{
	}
	
	
	
	
	private InheritanceMap(ClassNode jc)
	{
		this.className = jc.name;
		
		for (FieldNode jf : jc.fields)
		{
			String fielddesc = jf.name + " " + jf.desc;
			
			if ((jf.access & 0x0002) > 0) privatefields.add(fielddesc);
			
			HashSet<FieldHolder> fieldslist = this.fields.get(fielddesc);				
			if (fieldslist == null)	
			{ 
				fieldslist = new HashSet<FieldHolder>(); 
				this.fields.put(fielddesc, fieldslist); 
			}					
			fieldslist.add(new FieldHolder(jc, jf));
		}
		
		for (MethodNode jm : jc.methods)
		{
			String methoddesc = jm.name + " " + jm.desc;
			
			if ((jm.access & 0x0002) > 0) privatemethods.add(methoddesc);
			
			HashSet<MethodHolder> methodslist = this.methods.get(methoddesc);				
			if (methodslist == null) 
			{
				methodslist = new HashSet<MethodHolder>();
				this.methods.put(methoddesc, methodslist);
			}
			methodslist.add(new MethodHolder(jc, jm));				
		}				
	}
	
	private void mergeMap(InheritanceMap cm)
	{
		for (String fielddesc : cm.fields.keySet())
		{			
			if (cm.privatefields.contains(fielddesc)) continue;
			
			if (this.fields.get(fielddesc) != null) this.fields.get(fielddesc).addAll(cm.fields.get(fielddesc));
			else
			{
				HashSet<FieldHolder> f = new HashSet<FieldHolder>();
				f.addAll(cm.fields.get(fielddesc));
				this.fields.put(fielddesc, f);
			}
		}
		
		for (String methoddesc : cm.methods.keySet())
		{
			if (cm.privatemethods.contains(methoddesc)) continue;
			
			if (this.methods.get(methoddesc) != null) this.methods.get(methoddesc).addAll(cm.methods.get(methoddesc));
			else
			{
				HashSet<MethodHolder> m = new HashSet<MethodHolder>();
				m.addAll(cm.methods.get(methoddesc));
				this.methods.put(methoddesc, m);
			}
		}
	}
	
	
	public InheritanceMap buildMap(String paramClassname) throws IOException
	{
		ClassNode jc = locateClass(paramClassname);
		if (jc == null) return null;
		return buildMap(jc);
	}
	
	
	public InheritanceMap buildMap(ClassNode paramClass) throws IOException
	{
		InheritanceMap classmap = InheritanceMap.mapCache.get(paramClass.name); 
		if (classmap != null) return classmap;				
				
		//System.out.println("Begin buildMap: " + paramClass.getClassName());
		
		classmap = new InheritanceMap(paramClass);
		
		for (String interfaceclassname : paramClass.interfaces)
		{			
			ClassNode interfaceclass = locateClass(interfaceclassname);
			if (interfaceclass == null) { System.out.println("ERROR: Unable to locate " + interfaceclassname); continue; }
			classmap.mergeMap(buildMap(interfaceclass));
		}
		
		if (paramClass.superName != null)
		{
			String superclassname = paramClass.superName;
			ClassNode superclass = locateClass(superclassname);
			if (superclass == null) System.out.println("ERROR: Unable to locate " + superclassname);
			else classmap.mergeMap(buildMap(superclass));
		}
		//System.out.println("Finish buildMap: " + paramClass.getClassName());
		
		InheritanceMap.mapCache.put(paramClass.name, classmap);
		
		return classmap;
	}
	
	
	
	public static ClassNode locateClassInJAR(String classname, String filename) throws IOException
	{
		JarFile jf = new JarFile(filename);
		
		for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();)
		{
			JarEntry je = e.nextElement();
			String name = je.getName();
			if (!name.endsWith(".class")) continue;
			
			if (!classname.equals(name.replace(".class", ""))) continue;
			
			ClassReader reader = new ClassReader(jf.getInputStream(je));
			ClassNode jc = new ClassNode();
			reader.accept(jc, 0);
			
			if (jc.name.equals(classname)) { jf.close(); return jc; }			
		}
		
		jf.close();
		return null;
	}
	
	public ClassNode locateClass(String classname) throws IOException
	{
		return DynamicMappings.getClassNode(classname);
		
		/*ClassNode jc = InheritanceMap.classCache.get(classname);
		if (jc != null) return jc;	
		
		for (String jarfile : InheritanceMap.libraries)
		{
			jc = locateClassInJAR(classname, jarfile);
			if (jc != null)
			{
				InheritanceMap.classCache.put(classname, jc);
				return jc;
			}
		}
		
		return null;*/
	}
	
	
	public static void addSystemLibrary()
	{
		String systemJAR = System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar";
		InheritanceMap.libraries.add(systemJAR);
	}
	
	public static void addLibrary(String paramLib)
	{
		InheritanceMap.libraries.add(paramLib);
	}
	
	/*public static void addLibraryDir(String libDir)
	{
		List<String> jars = FyddleUtil.walkDirForExt(libDir, ".jar");
		for (String jar : jars) InheritanceMap.addLibrary(jar);
	}
	
	public static void addClassPool(ClassPool classPool)
	{
		InheritanceMap.classCache.putAll(classPool.getPool());
	}*/
	
	public static void reset()
	{
		InheritanceMap.libraries.clear();
		InheritanceMap.classCache.clear();
		InheritanceMap.mapCache.clear();
	}
	

	
	public static void main(String[] args) throws IOException 
	{
		InheritanceMap base = new InheritanceMap();
		
		InheritanceMap map = base.buildMap("ake");
		System.out.println("Fields: ");
		for (String key : map.fields.keySet()) {
			System.out.println("  " + key);
			for (FieldHolder key2 : map.fields.get(key)) {
				System.out.println("    " + key2.cn.name);
			}
		}
		
		System.out.println("Methods: ");
		for (String key : map.methods.keySet()) {
			System.out.println("  " + key);
			for (MethodHolder key2 : map.methods.get(key)) {
				System.out.println("    " + key2.cn.name);
			}
		}
	}
	
}

