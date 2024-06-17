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
import java.util.List;
import java.util.Objects;

public class PayCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("pay")) {
            String pathString = Objects.requireNonNull(event.getOption("date")).getAsString();
            if (Objects.equals(pathString, "no date")) {
                event.reply("Invalid date!").setEphemeral(true).queue();
                return;
            }
            Path path = Path.of(pathString);
            Util.setPayed(path, Objects.requireNonNull(event.getOption("amount")).getAsInt());

            Util.removeFromNotPayedList(path);
            Util.updateNotPayedBoard();

            String[] pathArr = Objects.requireNonNull(event.getOption("date")).getAsString().split("/");
            String fileName = pathArr[pathArr.length - 1];
            String fileName2 = fileName.substring(0, fileName.length() - 5);
            fileName2 = fileName2.replace("_", " ");
            System.out.println("/pay: [" + fileName + "] -> " + Util.readMonthDataFromFile(path).payStatus());
            event.reply("Set " + fileName2 + " as payed [" + Util.readMonthDataFromFile(path).payStatus().amount() + Util.CONFIG.currency() + "]").setEphemeral(true).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("pay") && event.getFocusedOption().getName().equals("date")) {
            String userInput = event.getFocusedOption().getValue();
            List<Command.Choice> options = Util.collectJsonFilesForPay(userInput);
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