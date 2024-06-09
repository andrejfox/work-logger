package io.github.andrej6693.worklogger.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.github.andrej6693.worklogger.Util.*;

public class RmCommand extends ListenerAdapter {
        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            if (event.getName().equals("rm")) {
                String pathString = Objects.requireNonNull(event.getOption("date")).getAsString();
                if (Objects.equals(pathString, "no date")) {
                    event.reply("Invalid date!").setEphemeral(true).queue();
                    return;
                }
                Path path = Path.of(pathString);
                String detail = removeWork(path, getPaymentTypeFromIndex(Objects.requireNonNull(event.getOption("type")).getAsInt()), Objects.requireNonNull(event.getOption("work")).getAsInt());

                String[] pathArr = Objects.requireNonNull(event.getOption("date")).getAsString().split("/");
                String fileName = pathArr[pathArr.length - 1];
                String fileName2 = fileName.substring(0, fileName.length() - 5);
                fileName2 = fileName2.replace("_", " ");
                System.out.println("/rm: [" + fileName + "] <" + getPaymentTypeFromIndex(Objects.requireNonNull(event.getOption("type")).getAsInt()).tag() + "> " + detail);
                event.reply("Removed " + fileName2 + " [" + getPaymentTypeFromIndex(Objects.requireNonNull(event.getOption("type")).getAsInt()).tag() + "]: " + detail).setEphemeral(true).queue();
            }
        }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        String commandName = event.getName();
        String focusedOptionName = event.getFocusedOption().getName();
        String userInput = event.getFocusedOption().getValue();
        List<Command.Choice> options = new ArrayList<>();

        String date;
        if (commandName.equals("rm")) {
            switch (focusedOptionName) {
                case "date":
                    options = collectJsonFiles(userInput);
                    break;
                case "type":
                    date = Objects.requireNonNull(event.getOption("date")).getAsString();
                    options = collectTypes(date, userInput);
                    break;
                case "work":
                    date = Objects.requireNonNull(event.getOption("date")).getAsString();
                    int typeIndex = Objects.requireNonNull(event.getOption("type")).getAsInt();
                    options = collectWorkEntries(Path.of(date), getPaymentTypeFromIndex(typeIndex), userInput);
                    break;
            }

            if (options == null) {
                return;
            }

            boolean isValidInput = options.stream().anyMatch(choice -> choice.getName().equalsIgnoreCase(userInput));
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