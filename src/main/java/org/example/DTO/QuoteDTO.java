package org.example.DTO;

import java.sql.Timestamp;

public class QuoteDTO {
    public String text;
    public String userName;
    public Timestamp date;

    public QuoteDTO (String text, String userName, Timestamp date) {
        this.text = text;
        this.userName = userName;
        this.date = date;
    }
}
