package net.fybertech.servergenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ServerGenerator 
{

	public static final String SERVER_URL = "http://s3.amazonaws.com/Minecraft.Download/versions/<VERSION>/minecraft_server.<VERSION>.jar";
	public static final String MEDDLE_URL = "http://www.fybertech.net/maven/net/fybertech/meddle/<VERSION>/meddle-<VERSION>.jar";	
	public static final String LIBRARY_URL_BASE = "https://libraries.minecraft.net/";
	public static final String[] LIBRARIES = {"net.minecraft:launchwrapper:1.11", "org.ow2.asm:asm-all:5.0.3", "net.sf.jopt-simple:jopt-simple:4.6"};
	public static final String MAIN_CLASS = "net.fybertech.meddle.server.Main";
	
		
	public static byte[] downloadFile(String urlstring)	
	{
		try {	
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			URL url = new URL(urlstring);
			URLConnection connection = url.openConnection();
			InputStream is = connection.getInputStream();	
			
			byte[] downloadbuffer = new byte[1024];
			int count;
			while((count = is.read(downloadbuffer)) != -1)
			{
				baos.write(downloadbuffer, 0, count);
			}
			
			baos.close();
			is.close();
			
			return baos.toByteArray();
		}
		catch (IOException e)
		{
			return null;
		}
	}
	
	
	public static void writeDataToFile(File file, byte[] data)
	{		
		file.toPath().getParent().toFile().mkdirs();
		
		try {
			FileOutputStream stream = new FileOutputStream(file);
			stream.write(data);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	
	public static byte[] getFileFromZip(ZipEntry entry, ZipFile zipFile)
	{
		byte[] buffer = null;		

		if (entry != null)
		{			
			try {
				InputStream stream = zipFile.getInputStream(entry);				
				int pos = 0;
				buffer = new byte[(int)entry.getSize()];
				while (true)
				{
					int read = stream.read(buffer, pos, Math.min(1024, (int)entry.getSize() - pos));					
					pos += read;					
					if (read < 1) break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return buffer;
	}
	
	
	public static void writeFilesToJar(JarOutputStream outJar, Map<String, byte[]> files) throws IOException 
	{
		for (String filename : files.keySet())
		{
			ZipEntry ze = new ZipEntry(filename);
			outJar.putNextEntry(ze);
			outJar.write(files.get(filename));
		}		
	}
	
	
	
	
	public static Map<String, byte[]> readFilesFromZipStream(InputStream stream) throws IOException
	{
		Map<String, byte[]> files = new HashMap<String, byte[]>();
		
		ZipInputStream zip = new ZipInputStream(stream);
		while (true) 
		{
			ZipEntry entry = null;
			entry = zip.getNextEntry();			
			if (entry == null) break;
			if (entry.isDirectory()) continue;
			
			int size = (int)entry.getSize();
			byte[] data = new byte[size];
			
			int pos = 0;
			while (true) {
				int read = zip.read(data, pos, Math.min(4096, size - pos));					
				pos += read;					
				if (read < 1) break;
			}
			
			files.put(entry.getName(), data);
		}
		
		return files;
	}
	
	
	public static void removeFiles(Map<String, byte[]> files, String pattern)
	{
		List<String> removeQueue = new ArrayList<String>();		
		for (String filename : files.keySet()) {
			if (filename.startsWith(pattern)) removeQueue.add(filename);
		}
		for (String filename : removeQueue) {
			files.remove(filename);
		}
	}
	
	
	public static boolean downloadFileTo(String url, File dest)
	{
		byte[] bytes = downloadFile(url);		
		if (bytes == null) return false;
		writeDataToFile(dest, bytes);
		return true;		
	}
	
	
	public static void main(String[] args) throws IOException
	{
		if (args.length < 3) { 
			System.out.println("Syntax: ServerGenerator <server_version> <meddle_version> <destination_dir>"); 
			return; 
		}
		
		String serverVersion = args[0];
		String meddleVersion = args[1];
		String destinationDirName = args[2];
		
		File destinationDir = new File(destinationDirName);
		File librariesDir = new File(destinationDir, "libraries/");		
		
		String serverUrl = SERVER_URL.replace("<VERSION>", serverVersion);
		String meddleUrl = MEDDLE_URL.replace("<VERSION>", meddleVersion);
		
		System.out.print("Downloading Minecraft server " + serverVersion + "...");
		String serverFilename = "minecraft_server." + serverVersion + ".jar";
		File serverFile = new File(librariesDir, serverFilename);
		if (downloadFileTo(serverUrl, serverFile)) System.out.println("done");
		else { System.out.println("failed!"); System.exit(1); }	
		
		String classpath = "libraries/" + serverFilename;
		
		Map<String, byte[]> libraryBytes = new HashMap<String, byte[]>();
		for (String library : LIBRARIES) 
		{
			String[] split = library.split(":");
			String shortName = split[1] + "-" + split[2];
			String expanded = split[0].replace(".",  "/") + "/" + split[1] + "/" + split[2] + "/" + shortName;
			if (split.length > 3) expanded += "-" + split[3];
			String libUrl = LIBRARY_URL_BASE + expanded + ".jar";			
			
			System.out.print("Downloading library " + shortName + "...");
			if (downloadFileTo(libUrl, new File(librariesDir, shortName + ".jar"))) System.out.println("done");
			else { System.out.println("failed!"); System.exit(1); }
			
			classpath += " libraries/" + shortName + ".jar";
		}		
		
		System.out.print("Downloading Meddle " + meddleVersion + "...");
		byte[] meddleBytes = downloadFile(meddleUrl);
		System.out.println(meddleBytes != null ? "done" : "failed");
		if (meddleBytes == null) System.exit(1);	
		
		
		System.out.print("Customizing jar...");
		Map<String, byte[]> meddleFiles = readFilesFromZipStream(new ByteArrayInputStream(meddleBytes));
		
		ByteArrayOutputStream manifestStream = new ByteArrayOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(manifestStream));			
		String separator = System.getProperty("line.separator");
		writer.write("Manifest-Version: 1.0" + separator);
		writer.write("Main-Class: " + MAIN_CLASS + separator);
		writer.write("Class-Path: " + classpath + separator);		
		writer.close();
		
		meddleFiles.put("META-INF/MANIFEST.MF", manifestStream.toByteArray());
		
		JarOutputStream outJar = new JarOutputStream(new FileOutputStream(new File(destinationDir, "meddle_server." + serverVersion + "-" + meddleVersion + ".jar")));		
		writeFilesToJar(outJar, meddleFiles);
		outJar.close();
		System.out.println("done");
		
	}


	
}

