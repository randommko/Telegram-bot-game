package org.example.DTO;

import java.time.LocalDate;


public record AVGCockSizeDTO(Long userID,
                             Float AVGSize,
                             LocalDate firstMeasurementDate,
                             LocalDate lastMeasurementDate,
                             Integer measurementCount) {

}
