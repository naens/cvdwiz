package com.covidwizard.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import com.covidwizard.model.Country;
import com.covidwizard.model.CountryGroup;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@WebServlet("/dynamics.json")
public class DynamicsJsonServlet extends HttpServlet {

	private static final long serialVersionUID = 72804363534429971L;
    static final Logger LOGGER = Logger.getLogger(Country.class.getName());

	private static CountryDao countryDao = new CountryDao();
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
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		String countryParameter = request.getParameter("country");
		boolean repair = request.getParameter("repair").equals("repair");
		String predictionString = request.getParameter("prediction");
		String graph = request.getParameter("graph");

		CovidStat covidStat = null;
		Integer predictionDays = predictionString.isEmpty() ? null : CovidTools.dateToDay(predictionString);
		if (CovidTools.isNumeric(countryParameter)) {
			int countryId = Integer.parseInt(countryParameter);
			Country country = countryDao.get(countryId).get();
			LOGGER.log(Level.INFO, String.format("DynamicsJsonServlet: country=%s", country.getName()));
			covidStat = CovidStat.make(country, repair, predictionDays);
		} else if (countryParameter.equals("all")) {
			covidStat = CovidStat.make(repair, predictionDays);
			LOGGER.log(Level.INFO, "DynamicsJsonServlet: country=all");
		} else if (countryParameter.startsWith("gr")) {
			int groupId = Integer.parseInt(countryParameter.substring(2));
			CountryGroup countryGroup = countryGroupDao.get(groupId).get();
			covidStat = CovidStat.make(countryGroup, repair, predictionDays);
			LOGGER.log(Level.INFO, String.format("DynamicsJsonServlet: country_group=%s", countryGroup.getName()));
		} else {
			throw new RuntimeException("DynamicsJsonServlet: unknown country parameter");
		}

		int firstDay = covidStat.getFirstDay();
		int maxDBDay = covidStat.getMaxDBDay();
		int lastDay = covidStat.getLastDay();

		Map<Integer, Double> graphMap = null;
		int graphPredictionStartDay = -1;
		String variableGraphName = null;
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
			graphPredictionStartDay = maxDBDay  - 9;
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
		result.add(arrayBeanGraphPrediction);
		result.add(arrayBeanTotalRate);
		result.add(arrayBeanEpidemicThreshold);
		result.add(arrayBeanTotalRate1);

		for (int k = firstDay; k <= lastDay; ++k) {
			if (graphMap.containsKey(k)) {
				if  (k <= graphPredictionStartDay) {
					arrayBeanGraph.x.add(CovidTools.dayToDate(k+1));		// +1 day fix
					arrayBeanGraph.y.add(graphMap.get(k));
				} else {
					arrayBeanGraphPrediction.x.add(CovidTools.dayToDate(k+1));		// +1 day fix
					arrayBeanGraphPrediction.y.add(graphMap.get(k));
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

		Info info = new Info();
		info.population = covidStat.getPopulation();
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

		String resultJsonString = gson.toJson(jsonArray);
		out.print(resultJsonString);
		out.flush();
		out.close();
	}

}
