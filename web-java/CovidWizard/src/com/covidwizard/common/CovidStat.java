package com.covidwizard.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.covidwizard.dao.CountryDao;
import com.covidwizard.dao.CountryGroupDao;
import com.covidwizard.dao.DataDao;
import com.covidwizard.model.Country;
import com.covidwizard.model.CountryGroup;
import com.covidwizard.model.DataItem;

public class CovidStat {

	private static final Logger LOGGER = Logger.getLogger(Country.class.getName());

	private static List<Country> countryList = new LinkedList<Country>();
	private static Map<Country, Double> densityMap = new HashMap<Country, Double>();
	private static DataDao dataDao = new DataDao();
	private static CountryDao countryDao = new CountryDao();
	private static CountryGroupDao countryGroupDao = new CountryGroupDao();

	static {
		Collection<Country> countries = countryDao.getAll();
		for (Country country : countries) {
			countryList.add(country);
		}
	}

	private Map<Integer, Double> cases = null;
	private Map<Integer, Double> totalCases = new HashMap<Integer, Double>();
	private Map<Integer, Double> hiddenHolders = new HashMap<Integer, Double>();
	private Map<Integer, Double> infectionRate = new HashMap<Integer, Double>();
	private Map<Integer, Double> totalInfectionRate = new HashMap<Integer, Double>();
	private Map<Integer, Double> densityList = new HashMap<Integer, Double>();

	private int firstDay;
	private int maxDBDay;
	private int predictionDays;

	private long population;

	public CovidStat(long population, Map<Integer, Double> cases, int firstDay, int maxDBDay, boolean repair, int predictionDays) {
		assert(predictionDays > 0);
		this.population = population;
		this.cases = cases;
		this.firstDay = firstDay;
		this.maxDBDay = maxDBDay;
		this.predictionDays = predictionDays;
		fillArrays(repair);
	}

	private static CovidStat make(long population, List<DataItem> items, int firstDay, int maxDBDay, boolean repair, Integer predictionDays) {
		// Integer predictionDays = predictionString.isEmpty() ? 0 : CovidTools.dateToDay(predictionString) - maxDBDay;
		Map<Integer, Double> cases = new HashMap<Integer, Double>();
		for (int i = 0; i < items.size(); ++i) {
			DataItem item = items.get(i);
			cases.put(item.getDay(), item.getNewCases());
		}
		return new CovidStat(population, cases, firstDay, maxDBDay, repair, predictionDays != null ? predictionDays - maxDBDay : 0);
		
	}

	public static CovidStat make(Country country, boolean repair, Integer predictionDays) {
		long population = country.getPopulation();
		List<DataItem> items = dataDao.getDataByCountry(country);
		int firstDay = items.get(0).getDay();
		int maxDBDay = dataDao.getMaxDay(country);
		return CovidStat.make(population, items, firstDay, maxDBDay, repair, predictionDays);
	}

	public static CovidStat make(CountryGroup countryGroup, boolean repair, Integer predictionDays) {
		long population = countryGroupDao.getGroupPopulation(countryGroup).get();
		List<DataItem> items = dataDao.getGroupData(countryGroup);
		int firstDay = items.get(0).getDay();
		int maxDBDay = dataDao.getGroupMaxDay(countryGroup);
		return CovidStat.make(population, items, firstDay, maxDBDay, repair, predictionDays);
	}

	public static CovidStat make(boolean repair, Integer predictionDays) {			// for the world
		long population = countryGroupDao.getWorldPopulation().get();
		List<DataItem> items = dataDao.getWorldData();
		int firstDay = items.get(0).getDay();
		int maxDBDay = dataDao.getWorldMaxDay();
		return CovidStat.make(population, items, firstDay, maxDBDay, repair, predictionDays);
	}

	private void fixNegative() {
		double s = 0;
		for (int k = firstDay; k <= maxDBDay; ++k) {
			double s1 = s + cases.getOrDefault(k, 0.0);
			if (cases.getOrDefault(k, 0.0) < 0.0) {
				double r = s / s1;
				cases.put(k, 0.0);
				for (int j = firstDay; j <= k; ++j) {
					cases.put(j, cases.getOrDefault(j, 0.0) / r);
				}
			}
			s = s1;
		}
//		for (int k = firstDay; k <= lastDay; ++k) {
//			if (Math.abs(casesOld.getOrDefault(k, 0.0) - cases.getOrDefault(k, 0.0)) > 0.0001) {
//				LOGGER.log(Level.INFO, String.format("fixNegative: k=%d %2f -> %2f",
//						k, casesOld.getOrDefault(k, 0.0), cases.getOrDefault(k, 0.0)));
//			}
//		}
	}

	private void repair() {
		final double drop = 3.0;
		double alevel = 0;
		for (int k = firstDay; k <= maxDBDay; ++k) {
			int k2 = k;
			double s = 0;
			if (cases.getOrDefault(k, 0.0) < alevel) {
				int k1 = k;
				while (cases.getOrDefault(k2, 0.0) <= alevel && k2 <= maxDBDay) {
					s += cases.getOrDefault(k2, 0.0);
					++k2;
				}
				if (k2 <= maxDBDay) {
					s += cases.getOrDefault(k2, 0.0);
					double newLevel = s / (k2 - k1 + 1);
					for (int kk = k1; kk <= k2; ++kk) {
						cases.put(kk, newLevel);
					}
					k = k2;
				}
			}
			alevel = cases.getOrDefault(k2, 0.0) / drop;
		}
	}

	private void fillTotalCases() {
		double prev = 0;
		for (int k = firstDay; k <= maxDBDay; ++k) {
			prev = prev + cases.getOrDefault(k, 0.0);
			totalCases.put(k, prev);
//			if (k > lastDay - 20) {
//				LOGGER.log(Level.INFO, String.format("fillTotalCases: k=%d: =%d", k, totalCases.get(k)));
//			}
		}
	}

	private void fillHiddenHolders() {
		for (int k = firstDay; k <= maxDBDay - 9; ++k) {
			if (totalCases.containsKey(k + 9) && totalCases.containsKey(k - 1)
					&& totalCases.getOrDefault(k + 9, 0.0) > 0) {
				hiddenHolders.put(k, totalCases.get(k + 9) - totalCases.get(k - 1));
//				if (k > lastDay - 20) {
//					LOGGER.log(Level.INFO, String.format("fillHiddenHolders: k=%d: %s", k, hiddenHolders.get(k)));
//				}
			}
		}
	}

	private void fillInfectionRate() {
		for (int k = firstDay; k <= maxDBDay - 8; ++k) {
			if (hiddenHolders.containsKey(k)) {
				double h = hiddenHolders.get(k);
				if (cases.containsKey(k + 10) && h > 0.0) {
					infectionRate.put(k, (cases.get(k + 10)) / h);
				}
			}
		}
	}

	private void fillTotalInfectionRate() {
		for (int k = firstDay; k <= maxDBDay - 8; ++k) {
			double G = 0;
			for (int i = 0; i <= 9; ++i) {
				if (infectionRate.containsKey(k - i)) {
					G += infectionRate.get(k - i);
				}
			}
			totalInfectionRate.put(k, G);
		}
	}

	private void fillInfectionRatePrediction() {
		for (int k = maxDBDay - 9; k <= maxDBDay + predictionDays; ++k) {
			double s = 0;
			for (int i = 1; i <= 10; ++i) {
				double a = 0;
				if (infectionRate.containsKey(k - i)) {
					a = infectionRate.get(k - i);
				}
				s += a;
			}
			infectionRate.put(k, s / 10);
		}
	}

	private void fillTotalInfectionRatePrediction() {
		for (int k = maxDBDay - 9; k <= maxDBDay + predictionDays; ++k) {
			double G = 0;
			for (int i = 0; i <= 9; ++i) {
				double g = 0;
				if (infectionRate.containsKey(k - i)) {
					g = infectionRate.get(k - i);
				}
				G += g;
			}
			totalInfectionRate.put(k, G);
		}
	}

	private void fillHiddenHoldersPrediction() {
		for (int k = maxDBDay - 8; k <= maxDBDay + predictionDays; ++k) {
			double s = 0;
			for (int i = 1; i <= 10; ++i) {
				double g = 0;
				double h = 0;
				if (infectionRate.containsKey(k - i)) {
					g = infectionRate.get(k - i);
				}
				if (hiddenHolders.containsKey(k - i)) {
					h = hiddenHolders.get(k - i);
				}
				s += (g * h);
			}
			hiddenHolders.put(k, s);
		}

	}

	private void fillTotalCasesPrediction() {
		for (int k = maxDBDay + 1; k <= maxDBDay + predictionDays; ++k) {
			if (totalCases.containsKey(k - 1) && totalCases.containsKey(k - 11) && infectionRate.containsKey(k - 10)) {
				double td = totalCases.get(k - 1);
				double g = infectionRate.get(k - 10);
				totalCases.put(k, td + g * (td - totalCases.get(k - 11)));
			}
		}
	}

	private void fillCasesPrediction() {
		for (int k = maxDBDay + 1; k <= maxDBDay + predictionDays; ++k) {
			if (totalCases.containsKey(k) && totalCases.containsKey(k - 1)) {
				cases.put(k, totalCases.get(k) - totalCases.get(k - 1));
			}
		}
	}

	private void fillDensityList() {
		for (int k = firstDay; k <= maxDBDay + predictionDays; ++k) {
			if (hiddenHolders.containsKey(k)) {
				double hh = hiddenHolders.get(k);
				double density = 1000.0 * hh / population;
				densityList.put(k, density);
			}
		}
	}

	private void fillArrays(boolean repair) {
		fixNegative();
		if (repair) {
			repair();
		}
		fillTotalCases();
		fillHiddenHolders();
		fillInfectionRate();
		fillTotalInfectionRate();
		fillInfectionRatePrediction();
		fillTotalInfectionRatePrediction();
		fillHiddenHoldersPrediction();
		fillTotalCasesPrediction();
		fillCasesPrediction();
		fillDensityList();
	}

	public Map<Integer, Double> getCases() {
		return cases;
	}

	public Map<Integer, Double> getTotalCases() {
		return totalCases;
	}

	public Map<Integer, Double> getHiddenHolders() {
		return hiddenHolders;
	}

	public Map<Integer, Double> getInfectionRate() {
		return infectionRate;
	}

	public Map<Integer, Double> getTotalInfectionRate() {
		return totalInfectionRate;
	}

	public Map<Integer, Double> getDensityList() {
		return densityList;
	}

	public static void clearDensities() {
		densityMap.clear();
	}

	private double getDensity(Country country) {
		if (!getHiddenHolders().isEmpty()) {
			long population = country.getPopulation();
			double hlast = getHiddenHolders().get(maxDBDay - 9);
			return 1000.0 * hlast / population;
		} else {
			LOGGER.log(Level.INFO, String.format("[!!!] getDensity: no data for %s(%d)", country.getName(), country.getId()));
			return 0;
		}
	}

	// calculate the density for the country
	private static double getCountryDensity(Country country) {
		int firstDay = dataDao.getFirstDay(country);
		int maxDay = dataDao.getMaxDay(country);

		List<DataItem> items = dataDao.getDataByCountry(country);
		Map<Integer, Double> cases = new HashMap<Integer, Double>();
		for (int i = 0; i < items.size(); ++i) {
			DataItem item = items.get(i);
			cases.put(item.getDay(), item.getNewCases());
		}

		CovidStat covidStat = new CovidStat(country.getPopulation(), cases, firstDay, maxDay, false, 0);
		return covidStat.getDensity(country);
	}

	public static void refreshDensities() {
		densityMap.clear();
		fillDensities();
	}

	private static void fillDensities() {
		assert(densityMap.isEmpty());
		for (Country country : countryList) {
			if (countryDao.hasData(country)) {
				densityMap.put(country, getCountryDensity(country));
			}
		}
	}

	public static Map<Country, Double> getDensities() {
		if (densityMap.isEmpty()) {
			fillDensities();
		}

		List<Entry<Country, Double>> list = new ArrayList<Entry<Country, Double>>(densityMap.entrySet());
		list.sort(Entry.comparingByValue(new Comparator<Double>() {

			@Override
			public int compare(Double o1, Double o2) {
				return o1 > o2 ? -1 : o1 < o2 ? 1 : 0;
			}
		}));

		Map<Country, Double> result = new LinkedHashMap<>();
		for (Entry<Country, Double> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}

	public int getFirstDay() {
		return firstDay;
	}

	public int getLastDay() {
		return firstDay + maxDBDay;
	}

	public int getMaxDBDay() {
		return maxDBDay;
	}

	public long getPopulation() {
		return population;
	}

}
