package com.covidwizard.model;

public class DayData {
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
		return "DayData [day=" + day + ", country=" + country.getId() + ", newCases=" + newCases + "]";
	}

}
