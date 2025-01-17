package org.example.DTO;

import java.util.Date;

public class AVGCockSizeDTO {
    public Long userID;
    public Float AVGSize;
    public Date firstMeasurementDate;
    public Date lastMeasurementDate;
    public Integer measurementCount;
    public AVGCockSizeDTO(Long userID, Float AVGSize, Date firstMeasurementDate, Date lastMeasurementDate, Integer measurementCount) {
        this.userID = userID;
        this.AVGSize = AVGSize;
        this.firstMeasurementDate = firstMeasurementDate;
        this.lastMeasurementDate = lastMeasurementDate;
        this.measurementCount = measurementCount;
    }

}
