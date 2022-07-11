package de.intranda.goobi.plugins;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

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
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class ImageDuplicatorPlugin implements IStepPlugin, IPlugin {
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
        return getTitle();
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
            StorageProvider.getInstance().copyDirectory(masterFolder, mediaFolder);
        } catch (Exception e) {
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
        return null;
    }

    /**
     * main method to allow easier development
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {
        File masterFolder = new File("/opt/digiverso/g2g/goobi/metadata/11/images/master_06022001_media");
        File mediaFolder = new File("/opt/digiverso/g2g/goobi/metadata/11/images/06022001_media");
        StorageProvider.getInstance().copyDirectory(masterFolder.toPath(), mediaFolder.toPath());
    }

}
