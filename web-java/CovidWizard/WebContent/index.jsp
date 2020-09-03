<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<!-- ANALYTICS -->
<c:set var="serverIP" value="<%=request.getLocalAddr()%>" />
<c:if test="${serverIP.equals('139.162.157.71')}">
	<!-- Global site tag (gtag.js) - Google Analytics -->
	<script async
		src="https://www.googletagmanager.com/gtag/js?id=UA-5996825-29"></script>
	<script>
		window.dataLayer = window.dataLayer || [];
		function gtag() {
			dataLayer.push(arguments);
		}
		gtag('js', new Date());

		gtag('config', 'UA-5996825-29');
	</script>
</c:if>
<!-- FAVICON -->
<link rel="apple-touch-icon" sizes="57x57" href="/images/logo/apple-icon-57x57.png">
<link rel="apple-touch-icon" sizes="60x60" href="/images/logo/apple-icon-60x60.png">
<link rel="apple-touch-icon" sizes="72x72" href="/images/logo/apple-icon-72x72.png">
<link rel="apple-touch-icon" sizes="76x76" href="/images/logo/apple-icon-76x76.png">
<link rel="apple-touch-icon" sizes="114x114" href="/images/logo/apple-icon-114x114.png">
<link rel="apple-touch-icon" sizes="120x120" href="/images/logo/apple-icon-120x120.png">
<link rel="apple-touch-icon" sizes="144x144" href="/images/logo/apple-icon-144x144.png">
<link rel="apple-touch-icon" sizes="152x152" href="/images/logo/apple-icon-152x152.png">
<link rel="apple-touch-icon" sizes="180x180" href="/images/logo/apple-icon-180x180.png">
<link rel="icon" type="image/png" sizes="192x192" href="/images/logo/android-icon-192x192.png">
<link rel="icon" type="image/png" sizes="32x32" href="/images/logo/favicon-32x32.png">
<link rel="icon" type="image/png" sizes="96x96" href="/images/logo/favicon-96x96.png">
<link rel="icon" type="image/png" sizes="16x16" href="/images/logo/favicon-16x16.png">

<meta name="msapplication-TileColor" content="#ffffff">
<meta name="msapplication-TileImage" content="/images/logo/ms-icon-144x144.png">
<meta name="theme-color" content="#ffffff">

<script type="text/javascript" src="/js/jquery-3.5.1.min.js"></script>
<script type="text/javascript" src="/js/plotly-latest.min.js"></script>
<script type="text/javascript" src="/js/jquery.modal.min.js"></script>
<script type="text/javascript" src="/js/countries.js"></script>
<script type="text/javascript" src="/js/dynamics.js"></script>
<link rel="stylesheet" href="/css/jquery.modal.min.css" />
<link rel="stylesheet" href="/css/styles.css">
<meta charset="UTF-8">
<title>Covid Wizard</title>
<%@ page import="java.net.*"%>
</head>
<body>
	<div class="content">
		<div id="table_header">
			<div  id="logo70" >
				<img src="images/logo/logo70.png"/>
			</div>
			<div id="faq" class="modal"><%=(String)request.getAttribute("FAQ")%></div>
			<div id="login"><a href="/" id="login_a">Login</a></div>
			<div id="faq01"><a href="#faq" rel="modal:open" id="open_faq">FAQ (version 2)</a></div>
			<h1>COVID WIZARD</h1>
			<div id="table_bar">
				<select id="country" onchange="onCountrySelect()">
					<c:forEach items="${countries}" var="country">
						<option value="${country.id}">${country.name}</option>
					</c:forEach>
    				<option disabled>----------</option>
					<option value="all" selected="selected">All</option>
					<c:forEach items="${countryGroups}" var="countryGroup">
						<option value="gr${countryGroup.id}">${countryGroup.name}</option>
					</c:forEach>
				</select>
				<div id="table_dates">
					<label>Dates: From</label>
					<input type="date" id="date_from" name="date_from" autocomplete="off" onchange="onDateChange()" required />
					<label id="date_from_label">Up to</label>
					<label id=last_day></label>
<!-- 					<input type="date" id="date_to" name="date_to" autocomplete="off" onchange="onDateChange()" required /> -->
				</div>
			</div>
			<div id="population">Population: <div id="population_field"></div></div>
		</div>
		<div id="plot" style="width: 100%; height: 650px;"></div>
		<c:if test="${serverIP.equals('139.162.157.71')}">
			<script>
				getUserCountry(selectCountry);
			</script>
		</c:if>
		<c:if test="${serverIP.equals('127.0.0.1')}">
			<script>
				selectCountry('Belgium');
			</script>
		</c:if>

		<div id="table_popup" class="modal">
			<div id="data_table"></div>
		</div>

		<div id="density_popup" class="modal">
			<select id="country_group" onchange="onCountryGroupSelect()">
				<option value="all" selected="selected">All</option>
				<c:forEach items="${countryGroups}" var="countryGroup">
					<option value="${countryGroup.id}">${countryGroup.name}</option>
				</c:forEach>
			</select>
			<script> $('#country_group').val('all'); </script>
			<div id="density_table"></div>
		</div>

		<div id="buttons">
			<input type="hidden" id="repair" name="repair" value="restore"/>
			<button id="repair_button" type="button" onclick="repairClick()">Repair</button>
			<div id="cases">Safe / Last # Cases: <div id="cases_field"></div> / <div id="new_cases_field"></div></div>
			<div id="density"><a href="#density_popup" rel="modal:open">Transmission Risk</a>: <div id="density_field"></div>&nbsp;(<div id="density_table_field"></div>/<div id="density_total_size"></div>)&nbsp;<div id="arrow"></div></div>
		</div>
		<div id="footer">
			<div id="ft_addr">
				&copy; <a href="https://scholar.google.com/citations?user=DJ8Ep8YAAAAJ&hl=en&oi=ao">Yurii.Nesterov@uclouvain.be</a>
			</div>
			<div id="ft_bkg">
				<a href="https://arxiv.org/abs/2007.11429">Scientific Model (HIT)</a>
			</div>
			<div id="open_table">
				<a href="#table_popup" rel="modal:open">Data (from github.com)</a>
			</div>
		</div>
	</div>
</body>
</html>
