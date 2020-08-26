package com.covidwizard.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.covidwizard.dao.DataDao;
import com.covidwizard.model.Country;
import com.covidwizard.model.DataItem;

public class CovidStat {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(Country.class.getName());

	private static List<Country> countryList = new LinkedList<Country>();
	private static Map<Country, Double> densityMap = new HashMap<Country, Double>();
	private static DataDao dataDao = new DataDao();
	private static CountryDao countryDao = new CountryDao();

	static {
		Collection<Country> countries = countryDao.getAll();
		for (Country country : countries) {
			countryList.add(country);
		}
	}

	private Map<Integer, Integer> cases = null;
	private Map<Integer, Integer> totalCases = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> hiddenHolders = new HashMap<Integer, Integer>();
	private Map<Integer, Double> infectionRate = new HashMap<Integer, Double>();
	private Map<Integer, Double> totalInfectionRate = new HashMap<Integer, Double>();
	private Map<Integer, Integer> sum = new HashMap<Integer, Integer>();
	private Map<Integer, Double> infectionRate1 = new HashMap<Integer, Double>();
	private Map<Integer, Double> totalInfectionRate1 = new HashMap<Integer, Double>();
	private Map<Integer, Double> hiddenHolders1 = new HashMap<Integer, Double>();

	private int firstDay;
	private int lastDay;

	public CovidStat(Map<Integer, Integer> cases, int firstDay, int lastDay, boolean repair) {
		this.cases = cases;
		this.firstDay = firstDay;
		this.lastDay = lastDay;
		if (repair) {
			repair();
		}
		fillArrays();
	}

	private void repair() {
		final int drop = 3;
		int alevel = 0;
		for (int k = firstDay; k <= lastDay; ++k) {
			int k2 = k;
			int s = 0;
			if (cases.getOrDefault(k, 0) < alevel) {
				int k1 = k;
				while (cases.getOrDefault(k2, 0) <= alevel && k2 <= lastDay) {
					s += cases.getOrDefault(k2, 0);
					++k2;
				}
				if (k2 <= lastDay) {
					s += cases.getOrDefault(k2, 0);
					int newLevel = s / (k2 - k1 + 1);
					for (int kk = k1; kk <= k2 - 1; ++kk) {
						cases.put(kk, newLevel);
					}
					cases.put(k2, s - (k2 - k1) * newLevel);
					k = k2;
				}
			}
			alevel = cases.getOrDefault(k2, 0) / drop;
		}
	}

	private void fillTotalCases() {
		int prev = 0;
		for (int k = firstDay; k <= lastDay; ++k) {
			if (cases.containsKey(k)) {
				prev = prev + cases.get(k);
				totalCases.put(k, prev);
			} else {
				totalCases.put(k, 0);
			}
		}
	}

	private void fillHiddenHolders() {
		for (int k = firstDay; k <= lastDay; ++k) {
			if (totalCases.containsKey(k + 9) && totalCases.containsKey(k - 1)
					&& totalCases.get(k + 9) > totalCases.get(k - 1)) {
				hiddenHolders.put(k, totalCases.get(k + 9) - totalCases.get(k - 1));
//				LOGGER.log(Level.INFO, String.format("fillHiddenHolders: k=%d: =%d", k, hiddenHolders.get(k)));
			}
		}
	}

	private void fillInfectionRate() {
		for (int k = firstDay; k <= lastDay; ++k) {
			if (hiddenHolders.containsKey(k)) {
				int h = hiddenHolders.get(k);
				if (cases.containsKey(k + 10) && h != 0) {
					infectionRate.put(k, ((double) cases.get(k + 10)) / ((double) h));
//					LOGGER.log(Level.INFO, String.format("k=%d, ir=%f", k, infectionRate.get(k)));
				}
			}
		}
	}

	private void fillTotalInfectionRate() {
		for (int k = firstDay; k <= lastDay; ++k) {
			double G = 0;
			for (int i = 0; i <= 9; ++i) {
				if (infectionRate.containsKey(k - i)) {
					G += infectionRate.get(k - i);
				}
			}
			totalInfectionRate.put(k, G);
		}
	}

	private void fillSum() {
		int sum = 0;
		for (int k = firstDay; k <= lastDay; ++k) {
			sum += cases.getOrDefault(k, 0);
			this.sum.put(k, sum);
		}
	}

	private void fillInfectionRate1() {
		for (int k = lastDay - 11; k <= lastDay; ++k) {
			double s = 0;
			for (int i = 1; i <= 10; ++i) {
				double a = 0;
				if (infectionRate.containsKey(k - i)) {
					a = infectionRate.get(k - i);
				} else if (infectionRate1.containsKey(k - i)) {
					a = infectionRate1.get(k - i);
				}
//				LOGGER.log(Level.INFO, String.format("k=%d, i=%d, k-i=%d, a=%f", k, i, k-i, a));
				s += a;
			}
			infectionRate1.put(k, s / 10);
//			LOGGER.log(Level.INFO, String.format("k=%d, s/10=%f", k, s/10));
		}
	}

	private void fillTotalInfectionRate1() {
		for (int k = lastDay - 11; k <= lastDay; ++k) {
			double G = 0;
			for (int i = 0; i <= 9; ++i) {
				double g = 0;
				if (infectionRate.containsKey(k - i)) {
					g = infectionRate.get(k - i);
				} else if (infectionRate1.containsKey(k - i)) {
					g = infectionRate1.get(k - i);
				}
				G += g;
//				LOGGER.log(Level.INFO, String.format("k=%d, g=%f", k, g));
			}
			totalInfectionRate1.put(k, G);
//			LOGGER.log(Level.INFO, String.format("k=%d, G=%f", k, G));
		}
	}

	private void fillHiddenHolders1() {
		for (int k = lastDay - 10; k <= lastDay; ++k) {
			double s = 0;
			for (int i = 1; i <= 10; ++i) {
				double g = 0;
				double h = 0;
				if (infectionRate.containsKey(k - i)) {
					g = infectionRate.get(k - i);
				} else if (infectionRate1.containsKey(k - i)) {
					g = infectionRate1.get(k - i);
				}
				if (hiddenHolders.containsKey(k - i)) {
					h = hiddenHolders.get(k - i);
				} else if (hiddenHolders1.containsKey(k - i)) {
					h = hiddenHolders1.get(k - i);
				}
				s += (g * h);
			}
			hiddenHolders1.put(k, s);
		}

	}

	private void fillArrays() {
		fillTotalCases();
		fillHiddenHolders();
		fillInfectionRate();
		fillTotalInfectionRate();
		fillSum();
		fillInfectionRate1();
		fillTotalInfectionRate1();
		fillHiddenHolders1();
	}

	public Map<Integer, Integer> getCases() {
		return cases;
	}

	public Map<Integer, Integer> getTotalCases() {
		return totalCases;
	}

	public Map<Integer, Integer> getHiddenHolders() {
		return hiddenHolders;
	}

	public Map<Integer, Double> getInfectionRate() {
		return infectionRate;
	}

	public Map<Integer, Double> getTotalInfectionRate() {
		return totalInfectionRate;
	}

	public Map<Integer, Integer> getSum() {
		return sum;
	}

	public Map<Integer, Double> getInfectionRate1() {
		return infectionRate1;
	}

	public Map<Integer, Double> getTotalInfectionRate1() {
		return totalInfectionRate1;
	}

	public Map<Integer, Double> getHiddenHolders1() {
		return hiddenHolders1;
	}

	public static void clearDensities() {
		densityMap.clear();
	}

	private double getDensity(Country country) {
		long population = country.getPopulation();
		int maxDay = Collections.max(getHiddenHolders().keySet());
//		LOGGER.log(Level.INFO, String.format("getDensity: maxDay=%d", maxDay));
		double hlast = getHiddenHolders().get(maxDay);
		return 1000.0 * hlast / population;
	}

	// calculate the density for the country
	private static double getCountryDensity(Country country) {
		int firstDay = dataDao.getFirstDay(country);
		int lastDay = dataDao.getLastDay(country);
//		LOGGER.log(Level.INFO, String.format("getCountryDensity: country=%s(%d), firstDay=%d, lastDay=%d", country.getName(), country.getId(), firstDay, lastDay));

		List<DataItem> items = dataDao.getDataByCountry(country);
		Map<Integer, Integer> cases = new HashMap<Integer, Integer>();
		for (int i = 0; i < items.size(); ++i) {
			DataItem item = items.get(i);
			cases.put(item.getDay(), item.getNewCases());
		}

		CovidStat covidStat = new CovidStat(cases, firstDay, lastDay, false);
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

		List<Entry<Country, Double>> list = new ArrayList<>(densityMap.entrySet());
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

}
