const hidden_virus_holders_index = 0;
const total_infection_rate_index = 1;
const epidemic_threshold_index = 2;
const total_infection_rate_1 = 3;
const hidden_virus_holders_1 = 4;
const info = 5;
var pdata;
var playout;
var lastDate;

function getRepair() {
	var repair = $('#repair').val();
	if (typeof repair !== 'undefined') {
		return repair;
	}
	return 'restore';
}

function onCountrySelect() {
	$('#repair').val('restore');
	$('#repair_button').html('Repair');
	updateGraphs();
}

function round_num(number, digits) {
	return number.toLocaleString(undefined, { maximumFractionDigits: digits, minimumFractionDigits: digits });
}
		
		
function updateGraphs() {
	resetDate();
	var country = document.getElementById("country").value;
	var repair = getRepair();
	$.getJSON(`/dynamics.json?country=${country}&repair=${repair}`, function(data) {
		var data_line = {
			shape: "spline",
			smoothing: 1.3,
			width: 3
		};
//		alert(data[info].population)
		$('#population_field').html(round_num(data[info].population, 0));
		$('#cases_field').html(round_num(data[info].hlast / 10, 0));
		$('#concentration_field').html(round_num(1000 * data[info].hlast / data[info].population, 3));
		pdata = data;
		lastDate = new Date(pdata[hidden_virus_holders_1].x[pdata[hidden_virus_holders_1].x.length - 1]);
		data[hidden_virus_holders_index].line = Object.assign({}, data_line);
		data[total_infection_rate_index].line = Object.assign({}, data_line);
		data[epidemic_threshold_index].line = Object.assign({}, data_line);
		data[total_infection_rate_1].line = Object.assign({}, data_line);
		data[hidden_virus_holders_1].line = Object.assign({}, data_line);

		data[hidden_virus_holders_index].line.color = 'blue';
		data[total_infection_rate_index].line.color = 'red';
		data[epidemic_threshold_index].line.color = 'black';
		data[total_infection_rate_1].line.color = 'green';
		data[hidden_virus_holders_1].line.color = 'orange';

		data[hidden_virus_holders_index].type = 'scatter';
		data[total_infection_rate_index].type = 'scatter';
		data[epidemic_threshold_index].type = 'scatter';
		data[total_infection_rate_1].type = 'scatter';
		data[hidden_virus_holders_1].type = 'scatter';

		data[hidden_virus_holders_index].mode = 'lines+markers';
		data[total_infection_rate_index].mode = 'lines+markers';
		data[total_infection_rate_1].mode = 'lines+markers';
		data[hidden_virus_holders_1].mode = 'lines+markers';

		data[hidden_virus_holders_index].name = 'Hidden Virus Holders (HVH)';
		data[total_infection_rate_index].name = 'Total Infection Rate (TIR)';
		data[epidemic_threshold_index].name = 'Epidemic Threshold TIR = 1';
		data[total_infection_rate_1].name = 'Prediction for TIR';
		data[hidden_virus_holders_1].name = 'Prediction for HVH';

		data[total_infection_rate_index].yaxis = 'y2';
		data[epidemic_threshold_index].yaxis = 'y3';
		data[total_infection_rate_1].yaxis = 'y4';
		data[hidden_virus_holders_1].yaxis = 'y5';

		data[epidemic_threshold_index].hoverinfo = 'skip';
		plot = document.getElementById('plot');
		color1 = 'green';
		color2 = 'black';
		var x_range = [$('#date_from').val(), getDateString(lastDate)];
		var ys = maxValues(data, x_range[0], x_range[1]);
		var y_range = [0, ys[0] * 1.05];
		var y2_range = [0, ys[1] * 1.05];
		var layout = {
			margin: { t: 0 },
			legend: {
				x: 0.80,
				y: 1.25,
				yanchor: 'top',
				traceorder: 'normal',
				font: {
					family: 'sans-serif',
					size: 12,
					color: '#000'
				},
				bgcolor: '#E2E2E2',
				bordercolor: '#FFFFFF',
				borderwidth: 2,
				//orientation: 'h'
			},
			xaxis: {
				range: x_range,
				fixedrange: false,
				dtick: 86400000.0 * 7,	//once a week
				tick0: '2019-12-30'     // a monday
			},
			yaxis: {
				//showgrid: false,
				range: y_range,
				fixedrange: true,
				titlefont: { color: color1 },
				tickfont: { color: color1 },
				rangemode: 'nonnegative',
				gridcolor: color1,
				side: 'left'
			},
			yaxis2: {
				//showgrid: false,
				range: y2_range,
				fixedrange: true,
				titlefont: { color: color2 },
				tickfont: { color: color2 },
				dtick: 0.5,
				rangemode: 'nonnegative',
				gridcolor: color2,
				overlaying: 'y',
				side: 'right',
			},
			yaxis3: {
				showgrid: false,
				range: y2_range,
				fixedrange: true,
				rangemode: 'nonnegative',
				overlaying: 'y',
				showticklabels: false
			},
			yaxis4: {
				showgrid: false,
				range: y2_range,
				fixedrange: true,
				rangemode: 'nonnegative',
				overlaying: 'y',
				showticklabels: false
			},
			yaxis5: {
				showgrid: false,
				range: y_range,
				fixedrange: true,
				rangemode: 'nonnegative',
				overlaying: 'y',
				showticklabels: false
			}
		}
		$('#last_day').html(lastDate.toLocaleDateString());
		playout = layout;
		Plotly.newPlot(plot, data, layout);
		//drawLine1();
		plot.on('plotly_relayout',
			function(eventdata) {
				if (eventdata['xaxis.autorange']) {
					var ys = maxValues(data, pdata[hidden_virus_holders_index].x[0],
						pdata[hidden_virus_holders_index].x[pdata[hidden_virus_holders_index].x.length - 1]);
					var y_range = [0, ys[0] * 1.05];
					var y2_range = [0, ys[1] * 1.05];
					var update = {
						'yaxis.range': y_range,
						'yaxis2.range': y2_range,
						'yaxis3.range': y2_range,
						'yaxis4.range': y2_range,
						'yaxis5.range': y_range,
						'xaxis.dtick': 86400000.0 * 7
					};
					Plotly.relayout(plot, update)
					var date_from = new Date(pdata[hidden_virus_holders_index].x[0]);
					var date_to = new Date(pdata[hidden_virus_holders_index].x[pdata[hidden_virus_holders_index].x.length - 1]);
					setDate('#date_from', date_from);
					setDate('#date_to', date_to);
				}
				if (typeof eventdata['xaxis.range[0]'] !== 'undefined' && typeof eventdata['xaxis.range[1]'] !== 'undefined') {
					var ys = maxValues(plot.data, eventdata['xaxis.range[0]'], eventdata['xaxis.range[1]']);
					var y_range = [0, ys[0] * 1.05];
					var y2_range = [0, ys[1] * 1.05];
					var date_from = new Date(eventdata['xaxis.range[0]']);
					var date_to = new Date(eventdata['xaxis.range[1]']);
					var difftime = date_to.getTime() - date_from.getTime();
					var diffdays = difftime / (1000 * 3600 * 24);
					var need_dtick_update = false;
					var dtick = plot.layout.xaxis.dtick;
					if (dtick > 86400000.0 * 3 && diffdays < 42) {
						dtick = 86400000.0;
						need_dtick_update = true;
					} else if (dtick < 86400000.0 * 3 && diffdays > 42) {
						dtick = 86400000.0 * 7;
						need_dtick_update = true;
					}
					if (!(typeof plot.layout.yaxis.range !== 'undefined' && typeof plot.layout.yaxis2.range !== 'undefined')) {
						need_range_update = true;
					} else if (typeof plot.layout.yaxis.range !== 'undefined'
						&& (plot.layout.yaxis[0] != y_range[0] || plot.layout.yaxis[1] != y_range[1])) {
						need_range_update = true;
					}
					var update = {};
					if (need_dtick_update) {
						update['xaxis.dtick'] = dtick;
					}
					if (need_range_update) {
						update['yaxis.range'] = y_range;
						update['yaxis2.range'] = y2_range;
						update['yaxis3.range'] = y2_range;
						update['yaxis4.range'] = y2_range;
						update['yaxis5.range'] = y_range;
					}
					if (need_dtick_update || need_range_update) {
						Plotly.relayout(plot, update)
					}
					$('#date_from').val(getDateString(date_from));
					//					setDate('#date_from', date_from);
					//					setDate('#date_to', date_to);
				}
			});
	});
}

function getMaxY(data, n) {
	var y_max = 0;
	var i = 0;
	var i_to = data[n].x.length;
	while (i <= i_to) {
		var y = data[n].y[i];
		if (y > y_max) {
			y_max = y;
		}
		i++;
	}
	return y_max;
}

function getMaxYDates(data, n, x_from, x_to) {
	var y_max = 0;
	var i = Plotly.d3.bisectLeft(data[n].x, x_from);
	var i_to = Plotly.d3.bisectLeft(data[n].x, x_to);
	while (i <= i_to) {
		var y = data[n].y[i];
		if (y > y_max) {
			y_max = y;
		}
		i++;
	}
	return y_max;

}

function maxValues(data, x_from, x_to) {
	var yhh_max = getMaxYDates(data, hidden_virus_holders_index, x_from, x_to);
	var max_hvh_1 = getMaxY(data, hidden_virus_holders_1);
	if (max_hvh_1 > yhh_max) {
		yhh_max = max_hvh_1;
	}

	var ytir_max = getMaxYDates(data, total_infection_rate_index, x_from, x_to);
	var max_tir_1 = getMaxY(data, total_infection_rate_1);
	if (max_tir_1 > ytir_max) {
		ytir_max = max_tir_1;
	}
	return [yhh_max, ytir_max];
}

function onDateChange() {
	var xaxis = {};
	var date_from = $('#date_from').val();
	//	var date_to = $('#date_to').val();
	xaxis.range = [date_from, getDateString(lastDate)];
	playout.xaxis = xaxis;
	plot = document.getElementById('plot');
	Plotly.newPlot(plot, pdata, playout);
	//drawLine1();
}

function drawLine1() {
	var line_label = '1';
	var line_index = -1;
	Plotly.d3.selectAll('.y2tick').filter(function(_d, i) {
		if (this.textContent == line_label) {
			line_index = i;
			return i;
		}
	})

	var line = Plotly.d3.selectAll('.y2grid').filter('.crisp').filter(function(_d, i) { return i == line_index - 1 });
	line.style("stroke", color2);
	line.style("stroke-width", "3px");
}

function getDateString(date) {
	var day = ("0" + date.getDate()).slice(-2);
	var month = ("0" + (date.getMonth() + 1)).slice(-2);
	var date_str = date.getFullYear() + "-" + (month) + "-" + (day);
	return date_str;
}

function resetDate() {
	var date_from = new Date();
	date_from.setDate(date_from.getDate() - 140);
	var date_to = new Date();
	date_to.setDate(date_to.getDate() - 11);
	$('#date_from').val(getDateString(date_from));
}

$(document).ready(function() {

	resetDate();

	$('#open_table').click(function(event) {
		event.preventDefault();

		var country = document.getElementById("country").value;
		var repair = $('#repair').val();

		$.get(`/newcases?country=${country}&repair=${repair}`, function(html) {
			$('#data_table').html(html);
		});
	});

});

function selectCountry(country) {
	var dd = document.getElementById('country');
	for (var i = 0; i < dd.options.length; i++) {
		if (dd.options[i].text === country) {
			dd.selectedIndex = i;
			updateGraphs();
			break;
		}
	}
}

function repairClick() {
	if ($('#repair').val() === 'repair') {
		$('#repair').val('restore');
		$('#repair_button').html('Repair');
	} else {
		$('#repair').val('repair');
		$('#repair_button').html('Resore');
	}
	updateGraphs();
}
