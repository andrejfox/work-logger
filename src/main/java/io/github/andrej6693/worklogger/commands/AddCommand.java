package io.github.andrej6693.worklogger.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;

import static io.github.andrej6693.worklogger.Util.*;

public class AddCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("add")) {
            int year;
            String month;
            Date date;
            try {
                date = parseDate(Objects.requireNonNull(event.getOption("date")).getAsString());
                year = getYear(date);
                month = getMonthName(date);
            } catch (ParseException e) {
                System.out.println("Invalid date format:\n" + e);
                event.reply("Invalid date format:\n" + e).queue();
                return;

            }
            Path path = Path.of("data/" + year + "/" + month + "_" + year + ".json");
            int index = Objects.requireNonNull(event.getOption("type")).getAsInt();
            WorkDetail workDetail = new WorkDetail(
                    date,
                    Objects.requireNonNull(event.getOption("duration")).getAsInt(),
                    Objects.requireNonNull(event.getOption("note")).getAsString()
            );

            createMonthJsonIfNotExists(path);
            addData(getPaymentTypeFromIndex(index), workDetail, path);

            event.reply("Successfully added work for " + getDateString(date)).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("add") && event.getFocusedOption().getName().equals("type")) {
            String userInput = event.getFocusedOption().getValue();
            List<Command.Choice> options = collectTypes(userInput);
            boolean isValidInput = options.stream().anyMatch(choice -> choice.getName().equalsIgnoreCase(userInput));

            if (!isValidInput) {
                event.replyChoices(options).queue();
            }
        }
    }

    public static CommandData register() {
        return Commands.slash("add", "Add work.")
                .addOption(OptionType.INTEGER, "type", "Type of payment.", true, true)
                .addOption(OptionType.STRING, "date", "Date of work. Format: dd/mm/yyyy", true)
                .addOption(OptionType.STRING, "duration", "Duration of work.", true)
                .addOption(OptionType.STRING, "note", "Description of work.", true);
    }
}