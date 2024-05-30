package io.github.andrej6693.worklogger;

import com.google.gson.*;
import com.moandjiezana.toml.Toml;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public final class Util {
    private Util () {}

    public record Config(String botToken, String languageTag, String currency, List<PaymentType> paymentTypes) {}
    public static Config CONFIG = null;

    public record MonthData(PayStatus payStatus, List<WorkEntry> workEntries) {}
    public record WorkEntry(PaymentType paymentType, List<WorkDetail> workDetails) {}
    public record WorkDetail(Date date, int duration, String note) {}
    public record PayStatus(boolean payed, int amount) {}
    public record PaymentType(String tag, int type) {}

    private static final HashMap<String, Integer> tagOrder = new HashMap<>();

    public static void loadConfig() {
        CONFIG = new Toml().read(new File("config.toml")).to(Config.class);
    }
    public static void loadTagOrder() {
        for (int i = 0; i < CONFIG.paymentTypes.size(); i++) {
            tagOrder.put(CONFIG.paymentTypes.get(i).tag, i);
        }
    }
    public static void createDefaultIfNotExists(String name, String defaultLocation) {
        try (InputStream inputStream = Main.class.getResourceAsStream(defaultLocation)) {
            Files.copy(Objects.requireNonNull(inputStream), Path.of(name));
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Unable to create default " + name + " file.", e);
        }
    }

    public static String writeMail(Path path) {
        Gson gson = new Gson();
        MonthData data;
        String mail;
        try {
            data = gson.fromJson(Files.readString(path), MonthData.class);
            mail =  Files.readString(Path.of("mail.txt"));
        } catch (IOException e) {
            throw new RuntimeException("Error reading MonthData from file", e);
        }

        boolean empty = true;
        for (WorkEntry workEntry : data.workEntries) {
            if (!workEntry.workDetails.isEmpty()) {
                empty = false;
                break;
            }
        }
        if (empty) {
            return "No data for this month!";
        }

        mail = mail.replace("{MONTH}", getMonthLocalName(data));
        mail = mail.replace("{WORK_DATA}", createWorkData(data));
        mail = mail.replace("{DATA_SUM}", createDataSum(data));

        return mail;
    }

    public static String createWorkData(MonthData data) {
        StringBuilder workDataBuilder = new StringBuilder();

        for (WorkEntry workEntry : data.workEntries) {
            String tag = workEntry.paymentType.tag;
            int multiplier = workEntry.paymentType.type;
            int hoursOfWork = 0;
            StringBuilder workEntryBuilder = new StringBuilder();

            int maxDateLength = 0;
            int maxDurationLength = 0;
            for (WorkDetail workDetail : workEntry.workDetails) {
                int dazeLength = new SimpleDateFormat("d.M.", Locale.forLanguageTag(CONFIG.languageTag)).format(workDetail.date).length();
                int durationLength = Integer.toString(workDetail.duration).length();
                if (maxDateLength < dazeLength) {
                    maxDateLength = dazeLength;
                }
                if (maxDurationLength < durationLength) {
                    maxDurationLength = durationLength;
                }
            }

            for (WorkDetail workDetail : workEntry.workDetails) {
                StringBuilder date = new StringBuilder(new SimpleDateFormat("d.M.", Locale.forLanguageTag(CONFIG.languageTag)).format(workDetail.date));
                if (date.length() != maxDateLength) {
                    for (int i = 0; i < maxDateLength - date.length(); i++) {
                        date.insert(0," ");
                    }
                }

                String duration = workDetail.duration + "h";
                if (duration.length() - 1 < maxDurationLength) {
                    for (int i = 0; i < maxDurationLength - (duration.length() - 1); i++) {
                        duration = duration.concat(" ");
                    }
                }

                workEntryBuilder
                        .append("   ")
                        .append(date)
                        .append(" - ")
                        .append(duration)
                        .append(" - ")
                        .append(workDetail.note)
                        .append("\n");

                hoursOfWork += workDetail.duration;
            }

            workDataBuilder
                    .append(tag)
                    .append(": [").append(hoursOfWork)
                    .append("h * ").append(multiplier)
                    .append(CONFIG.currency)
                    .append("/h = ")
                    .append(hoursOfWork * multiplier)
                    .append(CONFIG.currency)
                    .append("]\n")
                    .append(workEntryBuilder)
                    .append("\n");
        }

        workDataBuilder.delete(workDataBuilder.length() - 2, workDataBuilder.length());
        return String.valueOf(workDataBuilder);
    }

    public static String createDataSum(MonthData data) {
        StringBuilder dataSumBuilder = new StringBuilder();

        int max = 0;
        int sum = 0;
        for (WorkEntry workEntry : data.workEntries) {
            String tag = workEntry.paymentType.tag;
            int multiplier = workEntry.paymentType.type;
            int hoursOfWork = 0;
            int stringSize;
            for (WorkDetail workDetail : workEntry.workDetails) {
                hoursOfWork += workDetail.duration;
            }
            sum += multiplier * hoursOfWork;

            dataSumBuilder
                    .append(multiplier * hoursOfWork)
                    .append(CONFIG.currency)
                    .append(" ");

            dataSumBuilder
                    .append("[")
                    .append(tag)
                    .append("]\n");

            stringSize = Integer.toString(multiplier * hoursOfWork).length() + tag.length() + CONFIG.currency.length() + 3;
            if (stringSize > max) max = stringSize;
        }

        dataSumBuilder.append("=".repeat(max));
        dataSumBuilder.append("\n");
        dataSumBuilder.append(sum).append(CONFIG.currency);

        return String.valueOf(dataSumBuilder);
    }

    public static String getMonthLocalName(MonthData data) {
        Date date = null;
        for (WorkEntry workEntry : data.workEntries) {
            if (!workEntry.workDetails.isEmpty()) {
                date = workEntry.workDetails.getFirst().date();
                break;
            }
        }

        Calendar calendar = Calendar.getInstance();
        assert date != null;
        calendar.setTime(date);
        return new SimpleDateFormat("MMMM", Locale.forLanguageTag(CONFIG.languageTag)).format(date);
    }

    public static void addData(PaymentType paymentType, WorkDetail workDetail, Path path) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").setPrettyPrinting().create();
        MonthData monthData = readMonthDataFromFile(path);

        if (monthData == null) {
            monthData = new MonthData(new PayStatus(false, 0),new ArrayList<>());
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

        monthData.workEntries.get(index).workDetails.sort(Comparator.comparing(WorkDetail::date));
        monthData.workEntries().sort(Comparator.comparing(entry ->
                tagOrder.getOrDefault(entry.paymentType().tag(), Integer.MAX_VALUE)
        ));

        String jsonData = gson.toJson(monthData);
        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
            fileWriter.write(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPayed(Path path, int amount){
        MonthData monthData = readMonthDataFromFile(path);
        monthData = new MonthData(new PayStatus(true, amount), monthData.workEntries);

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").setPrettyPrinting().create();
        String jsonData = gson.toJson(monthData);
        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
            fileWriter.write(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createMonthJsonIfNotExists(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Unable to create data folder or file.", e);
        }

        try {
            Gson gson = new Gson();
            String jsonData = gson.toJson(new MonthData(new PayStatus(false, 0), new ArrayList<>()));
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

    public static MonthData readMonthDataFromFile(Path path) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(Files.readString(path), MonthData.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading MonthData from file", e);
        }
    }

    public static PaymentType getPaymentTypeFromIndex(int index) {
        return CONFIG.paymentTypes.get(index);
    }

    public static List<Command.Choice> collectJsonFiles(String query) {
        List<String> jsonFiles;
        try (Stream<Path> paths = Files.walk(Path.of("data/"))) {
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

        String lowercaseQuery = query.toLowerCase(Locale.ROOT);
        return jsonFiles.stream()
                .filter(item -> item.toLowerCase(Locale.ROOT).contains(lowercaseQuery))
                .limit(25)
                .map(item -> new Command.Choice(item, getPathFromDate(item)))
                .toList();
    }

    public static List<Command.Choice> collectWorkEntries(Path path, PaymentType type, String query) {
        MonthData data = readMonthDataFromFile(path);
        HashMap<String, Integer> currentTagOrder = new HashMap<>();
        for (int i = 0; i < data.workEntries.size(); i++) {
            currentTagOrder.put(data.workEntries.get(i).paymentType.tag, i);
        }

        WorkEntry entry = readMonthDataFromFile(path).workEntries.get(currentTagOrder.get(type.tag));
        if (entry.workDetails.isEmpty()) {
            return null;
        }

        List<String> options;
        try (Stream<WorkDetail> details = entry.workDetails.stream()) {
            options = details
                    .map(Util::getWorkDetailString)
                    .toList();
        }

        String lowercaseQuery = query.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(item -> item.toLowerCase(Locale.ROOT).contains(lowercaseQuery))
                .limit(25)
                .map(item -> new Command.Choice(item, entry.workDetails.indexOf(getWorkDetailFromString(item, getYearFromPath(path)))))
                .toList();
    }

    public static List<Command.Choice> collectTypes(String query) {
        List<String> options;
        try (Stream<PaymentType> details = CONFIG.paymentTypes.stream()) {
            options = details
                    .map(Util::getPaymentTypeString)
                    .toList();
        }

        String lowercaseQuery = query.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(item -> item.toLowerCase(Locale.ROOT).contains(lowercaseQuery))
                .limit(25)
                .map(item -> new Command.Choice(item, getPaymentTypeIndexFromString(item)))
                .toList();
    }

    public static List<Command.Choice> collectTypes(String pathString, String query) {
        Path path = Path.of(pathString);
        MonthData entry = readMonthDataFromFile(path);
        List<String> options;
        try (Stream<WorkEntry> details = entry.workEntries.stream()) {
            options = details
                    .map(e -> getPaymentTypeString(CONFIG.paymentTypes.get(tagOrder.get(e.paymentType.tag))))
                    .toList();
        }

        String lowercaseQuery = query.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(item -> item.toLowerCase(Locale.ROOT).contains(lowercaseQuery))
                .limit(25)
                .map(item -> new Command.Choice(item, getPaymentTypeIndexFromString(item)))
                .toList();
    }


    private static String getPaymentTypeString(PaymentType type) {
        return type.type + " " + CONFIG.currency() + "/h (" + type.tag + ")";
    }

    private static PaymentType getPaymentTypeFromString(String string) {
        String[] arr = string.split(" " + CONFIG.currency() + "/h \\(");
        int type = Integer.parseInt(arr[0]);
        String tag = arr[1];
        for (int i = 2; i < arr.length; i++) {
            tag = tag.concat(" " + CONFIG.currency() + "/h \\(" + arr[i]);
        }
        tag = tag.substring(0, tag.length() - 1);

        return new PaymentType(tag, type);
    }

    public static int getPaymentTypeIndexFromString(String string) {
        PaymentType type = getPaymentTypeFromString(string);
        return tagOrder.get(type.tag);
    }

    private static int getYearFromPath(Path path) {
        String pathString = path.toString();
        pathString = pathString.substring(0, pathString.length() - 5);
        String[] arr = pathString.split("_");
        pathString = arr[arr.length - 1];
        return Integer.parseInt(pathString);
    }

    public static String removeWork(Path path, PaymentType type, int index) {
        MonthData data = readMonthDataFromFile(path);
        HashMap<String, Integer> currentTagOrder = new HashMap<>();
        for (int i = 0; i < data.workEntries.size(); i++) {
            currentTagOrder.put(data.workEntries.get(i).paymentType.tag, i);
        }

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").setPrettyPrinting().create();
        MonthData monthData = readMonthDataFromFile(path);

        int orderIndex = currentTagOrder.get(type.tag);
        String ret = monthData.workEntries.get(orderIndex).workDetails.get(index).toString();
        monthData.workEntries.get(orderIndex).workDetails.remove(index);
        if (monthData.workEntries.get(orderIndex).workDetails().isEmpty()) {
            monthData.workEntries.remove(orderIndex);
        }

        if (monthData.workEntries.isEmpty()) {
            try {
                Files.delete(path);
                System.out.println("deleted: " + path);
            } catch (IOException e) {
                System.err.println("Failed to delete file: " + e.getMessage());
            }

            String[] dirPathArr = path.toString().split("/");
            StringBuilder dirPath = new StringBuilder();
            for (int i = 0; i < dirPathArr.length - 1; i++) {
                dirPath.append(dirPathArr[i]);
                dirPath.append("/");
            }

            File dir = new File(dirPath.toString());
            if (dir.isDirectory() && Objects.requireNonNull(dir.list()).length == 0) {
                try {
                    Files.delete(dir.toPath());
                    System.out.println("deleted: " + dir);
                } catch (IOException e) {
                    System.err.println("Failed to delete file: " + e.getMessage());
                }
            }
            return ret;
        }

        String jsonData = gson.toJson(monthData);
        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
            fileWriter.write(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ret;
    }

    public static String getWorkDetailString(WorkDetail detail) {
        String detailString = new SimpleDateFormat("d.M.", Locale.forLanguageTag(CONFIG.languageTag)).format(detail.date);
        return detailString + " " + detail.duration + " " + detail.note;
    }

    public static WorkDetail getWorkDetailFromString(String detailString, int year) {
        String[] strArr = detailString.split(" ");
        String note = strArr[2];
        for (int i = 3; i < strArr.length; i++) {
            note = note.concat(" " + strArr[i]);
        }
        return new WorkDetail(getDateFromString(strArr[0], year), Integer.parseInt(strArr[1]), note);
    }

    private static Date getDateFromString(String strDate, int year) {
        strDate = strDate.concat(Integer.toString(year));
        try {
             return new SimpleDateFormat("d.M.yyyy", Locale.forLanguageTag(CONFIG.languageTag)).parse(strDate);
        } catch (ParseException ignored) {}
        return null;
    }

    public static String getPathFromDate(String fileName) {
        try (Stream<Path> paths = Files.walk(Paths.get("./data/"), FileVisitOption.FOLLOW_LINKS)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> p.getFileName().toString().equals(fileName + ".json"))
                    .map(Path::toString)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return "no date";
        }
    }
}