package io.github.andrejfox.worklogger.commands;

import io.github.andrejfox.worklogger.Util;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MailCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("mail")) {
            String path;
            if (event.getOption("date") == null) {
                Date today = new Date();
                path = "./data/" + Util.getYear(today) + "/" + Util.getMonthName(today) + "_" + Util.getYear(today) + ".json";
            } else {
                path = Objects.requireNonNull(event.getOption("date")).getAsString();
                if (Objects.equals(path, "no date")) {
                    event.reply("Invalid date!").setEphemeral(true).queue();
                    return;
                }
            }
            System.out.println(path);
            String[] pathArr = path.split("/");
            String fileName = pathArr[pathArr.length - 1];
            System.out.println("/mail: [" + fileName + "]");
            event.reply("```" + Objects.requireNonNull(Util.writeMail(Path.of(path))) + "```").setEphemeral(true).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("mail") && event.getFocusedOption().getName().equals("date")) {
            String userInput = event.getFocusedOption().getValue();
            List<Command.Choice> options = Util.collectJsonFiles(userInput);
            boolean isValidInput = options.stream().anyMatch(choice -> choice.getName().equalsIgnoreCase(userInput));

            if (!isValidInput) {
                event.replyChoices(options).queue();
            }
        }
    }

    public static CommandData register() {
        return Commands.slash("mail", "writes a mail.")
                .addOption(OptionType.STRING, "date", "Date of data for mail.", false, true);
    }
}