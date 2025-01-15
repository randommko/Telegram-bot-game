package org.example.CockSize;

import java.util.Date;

public class CockSizeAVG {
    public Long userID;
    public Float AVGSize;
    public Date firstMeasurementDate;
    public Date lastMeasurementDate;
    public Integer measurementCount;
    public CockSizeAVG (Long userID, Float AVGSize, Date firstMeasurementDate, Date lastMeasurementDate, Integer measurementCount) {
        this.userID = userID;
        this.AVGSize = AVGSize;
        this.firstMeasurementDate = firstMeasurementDate;
        this.lastMeasurementDate = lastMeasurementDate;
        this.measurementCount = measurementCount;
    }

}
