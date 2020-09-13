package com.covidwizard.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
		String variableGraphName;
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
		String predictionString = request.getParameter("prediction");
//		LOGGER.log(Level.INFO, String.format("DynamicsJsonServlet: predictionString=\"%s\"", predictionString));
		String graph = request.getParameter("graph");

		List<DataItem> items = null;
		int maxDBDay = -1;			// maximum day in the database
		long population = -1;
		if (CovidTools.isNumeric(countryParameter)) {
			int countryId = Integer.parseInt(countryParameter);
			Country country = countryDao.get(countryId).get();
			items = dataDao.getDataByCountry(country);
			maxDBDay = dataDao.getMaxDay(country);
			population = country.getPopulation();
		} else if (countryParameter.equals("all")) {
			items = dataDao.getWorldData();
			maxDBDay = dataDao.getWorldMaxDay();
			population = countryGroupDao.getWorldPopulation().get();
		} else if (countryParameter.startsWith("gr")) {
			int groupId = Integer.parseInt(countryParameter.substring(2));
			CountryGroup countryGroup = countryGroupDao.get(groupId).get();
			items = dataDao.getGroupData(countryGroup);
			maxDBDay = dataDao.getGroupMaxDay(countryGroup);
			population = countryGroupDao.getGroupPopulation(countryGroup).get();
		} else {
			throw new RuntimeException("DynamicsJsonServlet: unknown country parameter");
		}
		int predictionDays = predictionString.isEmpty() ? 0 : CovidTools.dateToDay(predictionString) - maxDBDay;
		int firstDay = items.get(0).getDay();
		Map<Integer, Double> cases = new HashMap<Integer, Double>();
		for (int i = 0; i < items.size(); ++i) {
			DataItem item = items.get(i);
			cases.put(item.getDay(), item.getNewCases());
		}

		CovidStat covidStat = new CovidStat(population, cases, firstDay, maxDBDay, repair, predictionDays);

		Map<Integer, Double> graphMap = null;
		int graphPredictionStartDay = -1;
		String variableGraphName = null;
		LOGGER.log(Level.INFO, String.format("DynamicsJsonServlet: graph=%s", graph));
		switch (graph) {
		case "hidden_holders":
			variableGraphName = "Hidden Holders";
			graphPredictionStartDay = maxDBDay - 9;
			graphMap = covidStat.getHiddenHolders();
			break;
		case "new_cases":
			variableGraphName = "New Cases";
			graphPredictionStartDay = maxDBDay + 2;
			graphMap = covidStat.getCases();
			break;
		case "total_cases":
			variableGraphName = "Total Cases";
			graphPredictionStartDay = maxDBDay + 1;
			graphMap = covidStat.getTotalCases();
			break;
		case "trisk":
			variableGraphName = "TRisk";
			graphPredictionStartDay = maxDBDay + predictionDays;
			graphMap = covidStat.getDensityList();
			break;
		default:
			throw new RuntimeException("DynamicsJsonServlet: unknown graph parameter");
		}

		Gson gson = new Gson();
		ArrayList<ArrayBean> result = new ArrayList<ArrayBean>();
		ArrayBean arrayBeanGraph = new ArrayBean();
		ArrayBean arrayBeanTotalRate = new ArrayBean();
		ArrayBean arrayBeanEpidemicThreshold = new ArrayBean();
		ArrayBean arrayBeanTotalRate1 = new ArrayBean();
		ArrayBean arrayBeanGraphPrediction = new ArrayBean();
		result.add(arrayBeanGraph);
		result.add(arrayBeanTotalRate);
		result.add(arrayBeanEpidemicThreshold);
		result.add(arrayBeanTotalRate1);
		result.add(arrayBeanGraphPrediction);

		for (int k = firstDay; k <= maxDBDay + predictionDays; ++k) {
			if (graphMap.containsKey(k)) {
				if  (k <= graphPredictionStartDay) {
					arrayBeanGraph.x.add(CovidTools.dayToDate(k+1));		// +1 day fix
					arrayBeanGraph.y.add(graphMap.get(k));
					LOGGER.log(Level.INFO, String.format("DynamicsJsonServlet(A): x=%d, y=%.2f", k, graphMap.get(k)));
				} else {
					arrayBeanGraphPrediction.x.add(CovidTools.dayToDate(k+1));		// +1 day fix
					arrayBeanGraphPrediction.y.add(graphMap.get(k));
					LOGGER.log(Level.INFO, String.format("DynamicsJsonServlet(B): x=%d, y=%.2f", k, graphMap.get(k)));
				}
			}
			if (covidStat.getTotalInfectionRate().containsKey(k)) {
				if  (k <= maxDBDay - 10) {
					arrayBeanTotalRate.x.add(CovidTools.dayToDate(k+1));			// +1 day fix
					arrayBeanTotalRate.y.add(covidStat.getTotalInfectionRate().get(k));
				} else {
					arrayBeanTotalRate1.x.add(CovidTools.dayToDate(k+1));			// +1 day fix
					arrayBeanTotalRate1.y.add(covidStat.getTotalInfectionRate().get(k));
				}
				
			}

			arrayBeanEpidemicThreshold.x.add(CovidTools.dayToDate(k+1));	// +1 day fix
			arrayBeanEpidemicThreshold.y.add(1.0);
		}
		LOGGER.log(Level.INFO, String.format("DynamicsJsonServlet: lastDay=%d", maxDBDay));

		Info info = new Info();
		info.population = population;
//		info.hlast = covidStat.getHiddenHolders().get(Collections.max(covidStat.getHiddenHolders().keySet()));
		info.hlast = covidStat.getHiddenHolders().get(maxDBDay - 9);
		info.lastCases = covidStat.getCases().get(maxDBDay);
		info.variableGraphName = variableGraphName;

		info.zeroCases = 0;
		boolean hasNoZero = false;
		for (int k = maxDBDay; k >= firstDay && !hasNoZero; --k) {
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
