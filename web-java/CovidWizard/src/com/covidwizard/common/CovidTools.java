package com.covidwizard.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class CovidTools {

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static LocalDate date0 = LocalDate.parse("2019-01-01");

	public static int dateToDay(String date) {
		LocalDate ld = LocalDate.parse(date);
		return (int) ChronoUnit.DAYS.between(date0, ld);
	}

	public static String dayToDate(int day) {
		LocalDate ld = date0.plusDays(day);
		return ld.format(FMT);
	}

	public static int today() {
		LocalDate ld = LocalDate.now();
		return (int) ChronoUnit.DAYS.between(date0, ld);
	}
}
