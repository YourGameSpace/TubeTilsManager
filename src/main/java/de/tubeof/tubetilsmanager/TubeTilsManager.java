package de.tubeof.tubetilsmanager;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class TubeTilsManager {

    private final String prefix;
    private final String snapshot;
    private final String version;
    private final Plugin runningPlugin;

    /**
     * Creates a new instance and manage TubeTils
     * @param prefix The prefix, which will be used in the console
     * @param plugin Your plugin main Class
     * @param snapshot The Snapshot-Version, which should be installed
     * @param version The Plugin-Version, which will be used for the update check
     * @param autoRun Determines whether the manager is automatically executed when the instance is created
     */
    public TubeTilsManager(String prefix, Plugin plugin, String snapshot, String version, boolean autoRun) {
        this.prefix = prefix;
        this.snapshot = snapshot;
        this.version = version;
        this.runningPlugin = plugin;

        if(autoRun) check();
    }

    private final ConsoleCommandSender ccs = Bukkit.getConsoleSender();
    private final PluginManager pluginManager = Bukkit.getPluginManager();
    private final Plugin tubeTils = pluginManager.getPlugin("TubeTils");

    @SuppressWarnings("ConstantConditions")
    public void check() {
        if(!isInstalled()) {
            download(snapshot);
            enablePlugin();
            return;
        }

        if(isInstalled()) {
            if(!getVersion().equals(version)) download(snapshot);
        }
    }

    public String getVersion() {
        return tubeTils != null ? tubeTils.getDescription().getVersion() : null;
    }

    private boolean isInstalled() {
        return tubeTils != null;
    }

    private float downloadProgress = 0;
    private void download(String downloadSnapshot) {
        try {
            URL url = new URL("https://hub.yourgamespace.com/repo/de/tubeof/TubeTils/" + downloadSnapshot + "/TubeTils-" + downloadSnapshot + ".jar");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "TubeApiBridgeConnector");
            connection.setRequestProperty("Header-Token", "SD998FS0FG07");
            int filesize = connection.getContentLength();

            Timer timer = new Timer();
            Thread thread = new Thread(() -> {
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        ccs.sendMessage(prefix + "Downloading TubeTils ...  " + (int)downloadProgress + "%");
                    }
                };
                timer.schedule(timerTask, 0, 250);
            });
            thread.start();

            float totalDataRead = 0;
            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            FileOutputStream fos = new FileOutputStream("plugins/TubeTils.jar");
            BufferedOutputStream bout = new BufferedOutputStream(fos,1024);
            byte[] data = new byte[1024];
            int i = 0;

            while((i=in.read(data,0,1024))>=0) {
                totalDataRead=totalDataRead+i;
                bout.write(data,0,i);
                downloadProgress = (totalDataRead*100) / filesize;
            }
            timer.cancel();
            thread.interrupt();
            ccs.sendMessage(prefix + "Downloading TubeTils ... " + (int)downloadProgress + "%");

            bout.close();
            in.close();

        } catch (IOException exception) {
            ccs.sendMessage(prefix + "Error while downloading TubeTils! Disabling plugin ...");
            exception.printStackTrace();

            pluginManager.disablePlugin(runningPlugin);
        }
    }

    private void enablePlugin() {
        try {
            File file = new File("plugins/TubeTils.jar");
            Plugin plugin = pluginManager.loadPlugin(file);
            pluginManager.enablePlugin(plugin);
        } catch (InvalidPluginException | InvalidDescriptionException exception) {
            ccs.sendMessage(prefix + "Error while enabling TubeTils! Disabling plugin ...");
            exception.printStackTrace();

            pluginManager.disablePlugin(runningPlugin);
        }
    }

}
