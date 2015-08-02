package net.fybertech.meddle;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class StageTwo implements ITweaker {

	
	public StageTwo()
	{
		Meddle.LOGGER.info("[Meddle] Stage Two - Inserting tweaks");
		List<ITweaker> tweaks = (List<ITweaker>)Launch.blackboard.get("Tweaks");
		tweaks.addAll(Meddle.tweakCache);
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
