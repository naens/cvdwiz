package com.covidwizard.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.covidwizard.common.CovidStat;
import com.covidwizard.common.CovidTools;
import com.covidwizard.dao.CountryGroupDao;
import com.covidwizard.model.Country;
import com.google.gson.Gson;

@WebServlet("/density.json")
public class DensityServlet extends HttpServlet {

	private static final long serialVersionUID = 364554604663779284L;
	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(Country.class.getName());
//	private static CountryDao countryDao = new CountryDao();
	private static CountryGroupDao countryGroupDao = new CountryGroupDao();

	class Data {
		ArrayList<String> header = new ArrayList<String>();
		ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
		Integer rank;
		int length;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Integer countryId = null;
		String countryParameter = request.getParameter("country");
		if (CovidTools.isNumeric(countryParameter)) {
			countryId = Integer.parseInt(countryParameter);
		}
//		Country country = countryDao.get(countryId).get();
		String countryGroupString = request.getParameter("country_group");
		Set<Country> countryGroupSet = null;
//		LOGGER.log(Level.INFO, String.format("DENSITY_SERVLET: countryGroupString: %s", countryGroupString));
		if (CovidTools.isNumeric(countryGroupString)) {
			int countryGroupId = Integer.parseInt(request.getParameter("country_group"));
			countryGroupSet = countryGroupDao.getCountryGroupCountries(countryGroupId);
//			LOGGER.log(Level.INFO, String.format("DENSITY_SERVLET: found %d countries in group %d",
//					countryGroupSet.size(), countryGroupId));
		}

		Map<Country, Double> densities = CovidStat.getDensities();

		PrintWriter writer = response.getWriter();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		Data data = new Data();
		data.header = new ArrayList<String>(Arrays.asList("#", "Country", "Transmission Risk"));
		data.rows = new ArrayList<ArrayList<String>>();
		data.length = densities.size();
		data.rank = null;

		int i = 1;
		for (Entry<Country, Double> entry : densities.entrySet()) {
			String number = Integer.toString(i);
			String countryName = entry.getKey().getName();
			String density = String.format("%6f", entry.getValue());
			if (countryGroupSet == null || countryGroupSet.contains(entry.getKey())) {
//				LOGGER.log(Level.INFO, String.format("DENSITY_SERVLET: adding country: %s", entry.getKey().getName()));
				data.rows.add(new ArrayList<String>(Arrays.asList(number, countryName, density)));
			}
			if (countryId != null && entry.getKey().getId() == countryId) {
				data.rank = i;
			}
			++i;
		}

		Gson gson = new Gson();
		writer.print(gson.toJson(data));
		writer.flush();
		writer.close();
	}

}
