package net.fybertech.meddleapi.tweak;

import java.io.File;
import java.util.List;

import net.fybertech.meddle.MeddleMod;
import net.fybertech.meddle.MeddleUtil;
import net.fybertech.meddleapi.transformer.ClientTransformer;
import net.fybertech.meddleapi.transformer.ReobfTransformer;
import net.fybertech.meddleapi.transformer.Transformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

@MeddleMod(id="meddleapi", name="MeddleAPI", version="1.0", author="FyberOptic", depends={"dynamicmappings"})
public class APITweaker implements ITweaker
{
	
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) 
	{		
		classLoader.registerTransformer(Transformer.class.getName());
		if (MeddleUtil.isClientJar()) classLoader.registerTransformer(ClientTransformer.class.getName());
		classLoader.registerTransformer(ReobfTransformer.class.getName());
	}

	@Override
	public String getLaunchTarget() {
		return null;
	}

	@Override
	public String[] getLaunchArguments() {
		return new String[0];
	}

}
