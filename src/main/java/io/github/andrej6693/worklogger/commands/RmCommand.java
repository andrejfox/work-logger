package io.github.andrej6693.worklogger.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.nio.file.Path;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static io.github.andrej6693.worklogger.Util.*;

public class RmCommand extends ListenerAdapter {
        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            if (event.getName().equals("rm")) {
                String path = Objects.requireNonNull(event.getOption("date")).getAsString();
                if (Objects.equals(path, "no date")) {
                    event.reply("Invalid date!").setEphemeral(true).queue();
                    return;
                }
                removeWork(Path.of(path), getPaymentTypeFromIndex(Objects.requireNonNull(event.getOption("type")).getAsInt()), Objects.requireNonNull(event.getOption("work")).getAsInt());
                event.reply("```" + "```").setEphemeral(true).queue();
            }
        }

        @Override
        public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
            if (event.getName().equals("rm") && event.getFocusedOption().getName().equals("date")) {
                String userInput = event.getFocusedOption().getValue();
                List<Command.Choice> options = collectJsonFiles(userInput);
                boolean isValidInput = options.stream().anyMatch(choice -> choice.getName().equalsIgnoreCase(userInput));

                if (!isValidInput) {
                    event.replyChoices(options).queue();
                }
            }

            if (event.getName().equals("rm") && event.getFocusedOption().getName().equals("type")) {
                String userInput = event.getFocusedOption().getValue();
                List<Command.Choice> options = collectTypes(userInput);
                boolean isValidInput = options.stream().anyMatch(choice -> choice.getName().equalsIgnoreCase(userInput));

                if (!isValidInput) {
                    event.replyChoices(options).queue();
                }
            }

            if (event.getName().equals("rm") && event.getFocusedOption().getName().equals("work")) {
                String userInput1 = event.getFocusedOption().getValue();
                List<Command.Choice> options = collectWorkEntries(Path.of(Objects.requireNonNull(event.getOption("date")).getAsString()), getPaymentTypeFromIndex(Objects.requireNonNull(event.getOption("type")).getAsInt()), userInput1);
                boolean isValidInput = options.stream().anyMatch(choice -> choice.getName().equalsIgnoreCase(userInput1));

                if (!isValidInput) {
                    event.replyChoices(options).queue();
                }
            }
        }

        public static CommandData register() {
            return Commands.slash("rm", "Removes work.")
                    .addOption(OptionType.STRING, "date", "Date of data to be deleted.", true, true)
                    .addOption(OptionType.INTEGER, "type", "Type of payment.", true, true)
                    .addOption(OptionType.STRING, "work", "name of work to be deleted.", true, true);
        }


}