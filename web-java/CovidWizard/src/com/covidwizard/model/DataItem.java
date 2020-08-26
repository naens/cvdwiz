package com.covidwizard.model;

import com.covidwizard.common.CovidTools;

public class DataItem {

	private int day;
	private Country country;
	private int newCases;

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	public int getNewCases() {
		return newCases;
	}

	public void setNewCases(int newCases) {
		this.newCases = newCases;
	}

	@Override
	public String toString() {
		return String.format("DataItem [day=%d=%s, country=%s, newCases=%d]",
				day, CovidTools.dayToDate(day), country.getName(), newCases);
	}

	public DataItem(int day, Country country, int newCases) {
		super();
		this.day = day;
		this.country = country;
		this.newCases = newCases;
	}

}
