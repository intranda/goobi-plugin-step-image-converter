package de.intranda.goobi.plugins;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import de.sub.goobi.helper.StorageProvider;
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
public class ImageDuplicatorPlugin implements IStepPlugin, IPlugin {
	private static final Logger logger = Logger.getLogger(ImageDuplicatorPlugin.class);
	private static final String PLUGIN_NAME = "intranda_step_imageDuplicator";

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
			StorageProvider.getInstance().copyDirectory(masterFolder.toPath(), mediaFolder.toPath());
		} catch (Exception e) {
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
	 * main method to allow easier development
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		File masterFolder = new File("/opt/digiverso/g2g/goobi/metadata/11/images/master_06022001_media");
		File mediaFolder = new File("/opt/digiverso/g2g/goobi/metadata/11/images/06022001_media");
	    StorageProvider.getInstance().copyDirectory(masterFolder.toPath(), mediaFolder.toPath());
	}

}
