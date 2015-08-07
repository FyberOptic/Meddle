package net.fybertech.meddle;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.fybertech.meddle.Meddle.ModContainer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class StageTwo implements ITweaker {


	public StageTwo()
	{
		// Fix classloader exception again since loading this tweaker
		// breaks it again.
		Meddle.classloaderExceptions.remove("net.fybertech.meddle");
		Meddle.classloaderExceptions.add("net.fybertech.meddle.");


		Meddle.LOGGER.info("[Meddle] Sorting tweaks");

		@SuppressWarnings("unchecked")
		List<ITweaker> tweaks = (List<ITweaker>)Launch.blackboard.get("Tweaks");

		List<String> sortedModIDs = new ArrayList<String>();
		List<String> unsortedModIDs = new ArrayList<String>();

		// Add mods with no dependency first
		for (String id : Meddle.loadedModsList.keySet()) {
			ModContainer mc = Meddle.loadedModsList.get(id);
			if (mc.meta == null || mc.meta.depends().length == 0) sortedModIDs.add(id);
			else unsortedModIDs.add(id);
		}

		// Sort alphabetically
		Collections.sort(sortedModIDs);

		// If DynamicMappings is being loaded, put it first, for legacy purposes
		if (sortedModIDs.contains("dynamicmappings")) {
			sortedModIDs.remove("dynamicmappings");
			sortedModIDs.add(0, "dynamicmappings");
		}


		// Order the rest based on dependencies
		List<String> localList = new ArrayList<String>();
		while (true)
		{
			localList.clear();
			// Add mods to local list that have dependencies met
			for (String id : unsortedModIDs)
			{
				ModContainer mc = Meddle.loadedModsList.get(id);
				boolean allmatch = true;
				for (String dep : mc.meta.depends()) {
					if (!sortedModIDs.contains(dep)) allmatch = false;
				}
				if (allmatch) localList.add(id);
			}
			// If we didn't do anything this pass, we're done
			if (localList.isEmpty()) break;
			// Sort this round alphabetically
			Collections.sort(localList);
			// Set up for next pass
			sortedModIDs.addAll(localList);
			unsortedModIDs.removeAll(localList);
		}


		// Tell user about unmet dependencies
		for (String id : unsortedModIDs)
		{
			ModContainer mod = Meddle.loadedModsList.remove(id);

			String missing = null;
			for (String dep : mod.meta.depends()) {
				if (!sortedModIDs.contains(dep)) {
					if (missing == null) missing = "";
					else missing += ", ";
					missing += dep;
				}
			}

			Meddle.LOGGER.error("[Meddle] " + id + " is missing the following dependencies: " + missing);
		}


		Meddle.LOGGER.info("[Meddle] Initializing tweaks");
		for (String id : sortedModIDs)
		{
			ModContainer mod = Meddle.loadedModsList.get(id);
			if (mod.tweakClass == null) continue;

			ITweaker tweaker = null;
			try {
				tweaker = (ITweaker) mod.tweakClass.getConstructor((Class[])null).newInstance();
			} catch (Exception e) {
				Meddle.LOGGER.error("[Meddle] Couldn't initialize TweakClass " + mod.tweakClass, e);
				continue;
			}

			tweaks.add(tweaker);
		}
	}


	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {}

	@Override
	public String getLaunchTarget() {
		return null;
	}

	@Override
	public String[] getLaunchArguments() {
		return new String[0];
	}

}
