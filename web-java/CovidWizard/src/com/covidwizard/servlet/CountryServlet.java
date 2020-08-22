package com.covidwizard.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.covidwizard.dao.CountryDao;
import com.covidwizard.model.Country;

@WebServlet("/country")
public class CountryServlet extends HttpServlet {

	private static final long serialVersionUID = -1023817484327133387L;

	private Pattern numericPattern = Pattern.compile("-?\\d+(\\.\\d+)?");

	private Pattern codePattern = Pattern.compile("[A-Z]{3}");

	private static CountryDao countryDao = new CountryDao();
	 
	public boolean isNumeric(String string) {
	    if (string == null) {
	        return false; 
	    }
	    return numericPattern.matcher(string).matches();
	}

	public boolean isCountryCode(String string) {
	    if (string == null) {
	        return false; 
	    }
	    return codePattern.matcher(string).matches();
		
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String countryString = request.getParameter("country");
		Optional<Country> country = null;
		String method = null;
		if (isNumeric(countryString)) {
			method = "by id";
			country = countryDao.get(Integer.parseInt(countryString));
		} else if (isCountryCode(countryString)) {
			method = "by code";
			country = countryDao.getByCode(countryString);
		} else {
			method = "by name";
			country = countryDao.getByName(countryString);
		}

		PrintWriter writer = response.getWriter();
		if (country == null) {
			writer.println("unknown error");
			writer.close();
			return;
		}

		try {
			writer.println(String.format("id: %d", country.get().getId()));
			writer.println(String.format("code: %s", countryDao.getCountryCode(country.get()).get()));
			writer.println(String.format("name: %s", country.get().getName()));
			writer.println(String.format("method: %s", method));
		} catch (NoSuchElementException e) {
			writer.println("error: no such element");
			writer.close();
		}
		writer.close();
	}
}
