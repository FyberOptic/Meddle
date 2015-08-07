package net.fybertech.meddle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class MeddleUtil {

	private static String minecraftVersion = null;


	public static String findMinecraftVersion()
	{
		if (minecraftVersion != null) return minecraftVersion;

		minecraftVersion = "unknown";

		// Get the only class that we can guarantee to be in both client and server and that's easily findable
		InputStream stream = MeddleUtil.class.getClassLoader().getResourceAsStream("net/minecraft/server/MinecraftServer.class");
		if (stream == null) return minecraftVersion;

		ClassReader reader = null;
		try {
			reader = new ClassReader(stream);
		} catch (IOException e) { return minecraftVersion; }

		ClassNode cn = new ClassNode();
		reader.accept(cn, 0);



		List<String> strings = new ArrayList<String>();
		MethodNode runMethod = null;

		// Find all the methods that only return a string from the constant pool
		for (Iterator<MethodNode> iterator = cn.methods.iterator(); iterator.hasNext();)
		{
			MethodNode method = iterator.next();
			if (method.name.equals("run")) runMethod = method;
			if (!method.desc.equals("()Ljava/lang/String;")) continue;

			AbstractInsnNode realFirst = null;
			int realOpcodeCount = 0;

			for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext())
			{
				if (node.getOpcode() >= 0) { realOpcodeCount++; if (realFirst == null) realFirst = node; }
			}

			if (realOpcodeCount != 2 || !(realFirst instanceof LdcInsnNode)) continue;
			LdcInsnNode ldc = (LdcInsnNode)realFirst;
			if (!(ldc.cst instanceof String)) continue;
			strings.add((String)ldc.cst);
		}

		// There should have been only be three methods like this, and two should have known strings
		if (strings.size() == 3 && strings.contains("Server") && strings.contains("vanilla"))
		{
			for (String s : strings)
			{
				if (!s.equals("Server") && !s.equals("vanilla")) minecraftVersion = s;
			}
		}

		// Attempt to confirm it from another location
		String runVersion = null;
		if (runMethod != null)
		{
			for (AbstractInsnNode node = runMethod.instructions.getFirst(); node != null; node = node.getNext())
			{
				if (!(node instanceof LdcInsnNode)) continue;
				LdcInsnNode ldc = (LdcInsnNode)node;
				if (ldc.cst instanceof String) { runVersion = (String)ldc.cst; break; }
			}
		}

		if (minecraftVersion.equals("unknown"))
			System.out.println("Unable to determine Minecraft version!");
		else if (!minecraftVersion.equals(runVersion))
			System.out.println("Unable to confirm Minecraft version!  Assuming " + minecraftVersion + ".");


		return minecraftVersion;
	}


	private static int clientOrServer = -1;

	public static boolean isClientJar()
	{
		if (clientOrServer == 1) return true;
		else if (clientOrServer == 2) return false;

		if (MeddleUtil.class.getClassLoader().getResourceAsStream("net/minecraft/client/main/Main.class") != null)
			clientOrServer = 1;
		else clientOrServer = 2;

		return clientOrServer == 1;
	}


	// Returns true if all objects are non-null
	public static boolean notNull(Object... objects)
	{
		for (int n = 0; n < objects.length; n++) {
			if (objects[n] == null) return false;
		}
		return true;
	}

}
