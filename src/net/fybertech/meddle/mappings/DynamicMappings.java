package net.fybertech.meddle.mappings;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import net.fybertech.meddle.Meddle;
import net.minecraft.launchwrapper.Launch;


// Note: This is a separate mod now; this is purely for backwards compatibility.


@Deprecated
public class DynamicMappings
{

	private static Class<? extends Object> dmclass = null;

	static
	{
		Meddle.LOGGER.warn("[Meddle] A mod is attempting to use the deprecated Meddle v1.1 DynamicMappings system.");

		if (Meddle.isModLoaded("dynamicmappings"))
		{
			try {
				dmclass = Launch.classLoader.findClass("net.fybertech.dynamicmappings.DynamicMappings");
			} catch (Exception e) { }
		}

		if (dmclass == null) Meddle.LOGGER.error("[Meddle] DynamicMappings mod must be present for backwards compatibility!");
		else Meddle.LOGGER.info("[Meddle] DynamicMappings compatibility layer in use.");
	}


	public static boolean checkMethodParameters(MethodNode method, int... types)
	{
		if (dmclass != null) {
			try {
				return (boolean)dmclass.getMethod("checkMethodParameters", MethodNode.class, int[].class).invoke(null, method, types);
			} catch (Exception e) {}
		}
		return false;
	}


	public static boolean searchConstantPoolForStrings(String className, String... matchStrings)
	{
		if (dmclass != null) {
			try {
				return (boolean)dmclass.getMethod("searchConstantPoolForStrings", String.class, String[].class).invoke(null, className, matchStrings);
			} catch (Exception e) {}
		}
		return false;
	}


	public static FieldNode getFieldByName(ClassNode cn, String fieldName)
	{
		if (dmclass != null) {
			try {
				return (FieldNode)dmclass.getMethod("getFieldByName", ClassNode.class, String.class).invoke(null, cn, fieldName);
			} catch (Exception e) {}
		}
		return null;
	}


	public static String getFieldDesc(ClassNode cn, String fieldName)
	{
		if (dmclass != null) {
			try {
				return (String)dmclass.getMethod("getFieldDesc", ClassNode.class, String.class).invoke(null, cn, fieldName);
			} catch (Exception e) {}
		}
		return null;
	}


	public static boolean isLdcWithInteger(AbstractInsnNode node, int val)
	{
		if (dmclass != null) {
			try {
				return (boolean)dmclass.getMethod("isLdcWithInteger", AbstractInsnNode.class, int.class).invoke(null, node, val);
			} catch (Exception e) {}
		}
		return false;
	}


	public static String getLdcString(AbstractInsnNode node)
	{
		if (dmclass != null) {
			try {
				return (String)dmclass.getMethod("getLdcString", AbstractInsnNode.class).invoke(null, node);
			} catch (Exception e) {}
		}
		return null;
	}



	public static String getClassMapping(String className)
	{
		if (dmclass != null) {
			try {
				return (String)dmclass.getMethod("getClassMapping", String.class).invoke(null, className);
			} catch (Exception e) {}
		}
		return null;
	}


	public static boolean isLdcWithString(AbstractInsnNode node, String string)
	{
		if (dmclass != null) {
			try {
				return (boolean)dmclass.getMethod("isLdcWithString", AbstractInsnNode.class, String.class).invoke(null, node, string);
			} catch (Exception e) {}
		}
		return false;
	}


	public static ClassNode getClassNode(String className)
	{
		if (dmclass != null) {
			try {
				return (ClassNode)dmclass.getMethod("getClassNode",String.class).invoke(null, className);
			} catch (Exception e) {}
		}
		return null;
	}


}
