package com.ubcsolar.sim;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import jdk.nashorn.internal.objects.Global;
import org.jfree.data.Values;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.github.dvdme.ForecastIOLib.FIOCurrently;
import com.github.dvdme.ForecastIOLib.FIODataBlock;
import com.github.dvdme.ForecastIOLib.FIODataPoint;
import com.github.dvdme.ForecastIOLib.ForecastIO;
import com.ubcsolar.Main.GlobalValues;
import com.ubcsolar.common.ForecastReport;
import com.ubcsolar.common.GeoCoord;
import com.ubcsolar.common.LocationReport;
import com.ubcsolar.common.LogType;
import com.ubcsolar.common.Route;
import com.ubcsolar.common.SimFrame;
import com.ubcsolar.common.SolarLog;
import com.ubcsolar.common.TelemDataPacket;
import com.ubcsolar.exception.NotEnoughChargeException;

public class SimEngine {
	private final double RECHARGE_TIME_MS = 3 * 1000;
	private final int EFF_SOLAR_CONSTANT = 990;
	protected int totalCharge;

	public SimEngine() {
		inUseCarModel = new DefaultCarModel();
	}

	private CarModel inUseCarModel;

	/**
	 * Runs a simulation of the car's performance on the given portion of the provided route,
	 * assuming that the car follows the speed profile provided. The method will return a
	 * SimResult object (containing the time taken, the final TelemDataPacket, and a list of the
	 * SimFrames used to do the simulation)
	 * LIMITATION: Currently can only do one lap at a time. To do multiple laps, call this method
	 * multiple times from the SimController (generally, all sims should be done in chunks anyway)
	 *
	 * @param toTraverse:    The complete route that the simulation is run on
	 * @param startLoc:      The starting location for the route chunk to be simulated. startLoc must
	 *                       be part of toTraverse
	 * @param endLoc:        The ending location for the route chunk to be simulated. endLoc must be part
	 *                       of toTraverse
	 * @param carStartState: the car's telemetry data at the start of the simulated route chunk
	 * @param startTime:     The time at which the race will begin (in Unix format, i.e. s from 1/1/70)
	 * @param lapNum:        The lap that the simulation is simulating
	 * @param minCharge:     The minimum percentage of charge that is acceptable at the end of this segment
	 *                       of the race
	 * @return a SimResult object, containing the simulated travel time, the final TelemDataPacket,
	 * and a list of the SimFrames used to do the simulation
	 * @throws NotEnoughChargeException if the end charge is less than minCharge
	 */
	public SimResult runSimV2(Route toTraverse, GeoCoord startLoc, GeoCoord endLoc,
							  TelemDataPacket carStartState,
							  double startSpeed, double endSpeed, long startTime, int lapNum, double minCharge,
							  ForecastIO inflectionPoint) {

		int startingIndex = toTraverse.getIndexOfClosestPoint(startLoc);
		int endingIndex = toTraverse.getIndexOfClosestPoint(endLoc);

		GeoCoord currPoint = toTraverse.getClosestPointOnRoute(startLoc);

		if (endingIndex < startingIndex) {
			throw new IllegalArgumentException("ending location must be after starting location");
		}

		FIODataPoint startWeatherPoint = chooseReport(inflectionPoint, startTime);
		LocationReport startLocationReport = new LocationReport(currPoint, "Raven", "Simmed");
		totalCharge = carStartState.getTotalVoltage();
		SimFrame startSimFrame = new SimFrame(startWeatherPoint, carStartState, 0, startLocationReport, startTime, lapNum);

		List<SimFrame> listOfFrames = new ArrayList<SimFrame>();
		listOfFrames.add(startSimFrame);
		GeoCoord prevPoint = currPoint;

		long currTime = startTime;
		long prevTime = startTime;

		TelemDataPacket prevStatus = carStartState;
		TelemDataPacket currStatus = prevStatus;

		Map<GeoCoord, Double> speedProfile = new LinkedHashMap<GeoCoord, Double>();
		double currSpeed = startSpeed;
		speedProfile.put(toTraverse.getTrailMarkers().get(startingIndex), currSpeed);

		JsonObject dailyData = (JsonObject) ((JsonArray) inflectionPoint.getDaily().get("data")).get(0);
		long sunriseTime = Long.parseLong(dailyData.get("sunriseTime").toString())*1000;
		long sunsetTime = Long.parseLong(dailyData.get("sunsetTime").toString())*1000;

		double currCharge = carStartState.getStateOfCharge();

		boolean runSuccessful = true;
		int isAccelerating = 0; //0 = not accelerating, 1 = increasing speed, -1 = decreasing speed

		for (int i = startingIndex + 1; i <= endingIndex; i++) {
			currPoint = toTraverse.getTrailMarkers().get(i);

			speedProfile.put(currPoint, currSpeed);

			double distance = prevPoint.calculateDistance(currPoint);
			double timeIncHr = distance / currSpeed;
			double timeIncMillis = timeIncHr * 3600000;
			currTime += timeIncMillis;

			isAccelerating = 0;
			if (currSpeed != endSpeed) {
				double speedDiff = GlobalValues.MAX_ACCELERATION_KMH2 * timeIncHr;
				if (currSpeed < endSpeed) {
					currSpeed += speedDiff;
					isAccelerating = 1;
					if(currSpeed > endSpeed)
						currSpeed = endSpeed;
				} else if (currSpeed > endSpeed) {
					currSpeed -= speedDiff;
					isAccelerating = -1;
					if(currSpeed < endSpeed)
						currSpeed = endSpeed;
				}
			}

			FIODataPoint currWeatherPoint = chooseReport(inflectionPoint, currTime);

			double latitude = currPoint.getLat();
			double chargeDiff = calculateChargeDiff(prevPoint, currPoint,
					currWeatherPoint, currSpeed, timeIncHr, sunriseTime, sunsetTime, latitude, currTime, isAccelerating);
			currCharge += chargeDiff;
			if (currCharge > 100)
				currCharge = 100;
			if (currCharge < 0)
				currCharge = 0;
			if (currCharge <= minCharge) {
				runSuccessful = false;
				break;
			}
			currStatus = new TelemDataPacket(currSpeed, carStartState.getTotalVoltage(), carStartState.getTemperatures(),
					carStartState.getCellVoltages(), currCharge);

			LocationReport currLocReport = new LocationReport(currPoint, "Raven", "Simmed");

			SimFrame currSimFrame = new SimFrame(currWeatherPoint, currStatus, (currTime-prevTime), currLocReport, currTime, lapNum);
			listOfFrames.add(currSimFrame);
			prevTime = currTime;
			prevPoint = currPoint;
		}

		SimResult result = new SimResult(listOfFrames, currTime - startTime, currStatus, speedProfile, runSuccessful);

		return result;
	}

	public double getInclinationAngle(GeoCoord startPoint, GeoCoord endPoint) {
		double inclinationAngle = 0;
		double distance = startPoint.calculateDistance(endPoint) * 1000;
		double heightDifference = endPoint.getElevation() - startPoint.getElevation();
		/*
		 * If heightDifference returns a positive number, this means that we are
		 * elevating from a lower starting point. Thus, the inclinationAngle
		 * should be positive.
		 */

		inclinationAngle = Math.atan(heightDifference / distance);

		return inclinationAngle;
	}

	public double calculateChargeDiff(GeoCoord startLoc, GeoCoord endLoc, FIODataPoint forecastForPoint,
									  double speed, double timeTaken, long sunriseTime, long sunsetTime, double latitude,
									  long currTime, int isAccelerating) {
		double resistivePower = calculateResistivePower(forecastForPoint, endLoc, startLoc, speed);
		double accelerationPower = calculateAccelerationPower(speed, isAccelerating);
		double sunPower = calculateSunPower(forecastForPoint, sunriseTime, sunsetTime, latitude, currTime);

		double netAccelerationPower = accelerationPower + resistivePower;
		if (netAccelerationPower < 0) netAccelerationPower = 0;
		double netPower = sunPower - netAccelerationPower;//in Watts

		double changeInCharge = netPower / totalCharge * timeTaken;//in amp-hours
		double changeInChargePerCent = changeInCharge / GlobalValues.BATTERY_MAX_CHARGE;
		return changeInChargePerCent * 100;
	}

	public double getGradientResistanceForce(double angle) {
		// F = mgsin(theta)
		double force = GlobalValues.CAR_MASS * 9.8 * Math.sin(angle);
		// force is positive if it opposes the direction of travel
		return force;
	}

	public double getRollingResistanceForce(double angle, double tirePressure, double velocity) {
		double rollingCoefficient = 0.005 + (1 / tirePressure) * (0.01 + 0.0095 * Math.pow(velocity / 100, 2));
		// Normal Force = mgcos(theta)
		double normalForce = GlobalValues.CAR_MASS * 9.8 * Math.cos(angle);
		double force = rollingCoefficient * normalForce;
		// force is positive if opposes the direction of travel

		return force;
	}

	/**
	 * Calculates total power coming from various resistive forces
	 *
	 * @param currForecast: the FIODataPoint corresponding to the forecast at the
	 *                      destination location at the current time for whichever
	 *                      simFrame is calling this method
	 * @param toLoc:        the end location of the simFrame calling this method
	 * @param fromLoc:      the start location of the car in the simFrame calling this
	 *                      method
	 * @param carSpeed:     the car's predicted speed in the simFrame calling this method
	 * @return power loss/gain due to resistive forces during the given interval
	 */

	public double calculateResistivePower(FIODataPoint currForecast, GeoCoord toLoc, GeoCoord fromLoc,
										  double carSpeed) {
		double inclinationAngle = getInclinationAngle(fromLoc, toLoc);
		double gradientForce = getGradientResistanceForce(inclinationAngle);
		double frictionForce = getRollingResistanceForce(inclinationAngle, GlobalValues.TIRE_PRESSURE, carSpeed);
		double dragForce = calculateDrag(toLoc, fromLoc, carSpeed, currForecast);
		double resistivePower = (gradientForce + frictionForce + dragForce) * carSpeed * GlobalValues.KMH_TO_MS_FACTOR
				/ GlobalValues.ENGINE_EFF;
		return resistivePower;
	}

	public double calculateAccelerationPower(double carSpeed, int isAccelerating) {
		double accelerationForce = GlobalValues.CAR_MASS * GlobalValues.MAX_ACCELERATION_MS2;
		double accelerationPower = accelerationForce * carSpeed * GlobalValues.KMH_TO_MS_FACTOR / GlobalValues.ENGINE_EFF;
		return accelerationPower * isAccelerating;
	}

	/**
	 * Calculates power gain from solar panels on the car, assuming it is
	 * experiencing the weather given in forecastForPoint, according to formulas
	 * given at
	 * http://scool.larc.nasa.gov/lesson_plans/CloudCoverSolarRadiation.pdf and
	 * at http://photovoltaic-software.com/PV-solar-energy-calculation.php
	 *
	 * @param forecastForPoint:       the forecast for the point you're trying to predict power
	 *                                output at.
	 * @param sunriseTime/sunsetTime: time given in UNIX time (seconds from 1/1/1970 00:00 GMT)
	 * @return the amount of power (in Watts) that the panels will produce in
	 * the given situation
	 */

	// TODO: make this private after JUnit testing
	public double calculateSunPower(FIODataPoint forecastForPoint, long sunriseTime, long sunsetTime, double latitude, long currTime) {
		if (currTime < sunriseTime || currTime > sunsetTime)
			return 0;

		double cloudCover = forecastForPoint.cloudCover();
		double cloudCoverFactor = 990.0 * (1 - 0.75 * cloudCover * cloudCover * cloudCover);
		double panelArea = inUseCarModel.getSolarPanelArea();
		double sunAngle = calculateSunAltitudeAngle(sunriseTime, sunsetTime, latitude, currTime);
		double sunPower = panelArea * GlobalValues.PANEL_EFFICIENCY * cloudCoverFactor * Math.cos(sunAngle);
		if(sunPower < 0) sunPower = 0;
		return sunPower;
	}

	public double calculateSunAltitudeAngle(long sunriseTime, long sunsetTime, double latitude, long currTime) {
		long solarNoon = (sunriseTime + sunsetTime) / 2;
		double hourAngle = (15.0 * Math.PI / 180.0) * (currTime - solarNoon) / 3600000.0;

		Calendar currCalendar = Calendar.getInstance();
		currCalendar.setTimeInMillis(currTime);
		int dayOfMonth = currCalendar.get(Calendar.DAY_OF_MONTH);
		int month = currCalendar.get(Calendar.MONTH);
		int year = currCalendar.get(Calendar.YEAR);
		int dayNumber = findDayNumber(dayOfMonth, month, year);
		double declinationAngle = (Math.PI / 180.0) * 23.45 * Math.sin(360.0 / 365.0 * (284 + dayNumber));
		double altitudeAngleFromHorizontal = (Math.cos(latitude) * Math.cos(declinationAngle) *
				Math.cos(hourAngle)) + (Math.sin(latitude) * Math.sin(declinationAngle));
		double altitudeAngleFromVertical = Math.PI / 2.0 - altitudeAngleFromHorizontal;
		return altitudeAngleFromVertical;

	}

	public int findDayNumber(int dayOfMonth, int month, int year) {
		int monthCounter = 0;
		int dayNumber = dayOfMonth;
		while (monthCounter < month) {
			switch (month) {
				case 0:
					dayNumber += 31;
					break;
				case 1:
					dayNumber += 28;
					if (year % 4 == 0) dayNumber++;
					break;
				case 2:
					dayNumber += 31;
					break;
				case 3:
					dayNumber += 30;
					break;
				case 4:
					dayNumber += 31;
					break;
				case 5:
					dayNumber += 30;
					break;
				case 6:
					dayNumber += 31;
					break;
				case 7:
					dayNumber += 31;
					break;
				case 8:
					dayNumber += 30;
					break;
				case 9:
					dayNumber += 31;
					break;
				case 10:
					dayNumber += 30;
					break;
			}
			monthCounter++;
		}
		return dayNumber;
	}

	/**
	 * Helper function, picks the hourly report that is closest in time to
	 * timeFrame
	 *
	 * @param ForecastIO with full selection of weather data, and a set of hourly
	 *                   forecasts that is sorted by time
	 * @param timeFrame: time in ms since jan1 1970 (see System.currentTimeMillis)
	 * @return an FIODataPoint containing the hourly forecast for the hour
	 * closest to timeFrame
	 */

	public FIODataPoint chooseReport(ForecastIO weather, double timeFrame) {
		JsonObject hourly = weather.getHourly();
		JsonArray hourlyData = (JsonArray) hourly.get("data");
		long currTime = Long.parseLong(((JsonObject) hourlyData.get(0)).get("time").toString())*1000;
		int bestIndex = 0;
		double smallestDiff = Math.abs(timeFrame - currTime);
		double prevDiff = smallestDiff;
		for (int i = 1; i < hourlyData.size(); i++) {
			currTime = Long.parseLong(((JsonObject) hourlyData.get(i)).get("time").toString())*1000;
			double currDiff = Math.abs(timeFrame - currTime);
			if (currDiff < smallestDiff) {
				smallestDiff = currDiff;
				bestIndex = i;
			}
			if (currDiff > prevDiff)
				break;
			prevDiff = currDiff;
		}
		FIODataPoint toReturn = new FIODataBlock(weather.getHourly()).datapoint(bestIndex);
		toReturn.setTimezone("PST");
		return toReturn;
	}
	// ^^^^^^^^^^^^^^^^^^^^^^^^^^THIS HAS BEEN
	// UPDATED^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

	/**
	 * Calculates the estimated resistive force due to air drag acting on the
	 * car in the interval from fromLoc to toLoc
	 *
	 * @param toLoc:      breadcrumb that simulated car is traveling to
	 * @param fromLoc:    breadcrumb that simulated car is traveling from
	 * @param carSpeed:   the simulated car's current speed (from the previous
	 *                    breadcrumb) in km/h
	 * @param toForecast: the weather forecast at the current time at the destination
	 *                    location, used to find the headwind
	 * @return the estimated resistive force acting on the car during the
	 * interval (fromLoc-toLoc) This value will be positive if the drag
	 * is resisting the car's motion (i.e. there is a headwind) or
	 * negative if it's assisting (tailwind)
	 */
	// TODO: change to private after JUnit testing
	public double calculateDrag(GeoCoord toLoc, GeoCoord fromLoc, double carSpeed, FIODataPoint toForecast) {
		double latDiff = toLoc.getLat() - fromLoc.getLat();
		double lonDiff = toLoc.getLon() - fromLoc.getLon();
		double carBearing;// measured as angle in degrees, with 0 at north and
		// measured clockwise
		double alpha = Math.atan(latDiff / lonDiff);
		// double alphaDegrees = alpha * 180.0 / Math.PI;
		if (lonDiff >= 0) {
			carBearing = Math.PI / 2.0 - alpha;
		} else {
			carBearing = 3.0 * Math.PI / 2.0 - alpha;
		}

		double windSpeed = toForecast.windSpeed() * GlobalValues.KMH_TO_MS_FACTOR;
		double carSpeedInMS = carSpeed * GlobalValues.KMH_TO_MS_FACTOR;
		double relativeVelocity = carSpeedInMS;
		if (windSpeed != 0) {
			// this if statement is necessary because windBearing() will be
			// undefined if windSpeed() is 0
			double windBearing = toForecast.windBearing() * Math.PI / 180;
			relativeVelocity = carSpeedInMS - windSpeed * Math.cos((windBearing - carBearing));
		}
		boolean isTailwind = false;
		if (Math.abs(relativeVelocity) < Math
				.abs(carSpeedInMS/* * Math.sin(carBearing) */
				))
			isTailwind = true;
		double dragMag = 0.5 * GlobalValues.CAR_CROSS_SECTIONAL_AREA * GlobalValues.DRAG_COEFF * relativeVelocity
				* relativeVelocity;
		if (isTailwind)
			return -1 * dragMag;
		else
			return dragMag;
	}
}