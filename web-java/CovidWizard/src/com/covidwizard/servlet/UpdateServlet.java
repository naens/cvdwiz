package com.covidwizard.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.covidwizard.common.CovidStat;
import com.covidwizard.common.CovidTools;
import com.covidwizard.dao.CountryDao;
import com.covidwizard.dao.DataDao;
import com.covidwizard.model.Country;
import com.covidwizard.model.DataItem;

@WebServlet("/update")
public class UpdateServlet extends HttpServlet {

	private static final long serialVersionUID = -7943887809248224414L;
	private static final Logger LOGGER = Logger.getLogger(Country.class.getName());
	private static final int DAYS_POSITION_START = 4;

	private static final String DATAFILE = "datafile";
//	private static final String DATAFILE_ADDRESS = "https://raw.githubusercontent.com/datasets/covid-19/master/data/countries-aggregated.csv";
	private static final String DATAFILE_ADDRESS = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_global.csv";

	private static CountryDao countryDao = new CountryDao();
	private static DataDao dataDao = new DataDao();

	private Map<String, Integer> countryNameMap = null;

	private Map<Integer, Country> countryIdMap = null;
	Map<Integer, Country> idMap = getCountryIdMap();

	private Map<String, Integer> getCountryNameMap() {
		if (countryNameMap == null) {
			countryNameMap = countryDao.getNameToIdMap();
		}
		return countryNameMap;
	}

	private Map<Integer, Country> getCountryIdMap() {
		if (countryIdMap == null) {
			countryIdMap = new HashMap<Integer, Country>();
			Collection<Country> countries = countryDao.getAll();
			for (Country country : countries) {
				countryIdMap.put(country.getId(), country);
			}
		}
		return countryIdMap;
	}

	private void downloadDataFile(PrintWriter writer) throws MalformedURLException, IOException {
		BufferedInputStream in = new BufferedInputStream(new URL(DATAFILE_ADDRESS).openStream());
		FileOutputStream fileOutputStream = new FileOutputStream(DATAFILE);
		byte dataBuffer[] = new byte[1024];
		int bytesRead;
		while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
			fileOutputStream.write(dataBuffer, 0, bytesRead);
		}
		fileOutputStream.close();
		in.close();
		writer.println("refresh:ok");
	}

	private Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

	public boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		return pattern.matcher(strNum).matches();
	}

	private String unquote(String string) {
		if (string.matches("^\".*\"$")) {
			return string.substring(1, string.length() - 1);
		}
		return string;
	}

	private void saveCountry(int country, int dayFrom, int dayTo, int lastDay, int days, Map<Integer, Integer> numbers) {
		int sum = 0;
		for (int i = 0; i < days; ++i) {
			int day = i + lastDay - days +1;
			if (day >= dayFrom - 1 && day <= dayTo) {
				int cases = numbers.getOrDefault(i, 0) - sum;
//				LOGGER.log(Level.INFO, String.format("saving: %s", new DataItem(day, idMap.get(country), cases)));
				dataDao.save(new DataItem(day, idMap.get(country), cases));
			}
			sum = numbers.getOrDefault(i, 0);
		}
	}

	private void update(PrintWriter writer, int dayFrom, int dayTo) throws FileNotFoundException, IOException {
		Map<String, Integer> nameMap = getCountryNameMap();
		LOGGER.log(Level.INFO, "update servlet update");
		try (BufferedReader br = new BufferedReader(new FileReader(DATAFILE))) {
			String line = br.readLine();
			LOGGER.log(Level.INFO, String.format("line: %s",  line));
			String[] header = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
			int size = header.length;
			int days = size - DAYS_POSITION_START;
			LOGGER.log(Level.INFO, String.format("days: %d",  days));
			int lastDay = CovidTools.dateToDay(header[size - 1]); // TODO convert dates like 2/16/20 to day number
			LOGGER.log(Level.INFO, String.format("lastDay: %d",  lastDay));
			Map<Integer, Integer> numbers = new HashMap<Integer, Integer>(days);
			String currentCountry = new String();
			Integer country = -1;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

				if (values[1].equals(currentCountry)) {
					for (int i = 0; i < size - DAYS_POSITION_START; ++i) {
						numbers.put(i, numbers.getOrDefault(i, 0) + Integer.parseInt(values[DAYS_POSITION_START + i]));
					}
				} else {
					if (!currentCountry.equals("")) {
						saveCountry(country, dayFrom, dayTo, lastDay, days, numbers);
//						LOGGER.log(Level.INFO, String.format("COUNTRY %s: SAVED (1)", currentCountry));
					}
					currentCountry = unquote(values[1]);
//					LOGGER.log(Level.INFO, String.format("COUNTRY %s", currentCountry));
					country = nameMap.get(currentCountry);
					for (int i = 0; i < size - DAYS_POSITION_START; ++i) {
						numbers.put(i, Integer.parseInt(values[DAYS_POSITION_START + i]));
					}
				}

			}
			// TODO: last conutry check?
			saveCountry(country, dayFrom, dayTo, lastDay, days, numbers);
//			LOGGER.log(Level.INFO, String.format("COUNTRY %s: SAVED (2)", currentCountry));
			writer.println("update:ok");
			CovidStat.refreshDensities();
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String command = request.getParameter("command");
		PrintWriter writer = response.getWriter();
		switch (command) {
		case "refresh":
			downloadDataFile(writer);
			break;
		case "update":
			String dateFrom = request.getParameter("date_from");
			String dateTo = request.getParameter("date_from");
			String allString = request.getParameter("all");
			boolean all = allString != null && allString.equals("true");
			Integer days;
			try {
				days = Integer.parseInt(request.getParameter("days"));
			} catch (NumberFormatException e) {
				days = null;
			}
			int dayFrom;
			int dayTo;
			if (all) {
				dayFrom = 0;
				dayTo = Integer.MAX_VALUE;
			} else if (dateFrom == null) {
				dayTo = CovidTools.today();
				dayFrom = dayTo - days + 1;
			} else if (dateTo == null) {
				dayFrom = CovidTools.dateToDay(dateFrom);
				dayTo = dayFrom;
			} else {
				dayFrom = CovidTools.dateToDay(dateFrom);
				dayTo = CovidTools.dateToDay(dateTo);
			}
			update(writer, dayFrom, dayTo);
			break;
		}
		writer.close();
	}

}
