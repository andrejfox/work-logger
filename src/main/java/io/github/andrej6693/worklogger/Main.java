package io.github.andrej6693.worklogger;

import io.github.andrej6693.worklogger.commands.AddCommand;
import io.github.andrej6693.worklogger.commands.MailCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static void main(String[] args) {
        Util.createDefaultIfNotExists("mail.txt", "/default-mail.txt");
        Util.createDefaultIfNotExists("config.toml", "/default-config.toml");
        Util.loadConfig();
        Util.loadTagOrder();

        JDA api = JDABuilder.createDefault(Util.CONFIG.botToken())
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .addEventListeners(
                        new AddCommand(),
                        new MailCommand())
                .build();

        api.updateCommands().addCommands(
                AddCommand.register(),
                MailCommand.register()
        ).queue();
    }
}
