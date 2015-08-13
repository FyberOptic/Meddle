package net.fybertech.dynamicmappings;

import java.io.File;
import java.util.List;

import net.fybertech.meddle.MeddleMod;
import net.fybertech.meddle.MeddleUtil;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;


// Changelog
// 
// Build 004
// - Mapping annotation, allows methods providing class, field, and method mappings, with dependency control 
// - More mappings!
// - Fixed EntityList detection to adapt to Entity.travelToDimension's paramters changing in 15w33a



@MeddleMod(id="dynamicmappings", name="Dynamic Mappings", author="FyberOptic", version="005-alpha")
public class Tweaker implements ITweaker
{

	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile)
	{
		DynamicMappings.generateClassMappings();
		if (MeddleUtil.isClientJar()) DynamicClientMappings.generateClassMappings();
	}

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
