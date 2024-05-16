package io.github.ANDREJ6693.discord_bot_java;

import com.google.gson.*;
import com.moandjiezana.toml.Toml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class Util {
    private Util () {}

    public record Config(String botToken, String currency, List<Integer> workTypes) {}
    public static Config CONFIG = null;

    public record MonthData(String month, int year, List<WorkEntry> workEntries) {}
    public record WorkEntry(int paymentType, List<WorkDetail> workDetails) {}
    public record WorkDetail(Date date, int duration, String note) {}

    public static void loadConfig() {
        CONFIG = new Toml().read(new File("config.toml")).to(Config.class);
    }
    public static void createDefaultConfigIfNotExists() {
        try (InputStream inputStream = Main.class.getResourceAsStream("/default-config.toml")) {
            assert inputStream != null;
            Files.copy(inputStream, Path.of("config.toml"));
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Unable to create default config file.", e);
        }
    }

    public static void addData(String month, int year, int paymentType, WorkDetail workDetail, Path path) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").setPrettyPrinting().create();
        MonthData monthData = readMonthDataFromFile(path);

        if (monthData == null) {
            monthData = new MonthData(month, year, new ArrayList<>());
        }

        if (monthData.workEntries.isEmpty()) {
            monthData.workEntries.add(new WorkEntry(paymentType, new ArrayList<>()));
        }

        int index;
        for (index = 0; index < monthData.workEntries.size(); index++) {
            if (Objects.equals(monthData.workEntries.get(index).paymentType, paymentType)) {
                break;
            }
        }

        if (index == monthData.workEntries.size()) {
            monthData.workEntries.add(new WorkEntry(paymentType, new ArrayList<>()));
            monthData.workEntries.getLast().workDetails.add(workDetail);
        } else {
            monthData.workEntries.get(index).workDetails.add(workDetail);
        }


        String jsonData = gson.toJson(monthData);
        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
            fileWriter.write(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createMonthJsonIfNotExists(String month, int year, Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Unable to create data folder or file.", e);
        }

        try {
            Gson gson = new Gson();
            String jsonData = gson.toJson(new MonthData(month, year, new ArrayList<>()));
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            try (FileWriter fileWriter = new FileWriter(path.toFile())) {
                fileWriter.write(jsonData);
            }
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Unable to create data folder or file.", e);
        }
    }

    public static Date parseDate(String stringDate) throws ParseException {
        DateFormat df = new SimpleDateFormat("d/M/y");
        return df.parse(stringDate);
    }

    public static String getMonthName(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return new SimpleDateFormat("MMMM", Locale.ENGLISH).format(date);
    }

    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    public static String getDateString(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.forLanguageTag("sl-SI"));
        return df.format(calendar.getTime());
    }

    private static MonthData readMonthDataFromFile(Path path) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(Files.readString(path), MonthData.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading MonthData from file", e);
        }
    }
}