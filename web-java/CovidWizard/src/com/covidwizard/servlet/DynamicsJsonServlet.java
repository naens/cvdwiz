package com.covidwizard.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.covidwizard.common.CovidStat;
import com.covidwizard.common.CovidTools;
import com.covidwizard.dao.CountryDao;
import com.covidwizard.dao.CountryGroupDao;
import com.covidwizard.dao.DataDao;
import com.covidwizard.model.Country;
import com.covidwizard.model.CountryGroup;
import com.covidwizard.model.DataItem;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@WebServlet("/dynamics.json")
public class DynamicsJsonServlet extends HttpServlet {

	private static final long serialVersionUID = 72804363534429971L;
    static final Logger LOGGER = Logger.getLogger(Country.class.getName());

	private static CountryDao countryDao = new CountryDao();
	private static DataDao dataDao = new DataDao();
	private static CountryGroupDao countryGroupDao = new CountryGroupDao();

	class ArrayBean {
		ArrayList<String> x = new ArrayList<String>();
		ArrayList<Double> y = new ArrayList<Double>();
	}

	class Info {
		long population;
		double hlast;
		Double lastCases;
		int zeroCases;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
//        LOGGER.log(Level.INFO, "DynamicsJsonServlet");
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		String countryParameter = request.getParameter("country");
		boolean repair = request.getParameter("repair").equals("repair");

		List<DataItem> items = null;
		int lastDay = -1;
		long population = -1;
		if (CovidTools.isNumeric(countryParameter)) {
			int countryId = Integer.parseInt(countryParameter);
			Country country = countryDao.get(countryId).get();
			items = dataDao.getDataByCountry(country);
			lastDay = dataDao.getLastDay(country);
			population = country.getPopulation();
		} else if (countryParameter.equals("all")) {
			items = dataDao.getWorldData();
			lastDay = dataDao.getWorldLastDay();
			population = countryGroupDao.getWorldPopulation().get();
		} else if (countryParameter.startsWith("gr")) {
			int groupId = Integer.parseInt(countryParameter.substring(2));
			CountryGroup countryGroup = countryGroupDao.get(groupId).get();
			items = dataDao.getGroupData(countryGroup);
			lastDay = dataDao.getGroupLastDay(countryGroup);
			population = countryGroupDao.getGroupPopulation(countryGroup).get();
		} else {
			throw new RuntimeException("DynamicsJsonServlet: unknown country parameter");
		}
		int firstDay = items.get(0).getDay();
		Map<Integer, Double> cases = new HashMap<Integer, Double>();
		for (int i = 0; i < items.size(); ++i) {
			DataItem item = items.get(i);
			cases.put(item.getDay(), item.getNewCases());
		}

		CovidStat covidStat = new CovidStat(cases, firstDay, lastDay, repair);

		Gson gson = new Gson();
		ArrayList<ArrayBean> result = new ArrayList<ArrayBean>();
		ArrayBean arrayBeanHiddenHolders = new ArrayBean();
		ArrayBean arrayBeanTotalRate = new ArrayBean();
		ArrayBean arrayBeanEpidemicThreshold = new ArrayBean();
		ArrayBean arrayBeanTotalRate1 = new ArrayBean();
		ArrayBean arrayBeanHiddenHolders1 = new ArrayBean();
		result.add(arrayBeanHiddenHolders);
		result.add(arrayBeanTotalRate);
		result.add(arrayBeanEpidemicThreshold);
		result.add(arrayBeanTotalRate1);
		result.add(arrayBeanHiddenHolders1);

		for (int k = firstDay; k <= lastDay; ++k) {
			if  (k <= lastDay - 9 && covidStat.getHiddenHolders().containsKey(k)) {
				arrayBeanHiddenHolders.x.add(CovidTools.dayToDate(k+1));		// +1 day fix
				arrayBeanHiddenHolders.y.add((double) covidStat.getHiddenHolders().get(k));
			} else if (covidStat.getHiddenHolders1().containsKey(k)) {
				arrayBeanHiddenHolders1.x.add(CovidTools.dayToDate(k+1));		// +1 day fix
				arrayBeanHiddenHolders1.y.add(covidStat.getHiddenHolders1().get(k));
			}

			if  (k <= lastDay - 10 && covidStat.getTotalInfectionRate().containsKey(k)) {
				arrayBeanTotalRate.x.add(CovidTools.dayToDate(k+1));			// +1 day fix
				arrayBeanTotalRate.y.add(covidStat.getTotalInfectionRate().get(k));
			} else if (covidStat.getTotalInfectionRate1().containsKey(k)) {
				arrayBeanTotalRate1.x.add(CovidTools.dayToDate(k+1));			// +1 day fix
				arrayBeanTotalRate1.y.add(covidStat.getTotalInfectionRate1().get(k));
			}

			arrayBeanEpidemicThreshold.x.add(CovidTools.dayToDate(k+1));	// +1 day fix
			arrayBeanEpidemicThreshold.y.add(1.0);
		}
		LOGGER.log(Level.INFO, String.format("DynamicsJsonServlet: lastDay=%d", lastDay));

		Info info = new Info();
		info.population = population;
//		info.hlast = covidStat.getHiddenHolders().get(Collections.max(covidStat.getHiddenHolders().keySet()));
		info.hlast = covidStat.getHiddenHolders().get(lastDay - 9);
		info.lastCases = covidStat.getCases().get(Collections.max(covidStat.getCases().keySet()));

		info.zeroCases = 0;
		boolean hasNoZero = false;
		for (int k = lastDay; k >= firstDay && !hasNoZero; --k) {
			if (covidStat.getCases().get(k) > 0.0001) {
				hasNoZero = true;
			} else {
				info.zeroCases++;
			}
		}

		JsonArray jsonArray = new JsonArray();
		for (ArrayBean ab: result) {
			JsonElement jsonElement = gson.toJsonTree(ab);
			jsonArray.add(jsonElement);
		}
		jsonArray.add(gson.toJsonTree(info));

//		String resultJsonString = gson.toJson(result);
		String resultJsonString = gson.toJson(jsonArray);
		out.print(resultJsonString);
		out.flush();
		out.close();
	}

}
