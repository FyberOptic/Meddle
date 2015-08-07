package net.fybertech.meddle.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Main
{
	public static void main(String[] args)
	{
		List<String> newArgs = new ArrayList<String>();
		newArgs.addAll(Arrays.asList(args));
		newArgs.add("--tweakClass");
		newArgs.add("net.fybertech.meddle.Meddle");

		// Added for now until GUI logging is sorted out.
		// newArgs.add("--nogui");
		//
		// NOPE, nevermind, a bug since 1.7.x occasionally causes a crash
		// at exit when using nogui, allowing for potential data loss.

		args = newArgs.toArray(new String[newArgs.size()]);

		net.minecraft.launchwrapper.Launch.main(args);
	}
}
