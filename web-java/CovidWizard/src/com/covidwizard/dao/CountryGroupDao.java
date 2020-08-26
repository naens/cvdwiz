package com.covidwizard.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.covidwizard.model.Country;
import com.covidwizard.model.CountryGroup;

public class CountryGroupDao implements Dao<CountryGroup, Integer> {

	private static final Logger LOGGER = Logger.getLogger(Country.class.getName());
	private final Optional<Connection> connection;

	public CountryGroupDao() {
		this.connection = JdbcConnection.getConnection();
	}

	public Set<Country> getCountryGroupCountries(int countryGroupId) {
		Set<Country> countries = new HashSet<Country>();
		String sql = "SELECT id, name, continent, population "
				+ "FROM country JOIN country_name ON country.id = country_name.country "
				+ "JOIN country_group_countries ON country.id = country_group_countries.country "
				+ "WHERE country_group_countries.country_group = ?;";

		connection.ifPresent(conn -> {
			try (PreparedStatement statement = conn.prepareStatement(sql)) {

				statement.setLong(1, countryGroupId);
                ResultSet resultSet = statement.executeQuery();

				while (resultSet.next()) {
					int countryId = resultSet.getInt("id");
					String name = resultSet.getString("name");
					String continent = resultSet.getString("continent");
					long population = resultSet.getLong("population");
					Country country = new Country(countryId, name, continent, population);
					countries.add(country);
				}

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		});

		return countries;
	}

	@Override
	public Optional<CountryGroup> get(int id) {
		return connection.flatMap(conn -> {
			String sql = "SELECT * FROM country_group WHERE id = ?;";
			Optional<CountryGroup> countryGroupOptional = Optional.empty();

			try (PreparedStatement statement = conn.prepareStatement(sql)) {

				statement.setLong(1, id);
                ResultSet resultSet = statement.executeQuery();

				if (resultSet.next()) {
					String name = resultSet.getString("name");
					CountryGroup countryGroup = new CountryGroup(id, name);
					countryGroupOptional = Optional.of(countryGroup);
				}

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}

			return countryGroupOptional;
		});
	}

	@Override
	public Collection<CountryGroup> getAll() {
		Collection<CountryGroup> countryGroups = new ArrayList<>();
		String sql = "SELECT id, name FROM country_group ORDER BY name;";

		connection.ifPresent(conn -> {
			try (Statement statement = conn.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {

				while (resultSet.next()) {
					int id = resultSet.getInt("id");
					String name = resultSet.getString("name");

					CountryGroup countryGroup = new CountryGroup(id, name);

					countryGroups.add(countryGroup);
				}

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		});

		return countryGroups;
	}

	@Override
	public Optional<Integer> save(CountryGroup countryGroup) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(CountryGroup countryGroup) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(CountryGroup countryGroup) {
		// TODO Auto-generated method stub
		
	}

}
