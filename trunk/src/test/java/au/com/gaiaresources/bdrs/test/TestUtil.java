package au.com.gaiaresources.bdrs.test;

import java.util.Date;

import com.ibm.icu.util.Calendar;

public class TestUtil {
	
	public static Date getDate(int year, int month, int date) {
		return getDate(year, month, date, 0, 0);
	}
	
	public static Date getDate(int year, int month, int date, int hour, int min) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month, date, hour, min);
		return cal.getTime();
	}
}
