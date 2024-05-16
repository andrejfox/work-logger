package io.github.ANDREJ6693.discord_bot_java;

import io.github.ANDREJ6693.discord_bot_java.commands.AddCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static void main(String[] args) {
        Util.createDefaultConfigIfNotExists();
        Util.loadConfig();

        JDA api = JDABuilder.createDefault(Util.CONFIG.botToken())
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .addEventListeners(new AddCommand())
                .build();

        api.updateCommands().addCommands(
                AddCommand.register()
        ).queue();
    }
}
