package com.covidwizard.model;

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
		return "DataItem [day=" + day + ", country=" + country.getName() + ", newCases=" + newCases + "]";
	}

	public DataItem(int day, Country country, int newCases) {
		super();
		this.day = day;
		this.country = country;
		this.newCases = newCases;
	}

}
