package org.example.Horoscope;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.dataformat.xml.annotation.*;
import org.example.DTO.HoroscopeDateInfoDTO;
import org.example.DTO.HoroscopeTextDTO;

import java.util.HashMap;
import java.util.Map;

@JacksonXmlRootElement(localName = "horo")
public class Horoscope {
    private final Map<String, HoroscopeTextDTO> horoscopeTextMap = new HashMap<>();
    //key - код знака зодиака
    //value - текст для всех дней
    @JacksonXmlProperty(localName = "date")
    private HoroscopeDateInfoDTO horoscopeDateInfoDTO;
    @JsonAnySetter
    public void addSign(String key, HoroscopeTextDTO value) {
        //key - код знака зодиака
        //value - текст гороскопа на этот день
        horoscopeTextMap.put(key, value);
    }
    public HoroscopeDateInfoDTO getDateInfo() {
        return horoscopeDateInfoDTO;
    }

    public Map<String, HoroscopeTextDTO> getHoroscopeTextMap() {
        return horoscopeTextMap;
    }

}
