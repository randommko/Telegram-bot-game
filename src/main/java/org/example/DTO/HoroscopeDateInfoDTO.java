package org.example.DTO;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class HoroscopeDateInfoDTO {
    @JacksonXmlProperty(isAttribute = true, localName = "yesterday")
    private String yesterday;

    @JacksonXmlProperty(isAttribute = true, localName = "today")
    private String today;

    @JacksonXmlProperty(isAttribute = true, localName = "tomorrow")
    private String tomorrow;

    @JacksonXmlProperty(isAttribute = true, localName = "tomorrow02")
    private String tomorrow02;

    // Геттеры и сеттеры
    public String getYesterday() {
        return yesterday;
    }

    public void setYesterday(String yesterday) {
        this.yesterday = yesterday;
    }

    public String getToday() {
        return today;
    }

    public void setToday(String today) {
        this.today = today;
    }

    public String getTomorrow() {
        return tomorrow;
    }

    public void setTomorrow(String tomorrow) {
        this.tomorrow = tomorrow;
    }

    public String getTomorrow02() {
        return tomorrow02;
    }

    public void setTomorrow02(String tomorrow02) {
        this.tomorrow02 = tomorrow02;
    }

}
