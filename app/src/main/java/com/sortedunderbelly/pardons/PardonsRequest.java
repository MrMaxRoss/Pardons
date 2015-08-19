package com.sortedunderbelly.pardons;

import java.util.Date;

/**
 * Created by max.ross on 8/19/15.
 */
public class PardonsRequest extends Pardons {
    public PardonsRequest(String id, String from, String fromDisplay, String to, String toDisplay, Date date, int quantity, String reason) {
        super(id, from, fromDisplay, to, toDisplay, date, quantity, reason);
    }

    public PardonsRequest(String from, String fromDisplay, String to, String toDisplay, Date date, int quantity, String reason) {
        super(from, fromDisplay, to, toDisplay, date, quantity, reason);
    }
}
