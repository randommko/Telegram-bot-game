package org.example.Horoscope;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.dataformat.xml.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JacksonXmlRootElement(localName = "horo")
public class Horoscope {
    private Map<String, HoroscopeText> horoscopeTextMap = new HashMap<>();
    //key - код знака зодиака
    //value - текст для всех дней
    @JacksonXmlProperty(localName = "date")
    private DateInfo dateInfo;
    @JsonAnySetter
    public void addSign(String key, HoroscopeText value) {
        //key - код знака зодиака
        //value - текст гороскопа на этот день
        horoscopeTextMap.put(key, value);
    }
    public DateInfo getDateInfo() {
        return dateInfo;
    }
    public void setDateInfo(DateInfo dateInfo) {
        this.dateInfo = dateInfo;
    }
    public Map<String, HoroscopeText> getHoroscopeTextMap() {
        return horoscopeTextMap;
    }

}
