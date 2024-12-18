package org.example.Horoscope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Zodiac {

    @JacksonXmlProperty(localName = "yesterday")
    private String yesterdayHoroscope;

    @JacksonXmlProperty(localName = "today")
    private String todayHoroscope;

    @JacksonXmlProperty(localName = "tomorrow")
    private String tomorrowHoroscope;

    @JacksonXmlProperty(localName = "tomorrow02")
    private String afterTomorrowHoroscope;

    public String getYesterdayHoroscope() { return yesterdayHoroscope; }
    public String getTodayHoroscope() { return todayHoroscope; }
    public String getZodiacName() { return todayHoroscope; }
    public String getTomorrowHoroscope() { return tomorrowHoroscope; }
    public String getAfterTomorrowHoroscope() { return afterTomorrowHoroscope; }

}
