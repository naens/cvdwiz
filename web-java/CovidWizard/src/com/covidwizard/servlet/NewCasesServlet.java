package com.covidwizard.servlet;

import java.io.IOException;
import java.io.PrintWriter;
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

@WebServlet("/newcases")
public class NewCasesServlet extends HttpServlet {

	private static final long serialVersionUID = 7828134101123495298L;
	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(Country.class.getName());

	private static CountryDao countryDao = new CountryDao();
	private static DataDao dataDao = new DataDao();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int countryId = Integer.parseInt(request.getParameter("country"));
		Country country = countryDao.get(countryId).get();
		boolean repair = request.getParameter("repair").equals("repair");
		PrintWriter writer = response.getWriter();
		writer.println("<table border=\"1\">");
		writer.println("<tbody>");
		writer.println("<tr><th>Date</th><th>New Cases</th><th>Total Cases</th><th>Hidden Holders</th><th>Infection Rate</th><th>Total Rate (TIR)</th></tr>");
		List<DataItem> items = dataDao.getDataByCountry(country);
		int firstDay = items.get(0).getDay();
		int lastDay = dataDao.getLastDay(country);
		Map<Integer, Integer> cases = new HashMap<Integer, Integer>();
		for (int i = 0; i < items.size(); ++i) {
			DataItem item = items.get(i);
			cases.put(item.getDay(), item.getNewCases());
		}

		CovidStat covidStat = new CovidStat(cases, firstDay, lastDay, repair);

		for (int k = lastDay; k >= firstDay; --k) {
			writer.println(String.format("<tr><td>%s</td><td>%d</td><td>%d</td><td>%s</td><td>%s</td><td>%s</td></tr>",
					CovidTools.dayToDate(k+1),
					covidStat.getCases().get(k),
					covidStat.getSum().get(k),
					k <= lastDay - 9
						? String.format("<div class=\"tab_hh_normal\">%d</div>", covidStat.getHiddenHolders().get(k))
						: String.format("<div class=\"tab_hh_prediction\">%.1f</div>", covidStat.getHiddenHolders1().get(k)),
					k <= lastDay - 10
						? String.format("<div class=\"tab_ir_normal\">%f</div>", covidStat.getInfectionRate().get(k))
						: String.format("<div class=\"tab_ir_prediction\">%f</div>", covidStat.getInfectionRate1().get(k)),
					k <= lastDay - 10
						? String.format("<div class=\"tab_tr_normal\">%f</div>", covidStat.getTotalInfectionRate().get(k))
						: String.format("<div class=\"tab_tr_prediction\">%f</div>", covidStat.getTotalInfectionRate1().get(k))));
		}
		writer.println("</tbody>");
		writer.println("</table>");
		writer.close();
	}

}
