package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.example.AiChat.UserMessages;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataManager {
    private final Map<Long, UserMessages> userMessages = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public void saveToJson(String filename) throws IOException {
        mapper.writeValue(new File(filename), userMessages);
    }

    public void loadFromJson(String filename) throws IOException {
        userMessages.clear();
        userMessages.putAll(mapper.readValue(
                new File(filename),
                new TypeReference<Map<Long, UserMessages>>() {}
        ));
    }
}