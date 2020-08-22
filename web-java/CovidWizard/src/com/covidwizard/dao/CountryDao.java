package com.covidwizard.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.covidwizard.model.Country;

public class CountryDao implements Dao<Country, Integer> {

	private static final Logger LOGGER = Logger.getLogger(Country.class.getName());
	private final Optional<Connection> connection;

	public CountryDao() {
		this.connection = JdbcConnection.getConnection();
	}

	@Override
	public Optional<Country> get(int id) {
		return connection.flatMap(conn -> {
			String sql = "SELECT * FROM country "
					+ "JOIN country_name ON country.id = country_name.country "
					+ "WHERE id = ?;";
			Optional<Country> country = Optional.empty();

			try (PreparedStatement statement = conn.prepareStatement(sql)) {

				statement.setLong(1, id);
                ResultSet resultSet = statement.executeQuery();

				if (resultSet.next()) {
					String name = resultSet.getString("name");
					String continent = resultSet.getString("continent");
					long population = resultSet.getLong("population");

					country = Optional.of(new Country(id, name, continent, population));
				}

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}

			return country;
		});
	}

	@Override
	public Collection<Country> getAll() {
		Collection<Country> countries = new ArrayList<>();
		String sql = "SELECT id, MIN(name) as name, continent, population "
				+ "FROM country JOIN country_name ON country.id = country_name.country "
				+ "GROUP BY country.id ORDER BY name;";

		connection.ifPresent(conn -> {
			try (Statement statement = conn.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {

				while (resultSet.next()) {
					int id = resultSet.getInt("id");
					String name = resultSet.getString("name");
					String continent = resultSet.getString("continent");
					long population = resultSet.getLong("population");

					Country country = new Country(id, name, continent, population);

					countries.add(country);

					// LOGGER.log(Level.INFO, "Found {0} in database", country);
				}

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		});

		return countries;
	}

	public Collection<Country> getAllDataLimited() {
		Collection<Country> countries = new ArrayList<>();
		String sql = "SELECT id, MIN(name) as name, continent, population "
				+ "FROM country JOIN country_name ON country.id = country_name.country "
				+ "WHERE id IN (select distinct country from data) "
				+ "GROUP BY country.id ORDER BY name;";
//		String sql = "SELECT * FROM country JOIN country_name ON country.id = country_name.country WHERE id IN (select distinct country from data)";

		connection.ifPresent(conn -> {
			try (Statement statement = conn.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {

				while (resultSet.next()) {
					int id = resultSet.getInt("id");
					String name = resultSet.getString("name");
					String continent = resultSet.getString("continent");
					long population = resultSet.getLong("population");

					Country country = new Country(id, name, continent, population);

					countries.add(country);

					// LOGGER.log(Level.INFO, "Found {0} in database", country);
				}

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		});

		return countries;
	}

	@Override
	public Optional<Integer> save(Country country) {
		String message = "The country to be added should not be null";
		Country nonNullCountry = Objects.requireNonNull(country, message);
		String sql = "INSERT INTO " + "country(continent, population) " + "VALUES(?, ?)";

		return connection.flatMap(conn -> {
			Optional<Integer> generatedId = Optional.empty();

			try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

				statement.setString(1, nonNullCountry.getContinent());
				statement.setLong(2, nonNullCountry.getPopulation());

				int numberOfInsertedRows = statement.executeUpdate();

				// Retrieve the auto-generated id
				if (numberOfInsertedRows > 0) {
					try (ResultSet resultSet = statement.getGeneratedKeys()) {
						if (resultSet.next()) {
							generatedId = Optional.of(resultSet.getInt(1));
						}
					}
				}

				LOGGER.log(Level.INFO, "{0} created successfully? {1}",
						new Object[] { nonNullCountry, (numberOfInsertedRows > 0) });
			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}

			return generatedId;
		});
	}

	@Override
	public void update(Country country) {
		String message = "The country to be updated should not be null";
		Country nonNullCountry = Objects.requireNonNull(country, message);
		String sql = "UPDATE country SET continent = ? population = ? WHERE id = ?";

		connection.ifPresent(conn -> {
			try (PreparedStatement statement = conn.prepareStatement(sql)) {

				statement.setString(1, nonNullCountry.getContinent());
				statement.setLong(2, nonNullCountry.getPopulation());
				statement.setInt(3, nonNullCountry.getId());

				int numberOfUpdatedRows = statement.executeUpdate();

				LOGGER.log(Level.INFO, "Was the country updated successfully? {0}", numberOfUpdatedRows > 0);

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		});
	}

	@Override
	public void delete(Country country) {
		String message = "The country to be deleted should not be null";
		Country nonNullCountry = Objects.requireNonNull(country, message);
		String sql = "DELETE FROM country WHERE id = ?";

		connection.ifPresent(conn -> {
			try (PreparedStatement statement = conn.prepareStatement(sql)) {

				statement.setLong(1, nonNullCountry.getId());

				int numberOfDeletedRows = statement.executeUpdate();

				LOGGER.log(Level.INFO, "Was the country deleted successfully? {0}", numberOfDeletedRows > 0);

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		});
	}

	public Optional<Country> getByName(String name) {
		return connection.flatMap(conn -> {
			String sql = "SELECT * FROM country "
					+ "JOIN country_name ON country.id = country_name.country "
					+ "WHERE LOWER(name) = LOWER(?);";
			Optional<Country> country = Optional.empty();

			try (PreparedStatement statement = conn.prepareStatement(sql)) {

				statement.setString(1, name);
                ResultSet resultSet = statement.executeQuery();

				if (resultSet.next()) {
					int id = resultSet.getInt("id");
					String countryName = resultSet.getString("name");
					String continent = resultSet.getString("continent");
					long population = resultSet.getLong("population");

					country = Optional.of(new Country(id, countryName, continent, population));
				}

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}

			return country;
		});
	}

	public Optional<Country> getByCode(String code) {
		return connection.flatMap(conn -> {
			String sql = "SELECT * FROM country "
					+ "JOIN country_code ON country.id = country_code.country "
					+ "JOIN country_name ON country.id = country_name.country "
					+ "WHERE code = ?;";
			Optional<Country> country = Optional.empty();

			try (PreparedStatement statement = conn.prepareStatement(sql)) {

				statement.setString(1, code);
                ResultSet resultSet = statement.executeQuery();

				if (resultSet.next()) {
					int id = resultSet.getInt("id");
					String name = resultSet.getString("name");
					String continent = resultSet.getString("continent");
					long population = resultSet.getLong("population");

					country = Optional.of(new Country(id, name, continent, population));
				}

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}

			return country;
		});
	}

	public Optional<String> getCountryCode(Country country) {
		return connection.flatMap(conn -> {
			String sql = "SELECT code FROM country JOIN country_code ON id = country WHERE id = ?;";
			Optional<String> code = Optional.empty();

			try (PreparedStatement statement = conn.prepareStatement(sql)) {

				statement.setInt(1, country.getId());
                ResultSet resultSet = statement.executeQuery();

				if (resultSet.next()) {
					code = Optional.of(resultSet.getString("code"));
				}

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}

			return code;
		});
	}

	public Map<String, Integer> getNameToIdMap() {
		Map<String, Integer> countryMap = new HashMap<String, Integer>();
		String sql = "SELECT * FROM country_name;";

		connection.ifPresent(conn -> {
			try (Statement statement = conn.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {

				while (resultSet.next()) {
					String name = resultSet.getString("name");
					int id = resultSet.getInt("country");
//					LOGGER.log(Level.INFO, String.format("country insert {%s}, %d", name, id));
					countryMap.put(name, id);
				}

			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		});

		return countryMap;
	}

}
