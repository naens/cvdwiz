package com.covidwizard.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.covidwizard.model.Country;

public class CovidTools {

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static LocalDate date0 = LocalDate.parse("2019-01-01");
	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(Country.class.getName());

	public static int dateToDay(String date) {
		if (date.contains("/")) {
			String[] strings = date.split("/");
			int year = Integer.parseInt(strings[2]) + 2000;
			int month = Integer.parseInt(strings[0]);
			int dayOfMonth = Integer.parseInt(strings[1]);
			LocalDate ld = LocalDate.of(year, month, dayOfMonth);
			return (int) ChronoUnit.DAYS.between(date0, ld);
		} else {
			LocalDate ld = LocalDate.parse(date);
			return (int) ChronoUnit.DAYS.between(date0, ld);
		}
	}

	public static String dayToDate(int day) {
		LocalDate ld = date0.plusDays(day);
		return ld.format(FMT);
	}

	public static int today() {
		LocalDate ld = LocalDate.now();
		return (int) ChronoUnit.DAYS.between(date0, ld);
	}

	private static Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
	 
	public static boolean isNumeric(String strNum) {
	    if (strNum == null) {
	        return false; 
	    }
	    return pattern.matcher(strNum).matches();
	}
	
	public static String getDensityColor(double density) {
		if (density < 0.2) {
			return "green";
		} else if (density < 0.4) {
			return "orange";
		} else if (density < 0.8) {
			return "red";
		} else {
			return "black";
		}
	}
}
