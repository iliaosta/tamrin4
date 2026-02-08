package rps.server.core.config;

import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {

    private final Properties props = new Properties();

    // default values
    private final String defaultIp = "127.0.0.1";
    private final int defaultPort = 9000;
    private final int defaultWinsRequired = 3;
    private final int defaultRoundTimeout = 30;

    public ConfigManager() {
        load();
    }

    private void load() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.out.println("⚠️ config.properties not found. Using default values.");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Failed to read config.properties. Using default values.");
        }
    }

    public String serverIp() {
        return props.getProperty("server.ip", defaultIp);
    }

    public int serverPort() {
        return parseInt(props.getProperty("server.port"), defaultPort);
    }

    public int winsRequired() {
        return parseInt(props.getProperty("wins.required"), defaultWinsRequired);
    }

    public int roundTimeoutSeconds() {
        return parseInt(props.getProperty("round.timeout"), defaultRoundTimeout);
    }

    private int parseInt(String value, int def) {
        try {
            if (value == null) return def;
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
