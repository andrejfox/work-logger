package io.github.ANDREJ6693.discord_bot_java;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Main {
    public static final Properties CONFIG = readConfig();

    private static Properties readConfig() {
        Path path = Path.of("config.properties");

        createDefaultConfigIfNotExists(path);

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
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_REACTIONS)
            .build();

        api.retrieveUserById(CONFIG.getProperty("id")).queue(user -> {
            if (user != null) {
                for (int i = 0; i < Integer.parseInt(CONFIG.getProperty("repeat")); i++) {
                        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(CONFIG.getProperty("message")).queue());
                    try {
                        Thread.sleep(Integer.parseInt(CONFIG.getProperty("offset")));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                System.out.println("User not found.");
            }
        });
    }

    private static void createDefaultConfigIfNotExists(Path configPath) {
        try (InputStream inputStream = Main.class.getResourceAsStream("/default-config.properties")) {
            assert inputStream != null;
            Files.copy(inputStream, configPath);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Unable to create default config file.", e);
        }
    }
}
