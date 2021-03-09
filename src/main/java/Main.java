import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.JSONObject;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.*;

public class Main {

    private static final Map<String, String> externalMap = new HashMap<>();
    private static final Map<String, String> coreMap = new HashMap<>();
    private static final Map<String, String> resultMap = new TreeMap<>();

    public static void main(String[] args) throws IOException, ParseException {
        final Properties properties = loadProps();

        File externalFile = new File(properties.getProperty("external_file_path"));
        File ourFile = new File(properties.getProperty("core_file_path"));

        FileReader sberReader = new FileReader(externalFile);
        FileReader ourReader = new FileReader(ourFile);
        fillMap(externalFile, externalMap);
        fillMap(ourFile, coreMap);

        compareMaps(coreMap, externalMap, properties.getProperty("check_type"));
    }

    private static void compareMaps(Map<String, String> coreMap, Map<String, String> externalMap, String checkType) throws IOException {
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
        }
    }

    private static void compare(Map<String, String> coreMap, Map<String, String> externalMap) throws IOException {
        List<String> errorsList = new ArrayList<>();
        for (Map.Entry<String, String> pair : externalMap.entrySet()) {
            if (coreMap.get(pair.getKey()) == null) {
                errorsList.add(pair.getKey() + " " + pair.getValue());
            } else {
                if (!coreMap.get(pair.getKey()).equals(pair.getValue())) {
                    resultMap.put(pair.getKey(), pair.getValue() + ">" + coreMap.get(pair.getKey()));
                }
            }
        }

        Collections.sort(errorsList);

        JSONObject result = new JSONObject();
        result.accumulate("differences", resultMap);
        result.accumulate("core_file_doesnt_contains", errorsList);
        writeToFile(result.toString());
    }

    private static void compareWithoutErrors(Map<String, String> coreMap, Map<String, String> externalMap) throws IOException {
        for (Map.Entry<String, String> pair : externalMap.entrySet()) {
            if (coreMap.get(pair.getKey()) != null) {
                if (!coreMap.get(pair.getKey()).equals(pair.getValue())) {
                    resultMap.put(pair.getKey(), pair.getValue() + ">" + coreMap.get(pair.getKey()));
                }
            }
        }
        JSONObject result = new JSONObject();
        result.accumulate("differences", resultMap);
        writeToFile(result.toString());
    }

    private static void compareWithoutBubbleAndErrors(Map<String, String> coreMap, Map<String, String> externalMap) throws IOException {
        for (Map.Entry<String, String> pair : externalMap.entrySet()) {
            if (coreMap.get(pair.getKey()) != null) {
                if (!pair.getKey().startsWith("@babel")) {
                    if (!coreMap.get(pair.getKey()).equals(pair.getValue())) {
                        resultMap.put(pair.getKey(), pair.getValue() + ">" + coreMap.get(pair.getKey()));
                    }
                }
            }
        }
        JSONObject result = new JSONObject();
        result.accumulate("differences", resultMap);
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

        while(bufferedReader.ready()){
            String dependency = bufferedReader.readLine();
            if (StringUtils.contains(dependency,'@')) {
                String version = bufferedReader.readLine().trim();
                if(StringUtils.startsWith(version, "version")){
                    final String[] depSplit = dependency.split("@");
                    final String[] verSplit = version.split(" ");

                    final String clearVersion = StringUtils.remove(verSplit[1], "\"");

                    map.put(depSplit[0], clearVersion);
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
}
