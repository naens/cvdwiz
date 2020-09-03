package com.covidwizard.model;

import com.covidwizard.common.CovidTools;

public class DataItem {

	private int day;
	private Country country;
	private double newCases;

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

	public double getNewCases() {
		return newCases;
	}

	public void setNewCases(int newCases) {
		this.newCases = newCases;
	}

	@Override
	public String toString() {
		if (country == null) {
			return String.format("DataItem [day=%d=%s, country=NULL, newCases=%d]",
					day, CovidTools.dayToDate(day), newCases);
		} else {
			return String.format("DataItem [day=%d=%s, country=%s, newCases=%d]",
					day, CovidTools.dayToDate(day), country.getName(), newCases);
		}
	}

	// country can be null if group or world
	public DataItem(int day, Country country, double newCases) {
		super();
		this.day = day;
		this.country = country;
		this.newCases = newCases;
	}

}
