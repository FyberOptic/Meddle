package net.fybertech.meddle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.launchwrapper.LogWrapper;

public class Meddle implements ITweaker {
	
	public List<String> args = new ArrayList<String>();
	public List<String> transformerClasses = new ArrayList<String>();
	
	public void checkJar(File file)
	{		
		Manifest manifest = null;
		
		try {
			manifest = new JarFile(file).getManifest();			
		} catch (IOException e) {}
		
		if (manifest != null)
		{
			Attributes attr = manifest.getMainAttributes();
			if (attr != null)
			{
				String tweakClass = attr.getValue("TweakClass");
				if (tweakClass != null && tweakClass.length() > 0)
				{					
					LogWrapper.info("[Meddle] Found Tweakclass in " + file.getName() + " (" + tweakClass + ")");
					try {
						Launch.classLoader.addURL(file.toURI().toURL());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					transformerClasses.add(tweakClass);
				}
			}
		}
	}
	
	
	@Override
	public void acceptOptions(List<String> inArgs, File gameDir, File assetsDir, String profile) 
	{		
		this.args.addAll(inArgs);
		
		if (profile != null)
		{
			this.args.add("--version");
			this.args.add(profile);
		}
		
		if (assetsDir != null)
		{
			this.args.add("--assetsDir");
			this.args.add(assetsDir.getPath());
		}
		
		
		if (gameDir == null) gameDir = new File(".");
		File tweakDir = new File(gameDir, "meddle/");
		tweakDir.mkdirs();
		
		File[] files = tweakDir.listFiles();
		for (File f : files)
		{
			if (f.getName().toLowerCase().endsWith(".jar")) checkJar(f);
		}		
	}

	
	@Override
	public String[] getLaunchArguments() {
		return args.toArray(new String[args.size()]);
	}

	
	@Override
	public String getLaunchTarget() {
		return "net.minecraft.client.main.Main";
	}

	
	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		for (String s : transformerClasses) classLoader.registerTransformer(s);
	}
	
}
