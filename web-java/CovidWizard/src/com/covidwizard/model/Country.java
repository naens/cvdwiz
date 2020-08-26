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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((continent == null) ? 0 : continent.hashCode());
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (population ^ (population >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Country other = (Country) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}
	
}
