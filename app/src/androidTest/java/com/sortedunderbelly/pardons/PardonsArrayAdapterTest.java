package com.sortedunderbelly.pardons;

import android.test.AndroidTestCase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by max.ross on 8/18/15.
 */
public class PardonsArrayAdapterTest extends AndroidTestCase {

    private Locale originalLocale;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalLocale = Locale.getDefault();
    }

    @Override
    protected void tearDown() throws Exception {
        PardonsArrayAdapter.clearNow();
        Locale.setDefault(originalLocale);
        super.tearDown();
    }

    private String getDateTimeString(Calendar cal) {
        return PardonsArrayAdapter.getDateTimeString(getContext(), cal.getTime());
    }

    private static final ImmutableMap<String, ImmutableList<String>> resultMap = ImmutableMap.of(
            "en", ImmutableList.of("2:05 AM", "2:05 PM", "8:05 PM", "Aug 17", "Feb 17", "7/9/14"),
            "de", ImmutableList.of("2:05 vorm.", "2:05 nachm.", "8:05 nachm.", "17. Aug.", "17. Feb.", "09.07.14"));

    private void testGetDateTimeString(String language, String country) {
        PardonsArrayAdapter.setNow(cal(2015, Calendar.AUGUST, 18, 14, 5, 33));
        setLocale(language, country);

        int resultIndex = 0;
        // input is earlier today
        assertEquals(resultMap.get(language).get(resultIndex++), getDateTimeString(
                cal(2015, Calendar. AUGUST, 18, 2, 5, 13)));

        // input is right now
        assertEquals(resultMap.get(language).get(resultIndex++), getDateTimeString(
                PardonsArrayAdapter.getNow()));

        // input is later today (shouldn't happen but lets make sure we can handle clock skew)
        assertEquals(resultMap.get(language).get(resultIndex++), getDateTimeString(
                cal(2015, Calendar.AUGUST, 18, 20, 5, 13)));

        // input was yesterday
        assertEquals(resultMap.get(language).get(resultIndex++), getDateTimeString(
                cal(2015, Calendar.AUGUST, 17, 3, 5, 13)));

        // input was last month
        assertEquals(resultMap.get(language).get(resultIndex++), getDateTimeString(
                cal(2015, Calendar.FEBRUARY, 17, 3, 5, 13)));

        // input was last year
        assertEquals(resultMap.get(language).get(resultIndex++), getDateTimeString(
                cal(2014, Calendar.JULY, 9, 3, 5, 13)));
    }

    public void testGetDateTimeString_en() {
        testGetDateTimeString("en", "EN");
    }

    public void testGetDateTimeString_de() {
        testGetDateTimeString("de", "DE");
    }

    public Calendar cal(int year, int month, int dayOfMonth, int hour, int minute, int sec) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth, hour, minute, sec);
        return cal;
    }

    private void setLocale(String language, String country) {
        Locale locale = new Locale(language, country);
        // here we update locale for date formatters
        Locale.setDefault(locale);
    }

}
