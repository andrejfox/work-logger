package io.github.ANDREJ6693.discord_bot_java.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;

import static io.github.ANDREJ6693.discord_bot_java.Util.*;

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
                throw new RuntimeException(e);
            }
            Path path = Path.of("data/" + year + "/" + month + "_" + year + ".json");
            int type = Objects.requireNonNull(event.getOption("type")).getAsInt();
            WorkDetail workDetail = new WorkDetail(
                    date,
                    Objects.requireNonNull(event.getOption("duration")).getAsInt(),
                    Objects.requireNonNull(event.getOption("note")).getAsString()
            );

            createMonthJsonIfNotExists(month, year, path);
            addData(month, year, type, workDetail, path);

            event.reply("Successfully added work for " + getDateString(date)).queue();
        }
    }

    public static CommandData register() {
        return Commands.slash("add", "Add work.")
                .addOptions(
                        createWorkTypeOptionDate(),
                        new OptionData(OptionType.STRING, "date", "Date of work. Format: dd/mm/yyyy")
                                .setRequired(true),
                        new OptionData(OptionType.STRING, "duration", "Duration of work.")
                                .setRequired(true),
                        new OptionData(OptionType.STRING, "note", "Description of work.")
                                .setRequired(true)
                );
    }

    private static OptionData createWorkTypeOptionDate() {
        OptionData data = new OptionData(OptionType.INTEGER, "type", "Type of payment.");
        for (Integer workType : CONFIG.workTypes()) {
            data.addChoice(workType + " " + CONFIG.currency() + "/h", workType);
        }
        return data.setRequired(true);
    }
}