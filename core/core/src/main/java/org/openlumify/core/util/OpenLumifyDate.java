package org.openlumify.core.util;

import org.openlumify.core.exception.OpenLumifyException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class OpenLumifyDate {
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    public enum Unit {
        DAY
    }

    private final String year;
    private final String month;
    private final String date;

    public OpenLumifyDate(String year, String month, String date) {
        this.year = cleanYearString(year);
        this.month = cleanMonthString(month);
        this.date = cleanDateString(date);
    }

    private static String cleanDateString(String date) {
        date = date == null ? "??" : date;
        if (date.length() == 1) {
            if (date.charAt(0) == '?') {
                date = "?" + date;
            } else {
                date = "0" + date;
            }
        }
        return date;
    }

    private static String cleanMonthString(String month) {
        month = month == null ? "??" : month;
        if (month.length() == 1) {
            if (month.charAt(0) == '?') {
                month = "?" + month;
            } else {
                month = "0" + month;
            }
        }
        return month;
    }

    private static String cleanYearString(String year) {
        year = year == null ? "????" : year;
        if (year.length() == 2) {
            year = "20" + year;
        }
        return year;
    }

    public OpenLumifyDate(Integer year, Integer month, Integer date) {
        this(
                year == null ? null : year.toString(),
                month == null ? null : month.toString(),
                date == null ? null : date.toString()
        );
    }

    public static OpenLumifyDate create(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return parse((String) obj);
        }
        if (obj instanceof Date) {
            return create((Date) obj);
        }
        throw new OpenLumifyException("Invalid object type to convert to " + OpenLumifyDate.class.getSimpleName() + ": " + obj.getClass().getName());
    }

    public static OpenLumifyDate create(Date date) {
        Calendar cal = Calendar.getInstance(GMT);
        cal.setTime(date);
        return new OpenLumifyDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DATE)
        );
    }

    private static OpenLumifyDate parse(String str) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            sdf.setTimeZone(GMT);
            return create(sdf.parse(str));
        } catch (ParseException e) {
            throw new OpenLumifyException("Could not parse date: " + str, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OpenLumifyDate that = (OpenLumifyDate) o;

        if (!year.equals(that.year)) {
            return false;
        }
        if (!month.equals(that.month)) {
            return false;
        }
        return date.equals(that.date);

    }

    @Override
    public int hashCode() {
        int result = year.hashCode();
        result = 31 * result + month.hashCode();
        result = 31 * result + date.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getYear() + "-" + getMonth() + "-" + getDate();
    }

    public Date toDate() {
        Calendar cal = Calendar.getInstance(GMT);
        cal.setTimeInMillis(0);
        cal.set(getYearInt(), getMonthInt() - 1, getDateInt(), 0, 0, 0);
        return cal.getTime();
    }

    public int getDateInt() {
        return Integer.parseInt(getDate());
    }

    public int getMonthInt() {
        return Integer.parseInt(getMonth());
    }

    public int getYearInt() {
        return Integer.parseInt(getYear());
    }

    public String getYear() {
        return year;
    }

    public String getMonth() {
        return month;
    }

    public String getDate() {
        return date;
    }

    public long getEpoch() {
        return toDate().getTime();
    }
}
