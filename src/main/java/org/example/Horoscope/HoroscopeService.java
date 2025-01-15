package org.example.Horoscope;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.example.TelegramBot;
import org.example.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;

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
    private static final Map<String, String> zodiacMap;  //key - код знака зодиака, value - название знака зодиака
    private final UsersService usersService = new UsersService();
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
    }
    public void sendHoroscope(Update update, String zodiacCode, String day) {
        Long chatID = update.getCallbackQuery().getMessage().getChatId();
        String userName = usersService.getUserNameByID(update.getCallbackQuery().getFrom().getId());
        updateHoroscope();

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
        String msg = "Гороскоп на " + horoscopeDate + " для знака зодиака " + zodiacMap.get(zodiacCode) + " по просьбе " + userName + "\n" +
                horoscopeText;

        bot.sendMessage(chatID, msg);
        lastSend = LocalDate.now();
    }
    public void updateHoroscope () {
        if (!Objects.equals(lastSend, LocalDate.now()))
            try {
                // Создаем XmlMapper
                XmlMapper xmlMapper = new XmlMapper();

                // Парсим XML в объект Horoscope
                horoscope = xmlMapper.readValue(getHoroscopeXML(), Horoscope.class);

            } catch (Exception e) {
                logger.error("Ошибка парсинга гороскопа" + e);
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
