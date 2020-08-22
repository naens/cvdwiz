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

import com.covidwizard.common.CovidTools;
import com.covidwizard.dao.CountryDao;
import com.covidwizard.dao.DataDao;
import com.covidwizard.model.Country;
import com.covidwizard.model.DataItem;

@WebServlet("/update")
public class UpdateServlet extends HttpServlet {

	private static final long serialVersionUID = -7943887809248224414L;
	private static final Logger LOGGER = Logger.getLogger(Country.class.getName());

	private static final String DATAFILE = "datafile";
	private static final String DATAFILE_ADDRESS = "https://raw.githubusercontent.com/datasets/covid-19/master/data/countries-aggregated.csv";

	private static CountryDao countryDao = new CountryDao();
	private static DataDao dataDao = new DataDao();

	private Map<String, Integer> countryNameMap = null;

	private Map<Integer, Country> countryIdMap = null;

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

	private void update(PrintWriter writer, int dayFrom, int dayTo) throws FileNotFoundException, IOException {
		Map<String, Integer> nameMap = getCountryNameMap();
		Map<Integer, Country> idMap = getCountryIdMap();
		Map<Integer, Integer> totalCasesMap = new HashMap<Integer, Integer>();
		LOGGER.log(Level.INFO, "update servlet update");
		try (BufferedReader br = new BufferedReader(new FileReader(DATAFILE))) {
			String line;
			br.readLine();
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				int day = CovidTools.dateToDay(values[0]);
				if (day < dayFrom - 1 || day > dayTo) {
					continue;
				}
				String countryString = unquote(values[1]);
				int country = nameMap.get(countryString);
				int sum = Integer.parseInt(values[2]);
				if (day >= dayFrom) {
					int newCases = sum - totalCasesMap.getOrDefault(country, 0);
					dataDao.save(new DataItem(day, idMap.get(country), newCases));
				}
				totalCasesMap.put(country, sum);
			}
			writer.println("update:ok");
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
			Integer days;
			try {
				days = Integer.parseInt(request.getParameter("days"));
			} catch (NumberFormatException e) {
				days = null;
			}
			int dayFrom;
			int dayTo;
			if (dateFrom == null) {
				dayTo = CovidTools.today();
				dayFrom = dayTo - days;
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
