package net.fybertech.meddle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;


public class Meddle implements ITweaker 
{
	public static final Logger LOGGER = LogManager.getLogger("Meddle");
	
	// Command-line arguments received from LaunchWrapper
	public final List<String> args = new ArrayList<String>();

	// Containers for all jars discovered in the Meddle folder
	public static final List<ModContainer> discoveredModsList = new ArrayList<ModContainer>();
	
	// Discovered mod IDs and their associated containers 
	public static final Map<String, ModContainer> loadedModsList = new HashMap<String, ModContainer>();

	// LaunchClassLoader's exception list, obtained via reflection
	static Set<String> classloaderExceptions = null;	
	

	// Changelog 
	// v1.2
	// - Added server-side compatibility
	// - Moved dynamic mappings to separate mod
	// - Added optional MeddleMod annotation
	// - Added mod priority (via MeddleMod)
	//
	// v1.2.1 
	// - Recompiled for Java 7
	//
	// v1.2.2
	// - Changed access of some objects for MeddleAPI


	@SuppressWarnings("unchecked")
	public Meddle()
	{
		// Prevent classloader collisions, mostly due to keeping
		// the deprecated DynamicMappings class.
		Launch.classLoader.addClassLoaderExclusion("org.objectweb.asm.");


		// Launchwrapper adds the package without a period on the end, which
		// covers any similarly-named packages.  We could solve by putting
		// Meddle's tweak class in a deeper package, but this works too
		// while maintaining backwards compatibility.

		try {
			Field exceptionsField = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
			exceptionsField.setAccessible(true);
			classloaderExceptions = (Set<String>) exceptionsField.get(Launch.classLoader);
		} catch (Exception e) { e.printStackTrace(); }

		classloaderExceptions.remove("net.fybertech.meddle");
		classloaderExceptions.add("net.fybertech.meddle.");
	}



	public static String getVersion()
	{
		return "1.2.2-alpha";
	}


	public static boolean isModLoaded(String modID)
	{
		return loadedModsList.get(modID) != null;
	}


	public static class ModContainer
	{
		public File jar;
		public Class<? extends ITweaker> tweakClass;
		public String transformerClass;
		String id;

		public MeddleMod meta;

		public ModContainer(File f)
		{
			jar = f;
		}
	}



	@SuppressWarnings("unchecked")
	private void checkJar(ModContainer mod)
	{
		Manifest manifest = null;
		try {
			manifest = new JarFile(mod.jar).getManifest();
		} catch (IOException e) {}
		if (manifest == null) return;

		Attributes attr = manifest.getMainAttributes();
		if (attr == null) return;

		String tweakClassName = attr.getValue("TweakClass");
		if (tweakClassName != null && tweakClassName.length() > 0)
		{
			LOGGER.info("[Meddle] Found tweak class in " + mod.jar.getName() + " (" + tweakClassName + ")");

			// We delay loading the tweaks until Stage Two because:
			// 1) Adding it to Launchwrapper's TweakClasses list adds the package to the
			// classloader exception list, resulting in class not found.
			// 2) You can't add it to the other Tweaks list because it's currently being iterated.
			// 3) We want to finish initializing everything before they're instantiated
			// 4) We still need to sort them

			Class<? extends ITweaker> tweakClass = null;
			try {
				tweakClass = (Class<? extends ITweaker>) Class.forName(tweakClassName, false, Launch.classLoader);
			} catch (Exception e) {}

			if (tweakClass != null) {
				mod.tweakClass = tweakClass;
				mod.meta = tweakClass.getAnnotation(MeddleMod.class);
				mod.id = mod.meta != null ? mod.meta.id() : mod.jar.getName();
				loadedModsList.put(mod.id, mod);
			}
			else Meddle.LOGGER.error("[Meddle] Couldn't load tweak class " + tweakClassName);
			return;
		}


		// NOTE: Transformer classes aren't sorted for dependencies!

		String transformerClassName = attr.getValue("TransformerClass");
		if (transformerClassName != null && transformerClassName.length() > 0)
		{
			LOGGER.info("[Meddle] Found transformer class in " + mod.jar.getName() + " (" + transformerClassName + ")");

			Class<? extends IClassTransformer> transformerClass = null;
			try {
				transformerClass = (Class<? extends IClassTransformer>) Class.forName(transformerClassName, false, Launch.classLoader);
			} catch (Exception e) {}

			if (transformerClass != null) {
				mod.transformerClass = transformerClassName;
				mod.meta = transformerClass.getAnnotation(MeddleMod.class);
				mod.id = mod.meta != null ? mod.meta.id() : mod.jar.getName();
				loadedModsList.put(mod.id, mod);
			}
			else Meddle.LOGGER.error("[Meddle] Couldn't load transformer class " + tweakClassName);
			return;
		}
	}


	@Override
	public void acceptOptions(List<String> inArgs, File gameDir, File assetsDir, String profile)
	{
		String version = MeddleUtil.findMinecraftVersion();
		LOGGER.info("[Meddle] Minecraft version detected: " + version + " (" + (MeddleUtil.isClientJar() ? "client)" : "server)"));

		@SuppressWarnings("unchecked")
		List<String> tweakClasses = (List<String>)Launch.blackboard.get("TweakClasses");
		tweakClasses.add(StageTwo.class.getName());

		this.args.clear();
		this.args.addAll(inArgs);

		if (profile != null) {
			this.args.add("--version");
			this.args.add(profile);
		}

		if (assetsDir != null) {
			this.args.add("--assetsDir");
			this.args.add(assetsDir.getPath());
		}

		if (gameDir == null) gameDir = new File(".");
		File tweakDir = new File(gameDir, "meddle/");
		tweakDir.mkdirs();

		File[] files = tweakDir.listFiles();
		Arrays.sort(files);
		for (File f : files) {
			if (!f.getName().toLowerCase().endsWith(".jar")) continue;

			// Add it to the classloader even if it's not a mod.  Allows for
			// libraries, resources, etc.
			try {
				Launch.classLoader.addURL(f.toURI().toURL());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			ModContainer mod = new ModContainer(f);
			discoveredModsList.add(mod);
			checkJar(mod);
		}
	}


	@Override
	public String[] getLaunchArguments() {
		return args.toArray(new String[args.size()]);
	}


	@Override
	public String getLaunchTarget() {
		if (MeddleUtil.isClientJar()) return "net.minecraft.client.main.Main";
		else return "net.minecraft.server.MinecraftServer";
	}


	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		for (ModContainer mod : discoveredModsList)
		{
			if (mod.transformerClass != null) {
				try {
					Class.forName(mod.transformerClass, true, classLoader);
				} catch (ClassNotFoundException e) {}
				classLoader.registerTransformer(mod.transformerClass);
			}
		}
	}

}
