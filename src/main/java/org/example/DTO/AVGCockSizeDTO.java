package org.example.DTO;

import java.time.LocalDate;
import java.util.Date;

public class AVGCockSizeDTO {
    public Long userID;
    public Float AVGSize;
    public LocalDate firstMeasurementDate;
    public LocalDate lastMeasurementDate;
    public Integer measurementCount;
    public AVGCockSizeDTO(Long userID, Float AVGSize, LocalDate firstMeasurementDate, LocalDate lastMeasurementDate, Integer measurementCount) {
        this.userID = userID;
        this.AVGSize = AVGSize;
        this.firstMeasurementDate = firstMeasurementDate;
        this.lastMeasurementDate = lastMeasurementDate;
        this.measurementCount = measurementCount;
    }

}
