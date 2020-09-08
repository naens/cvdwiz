package com.covidwizard.servlet;

import java.io.IOException;
import java.io.PrintWriter;
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

@WebServlet("/newcases")
public class NewCasesServlet extends HttpServlet {

	private static final long serialVersionUID = 7828134101123495298L;
	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(Country.class.getName());

	private static CountryDao countryDao = new CountryDao();
	private static DataDao dataDao = new DataDao();
	private static CountryGroupDao countryGroupDao = new CountryGroupDao();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String countryParameter = request.getParameter("country");

		boolean repair = request.getParameter("repair").equals("repair");
		String predictionString = request.getParameter("prediction");

		PrintWriter writer = response.getWriter();
		writer.println("<table border=\"1\">");
		writer.println("<tbody>");
		writer.println("<tr><th>Date</th><th>New Cases</th><th>Total Cases</th><th>Hidden Holders</th><th>Infection Rate</th><th>Total Rate (TIR)</th></tr>");

//		List<DataItem> items = dataDao.getDataByCountry(country);
//		int firstDay = items.get(0).getDay();
//		int lastDay = dataDao.getLastDay(country);
//		Map<Integer, Integer> cases = new HashMap<Integer, Integer>();
//		for (int i = 0; i < items.size(); ++i) {
//			DataItem item = items.get(i);
//			cases.put(item.getDay(), item.getNewCases());
//		}
		List<DataItem> items = null;
		int maxDBDay = -1;		// last day in the database
		if (CovidTools.isNumeric(countryParameter)) {
			int countryId = Integer.parseInt(countryParameter);
			Country country = countryDao.get(countryId).get();
			items = dataDao.getDataByCountry(country);
			maxDBDay = dataDao.getMaxDay(country);
		} else if (countryParameter.equals("all")) {
			items = dataDao.getWorldData();
			maxDBDay = dataDao.getWorldMaxDay();
		} else if (countryParameter.startsWith("gr")) {
			int groupId = Integer.parseInt(countryParameter.substring(2));
			CountryGroup countryGroup = countryGroupDao.get(groupId).get();
			items = dataDao.getGroupData(countryGroup);
			maxDBDay = dataDao.getGroupMaxDay(countryGroup);
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

		CovidStat covidStat = new CovidStat(cases, firstDay, maxDBDay, repair, predictionDays);

		LOGGER.log(Level.INFO, String.format("NewCasesServlet: predictionDays=%d", predictionDays));
		boolean hasNoZero = false;
		for (int k = maxDBDay + predictionDays; k >= firstDay; --k) {
//			LOGGER.log(Level.INFO, String.format("NewCasesServlet: country=%s, k=%d, firstDay = %d, lastDay=%d", countryParameter, k, firstDay, lastDay));
			if (covidStat.getCases().get(k) > 0.0001) {
				hasNoZero = true;
			}
			double hiddenHolders = covidStat.getHiddenHolders().getOrDefault(k, 0.0);
			double infectionRate = covidStat.getInfectionRate().getOrDefault(k, 0.0);
			double totalInfectionRate = covidStat.getTotalInfectionRate().getOrDefault(k, 0.0);
			String dayClass = k == maxDBDay ? "tab_today" : "tab_day";
			String hiddenHoldersClass = k <= maxDBDay - 9 ? "tab_hh_normal" : "tab_hh_prediction";
			String infectionRateClass = k <= maxDBDay - 10 ? "tab_ir_normal" : "tab_ir_prediction";
			String totalInfectionRateClass = k <= maxDBDay - 10 ? "tab_tr_normal" : "tab_tr_prediction";
			writer.println(String.format("<tr><td>%s</td><td>%s</td><td>%.1f</td><td>%s</td><td>%s</td><td>%s</td></tr>",
					String.format("<div id=\"%s\">%s</div>", dayClass, CovidTools.dayToDate(k+1)),
					hasNoZero ? String.format("%.1f", covidStat.getCases().get(k)) : "?",
					covidStat.getTotalCases().get(k),
					String.format("<div class=\"%s\">%.1f</div>", hiddenHoldersClass, hiddenHolders),
					String.format("<div class=\"%s\">%f</div>", infectionRateClass, infectionRate),
					String.format("<div class=\"%s\">%f</div>", totalInfectionRateClass, totalInfectionRate)));
		}
		writer.println("</tbody>");
		writer.println("</table>");
		writer.close();
	}

}
