package de.intranda.goobi.plugins;

import java.awt.image.RenderedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;
import org.jfree.util.Log;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.NIOFileUtils;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.JpegInterpreter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@Log4j2
@PluginImplementation
public class ImageConverterPlugin implements IStepPlugin, IPlugin {
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
            Path masterFolder = Paths.get(process.getImagesOrigDirectory(false));
            Path mediaFolder = Paths.get(process.getImagesTifDirectory(false));
            convertMasterToJpeg(masterFolder, mediaFolder);
        } catch (SwapException | DAOException | IOException | InterruptedException | ContentLibException e) {
            log.error("Error while converting images", e);
            Helper.addMessageToProcessLog(process.getId(), LogType.ERROR, "Error while converting images in ImageConverterPlugin: " + e.getMessage());
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

    @Override
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
    private boolean convertMasterToJpeg(Path masterFolder, Path mediaFolder)
            throws FileNotFoundException, IOException, ContentLibException, ImageManipulatorException, ImageManagerException, MalformedURLException {
        // get all image files from the master folder

        List<Path> images = StorageProvider.getInstance()
                .listFiles(masterFolder.toString(), NIOFileUtils.imageNameFilter);

        // if no images can be found, stop here
        if (images == null || images.isEmpty()) {
            log.error("No tif or jpeg images found in master folder " + masterFolder.toString());
            return false;
        }

        // create media folder if it still does not exist
        if (!StorageProvider.getInstance().isFileExists(mediaFolder)) {
            StorageProvider.getInstance().createDirectories(mediaFolder);
        }

        // run through all files and convert them to jpg
        for (Path file : images) {
            ImageManager im = null;
            JpegInterpreter pi = null;
            try {
                if (ConfigurationHelper.getInstance().useS3()) {
                    try {
                        URI uri = new URI(file.toString().replace(ConfigurationHelper.getInstance().getMetadataFolder(), "s3://" + ConfigurationHelper.getInstance().getS3Bucket() + "/"));
                        im = new ImageManager(uri);
                    } catch (URISyntaxException e) {
                        Log.error(e);
                    }
                } else {
                    im = new ImageManager(file.toUri());
                }
                ImageInterpreter ii = im.getMyInterpreter();
                RenderedImage ri2 = ii.getRenderedImage();
                pi = new JpegInterpreter(ri2);
                pi.setXResolution(ii.getXResolution());
                pi.setYResolution(ii.getYResolution());
                Path out = Paths.get(mediaFolder.toString(),
                        file.getFileName().toString().substring(0, file.getFileName().toString().lastIndexOf(".")) + ".jpg");
                OutputStream outputFileStream = StorageProvider.getInstance().newOutputStream(out);

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
    public static void main(String[] args) throws Exception {
        Path masterFolder = Paths.get("/opt/digiverso/goobi/metadata/38/images/master_abc_def_media");
        Path mediaFolder = Paths.get("/opt/digiverso/goobi/metadata/38/images/abc_def_media");

        ImageConverterPlugin icp = new ImageConverterPlugin();
        icp.convertMasterToJpeg(masterFolder, mediaFolder);
    }

}
