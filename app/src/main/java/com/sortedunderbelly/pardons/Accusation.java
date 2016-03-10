package com.sortedunderbelly.pardons;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by maxr on 2/26/16.
 */
public class Accusation implements Serializable {
    private final String id;
    private final String accuser;
    private final String accuserDisplay;
    private final String accused;
    private final String accusedDisplay;
    private final Date date;
    private final String reason;

    public Accusation(String id, String accuser, String accuserDisplay, String accused,
                      String accusedDisplay, Date date, String reason) {
        this.id = id;
        this.accuser = accuser;
        this.accuserDisplay = accuserDisplay;
        this.accused = accused;
        this.accusedDisplay = accusedDisplay;
        this.date = date;
        this.reason = reason;
    }

    public Accusation(String accuser, String accuserDisplay, String accused, String accusedDisplay,
                      Date date, String reason) {
        this(null, accuser, accuserDisplay, accused, accusedDisplay, date, reason);
    }

    public String getId() {
        return id;
    }

    public String getAccuser() {
        return accuser;
    }

    public String getAccuserDisplay() {
        return accuserDisplay;
    }

    public String getAccused() {
        return accused;
    }

    public String getAccusedDisplay() {
        return accusedDisplay;
    }

    public Date getDate() {
        return date;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Accusation accusation = (Accusation) o;

        return !(id != null ? !id.equals(accusation.id) : accusation.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Pardon{" +
                "id='" + id + '\'' +
                ", accuser='" + accuser + '\'' +
                ", accuserDisplay='" + accuserDisplay + '\'' +
                ", accused='" + accused + '\'' +
                ", accusedDisplay='" + accusedDisplay + '\'' +
                ", date=" + date +
                ", reason='" + reason + '\'' +
                '}';
    }

    public boolean hasId() {
        return id != null;
    }

}
