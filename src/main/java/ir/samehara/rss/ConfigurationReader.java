package ir.samehara.rss;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationReader {

    private Properties properties;
    public static ConfigurationReader getConfig = new ConfigurationReader();

    private ConfigurationReader() {
        this.properties = new Properties();
        try {
            this.properties.load(new FileReader("etc/rss.conf"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Properties getConfig() {
        return properties;
    }
}
