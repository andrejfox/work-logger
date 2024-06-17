package io.github.andrejfox.worklogger;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.moandjiezana.toml.Toml;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public final class Util {
    private Util () {}

    public record Config(String botToken, long channelID, long messageID, String languageTag, String currency, List<PaymentType> paymentTypes) {}
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
        } catch (NoSuchFileException e) {
            return e + "";
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
        boolean isNew = false;
        if (monthData == null) {
            monthData = new MonthData(new PayStatus(false, 0),new ArrayList<>());
            isNew = true;
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

        if (isNew) {
            addToNotPayedList(path);
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
                    .filter(p -> !p.toString().endsWith("notPayedList.json"))
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

    public static List<Command.Choice> collectJsonFilesForPay(String query) {
        List<String> jsonFiles;
        try (Stream<Path> paths = Files.walk(Path.of("data/"))) {
            jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> !p.toString().endsWith("notPayedList.json"))
                    .filter(p -> !readMonthDataFromFile(p).payStatus.payed)
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
                removeFromNotPayedList(path);
                updateNotPayedBoard();
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

    public static void createNotPayedListIfNotExists(Path inputPath) {
        Path path = Path.of("./data/notPayedList.json");
        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            List<String> list = List.of(new String[]{inputPath.toString()});
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").setPrettyPrinting().create();
            String jsonData = gson.toJson(list);
            try (FileWriter fileWriter = new FileWriter(path.toFile())) {
                fileWriter.write(jsonData);
            }
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Unable to create notPayedList.json.", e);
        }
    }

    public static List<String> readNotPayedListFromFile() {
        Path path = Path.of("./data/notPayedList.json");
        Gson gson = new Gson();
        try {
            Type listType = new TypeToken<List<String>>() {}.getType();
            return gson.fromJson(Files.readString(path), listType);
        } catch (IOException e) {
            throw new RuntimeException("Error reading notPayedList.json file", e);
        }
    }

    static void refreshToNotPayedList() {
        List<String> jsonFiles = null;
        try (Stream<Path> paths = Files.walk(Path.of("data/"))) {
            jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> !p.toString().endsWith("notPayedList.json"))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(str -> "./data/" + str.split("_")[1].substring(0, str.split("_")[1].length() - 5) + "/" + str)
                    .filter(str -> !readMonthDataFromFile(Path.of(str)).payStatus.payed)
                    .toList();
        } catch (IOException ignored) {}

        assert jsonFiles != null;
        for (String jsonFile : jsonFiles) {
            addToNotPayedList(Path.of(jsonFile.substring(2)));
        }
    }

    public static void addToNotPayedList(Path path) {
        Path filePath = Path.of("./data/notPayedList.json");
        if (!Files.exists(filePath)) {
            createNotPayedListIfNotExists(path);
            return;
        }

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").setPrettyPrinting().create();
        List<String> payList = readNotPayedListFromFile();

        if (readMonthDataFromFile(path).payStatus.payed) {
            return;
        }

        payList.add(path.toString());
        String jsonData = gson.toJson(payList);
        try (FileWriter fileWriter = new FileWriter(filePath.toFile())) {
            fileWriter.write(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeFromNotPayedList(Path path) {
        Path filePath = Path.of("./data/notPayedList.json");
        List<String> payList = readNotPayedListFromFile();
        String pathString = path.toString();
        pathString = pathString.substring(2);
        if (!payList.contains(pathString)) {
            return;
        }
        payList.remove(pathString);

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").setPrettyPrinting().create();
        String jsonData = gson.toJson(payList);
        try (FileWriter fileWriter = new FileWriter(filePath.toFile())) {
            fileWriter.write(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateNotPayedBoard() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("** Unpaid Board **");
        embedBuilder.setColor(Color.RED);
        embedBuilder.setDescription(getUnpayedString());
        TextChannel channel = Main.api.getTextChannelById(CONFIG.channelID);
        if (channel == null) {
            System.out.println("Channel not found");
            return;
        }

        if (doesMessageExist(channel, CONFIG.messageID)) {
            editMessage(channel, CONFIG.messageID, embedBuilder);
        } else {
            long id = sendMessageAndGetId(channel, embedBuilder);
            updateMessageId(id);
        }
    }

    private static String getUnpayedString() {
        List<String> payList = readNotPayedListFromFile();
        StringBuilder ret = new StringBuilder();
        for (String item : payList) {
            ret.append("- ");
            String name = item.split("/")[2];
            name = name.substring(0, name.length() - 5);
            name = name.split("_")[0] + " " + name.split("_")[1] + " [";
            ret.append(name);

            MonthData month = readMonthDataFromFile(Path.of("./" + item));
            int sum = 0;
            for (WorkEntry workEntry : month.workEntries) {
                for (WorkDetail workDetail : workEntry.workDetails) {
                    sum += workDetail.duration * workEntry.paymentType.type;
                }
            }

            ret.append(sum);
            ret.append(" ");
            ret.append(CONFIG.currency);
            ret.append("]\n");
        }

        return ret.substring(0, ret.length() -1);
    }

    public static boolean doesMessageExist(MessageChannel channel, long messageId) {
        RestAction<Message> messageAction = channel.retrieveMessageById(messageId);
        try {
            Message message = messageAction.complete();
            return (message != null);
        } catch (ErrorResponseException e) {
            if (e.getErrorResponse() != ErrorResponse.UNKNOWN_MESSAGE) {
                System.err.println("Error retrieving message: " + e.getMessage());
            }
            return false;
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            return false;
        }
    }

    public static void editMessage(MessageChannel channel, long messageId, EmbedBuilder eb) {
        channel.editMessageEmbedsById(messageId, eb.build()).queue();
    }

    public static long sendMessageAndGetId(MessageChannel channel, EmbedBuilder embedBuilder) {
        Message message = channel.sendMessageEmbeds(embedBuilder.build()).complete();
        return Long.parseLong(message.getId());
    }

    private static void updateMessageId(long newMessageID) {
        File tomlFile = new File("./config.toml");
        Config cfg = readeConfig();
        Config updatedCfg = new Config(
                cfg.botToken(),
                cfg.channelID(),
                newMessageID,
                cfg.languageTag(),
                cfg.currency(),
                cfg.paymentTypes()
        );

        try {
            writeConfig(updatedCfg, tomlFile);
        } catch (IOException e) {
            throw new RuntimeException("Error modifying config file", e);
        }
    }

    public static Config readeConfig() {
        File tomlFile = new File("./config.toml");
        Toml toml = new Toml();
        return toml.read(tomlFile).to(Config.class);
    }

    private static void writeConfig(Config config, File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("botToken = \"").append(config.botToken()).append("\"\n");
        sb.append("channelID = ").append(config.channelID()).append("\n");
        sb.append("messageID = ").append(config.messageID()).append("\n");
        sb.append("languageTag = \"").append(config.languageTag()).append("\"\n");
        sb.append("currency = \"").append(config.currency()).append("\"\n");

        sb.append("paymentTypes = [\n");
        for (PaymentType pt : config.paymentTypes()) {
            sb.append("  { tag = \"").append(pt.tag()).append("\", type = ").append(pt.type()).append(" },\n");
        }
        sb.append("]\n");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(sb.toString());
        }
    }
}