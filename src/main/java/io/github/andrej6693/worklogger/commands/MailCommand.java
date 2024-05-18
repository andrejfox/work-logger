package io.github.andrej6693.worklogger.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MailCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("mail")) {
            event.reply(getPathFromDate(Objects.requireNonNull(event.getOption("date")).getAsString())).queue();
        }
    }

    public static CommandData register() {
        return Commands.slash("mail", "writes a mail.")
                .addOption(OptionType.STRING, "date", "Date of data for mail.", true, true);
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("mail") && event.getFocusedOption().getName().equals("date")) {
            List<Command.Choice> options = collectJsonFiles(event.getFocusedOption().getValue());
            event.replyChoices(options).queue();
        }
    }

    public static List<Command.Choice> collectJsonFiles(String query) {
        List<String> jsonFiles;
        try (Stream<Path> paths = Files.walk(Paths.get("./data/"), FileVisitOption.FOLLOW_LINKS)) {
            jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(fileName -> fileName.substring(0, fileName.length() - 5))
                    .toList();
        } catch (IOException e) {
            return new ArrayList<>();
        }

        String lowercaseQuery = query.toLowerCase();

        return jsonFiles.stream()
                .filter(item -> item.toLowerCase().contains(lowercaseQuery))
                .limit(25)
                .map(item -> new Command.Choice(item, item.toLowerCase()))
                .collect(Collectors.toList());
    }

    public static String getPathFromDate(String fileName) {
        String finalFileName = fileName.substring(0, 1).toUpperCase() + fileName.substring(1);
        try (Stream<Path> paths = Files.walk(Paths.get("./data/"), FileVisitOption.FOLLOW_LINKS)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> p.getFileName().toString().equals(finalFileName + ".json"))
                    .map(Path::toString)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}