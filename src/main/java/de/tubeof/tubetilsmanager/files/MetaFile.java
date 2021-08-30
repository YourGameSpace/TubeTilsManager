package de.tubeof.tubetilsmanager.files;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MetaFile {

    private final ConsoleCommandSender ccs = Bukkit.getConsoleSender();

    private final String prefix;

    public MetaFile(String prefix, boolean autoCfg) {
        this.prefix = prefix;

        if(autoCfg) cfgMeta();
    }

    private final File file = new File("plugins/TubeTilsManager", "Meta.yml");
    private final FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

    private void saveMeta() {
        try {
            cfg.save(file);
        } catch (Exception exception) {
            ccs.sendMessage(prefix + "§cWARNING: Meta.yml could not be saved!");
            exception.printStackTrace();
        }
    }

    public void cfgMeta() {
        cfg.options().copyDefaults(true);
        cfg.options().header("WARNING! DO NOT CHANGE ANY VALUES HERE! THIS IS A META FILE!");

        cfg.addDefault("Build", -1);

        saveMeta();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception exception) {
                ccs.sendMessage(prefix + "§cWARNING: Meta.yml could not be created!");
                exception.printStackTrace();
            }
        }
    }

    /**
     * Set the current installed build, if a new version was installed
     * @param build The build number, of the new build
     */
    public void setBuild(int build) {
        cfg.set("Build", build);
        saveMeta();
    }

    /**
     * Get the current installed build
     * @return The installed build number
     */
    public int getBuild() {
        return cfg.getInt("Build");
    }
}
