package com.covidwizard.model;

public class Country {
	private int id;
	private String name;
	private String continent;
	private long population;

	public Country(int id, String name, String continent, long population) {
		super();
		this.id = id;
		this.name = name;
		this.continent = continent;
		this.population = population;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getContinent() {
		return continent;
	}

	public long getPopulation() {
		return population;
	}

	@Override
	public String toString() {
		return "Country [id=" + id + ", name=" + name + "]";
	}
	
}
