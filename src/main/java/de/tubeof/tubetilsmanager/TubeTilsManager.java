package de.tubeof.tubetilsmanager;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class TubeTilsManager {

    private final ConsoleCommandSender ccs = Bukkit.getConsoleSender();
    private final PluginManager pluginManager = Bukkit.getPluginManager();
    private final Plugin tubeTils = pluginManager.getPlugin("TubeTils");

    private final String prefix;
    private final int snapshot;
    private final String snapshotBuild;
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
    public TubeTilsManager(String prefix, Plugin plugin, int snapshot, String version, boolean autoRun) {
        this.prefix = prefix;
        this.snapshot = snapshot;
        this.snapshotBuild = "SNAPSHOT-" + this.snapshot;
        this.version = version;
        this.runningPlugin = plugin;

        if(autoRun) check();
    }

    private boolean isOnline = false;

    @SuppressWarnings("ConstantConditions")
    public void check() {
        onlineCheck();
        if(!isOnline) {
            ccs.sendMessage(prefix + "§cTubeTils could not be installed automatically: No connection to the internet could be established.");
            ccs.sendMessage(prefix + "§cTubeTils must be downloaded manually and then added to the plugins folder: " + getJenkinsDownloadUrl());
            pluginManager.disablePlugin(runningPlugin);
            return;
        }

        if(!isInstalled()) {
            download(snapshotBuild);
            enablePlugin();
            return;
        }

        if(isInstalled()) {
            if(!getVersion().equals(version)) {
                ccs.sendMessage(prefix + "§eThe currently installed TubeTils version does not meet the requirements! Disabling installed version ...");
                pluginManager.disablePlugin(tubeTils);

                download(snapshotBuild);
                enablePlugin();
            }
        }
    }

    private void onlineCheck() {
        boolean google = false;
        boolean cloudflare = false;

        try {
            google = InetAddress.getByName("8.8.8.8").isReachable(5000);
            cloudflare = InetAddress.getByName("1.1.1.1").isReachable(5000);
        } catch (IOException exception) {
            //Only catch
        }

        isOnline = google || cloudflare;
    }

    private String getJenkinsDownloadUrl() {
        return "https://hub.yourgamespace.com/jenkins/view/Libs/job/TubeTils/" + snapshot + "/artifact/target/TubeTils-" + snapshotBuild + ".jar";
    }

    public String getVersion() {
        return tubeTils != null ? tubeTils.getDescription().getVersion() : null;
    }

    private boolean isInstalled() {
        return tubeTils != null;
    }

    private float downloadProgress = 0;
    private Timer downloadTimer;
    private Thread downloadThread;
    private void download(String downloadSnapshot) {
        try {
            URL url = new URL("https://hub.yourgamespace.com/repo/de/tubeof/TubeTils/" + downloadSnapshot + "/TubeTils-" + downloadSnapshot + ".jar");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "TubeTilsUpdateChecker");
            connection.setRequestProperty("Header-Token", "SD998FS0FG07");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);

            // Check status code
            int responseCode = connection.getResponseCode();
            if(responseCode == 429) {
                ccs.sendMessage(prefix + "§cTubeTils could not be installed automatically: Request got blocked by rate limit!");
                ccs.sendMessage(prefix + "§cTubeTils must be downloaded manually and then added to the plugins folder: " + getJenkinsDownloadUrl());
                return;
            }
            else if(responseCode != 200) {
                ccs.sendMessage(prefix + "§cTubeTils could not be installed automatically: An unknown error has occurred!");
                ccs.sendMessage(prefix + "§cTubeTils must be downloaded manually and then added to the plugins folder: " + getJenkinsDownloadUrl());
                return;
            }

            // Connect
            try {
                connection.connect();
            } catch (SocketTimeoutException exception) {
                ccs.sendMessage(prefix + "§cTubeTils could not be installed automatically: Connection timeout!");
                ccs.sendMessage(prefix + "§cTubeTils must be downloaded manually and then added to the plugins folder: " + getJenkinsDownloadUrl());
                return;
            }

            int filesize = connection.getContentLength();

            downloadTimer = new Timer();
            downloadThread = new Thread(() -> {
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        ccs.sendMessage(prefix + "Downloading TubeTils ...  " + (int)downloadProgress + "%");
                    }
                };
                downloadTimer.schedule(timerTask, 0, 250);
            });
            downloadThread.start();

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
            downloadTimer.cancel();
            downloadThread.interrupt();
            ccs.sendMessage(prefix + "Downloading TubeTils ... " + (int)downloadProgress + "%");

            bout.close();
            in.close();

        } catch (IOException exception) {
            ccs.sendMessage(prefix + "§cError while downloading TubeTils! Disabling plugin ...");
            exception.printStackTrace();

            downloadTimer.cancel();
            downloadThread.interrupt();
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
