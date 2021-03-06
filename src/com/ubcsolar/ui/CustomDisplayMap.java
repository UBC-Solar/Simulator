/**
 * This class wraps a JMapViewer
 */

package com.ubcsolar.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.DefaultMapController;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapObjectImpl;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.Style;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

import com.ubcsolar.common.LocationReport;
import com.ubcsolar.common.LogType;
import com.github.dvdme.ForecastIOLib.FIODataBlock;
import com.github.dvdme.ForecastIOLib.ForecastIO;
import com.ubcsolar.Main.GlobalValues;
import com.ubcsolar.common.ForecastReport;
import com.ubcsolar.common.GeoCoord;
import com.ubcsolar.common.PointOfInterest;
import com.ubcsolar.common.Route;
import com.ubcsolar.common.SolarLog;
import com.ubcsolar.notification.NewMapLoadedNotification;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.event.ActionEvent;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.JButton;
import javax.swing.JTextPane;

public class CustomDisplayMap extends JMapViewer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Color defaultColorForThings = new Color(204, 0, 204);
	private final Font defaultFontForThings = new Font("Tahoma", Font.BOLD, 13);

	private MapMarker carCurrentLocation; // car's current location
	private boolean showCarLocation; // initial value set to equal checkbox
	private boolean showSpeeds;
	private List<MapMarker> speeds;
	private List<MapMarker> forecasts; // route forecasts
	private boolean showForecasts;// initial value set to equal checkbox
	private List<MapMarker> routePOIs; // POIs (Mostly cities) along the route
	private boolean showPOIs; // initial value set to equal checkbox
	private MapPolygon routeBreadcrumbs; // the line that shows the trail
	private boolean showRouteBreadcrumbs; // initial value set to equal checkbox
	private Map<GeoCoord, Map<Integer, Double>> simResult; //map to hold results of most recent simulation
	private Set<MapPolygon> accelBreadcrumbs;//lines that will show when to accel
	private Set<MapPolygon> deccelBreadcrumbs;
	private JRadioButton rdbtnSateliteoffline;
	private JRadioButton rdbtnSattelite;
	private JRadioButton rdbtnDefaultMapOffline;
	private JRadioButton rdbtnDefaultMap;
	private JSpinner spinner; //spinner for lap number displayed

	public CustomDisplayMap() {
		super(new saveToDiskCache(), 8); // 8 is used in the default constructor
		new DefaultMapController(this); // not called in the second
										// constructor... must be a bug?
		// super();
		// this.setTileSource(new
		// OfflineOsmTileSource("File:///Users/Noah/Desktop/testMapFiles/",1,2));
		JCheckBox chckbxForecasts = new JCheckBox("Forecasts");

		chckbxForecasts.setForeground(defaultColorForThings);
		chckbxForecasts.setFont(defaultFontForThings);
		chckbxForecasts.setOpaque(false);
		chckbxForecasts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showForecasts = chckbxForecasts.isSelected();
				refreshMap();
			}
		});
		chckbxForecasts.setSelected(true);
		showForecasts = chckbxForecasts.isSelected();
		chckbxForecasts.setBounds(268, 10, 97, 23);
		add(chckbxForecasts);

		JCheckBox chckbxCities = new JCheckBox("Cities");
		chckbxCities.setSelected(true);
		showPOIs = chckbxCities.isSelected();
		chckbxCities.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showPOIs = chckbxCities.isSelected();
				refreshMap();
			}
		});
		chckbxCities.setForeground(defaultColorForThings);
		chckbxCities.setFont(defaultFontForThings);
		chckbxCities.setOpaque(false);

		chckbxCities.setBounds(26, 10, 72, 23);
		add(chckbxCities);

		JCheckBox chckbxCarLocation = new JCheckBox("Car Location");
		chckbxCarLocation.setForeground(defaultColorForThings);
		chckbxCarLocation.setFont(defaultFontForThings);
		chckbxCarLocation.setOpaque(false);
		chckbxCarLocation.setSelected(true);
		showCarLocation = chckbxCarLocation.isSelected();
		chckbxCarLocation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showCarLocation = chckbxCarLocation.isSelected();
				refreshMap();
			}
		});
		chckbxCarLocation.setBounds(90, 10, 116, 23);
		add(chckbxCarLocation);

		JCheckBox chckbxRoute = new JCheckBox("Route");
		chckbxRoute.setForeground(defaultColorForThings);
		chckbxRoute.setFont(defaultFontForThings);
		chckbxRoute.setOpaque(false);
		chckbxRoute.setSelected(true);
		showRouteBreadcrumbs = chckbxRoute.isSelected();
		chckbxRoute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showRouteBreadcrumbs = chckbxRoute.isSelected();
				refreshMap();
			}
		});
		chckbxRoute.setBounds(197, 10, 83, 23);
		add(chckbxRoute);

		rdbtnSattelite = new JRadioButton("Satellite");
		rdbtnSattelite.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTileSource(MapSource.MAPQUEST_SAT);
			}
		});

		JCheckBox chckbxSpeeds = new JCheckBox("Recommended Speeds    Lap Number:\n");
		chckbxSpeeds.setForeground(defaultColorForThings);
		chckbxSpeeds.setBounds(361, 10, 267, 23);
		chckbxSpeeds.setFont(defaultFontForThings);
		chckbxSpeeds.setOpaque(false);
		chckbxSpeeds.setSelected(true);
		showSpeeds = chckbxSpeeds.isSelected();
		chckbxSpeeds.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				showSpeeds = chckbxSpeeds.isSelected();
				refreshMap();
			}
		});
		add(chckbxSpeeds);

		rdbtnSattelite.setForeground(defaultColorForThings);
		rdbtnSattelite.setFont(defaultFontForThings);
		rdbtnSattelite.setOpaque(false);
		rdbtnSattelite.setBounds(10, 259, 82, 23);
		add(rdbtnSattelite);

		rdbtnDefaultMapOffline = new JRadioButton("Map (offline)");
		rdbtnDefaultMapOffline.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTileSource(MapSource.OSM_MAP_OFFLINE);
			}
		});
		rdbtnDefaultMapOffline.setForeground(defaultColorForThings);
		rdbtnDefaultMapOffline.setFont(defaultFontForThings);
		rdbtnDefaultMapOffline.setOpaque(false);
		rdbtnDefaultMapOffline.setBounds(10, 233, 124, 23);
		add(rdbtnDefaultMapOffline);

		rdbtnDefaultMap = new JRadioButton("Map");
		rdbtnDefaultMap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTileSource(MapSource.OSM_MAP);
			}
		});
		rdbtnDefaultMap.setForeground(defaultColorForThings);
		rdbtnDefaultMap.setFont(defaultFontForThings);
		rdbtnDefaultMap.setOpaque(false);
		rdbtnDefaultMap.setBounds(10, 207, 52, 23);
		add(rdbtnDefaultMap);

		rdbtnSateliteoffline = new JRadioButton("Satellite (offline)");
		rdbtnSateliteoffline.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTileSource(MapSource.MAPQUEST_SAT_OFFLINE);
			}
		});
		rdbtnSateliteoffline.setForeground(defaultColorForThings);
		rdbtnSateliteoffline.setFont(defaultFontForThings);
		rdbtnSateliteoffline.setOpaque(false);
		rdbtnSateliteoffline.setBounds(10, 285, 140, 23);
		add(rdbtnSateliteoffline);

		JLabel lblTileSoure = new JLabel("Tile Soure");
		lblTileSoure.setForeground(defaultColorForThings);
		lblTileSoure.setFont(defaultFontForThings);
		lblTileSoure.setBounds(10, 184, 82, 20);
		add(lblTileSoure);
		
		this.simResult = new LinkedHashMap<GeoCoord, Map<Integer, Double>>(); //initialize the map for sim result
	
		spinner = new JSpinner();
		spinner.setModel(new SpinnerNumberModel(0, 0, 0, 1)); //set spinner to 0
		spinner.setBounds(629, 12, 44, 20);
		add(spinner);
		
		JTextPane textPane = new JTextPane();
		textPane.setBounds(667, 10, 6, 20);
		add(textPane);
		
		spinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				addSpeedsToMap();
			}
		});
		
		
		this.updateTileSource(MapSource.OSM_MAP); // default tiles
		
	}

	public void changeDrawnRoute(Route newRouteToLoad) {
		this.removeAllMapPolygons();
		this.removeAllMapMarkers();
		this.addNewRouteToMap(newRouteToLoad);
		//empty map just so it can compile, real map will be passed later
	}

	public void addNewCarLocationToMap(LocationReport newLocation) {
		this.removeMapMarker(carCurrentLocation); // not sure if this should be
													// here if we repaint anyway
		Style testStyle = new Style(Color.BLACK, Color.RED, null, this.defaultFontForThings);
		MapMarkerDot newLocationDot = new MapMarkerDot(null, "THE CAR",
				new Coordinate(newLocation.getLocation().getLat(), newLocation.getLocation().getLon()), testStyle);
		this.carCurrentLocation = newLocationDot; // so we can remove it next
													// time.
		this.refreshMap();
	}

	public void addNewRouteToMap(Route newRouteToLoad) {
		List<Coordinate> listForPolygon = new ArrayList<Coordinate>(newRouteToLoad.getTrailMarkers().size());
		// remove the old one
		this.removeMapPolygon(this.routeBreadcrumbs);
		if (this.routePOIs != null) {
			for (MapMarker m : routePOIs) {
				this.removeMapMarker(m);
			}
		}

		for (GeoCoord geo : newRouteToLoad.getTrailMarkers()) {
			listForPolygon.add(new Coordinate(geo.getLat(), geo.getLon()));
		}

		// adding this in to make it a single line, otherwise it draws a line
		// from end to start.
		// There may be a better way of doing this...
		for (int i = newRouteToLoad.getTrailMarkers().size() - 1; i >= 0; i--) {
			GeoCoord toAdd = newRouteToLoad.getTrailMarkers().get(i);
			listForPolygon.add(new Coordinate(toAdd.getLat(), toAdd.getLon()));
		}

		this.routeBreadcrumbs = new MapPolygonImpl(listForPolygon);

		this.routePOIs = new ArrayList<MapMarker>(newRouteToLoad.getPointsOfIntrest().size());
		for (PointOfInterest temp : newRouteToLoad.getPointsOfIntrest()) {
			GeoCoord newSpot = temp.getLocation();
			String name = temp.getName().split(",")[0]; // don't need the whole
														// "city, state,
														// country, continent,
														// earth" name
			routePOIs.add(new MapMarkerDot(name, new Coordinate(newSpot.getLat(), newSpot.getLon())));
		}
		this.refreshMap(); // will paint it on if it's supposed to be there
		this.repaint();

	}

	public void addForecastsToMap(ForecastReport theReport) {
		Style forecastStyle = new Style(Color.black, Color.GREEN, null, this.defaultFontForThings);

		if (this.forecasts != null) {
			for (MapMarker m : forecasts) {
				this.removeMapMarker(m);
			}
		}

		forecasts = new ArrayList<MapMarker>(theReport.getForecasts().size());
		for (int i = 0; i < theReport.getForecasts().size(); i++) {
			ForecastIO fc = theReport.getForecasts().get(i);
			Coordinate location = new Coordinate(fc.getLatitude(), fc.getLongitude());
			String name = new FIODataBlock(fc.getHourly()).icon();
			MapMarkerDot newLocationDot = new MapMarkerDot(null, name, location, forecastStyle);
			// newLocationDot.
			forecasts.add(newLocationDot);
		}
		this.refreshMap();
	}

	// *****************************************************BETA******************************************************
	//function will be called when new sim is loaded
	public void GetNewSimData(Map<GeoCoord, Map<Integer, Double>> speed_profile) {
		this.simResult = new LinkedHashMap<GeoCoord, Map<Integer, Double>>(speed_profile); //copy the new sim results to the sim result field
		if (this.simResult.size() != 0) {
			this.spinner.setModel((new SpinnerNumberModel(1, 1, ((Map) simResult.values().toArray()[0]).size(), 1))); //set new bounds for spinner, set to 1
		}
		addSpeedsToMap(); //add speeds to map
	}
	/**
	 * a way to classify points based on their relative speeds when visually displaying the simulation
	 * 
	 * ACCEL_START: start of acceleration (speed at prev point == speed at current point < speed at next point)
	 * ACCEL: accelerating (speed at prev point < speed at curr point < speed at next point)
	 * ACCEL_END: end of acceleration (speed at prev point < speed at curr point == speed at next point)
	 * 
	 * DECCEL_START: start of deceleration (speed at prev point == speed at curr point > speed at next point)
	 * DECCEL: decelerating (speed at prev point > speed at curr point > speed at next point)
	 * DECCEL_END: end of deceleration (speed at prev point > speed at curr point == speed at next point)
	 * 
	 * CONST: constant speed (speed at prev point == speed at curr point == speed at next point)
	 * INFLECT: sudden change in acceleration (speed at prev point < speed at current point > speed at next point) or
	 *                                        (speed at prev point > speed at current point < speed at next point)
	 */
	enum point_type {ACCEL_START, ACCEL, ACCEL_END, DECCEL_START, DECCEL, DECCEL_END, CONST, INFLECT};
	double DELTA = 0.000001; //for comparison of doubles
	
	//function adds speed markers to map
	private void addSpeedsToMap() {
		Style speedStyle = new Style(Color.black, Color.BLUE, null, this.defaultFontForThings); 
		int lap_number = (int) this.spinner.getValue(); //get value of spinner
		double filter_distance = 0.5; //distance between points shown
		GeoCoord last_marker = null; //variable to keep track of last marker displayed
		
		//if route chunks already exist, remove
		if (this.accelBreadcrumbs != null) {
			for (MapPolygon mapPoly : this.accelBreadcrumbs) {
				this.removeMapPolygon(mapPoly);
			}
		}
		
		if (this.deccelBreadcrumbs != null) {
			for (MapPolygon mapPoly : this.deccelBreadcrumbs) {
				this.removeMapPolygon(mapPoly);
			}
			
		}
		//if markers exist already, remove
		if (this.speeds != null) {
			for (MapMarker m : this.speeds) {
				this.removeMapMarker(m);
			}
		}
		
		//empty all the old collections of points
		this.speeds = new ArrayList<MapMarker>();
		this.accelBreadcrumbs = new HashSet<MapPolygon>();
		this.deccelBreadcrumbs = new HashSet<MapPolygon>();
		
		//instantiate lists to hold points for route chunks
		List<Coordinate> list_for_accel_polygon = null;
		List<Coordinate> list_for_deccel_polygon = null;
		
		//the loop iterates through the points
		for (int i = 0; i < this.simResult.size(); i++) {
			//Since this class does not hold any records of the route in a list,
			//points can only be accessed through the key set of the sim result
			GeoCoord curr = (GeoCoord) this.simResult.keySet().toArray()[i];
			
			//create a map marker to be posted on the map if the current point is:
			// - CONST and is the first point of the route (last marker is null), or the last time a point was posted was a certain distance away
			// - not CONST, ACCEL, or DECCEL (only post points that are start/end of acceleration/deceleration for user to see from where to where 
			// to accelerate/decelerate to)
			if ((getPointInfo(i, lap_number) == point_type.CONST && (last_marker == null ||
					curr.calculateDistance(last_marker)  > filter_distance)) ||
					(getPointInfo(i, lap_number) != point_type.CONST && getPointInfo(i, lap_number) != point_type.ACCEL &&
			         getPointInfo(i, lap_number) != point_type.DECCEL)) {
				Coordinate location = new Coordinate(curr.getLat(), curr.getLon());
				String speed = this.simResult.get(curr).get(lap_number).toString(); 
				MapMarkerDot newLocationDot = new MapMarkerDot(null, speed , location, speedStyle);
				this.speeds.add(newLocationDot); //add map marker to list that will be posted
				last_marker = curr; //keep track of last posted point (for filtering by distance)
			}
			//if point is the start of acceleration, create a new list to hold all the points to be connected in a line
			//the line will represent the acceleration, so all the points between ACCEL_START and ACCEL_END
			//will be added into this
			if (getPointInfo(i, lap_number) == point_type.ACCEL_START) {
				list_for_accel_polygon = new ArrayList<Coordinate>();
				list_for_accel_polygon.add(new Coordinate(curr.getLat(), curr.getLon()));
			}
			//same for start of deceleration
			else if (getPointInfo(i, lap_number) == point_type.DECCEL_START) {
				list_for_deccel_polygon = new ArrayList<Coordinate>();
				list_for_deccel_polygon.add(new Coordinate(curr.getLat(), curr.getLon()));
			}
			//if point is acceleration, add the point to the current list of acceleration
			//if point is acceleration, it should be between an acceleration start and acceleration end
			else if (getPointInfo(i, lap_number) == point_type.ACCEL) {
				list_for_accel_polygon.add(new Coordinate(curr.getLat(), curr.getLon()));
			}
			//same for deceleration
			else if (getPointInfo(i, lap_number) == point_type.DECCEL) {
				list_for_deccel_polygon.add(new Coordinate(curr.getLat(), curr.getLon()));
			}
			//if point is acceleration end, add the point to the current list of acceleration and
			//add call a function that turns the list into a list that generates a line
			//then, turn the line into a polygon and add it to the set of route chunks to be displayed
			else if (getPointInfo(i, lap_number) == point_type.ACCEL_END) {
				list_for_accel_polygon.add(new Coordinate(curr.getLat(), curr.getLon()));
				completeLinePolygon(list_for_accel_polygon);
				this.accelBreadcrumbs.add(new MapPolygonImpl(list_for_accel_polygon));
			}
			//same for deceleration
			else if (getPointInfo(i, lap_number) == point_type.DECCEL_END) {
				list_for_deccel_polygon.add(new Coordinate(curr.getLat(), curr.getLon()));
				completeLinePolygon(list_for_deccel_polygon);
				this.deccelBreadcrumbs.add(new MapPolygonImpl(list_for_deccel_polygon));
			}
		}
		
		this.refreshMap();
	}
	
	//function takes a list of points and turns it into a list that generates a line (same idea as noah)
	private void completeLinePolygon(List<Coordinate> incomplete_list) {
		//essentially, the function copies the entries of the list and append them to the end of the list
		//in reversed order. The function used to generate the line is actually a function that draws a 
		//polygon using a list of coordinates (it connects the coordinates in the list in the order they are 
		//in the list). by adding the same points to the end, the polygon produced is basically a 2d line
		//(after connecting all the points in the order of the route, the last point is then connected back
		//to the second last point etc all the way back to the first point)
		for (int i = incomplete_list.size() - 1; i >= 0; i-- ) {
			incomplete_list.add(incomplete_list.get(i));
		}
	}
	
	//function classifies the point at the index of the traveled route
	//returns the proper "point type"
	private point_type getPointInfo(int point_index, int lap_number) {
		//points that are used to classify
		GeoCoord curr = (GeoCoord) this.simResult.keySet().toArray()[point_index]; 
		GeoCoord prev;
		GeoCoord next;
		
		//if the first point of the route is checked, assume at the previous point, 
		//the speed was the same
		if (point_index == 0) {
			prev = curr;
		}
		else {
			prev = (GeoCoord) this.simResult.keySet().toArray()[point_index - 1];
		}
		
		//if the last point of the route is checked, assume at the next point, 
		//the speed will be the same
		if (point_index == (this.simResult.size() - 1)) {
			next = curr;
		}
		else {
			next = (GeoCoord) this.simResult.keySet().toArray()[point_index + 1];
		}
		
		//start classifying
		//if curr point speed == prev point speed....
		if (Math.abs(this.simResult.get(curr).get(lap_number) - this.simResult.get(prev).get(lap_number)) < DELTA) {
			//.. and curr point speed == next point speed, point is type CONST
			if (Math.abs(this.simResult.get(curr).get(lap_number) - this.simResult.get(next).get(lap_number)) < DELTA) {
				return point_type.CONST;
			}
			//.. and curr point speed  > next point speed, point is type DECCEL_START
			else if (this.simResult.get(curr).get(lap_number) > this.simResult.get(next).get(lap_number)) {
				return point_type.DECCEL_START;
			}
			//.. and curr point speed < next point speed, point is type ACCEL_START
			else {
				return point_type.ACCEL_START;
			}
		}
		
		//if curr point speed > prev point speed....
		else if (this.simResult.get(curr).get(lap_number) > this.simResult.get(prev).get(lap_number)) {
			//.. and curr point speed == next point speed, point is type ACCEL_END
			if (Math.abs(this.simResult.get(curr).get(lap_number) - this.simResult.get(next).get(lap_number)) < DELTA) {
				return point_type.ACCEL_END;
			}
			//.. and curr point speed < next point speed, point is type ACCEL
			else if (this.simResult.get(curr).get(lap_number) < this.simResult.get(next).get(lap_number)) {
				return point_type.ACCEL;
			}
			//.. and curr point speed > next point speed, point is type INFLECT (car should never be able to do this)
			else {
				return point_type.INFLECT;
			}
		}
		
		//if curr point speed < prev point speed ...
		else {
			//.. and curr point speed == prev point speed, point is type DECCEL_END
			if (Math.abs(this.simResult.get(curr).get(lap_number) - this.simResult.get(next).get(lap_number)) < DELTA) {
				return point_type.DECCEL_END;
			}
			//..and curr point speed < next point speed, point is type INFLECT (car should never be able to do this)
			else if (this.simResult.get(curr).get(lap_number) < this.simResult.get(next).get(lap_number)) {
				return point_type.INFLECT;
			}
			//..and curr point speed > next point speed, point is type DECCEL
			else {
				return point_type.DECCEL;
			}
		}
	}
	// **************************************************************************************************************

	public void refreshMap() {
		/*
		 * there's two ways to do this; one is compare each object and try to
		 * add or remove it according to it's true/false value. The other way is
		 * to remove all of them and re-add everything that's supposed to be
		 * there. The second choice is more computationally expensive, but less
		 * risk for bugs (can't forget about anything). Should performance
		 * become an issue, this is a good method to optimize.
		 */
		this.removeAllMapMarkers();
		this.removeAllMapPolygons();
		this.removeAllMapRectangles();

		if (this.showRouteBreadcrumbs && routeBreadcrumbs != null) {
			this.addMapPolygon(routeBreadcrumbs);
		}

		if (this.showPOIs && routePOIs != null) {
			for (MapMarker m : routePOIs) {
				this.addMapMarker(m);
			}
		}

		if (this.showSpeeds && speeds != null && accelBreadcrumbs != null && deccelBreadcrumbs != null) {
			for (MapMarker m : speeds) {
				this.addMapMarker(m);
			}
			for (MapPolygon chunk : this.accelBreadcrumbs) {
				((MapObjectImpl) chunk).setColor(Color.green);
				((MapObjectImpl) chunk).setStroke(new BasicStroke(5));
				this.addMapPolygon(chunk);
			}
			for (MapPolygon chunk : this.deccelBreadcrumbs) {
				((MapObjectImpl) chunk).setColor(Color.RED);
				((MapObjectImpl) chunk).setStroke(new BasicStroke(5));
				this.addMapPolygon(chunk);
			}
		}

		if (this.showForecasts && forecasts != null) {
			for (MapMarker m : forecasts) {
				this.addMapMarker(m);
			}
		}

		if (this.showCarLocation && carCurrentLocation != null) {
			this.addMapMarker(carCurrentLocation);
		}
	}

	private void deselectAllComboBoxes() {
		this.rdbtnDefaultMapOffline.setSelected(false);
		this.rdbtnDefaultMap.setSelected(false);
		this.rdbtnSateliteoffline.setSelected(false);
		this.rdbtnSattelite.setSelected(false);
	}

	private void updateTileSource(MapSource newSource) {
		switch (newSource) {
		case OSM_MAP:
			this.deselectAllComboBoxes();
			this.rdbtnDefaultMap.setSelected(true);
			SolarLog.write(LogType.SYSTEM_REPORT, System.currentTimeMillis(),
					"Tile source changed to standard map tiles");
			this.setTileSource(new OsmTileSource.Mapnik());
			this.getTileCache().clear();
			break;
		case OSM_MAP_OFFLINE:
			this.deselectAllComboBoxes();
			rdbtnDefaultMapOffline.setSelected(true);
			SolarLog.write(LogType.SYSTEM_REPORT, System.currentTimeMillis(),
					"Tile Source switched to offline standard map tiles");
			this.setTileSource(
					new OfflineOsmTileSource("File:///" + GlobalValues.DEFAULT_TILE_SAVE_LOCATION + "mapnik/", 1, 19));
			this.getTileCache().clear();
			break;
		case MAPQUEST_SAT:
			this.deselectAllComboBoxes();
			this.rdbtnSattelite.setSelected(true);
			SolarLog.write(LogType.SYSTEM_REPORT, System.currentTimeMillis(),
					"Tile source changed to BING Ariel tiles");
			this.setTileSource(new BingAerialTileSource());
			this.getTileCache().clear();
			break;
		case MAPQUEST_SAT_OFFLINE:
			this.deselectAllComboBoxes();
			this.rdbtnSateliteoffline.setSelected(true);
			this.setTileSource(new OfflineOsmTileSource(
					"File:///" + GlobalValues.DEFAULT_TILE_SAVE_LOCATION + "Bing Aerial Maps/", 1, 19));
			this.getTileCache().clear();
			break;
		}
	}

	private static class saveToDiskCache extends MemoryTileCache {
		@Override
		public Tile getTile(TileSource source, int x, int y, int z) {
			Tile justGotten = super.getTile(source, x, y, z);
			if (justGotten == null) {
				return null;
			}
			if (justGotten.getSource().getName().equalsIgnoreCase("offline")) {
				return justGotten;
			}
			String placeToSave = justGotten.getSource().getName() + "/";
			placeToSave += justGotten.getZoom() + "/";
			placeToSave += justGotten.getXtile() + "/";
			// placeToSave += tile.getYtile() + "/"; //this is the tile name I
			// think
			String totalFilePath = GlobalValues.DEFAULT_TILE_SAVE_LOCATION + placeToSave;
			File saveSpot = new File(totalFilePath);
			if (!saveSpot.exists()) {
				saveSpot.mkdirs();
			}
			String filename = totalFilePath + justGotten.getYtile() + ".png";
			File outputfile = new File(filename);
			if (!outputfile.exists() && justGotten.isLoaded()) {
				try {
					// retrieve image
					BufferedImage bi = justGotten.getImage();
					ImageIO.write(bi, "png", outputfile);
				} catch (IOException e) {
					SolarLog.write(LogType.ERROR, System.currentTimeMillis(),
							"Unable to save tile image, IOException thrown");
				}
			}
			return justGotten;
		}
	}

	private enum MapSource {
		OSM_MAP, OSM_MAP_OFFLINE, MAPQUEST_SAT, MAPQUEST_SAT_OFFLINE
	}
}
