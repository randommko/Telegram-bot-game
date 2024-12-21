package org.example.Horoscope;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.example.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HoroscopeService {
    private final Map<String, String> invertZodiacMap = new HashMap<>();  //key - название знака зодиака, value - код знака зодиака
    private static final Map<String, String> zodiacMap;  //key - код знака зодиака, value - название знака зодиака
    static {
        zodiacMap = new HashMap<>();
        zodiacMap.put("aries", "овен");
        zodiacMap.put("taurus", "телец");
        zodiacMap.put("gemini", "близнецы");
        zodiacMap.put("cancer", "рак");
        zodiacMap.put("leo", "лев");
        zodiacMap.put("virgo", "дева");
        zodiacMap.put("libra", "весы");
        zodiacMap.put("scorpio", "скорпион");
        zodiacMap.put("sagittarius", "стрелец");
        zodiacMap.put("capricorn", "козерог");
        zodiacMap.put("aquarius", "водолей");
        zodiacMap.put("pisces", "рыбы");
    }
    private final TelegramBot bot;
    LocalDate lastSend;
    private Horoscope horoscope;
    private static final String URL = "https://ignio.com/r/export/utf/xml/daily/com.xml";
    private static final Logger logger = LoggerFactory.getLogger(HoroscopeService.class);
    public HoroscopeService() {
        bot = TelegramBot.getInstance();
        horoscope = new Horoscope();
        for (Map.Entry<String, String> entry : zodiacMap.entrySet()) {
            invertZodiacMap.put(entry.getValue(), entry.getKey());
        }
    }
    public Integer sendHoroscope(Long chatID, String zodiacCode, String day) {
        updateHoroscope();

//        String text = message.getText();
//        Long chatID = message.getChatId();
 //       String[] parts = text.split(" ", 2); // Разделяем строку по первому пробелу
//        String zodiacSigns = String.join(", ", zodiacMap.values());
//        if (parts.length < 2) {
//            bot.sendMessage(chatID, "Не указан знак зодиака\n" +
//                    "Необходимо указать один из знаков: " + zodiacSigns);
//            return;
//        }

//        String singName = parts[1].toLowerCase();
//        String zodiacCode = getZodiacCodeByName(singName); // Параметр - знак зодиака

//        if (!zodiacMap.containsKey(zodiacCode)) {
//            bot.sendMessage(chatID, "Не верно указан знак зодиака\n" +
//                    "Необходимо указать один из знаков: " + zodiacSigns);
//            return;
//        }

        String horoscopeText = null;
        String horoscopeDate = null;
        switch (day) {
            case "today" -> {
                horoscopeText = horoscope.getHoroscopeTextMap().get(zodiacCode).getTodayHoroscope();
                horoscopeDate = horoscope.getDateInfo().getToday();
            }
            case "tomorrow" -> {
                horoscopeText = horoscope.getHoroscopeTextMap().get(zodiacCode).getTomorrowHoroscope();
                horoscopeDate = horoscope.getDateInfo().getTomorrow();
            }
        }
        String msg = "Гороскоп на " + horoscopeDate + " для знака зодиака " + zodiacMap.get(zodiacCode) + "\n" +
                horoscopeText;


        lastSend = LocalDate.now();
        return bot.sendMessage(chatID, msg);
    }
    public void updateHoroscope () {
        if (!Objects.equals(lastSend, LocalDate.now()))
            try {
                // Создаем XmlMapper
                XmlMapper xmlMapper = new XmlMapper();

                // Парсим XML в объект Horoscope
                horoscope = xmlMapper.readValue(getHoroscopeXML(), Horoscope.class);

            } catch (Exception e) {
                logger.debug("Ошибка парсинга гороскопа" + e);
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
    public String getZodiacCodeByName(String name) {
        return invertZodiacMap.get(name.toLowerCase());
    }
}
