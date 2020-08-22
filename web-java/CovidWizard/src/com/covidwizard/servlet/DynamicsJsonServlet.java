package com.covidwizard.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@WebServlet("/dynamics.json")
public class DynamicsJsonServlet extends HttpServlet {

	private static final long serialVersionUID = 72804363534429971L;
    static final Logger LOGGER = Logger.getLogger(Country.class.getName());

	private static CountryDao countryDao = new CountryDao();
	private static DataDao dataDao = new DataDao();

	class ArrayBean {
		ArrayList<String> x = new ArrayList<String>();
		ArrayList<Double> y = new ArrayList<Double>();
	}

	class Info {
		long population;
		double hlast;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
//        LOGGER.log(Level.INFO, "DynamicsJsonServlet");
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		int countryId = Integer.parseInt(request.getParameter("country"));

		Country country = countryDao.get(countryId).get();
		boolean repair = request.getParameter("repair").equals("repair");
		List<DataItem> items = dataDao.getDataByCountry(country);
		int firstDay = items.get(0).getDay();
		int lastDay = dataDao.getLastDay(country);
		Map<Integer, Integer> cases = new HashMap<Integer, Integer>();
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

		Info info = new Info();
		info.population = country.getPopulation();
		info.hlast = covidStat.getHiddenHolders().get(lastDay - 9);

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
	}

}
