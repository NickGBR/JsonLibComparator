import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.*;

public class Main {

    private static final Map<String, String> externalMap = new HashMap<>();
    private static final Map<String, String> coreMap = new HashMap<>();
    private static final Map<String, String> resultMap = new TreeMap<>();

    public static void main(String[] args) throws IOException, ParseException, NoSuchFieldException, IllegalAccessException {
        final Properties properties = loadProps();

        File externalFile = new File(properties.getProperty("external_file_path"));
        File ourFile = new File(properties.getProperty("core_file_path"));

        fillMap(externalFile, externalMap);
        fillMap(ourFile, coreMap);

        compareMaps(coreMap, externalMap, properties.getProperty("check_type"));
    }

    private static void compareMaps(Map<String, String> coreMap, Map<String, String> externalMap, String checkType) throws IOException, NoSuchFieldException, IllegalAccessException {
        switch (checkType) {
            case "0":
                compare(coreMap, externalMap);
                break;
            case "1":
                compareWithoutErrors(coreMap, externalMap);
                break;
            case "2":
                compareWithoutBubbleAndErrors(coreMap, externalMap);
                break;
            case "3":
                newFormatCompare(coreMap, externalMap);
                break;
            case "4":
                newFormatCompareWithoutErrors(coreMap, externalMap);
                break;
            case "5":
                newFormatCompareWithoutBubbleAndErrors(coreMap, externalMap);
                break;
        }
    }

    private static void compare(Map<String, String> coreMap, Map<String, String> externalMap) throws IOException, NoSuchFieldException, IllegalAccessException {
        List<String> errorsList = new ArrayList<>();
        for (Map.Entry<String, String> pair : externalMap.entrySet()) {
            if (coreMap.get(pair.getKey()) == null) {
                errorsList.add(pair.getKey() + " " + pair.getValue());
            } else {
                if (!coreMap.get(pair.getKey()).equals(pair.getValue())) {
                    resultMap.put(pair.getKey(), coreMap.get(pair.getKey()));
                }
            }
        }

        Collections.sort(errorsList);
        JSONObject differences = getOrderedJsonObject();
        resultMap.forEach(differences::put);

        JSONObject result = new JSONObject();
        result.put("differences", differences);
        result.accumulate("core_file_doesnt_contains", errorsList);
        writeToFile(result.toString());
    }

    private static void compareWithoutErrors(Map<String, String> coreMap, Map<String, String> externalMap) throws IOException, NoSuchFieldException, IllegalAccessException {
        for (Map.Entry<String, String> pair : externalMap.entrySet()) {
            if (coreMap.get(pair.getKey()) != null) {
                if (!coreMap.get(pair.getKey()).equals(pair.getValue())) {
                    resultMap.put(pair.getKey(), coreMap.get(pair.getKey()));
                }
            }
        }

        JSONObject differences = getOrderedJsonObject();
        resultMap.forEach(differences::put);

        JSONObject result = new JSONObject();
        result.accumulate("differences", differences);
        writeToFile(result.toString());
    }

    private static void compareWithoutBubbleAndErrors(Map<String, String> coreMap, Map<String, String> externalMap) throws IOException, NoSuchFieldException, IllegalAccessException {
        for (Map.Entry<String, String> pair : externalMap.entrySet()) {
            if (coreMap.get(pair.getKey()) != null) {
                if (!pair.getKey().startsWith("@babel")) {
                    if (!coreMap.get(pair.getKey()).equals(pair.getValue())) {
                        resultMap.put(pair.getKey(), coreMap.get(pair.getKey()));
                    }
                }
            }
        }

        JSONObject differences = getOrderedJsonObject();
        resultMap.forEach(differences::put);

        JSONObject result = new JSONObject();
        result.accumulate("differences", differences);
        writeToFile(result.toString());
    }

    private static void newFormatCompare(Map<String, String> coreMap, Map<String, String> externalMap) throws IOException, NoSuchFieldException, IllegalAccessException {
        List<String> errorsList = new ArrayList<>();
        for (Map.Entry<String, String> pair : externalMap.entrySet()) {
            if (coreMap.get(pair.getKey()) == null) {
                errorsList.add(pair.getKey() + " " + pair.getValue());
            } else {
                if (!coreMap.get(pair.getKey()).equals(pair.getValue())) {
                    resultMap.put(pair.getKey(), pair.getValue() + " > " + coreMap.get(pair.getKey()));
                }
            }
        }

        Collections.sort(errorsList);
        JSONObject differences = getOrderedJsonObject();
        resultMap.forEach(differences::put);

        JSONObject result = new JSONObject();
        result.put("differences", differences);
        result.accumulate("core_file_doesnt_contains", errorsList);
        writeToFile(result.toString());
    }

    private static void newFormatCompareWithoutErrors(Map<String, String> coreMap, Map<String, String> externalMap) throws IOException, NoSuchFieldException, IllegalAccessException {
        for (Map.Entry<String, String> pair : externalMap.entrySet()) {
            if (coreMap.get(pair.getKey()) != null) {
                if (!coreMap.get(pair.getKey()).equals(pair.getValue())) {
                    resultMap.put(pair.getKey(), pair.getValue() + " > " + coreMap.get(pair.getKey()));
                }
            }
        }

        JSONObject differences = getOrderedJsonObject();
        resultMap.forEach(differences::put);

        JSONObject result = new JSONObject();
        result.accumulate("differences", differences);
        writeToFile(result.toString());
    }

    private static void newFormatCompareWithoutBubbleAndErrors(Map<String, String> coreMap, Map<String, String> externalMap) throws IOException, NoSuchFieldException, IllegalAccessException {
        for (Map.Entry<String, String> pair : externalMap.entrySet()) {
            if (coreMap.get(pair.getKey()) != null) {
                if (!pair.getKey().startsWith("@babel")) {
                    if (!coreMap.get(pair.getKey()).equals(pair.getValue())) {
                        resultMap.put(pair.getKey(), pair.getValue() + " > " + coreMap.get(pair.getKey()));
                    }
                }
            }
        }

        JSONObject differences = getOrderedJsonObject();
        resultMap.forEach(differences::put);

        JSONObject result = new JSONObject();
        result.accumulate("differences", differences);
        writeToFile(result.toString());
    }

    private static void writeToFile(String result) throws IOException {
        final Properties properties = loadProps();
        File file = new File(properties.getProperty("output_file_name"));
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(result);
        fileWriter.flush();
        fileWriter.close();
    }

    private static void fillMap(File file, Map<String, String> map) throws IOException, ParseException {
        if (FilenameUtils.getExtension(file.getName()).equals("json")) {
            fillMapFromJson(new FileReader(file), map);
        }
        if (FilenameUtils.getExtension(file.getName()).equals("lock")) {
            fillMapFromLock(new FileReader(file), map);
        }
    }

    private static void fillMapFromJson(FileReader reader, Map<String, String> map) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();

        final Object object = jsonParser.parse(reader);

        JSONObject json = new JSONObject(object.toString());

        final JSONObject sberDependencies = json.getJSONObject("dependencies");
        final Iterator<String> keys = sberDependencies.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            final Object o = sberDependencies.get(key);
            JSONObject jsonObject = new JSONObject(o.toString());
            String version = jsonObject.get("version").toString();
            map.put(key, version);
        }
    }

    private static void fillMapFromLock(FileReader reader, Map<String, String> map) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);

        while (bufferedReader.ready()) {
            //Считываем файл построчно
            String dependency = bufferedReader.readLine();

            //Все зависимости имеют симфол @, нахаодим такие строки.
            if (StringUtils.contains(dependency, '@')) {
                String version = bufferedReader.readLine().trim();

                //Если зависимомть не сожержит поле "version" это внутрення зависимость, она нам не нужна.
                // Проверяем наличие поля "version" следующей строкой
                if (StringUtils.startsWith(version, "version")) {
                    dependency = StringUtils.remove(dependency, "\"");
                    final String[] verSplit = version.split(" ");
                    final String clearVersion = StringUtils.remove(verSplit[1], "\"");
                    final String[] depSplit = dependency.split("@");

                    // Отдельная проверка для @babel зависимостей.
                    if(dependency.trim().startsWith("@")) {
                        dependency = "@" + depSplit[1];
                    }
                    else {
                        dependency = depSplit[0];
                    }
                    map.put(dependency, clearVersion);
                }
            }
        }
    }

    private static Properties loadProps() throws IOException {
        InputStream inputStream = new FileInputStream("property.property");
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

    private static JSONObject getOrderedJsonObject() throws NoSuchFieldException, IllegalAccessException {
        JSONObject object = new JSONObject();
        for (Field declaredField : object.getClass().getDeclaredFields()) {
            if(declaredField.getType() == Map.class){
                declaredField.setAccessible(true);
                declaredField.set(object, new TreeMap<>());
                declaredField.setAccessible(false);
            }
        }
        return object;
    }
}
