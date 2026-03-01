package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.example.AiChat.UserMessages;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.example.Settings.Settings.RESOURCES_PATH;

public class DataManager {
    private final Map<Long, UserMessages> userMessages = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public DataManager() {
        // Создаем папку resources, если её не существует
        File resourcesDir = new File(RESOURCES_PATH);
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }
    }

    private File getFile(String filename) {
        return new File(RESOURCES_PATH + File.separator + filename);
    }

    /**
     * Проверяет существование файла в папке resources
     * @param filename имя файла
     * @return true если файл существует, false в противном случае
     */
    public boolean fileExists(String filename) {
        File file = getFile(filename);
        return file.exists() && file.isFile();
    }

    /**
     * Сохраняет данные в JSON файл
     * @param filename имя файла
     * @throws IOException если возникла ошибка при записи
     */
    public void saveToJson(String filename) throws IOException {
        File file = getFile(filename);
        mapper.writeValue(file, userMessages);
    }

    /**
     * Загружает данные из JSON файла
     * @param filename имя файла
     * @throws IOException если файл не найден или возникла ошибка при чтении
     */
    public void loadFromJson(String filename) throws IOException {
        File file = getFile(filename);

        if (!fileExists(filename)) {
            throw new IOException("Файл не найден: " + filename + " в директории " + RESOURCES_PATH);
        }

        userMessages.clear();
        userMessages.putAll(mapper.readValue(
                file,
                new TypeReference<Map<Long, UserMessages>>() {}
        ));
    }

    /**
     * Удаляет файл из папки resources
     * @param filename имя файла для удаления
     * @return true если файл успешно удален, false если файл не существовал
     */
    public boolean deleteFile(String filename) {
        File file = getFile(filename);

        if (file.exists() && file.isFile()) {
            return file.delete();
        }

        return false; // Файл не существует или это директория
    }

    /**
     * Удаляет файл с проверкой существования
     * @param filename имя файла для удаления
     * @throws IOException если файл не найден или не удалось удалить
     */
    public void deleteFileWithCheck(String filename) throws IOException {
        File file = getFile(filename);

        if (!file.exists() || !file.isFile()) {
            throw new IOException("Невозможно удалить: файл не найден - " + filename);
        }

        boolean deleted = file.delete();

        if (!deleted) {
            throw new IOException("Не удалось удалить файл: " + filename);
        }
    }

    /**
     * Возвращает копию карты сообщений пользователей
     */
    public Map<Long, UserMessages> getUserMessages() {
        return new HashMap<>(userMessages);
    }

    /**
     * Устанавливает карту сообщений пользователей
     */
    public void setUserMessages(Map<Long, UserMessages> messages) {
        userMessages.clear();
        userMessages.putAll(messages);
    }

    /**
     * Возвращает полный путь к файлу в папке resources
     * @param filename имя файла
     * @return полный путь к файлу
     */
    public String getFullPath(String filename) {
        return RESOURCES_PATH + File.separator + filename;
    }

    /**
     * Получает список всех JSON файлов в папке resources
     * @return массив имен JSON файлов
     */
    public String[] listJsonFiles() {
        File dir = new File(RESOURCES_PATH);
        if (dir.exists() && dir.isDirectory()) {
            return dir.list((d, name) -> name.endsWith(".json"));
        }
        return new String[0];
    }
}