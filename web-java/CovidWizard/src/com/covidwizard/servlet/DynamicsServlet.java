package com.covidwizard.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbottema.rtftohtml.RTF2HTMLConverter;
import org.bbottema.rtftohtml.impl.RTF2HTMLConverterJEditorPane;

import com.covidwizard.dao.CountryDao;
import com.covidwizard.model.Country;

@WebServlet("/index")
public class DynamicsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static CountryDao countryDao = new CountryDao();

	private static final String FAQ_RTF_FILE = "/WEB-INF/FAQ.rtf";

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Collection<Country> countries = countryDao.getAllDataLimited();
		request.setAttribute("countries", countries);
		RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");


		request.setAttribute("FAQ", getFAQ());
		dispatcher.forward(request, response);
	}

	public String getFAQ() throws IOException {
		RTF2HTMLConverter converter = RTF2HTMLConverterJEditorPane.INSTANCE;
		//RTF2HTMLConverter converter = RTF2HTMLConverterClassic.INSTANCE;
		//RTF2HTMLConverter converter = RTF2HTMLConverterRFCCompliant.INSTANCE;

		ServletContext context = getServletContext();
		String fullPath = context.getRealPath(FAQ_RTF_FILE);
        String rtf = new String(Files.readAllBytes(Paths.get(fullPath)));

		String html = converter.rtf2html(rtf);
		return html;
		//return new File(FAQ_RTF_FILE).getAbsolutePath();
	}
}
