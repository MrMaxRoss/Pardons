package com.sortedunderbelly.pardons;

import java.util.Date;

/**
 * Created by max.ross on 3/7/15.
 */
public class Pardons {
    private final String id;
    private final String from;
    private final String fromDisplay;
    private final String to;
    private final String toDisplay;
    private final Date date;
    private final int quantity;
    private final String reason;

    public Pardons(String id, String from, String fromDisplay, String to,
                   String toDisplay, Date date, int quantity, String reason) {
        this.id = id;
        this.from = from;
        this.fromDisplay = fromDisplay;
        this.to = to;
        this.toDisplay = toDisplay;
        this.date = date;
        this.quantity = quantity;
        this.reason = reason;
    }

    public Pardons(String from, String fromDisplay, String to, String toDisplay,
                   Date date, int quantity, String reason) {
        this(null, from, fromDisplay, to, toDisplay, date, quantity, reason);
    }

    public String getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public String getFromDisplay() {
        return fromDisplay;
    }

    public String getTo() {
        return to;
    }

    public String getToDisplay() {
        return toDisplay;
    }

    public Date getDate() {
        return date;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pardons pardons = (Pardons) o;

        return !(id != null ? !id.equals(pardons.id) : pardons.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Pardon{" +
                "id='" + id + '\'' +
                ", from='" + from + '\'' +
                ", fromDisplay='" + fromDisplay + '\'' +
                ", to='" + to + '\'' +
                ", toDisplay='" + toDisplay + '\'' +
                ", date=" + date +
                ", quantity=" + quantity +
                ", reason='" + reason + '\'' +
                '}';
    }

    public boolean hasId() {
        return id != null;
    }
}
