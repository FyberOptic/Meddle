package net.fybertech.meddle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class Meddle implements ITweaker {

	public List<String> args = new ArrayList<String>();
	public List<String> transformerClasses = new ArrayList<String>();

	public static List<ITweaker> tweakCache = new ArrayList<ITweaker>();
	public static final Logger LOGGER = LogManager.getLogger("Meddle");
	


	public Meddle() {
		Launch.classLoader.addClassLoaderExclusion("org.objectweb.asm.");
	}


	public static String getVersion()
	{
		return "1.1";
	}


	private void checkJar(File file)
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
					LOGGER.info("[Meddle] Found tweak class in " + file.getName() + " (" + tweakClass + ")");
					try {
						Launch.classLoader.addURL(file.toURI().toURL());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}

					// We delay loading the tweaks for two reasons:
					// 1) Adding it to Launchwrapper's TweakClasses list adds the package to the
					// classloader exception list, resulting in class not found.
					// 2) You can't add it to the other Tweaks list because it's being iterated.

					ITweaker tweaker = null;
					try {
						tweaker = (ITweaker) Class.forName(tweakClass, true, Launch.classLoader).newInstance();
					} catch (Exception e) {
						e.printStackTrace();
					}
					tweakCache.add(tweaker);
				}

				String transformerClass = attr.getValue("TransformerClass");
				if (transformerClass != null && transformerClass.length() > 0)
				{
					LOGGER.info("[Meddle] Found transformer class in " + file.getName() + " (" + transformerClass + ")");
					try {
						Launch.classLoader.addURL(file.toURI().toURL());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					transformerClasses.add(transformerClass);
				}
			}
		}
	}


	@Override
	public void acceptOptions(List<String> inArgs, File gameDir, File assetsDir, String profile)
	{
		List<String> tweakClasses = (List<String>)Launch.blackboard.get("TweakClasses");
		tweakClasses.add(StageTwo.class.getName());

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
		Arrays.sort(files);
		for (File f : files)
		{
			if (f.getName().toLowerCase().endsWith(".jar")) checkJar(f);
		}

		String version = MeddleUtil.findMinecraftVersion();
		LOGGER.info("[Meddle] Minecraft version detected: " + version);

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
