package org.example.Horoscope;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Horoscope {
    private static final String URL = "https://ignio.com/r/export/utf/xml/daily/com.xml";

    private static final Logger logger = LoggerFactory.getLogger(Horoscope.class);
    private Horoscope horoscope;
    private final TelegramBot bot;
    @JacksonXmlProperty(localName = "date")
    private DateInfo dateInfo;
    private final Map<String, Zodiac> signsMap = new HashMap<>();

    @JsonAnySetter
    public void addSign(String key, Zodiac value) {
        signsMap.put(key, value);
    }
    public DateInfo getDateInfo() {
        return dateInfo;
    }

    public void setDateInfo(DateInfo dateInfo) {
        this.dateInfo = dateInfo;
    }

    public Map<String, Zodiac> getSignsMap() {
        return signsMap;
    }

    public Horoscope() {
        bot = TelegramBot.getInstance();
    }

    public String getHoroscope () {
        try {
            // Создаем XmlMapper
            XmlMapper xmlMapper = new XmlMapper();

            // Парсим XML в объект Horoscope
            horoscope = xmlMapper.readValue(getHoroscopeXML(), Horoscope.class);

            StringBuilder result = new StringBuilder();
            result.append("Гороскоп на ").append(horoscope.dateInfo.getToday()).append("\n");
            for (Map.Entry<String, Zodiac> entry : horoscope.signsMap.entrySet()) {
                result.append(entry.getKey()).append(": ").append(entry.getValue().getTodayHoroscope()).append("\n");
            }
            return result.toString();

        } catch (Exception e) {
            logger.debug("Ошибка парсинга гороскопа" + e);
            return null;
        }
    }

    public static String getHoroscopeXML() throws IOException, InterruptedException {
        // Создаем клиент
        HttpClient client = HttpClient.newHttpClient();

        // Создаем запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .build();

        // Отправляем запрос и получаем ответ
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        if (response.statusCode() == 200) {
            return response.body(); // Возвращаем тело ответа
        } else {
            throw new IOException("Ошибка при получении данных: " + response.statusCode());
        }
    }
}
