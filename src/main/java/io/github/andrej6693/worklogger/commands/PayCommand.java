package io.github.andrej6693.worklogger.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static io.github.andrej6693.worklogger.Util.*;

public class PayCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("pay")) {
            String path = Objects.requireNonNull(event.getOption("date")).getAsString();
            if (Objects.equals(path, "no date")) {
                event.reply("Invalid date!").setEphemeral(true).queue();
                return;
            }
            setPayed(Path.of(path), Objects.requireNonNull(event.getOption("amount")).getAsInt());
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("pay") && event.getFocusedOption().getName().equals("date")) {
            String userInput = event.getFocusedOption().getValue();
            List<Command.Choice> options = collectJsonFiles(userInput);
            boolean isValidInput = options.stream().anyMatch(choice -> choice.getName().equalsIgnoreCase(userInput));

            if (!isValidInput) {
                event.replyChoices(options).queue();
            }
        }
    }

    public static CommandData register() {
        return Commands.slash("pay", "Sets a month as payed.")
                .addOption(OptionType.STRING, "date", "Date of data to be payed.", true, true)
                .addOption(OptionType.INTEGER, "amount", "Amount that was payed.", true);
    }
}