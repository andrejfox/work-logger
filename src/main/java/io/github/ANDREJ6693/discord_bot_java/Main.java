package io.github.ANDREJ6693.discord_bot_java;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Main {
    public static final Properties CONFIG = readConfig();

    private static Properties readConfig() {
        Path path = Path.of("config.properties");
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            Properties properties = new Properties();
            properties.load(reader);
            return properties;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {
        JDA api = JDABuilder.createDefault(CONFIG.getProperty("botToken"))
            .addEventListeners(new EventListener())
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .build();
    }
}
