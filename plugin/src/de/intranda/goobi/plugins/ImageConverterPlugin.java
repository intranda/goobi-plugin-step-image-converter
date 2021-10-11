package de.intranda.goobi.plugins;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.JpegInterpreter;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class ImageConverterPlugin implements IStepPlugin, IPlugin {
	private static final Logger logger = Logger.getLogger(ImageConverterPlugin.class);
	private static final String PLUGIN_NAME = "intranda_step_imageConverter";

	private String returnPath;
	private Step step;
	private Process process;

	@Override
	public PluginType getType() {
		return PluginType.Step;
	}

	@Override
	public String getTitle() {
		return PLUGIN_NAME;
	}

	public String getDescription() {
		return PLUGIN_NAME;
	}

	@Override
	public void initialize(Step step, String returnPath) {
		this.returnPath = returnPath;
		this.step = step;
		process = step.getProzess();
	}

	@Override
	public boolean execute() {
		try {
			File masterFolder = new File(process.getImagesOrigDirectory(false));
			File mediaFolder = new File(process.getImagesTifDirectory(false));
			convertMasterToJpeg(masterFolder, mediaFolder);
		} catch (SwapException | DAOException | IOException | InterruptedException | ContentLibException e) {
			logger.error("Error while converting images", e);
			Helper.addMessageToProcessLog(process.getId(), LogType.ERROR,
			        "Error while converting images in ImageConverterPlugin: " + e.getMessage());
		}
		return true;
	}

	@Override
	public String cancel() {
		return returnPath;
	}

	@Override
	public String finish() {
		return returnPath;
	}

	@Override
	public HashMap<String, StepReturnValue> validate() {
		return null;
	}

	@Override
	public Step getStep() {
		return step;
	}

	@Override
	public PluginGuiType getPluginGuiType() {
		return PluginGuiType.NONE;
	}

	public String getPagePath() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Convert all images of the master folder into jpg images without scaling
	 * 
	 * @param masterFolder
	 * @param mediaFolder
	 * @return a boolean that shows if a problem occurred
	 */
	private boolean convertMasterToJpeg(File masterFolder, File mediaFolder) throws FileNotFoundException, IOException, ContentLibException, ImageManipulatorException, ImageManagerException, MalformedURLException{
		// get all image files from the master folder
		File[] images = masterFolder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String myname = name.toLowerCase();
				return (myname.endsWith(".tif") || myname.endsWith(".tiff") || myname.endsWith(".jpeg")
		                || myname.endsWith(".jpg"));
			}
		});
		
		// if no images can be found, stop here
		if (images == null || images.length < 1) {
			logger.error("No tif or jpeg images found in master folder " + masterFolder.getAbsolutePath());
			return false;
		} 
		
		// create media folder if it still does not exist
		if (!mediaFolder.exists()){
			mediaFolder.mkdirs();
		}
		
		// run through all files and convert them to jpg
		for (File file : images) {
			ImageManager im = null;
			JpegInterpreter pi = null;
			try {
				im = new ImageManager(file.toURI().toURL());
				ImageInterpreter ii = im.getMyInterpreter();
				RenderedImage ri2 = ii.getRenderedImage();
				pi = new JpegInterpreter(ri2);
				pi.setXResolution(ii.getXResolution());
				pi.setYResolution(ii.getYResolution());
				File fileout = new File(mediaFolder, file.getName().substring(0,file.getName().lastIndexOf(".")) + ".jpg") ;
				FileOutputStream outputFileStream = new FileOutputStream(fileout);
				pi.writeToStream(null, outputFileStream);
				outputFileStream.close();
			} finally {
				if (im != null) {
					im.close();
				}
				if (pi != null) {
					pi.close();
				}
			}
		}
		return true;
	}

	/**
	 * main method to allow easier development
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		File masterFolder = new File("/opt/digiverso/goobi/metadata/38/images/master_abc_def_media");
		File mediaFolder = new File("/opt/digiverso/goobi/metadata/38/images/abc_def_media");

		ImageConverterPlugin icp = new ImageConverterPlugin();
		icp.convertMasterToJpeg(masterFolder, mediaFolder);
	}

}
