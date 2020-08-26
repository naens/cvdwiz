package com.covidwizard.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.covidwizard.dao.DataDao.DataItemKey;
import com.covidwizard.model.Country;
import com.covidwizard.model.DataItem;

public class DataDao implements Dao<DataItem, DataItemKey> {

    private static final Logger LOGGER = Logger.getLogger(Country.class.getName());
    private final Optional<Connection> connection;

    public DataDao() {
        this.connection = JdbcConnection.getConnection();
    }
	
	public class DataItemKey {
		private int countryId;
		private int day;
		public DataItemKey(int countryId, int day) {
			super();
			this.countryId = countryId;
			this.day = day;
		}
		public int getCountryId() {
			return countryId;
		}
		public int getDay() {
			return day;
		}
	}

	@Override
	public Optional<DataItem> get(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataItem> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<DataItemKey> save(DataItem dataItem) {
		//LOGGER.log(Level.INFO, "Saving {0} in database", dataItem);
		String sql =  "INSERT INTO data (day, country, new_cases) VALUES (?, ?, ?) "
				+ "ON CONFLICT (day, country) DO UPDATE SET new_cases=EXCLUDED.new_cases;";
		return connection.flatMap(conn -> {
			Optional<DataItemKey> dataItemKey = Optional.empty();

			try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

				statement.setInt(1, dataItem.getDay());
				statement.setLong(2, dataItem.getCountry().getId());
				statement.setInt(3, dataItem.getNewCases());

				int numberOfInsertedRows = statement.executeUpdate();
				if (numberOfInsertedRows == 0) {
					throw new RuntimeException("data item save: no rows inserted");
				}
			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}

			return dataItemKey;
		});
	}

	@Override
	public void update(DataItem t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(DataItem t) {
		// TODO Auto-generated method stub
		
	}

	public List<DataItem> getData(Country country, int dayFrom, int dayTo) {
		List<DataItem> items = new LinkedList<>();
        String sql = "SELECT day, country, new_cases FROM data "
        		+ "WHERE country = ? AND day >= ? AND day <= ? ORDER BY DAY;";

	    connection.ifPresent(conn -> {
	        try (PreparedStatement statement =
	                 conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                statement.setInt(1, country.getId());
                statement.setInt(2, dayFrom);
                statement.setInt(3, dayTo);

                ResultSet resultSet = statement.executeQuery();

	            while (resultSet.next()) {
	                int day = resultSet.getInt("day");
	                int newCases = resultSet.getInt("new_cases");

	                DataItem item = new DataItem(day, country, newCases);

	                items.add(item);

	                //LOGGER.log(Level.INFO, "Found {0} in database", country);
	            }

	        } catch (SQLException ex) {
	            LOGGER.log(Level.SEVERE, null, ex);
	        }
	    });

	    return items;
	}

	public List<DataItem> getDataByCountry(Country country) {
		List<DataItem> items = new LinkedList<DataItem>();
        String sql = "SELECT day, country, new_cases FROM data "
        		+ "WHERE country = ? ORDER BY DAY;";
//        LOGGER.log(Level.INFO, "getDataByCountry: {0}", country);

	    connection.ifPresent(conn -> {
	        try (PreparedStatement statement =
	                 conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                statement.setInt(1, country.getId());

                ResultSet resultSet = statement.executeQuery();

	            while (resultSet.next()) {
	                int day = resultSet.getInt("day");
	                int newCases = resultSet.getInt("new_cases");

	                DataItem item = new DataItem(day, country, newCases);

	                items.add(item);
	            }

	        } catch (SQLException ex) {
	            LOGGER.log(Level.SEVERE, null, ex);
	        }
	    });

	    return items;
	}

	public int getLastDay(Country country) {
        String sql = "SELECT MAX(day) as day FROM data WHERE country = ?;";
        List<Integer> days = new LinkedList<Integer>();

	    connection.ifPresent(conn -> {
	        try (PreparedStatement statement =
	                 conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, country.getId());

                ResultSet resultSet = statement.executeQuery();

	            if (resultSet.next()) {
	            	days.add(resultSet.getInt("day"));
	            }

	        } catch (SQLException ex) {
	            LOGGER.log(Level.SEVERE, null, ex);
	        }
	    });

	    return days.get(0);
	}

	public int getFirstDay(Country country) {
        String sql = "SELECT MIN(day) as day FROM data WHERE country = ?;";
        List<Integer> days = new LinkedList<Integer>();

	    connection.ifPresent(conn -> {
	        try (PreparedStatement statement =
	                 conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, country.getId());

                ResultSet resultSet = statement.executeQuery();

	            if (resultSet.next()) {
	            	days.add(resultSet.getInt("day"));
	            }

	        } catch (SQLException ex) {
	            LOGGER.log(Level.SEVERE, null, ex);
	        }
	    });

	    return days.get(0);
	}

}
