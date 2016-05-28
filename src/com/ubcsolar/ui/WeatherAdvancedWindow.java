package com.ubcsolar.ui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.github.dvdme.ForecastIOLib.FIODataBlock;
import com.github.dvdme.ForecastIOLib.FIODataPoint;
import com.github.dvdme.ForecastIOLib.ForecastIO;
import com.ubcsolar.Main.GlobalController;
import com.ubcsolar.common.DistanceUnit;
import com.ubcsolar.common.ForecastReport;
import com.ubcsolar.common.GeoCoord;
import com.ubcsolar.common.Listener;
import com.ubcsolar.common.LocationReport;
import com.ubcsolar.notification.NewForecastReport;
import com.ubcsolar.notification.NewLocationReportNotification;
import com.ubcsolar.notification.NewMapLoadedNotification;
import com.ubcsolar.notification.Notification;

import javax.swing.ImageIcon;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.SwingConstants;

public class WeatherAdvancedWindow extends JFrame implements Listener{

	private JPanel contentPane;
	private GlobalController mySession;
	private ChartPanel temperatureChart;
	private JFreeChart temperatureChartJFree;
	private ForecastReport currentForecastReport;
	private final String X_AXIS_LABEL = "Travel Distance (km)";
	private final int NUM_LINES = 4;
	private ChartPanel cloudCoverChart;
	private JFreeChart cloudCoverChartJFree;
	private ChartPanel precipitationChart;
	private JFreeChart precipitationChartJFree;
	private ChartPanel windSpeedChart;
	private JFreeChart windSpeedChartJFree;
	private JPanel windDirectionPanel;
	private JPanel panel;
	private JPanel fogPanel;
	private JLabel fogLabel;
	private JPanel stormPanel;
	private JLabel stormLabel;
	private JLabel windDirectionLabel;
	private GeoCoord currentLocation;
	private JMenuItem mntmLoadFakeForecast;
	private final int DEW_POINT_DIFF = 7;
	private double travelDistance;
	private double[] distances;
	private List<GeoCoord> forecastPoints;

	/**
	 * Launch the application.
	 *//*//don't need a main here.
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Weather frame = new Weather();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}*/

	/**
	 * Create the frame.
	 * @param mySession 
	 */
	public WeatherAdvancedWindow(final GlobalController mySession) {
		setTitle("Advanced Weather");
		this.mySession = mySession;
		travelDistance = 0.0;
		register();
		setTitle("Weather");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 1043, 655);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnForecasts = new JMenu("Forecasts");
		menuBar.add(mnForecasts);
		
		JMenuItem mntmLoadForecastsFor = new JMenuItem("Load Forecasts for Route (48 hours)");
		mntmLoadForecastsFor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mySession.getMyWeatherController().downloadNewForecastsForRoute(100);
			}
		});
		
		mnForecasts.add(mntmLoadForecastsFor);
		
		JMenuItem mntmLoadForecastsFor_1 = new JMenuItem("Load Forecasts for current location");
		mntmLoadForecastsFor_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Was asked currentLocation forecast");
			}
		});
		
		mnForecasts.add(mntmLoadForecastsFor_1);
		
		mntmLoadFakeForecast = new JMenuItem("Load Custom Forecast");
		mntmLoadFakeForecast.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e){
				JFrame frame = new FakeForecastAddWindow(mySession, travelDistance);
				frame.setVisible(true);
			}
		});
		mnForecasts.add(mntmLoadFakeForecast);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{0, 0, 0};
		gbl_contentPane.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_contentPane.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{1.0, 1.0, 1.0, 1.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);
				
		buildTemperatureChart(createBlankDataset());
		temperatureChart = new ChartPanel(temperatureChartJFree);
		GridBagConstraints gbc_temperatureChart = new GridBagConstraints();
		gbc_temperatureChart.weighty = 10.0;
		gbc_temperatureChart.insets = new Insets(0, 0, 5, 5);
		gbc_temperatureChart.fill = GridBagConstraints.BOTH;
		gbc_temperatureChart.gridx = 0;
		gbc_temperatureChart.gridy = 0;
		contentPane.add(temperatureChart, gbc_temperatureChart);
		
		buildCloudCoverChart(createBlankDataset());
		cloudCoverChart = new ChartPanel(cloudCoverChartJFree);
		GridBagConstraints gbc_cloudCoverChart = new GridBagConstraints();
		gbc_cloudCoverChart.insets = new Insets(0, 0, 5, 0);
		gbc_cloudCoverChart.fill = GridBagConstraints.BOTH;
		gbc_cloudCoverChart.gridx = 1;
		gbc_cloudCoverChart.gridy = 0;
		contentPane.add(cloudCoverChart, gbc_cloudCoverChart);
		
		buildPrecipitationChart(createBlankDataset());
		precipitationChart = new ChartPanel(precipitationChartJFree);
		GridBagConstraints gbc_precipitationChart = new GridBagConstraints();
		gbc_precipitationChart.weighty = 10.0;
		gbc_precipitationChart.insets = new Insets(0, 0, 5, 5);
		gbc_precipitationChart.fill = GridBagConstraints.BOTH;
		gbc_precipitationChart.gridx = 0;
		gbc_precipitationChart.gridy = 1;
		contentPane.add(precipitationChart, gbc_precipitationChart);
		
		buildWindSpeedChart(createBlankDataset());
		windSpeedChart = new ChartPanel(windSpeedChartJFree);
		GridBagConstraints gbc_windSpeedChart = new GridBagConstraints();
		gbc_windSpeedChart.insets = new Insets(0, 0, 5, 0);
		gbc_windSpeedChart.fill = GridBagConstraints.BOTH;
		gbc_windSpeedChart.gridx = 1;
		gbc_windSpeedChart.gridy = 1;
		contentPane.add(windSpeedChart, gbc_windSpeedChart);
		
		fogPanel = new JPanel();
		GridBagConstraints gbc_fogPanel = new GridBagConstraints();
		gbc_fogPanel.insets = new Insets(0, 0, 5, 5);
		gbc_fogPanel.gridx = 0;
		gbc_fogPanel.gridy = 2;
		contentPane.add(fogPanel, gbc_fogPanel);
		
		windDirectionPanel = new JPanel();
		GridBagConstraints gbc_windDirectionPanel = new GridBagConstraints();
		gbc_windDirectionPanel.insets = new Insets(0, 0, 5, 0);
		gbc_windDirectionPanel.gridx = 1;
		gbc_windDirectionPanel.gridy = 2;
		contentPane.add(windDirectionPanel, gbc_windDirectionPanel);
		
		stormPanel = new JPanel();
		GridBagConstraints gbc_stormPanel = new GridBagConstraints();
		gbc_stormPanel.insets = new Insets(0, 0, 0, 5);
		gbc_stormPanel.gridx = 0;
		gbc_stormPanel.gridy = 3;
		contentPane.add(stormPanel, gbc_stormPanel);
		
		windDirectionLabel = new JLabel("Wind is blowing from: None");
		windDirectionPanel.add(windDirectionLabel);
		
		fogLabel = new JLabel("No fog warning");
		fogLabel.setHorizontalAlignment(SwingConstants.LEFT);
		fogPanel.add(fogLabel);
		
		stormLabel = new JLabel("No storm warning");
		stormPanel.add(stormLabel);
		
		panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 3;
		contentPane.add(panel, gbc_panel);
		
		
		setTitleAndLogo();
		}
		
		private void setTitleAndLogo(){
			
			this.setIconImage(mySession.iconImage.getImage());
			this.setTitle("Weather");
		}
		
		/**
		 * All notifications that this class has registered for will come here
		 */
		@Override
		public void notify(Notification n){
			//add any notifications here
			
			//A new map has been loaded into the program
			if(n.getClass() == NewForecastReport.class){ //when a new map is loaded, propogate the new name. 
				NewForecastReport n2 = (NewForecastReport) n; 
				currentForecastReport = n2.getTheReport();
				//labelUpdate(n2.getMapLoadedName());
				//updateMap(n2.getRoute().getTrailMarkers(), -1, DistanceUnit.KILOMETERS);
			//	JOptionPane.showMessageDialog(this, "New map: " + (((NewMapLoadedNotification) n).getMapLoadedName()));	
			}
			if(n.getClass() == NewLocationReportNotification.class){
				NewLocationReportNotification n2 = (NewLocationReportNotification) n;
				currentLocation = n2.getCarLocation().getLocation();
				
			}
			updateCharts();
			updateLabels();

		}
		
		/**
		 * register for any notifications that this class needs to
		 */
		@Override
		public void register(){
			mySession.register(this, NewForecastReport.class);
			mySession.register(this, NewLocationReportNotification.class);
			//add any notifications you need to listen for here. 
		}
		
		public void updateCharts(){
			
			buildTemperatureChart(createChartDataset(WeatherChartType.TEMPERATURE));
			temperatureChart.setChart(temperatureChartJFree);
			buildCloudCoverChart(createChartDataset(WeatherChartType.CLOUD_COVER));
			cloudCoverChart.setChart(cloudCoverChartJFree);
			buildPrecipitationChart(createChartDataset(WeatherChartType.PRECIPITATION));
			precipitationChart.setChart(precipitationChartJFree);
			buildWindSpeedChart(createChartDataset(WeatherChartType.WIND_SPEED));
			windSpeedChart.setChart(windSpeedChartJFree);
			
			temperatureChart.repaint();
			temperatureChart.revalidate();
			cloudCoverChart.repaint();
			cloudCoverChart.revalidate();
			precipitationChart.repaint();
			precipitationChart.revalidate();
			windSpeedChart.repaint();
			windSpeedChart.revalidate();
			contentPane.repaint();
			contentPane.revalidate();
			
		}
		
		public void updateLabels(){
			if(currentLocation != null){
				//next block figures out which forecast is closest to the current car location
				//and which one will be the next one in the sequence
				List<ForecastIO> forecasts = currentForecastReport.getForecasts();
				List<FIODataBlock> hourlyForecasts = new ArrayList<FIODataBlock>();
				ForecastIO closestForecast = forecasts.get(0);
				GeoCoord closestForecastLocation = new GeoCoord(closestForecast.getLatitude(),
						closestForecast.getLongitude(), 0.0);
				ForecastIO nextForecast = null;
				boolean nextForecastExists = false;
				if(forecasts.size() > 1){
					nextForecast = forecasts.get(1);
					nextForecastExists = true;
				}
				for(int i = 1; i < forecasts.size(); i++){
					GeoCoord curr = new GeoCoord(forecasts.get(i).getLatitude(),
							forecasts.get(i).getLongitude(), 0.0);
					if(currentLocation.calculateDistance(curr, DistanceUnit.KILOMETERS) <
							currentLocation.calculateDistance(closestForecastLocation, DistanceUnit.KILOMETERS)){
						closestForecast = forecasts.get(i);
						closestForecastLocation = curr;
						if(i == forecasts.size()-1){
							nextForecastExists = false;
						}else{
							nextForecast = forecasts.get(i+1);
						}
					}					
				}
				FIODataBlock closestHourly = new FIODataBlock(closestForecast.getHourly());
				FIODataPoint closestForecastNow = closestHourly.datapoint(0);
				FIODataBlock nextHourly;
				if(nextForecastExists){
					nextHourly = new FIODataBlock(nextForecast.getHourly());
				}
				
				double windBearing = closestForecastNow.windBearing();
				String windDirection = findDirection(windBearing);
				
				boolean fogWarning = false;
				String dewDifference = "";
				if(closestForecastNow.dewPoint()+DEW_POINT_DIFF >= closestForecastNow.temperature()){
					fogWarning = true;
					 double dewDifferenceNum = closestForecastNow.temperature()-closestForecastNow.dewPoint();
					 dewDifference = new DecimalFormat("#.##").format(dewDifferenceNum);
				}
				
				boolean stormWarning = false;
				if(closestForecastNow.nearestStormDistance() < 100 && 
						closestForecastNow.nearestStormDistance() >= 0){
					stormWarning = true;
				}
				
				windDirectionLabel.setText("Wind is blowing from: " + windDirection + " (" 
						+ windBearing + "�)");
				if(fogWarning){
					fogLabel.setText("Fog warning. (Temp is " + 
							dewDifference + "� above dew point.)");
				}else{
					fogLabel.setText("No fog warning");
				}
				if(stormWarning){
					double stormBearing = closestForecastNow.nearestStormBearing();
					String stormDirection = findDirection(stormBearing);
					stormLabel.setText("Warning: There's a storm " + closestForecastNow.nearestStormDistance()
							+ " km to the " + stormDirection + " (" + stormBearing + "�)");
				}
				
				
			}	
			windDirectionLabel.repaint();
			windDirectionPanel.repaint();
			fogLabel.repaint();
			fogPanel.repaint();
			stormLabel.repaint();
			stormPanel.repaint();
		}

		private String findDirection(double windBearing){
			String windDirection;
			if(windBearing == -1){
				windDirection = "none";
			}else if((windBearing >= 0 && windBearing < 22.5) || (windBearing >= 337.5 )){
				windDirection = "N";
			}else if(windBearing >= 22.5 && windBearing < 67.5){
				windDirection = "NE";
			}else if(windBearing >= 67.5 && windBearing < 112.5){
				windDirection = "E";
			}else if(windBearing >= 112.5 && windBearing < 157.5){
				windDirection = "SE";
			}else if(windBearing >= 157.5 && windBearing < 202.5){
				windDirection = "S";
			}else if(windBearing >= 202.5 && windBearing < 247.5){
				windDirection = "SW";
			}else if(windBearing >= 247.5 && windBearing < 292.5){
				windDirection = "W";
			}else{
				windDirection = "NW";
			}
			return windDirection;
		}
		
		private void buildDefaultChart(){
			XYDataset ds = createDataset();
			JFreeChart elevationChart = 
					ChartFactory.createXYLineChart(
							"Height Chart",
							"X_AXIS_LABEL",
							"Y_AXIS_LABEL", 
							ds,
							PlotOrientation.VERTICAL, true, true, false);
			temperatureChartJFree = elevationChart;
		}
		
		private void buildTemperatureChart(XYDataset ds){
			JFreeChart temperatureChart = 
					ChartFactory.createXYLineChart(
							"Temperature",
							X_AXIS_LABEL,
							"Temperature (Celsius)", 
							ds,
							PlotOrientation.VERTICAL, true, true, false);
			temperatureChartJFree = temperatureChart;
		}
		
		private void buildCloudCoverChart(XYDataset ds){
			JFreeChart cloudCoverChart = 
					ChartFactory.createXYLineChart(
							"Cloud Cover",
							X_AXIS_LABEL,
							"Cloud Cover (%)", 
							ds,
							PlotOrientation.VERTICAL, true, true, false);
			cloudCoverChartJFree = cloudCoverChart;
		}
		
		private void buildPrecipitationChart(XYDataset ds){
			JFreeChart precipitationChart = 
					ChartFactory.createXYLineChart(
							"Precipitation",
							X_AXIS_LABEL,
							"Chance of Precipitation (%)", 
							ds,
							PlotOrientation.VERTICAL, true, true, false);
			precipitationChartJFree = precipitationChart;
		}
		
		private void buildWindSpeedChart(XYDataset ds){
			JFreeChart headwindChart = 
					ChartFactory.createXYLineChart(
							"Wind Speed",
							X_AXIS_LABEL,
							"Wind Speed (km/h)", 
							ds,
							PlotOrientation.VERTICAL, true, true, false);
			windSpeedChartJFree = headwindChart;
		}
		
		private XYDataset createChartDataset(WeatherChartType chartType){
			if(currentForecastReport == null){
				return createBlankDataset();
			}else{
				DefaultXYDataset dds = new DefaultXYDataset();
				List<ForecastIO> forecastsForChart = currentForecastReport.getForecasts();
				forecastPoints = new ArrayList<GeoCoord>();
				
				List<FIODataBlock> hourlyForecasts = new ArrayList<FIODataBlock>();
				for(int i = 0; i < forecastsForChart.size(); i++){
					forecastPoints.add(new GeoCoord(forecastsForChart.get(i).getLatitude(), 
							forecastsForChart.get(i).getLongitude(), 0.0));//uses 0 for elevation cause it doesn't matter for our uses
					hourlyForecasts.add(new FIODataBlock(forecastsForChart.get(i).getHourly()));
				}
				
				distances = new double[forecastsForChart.size()];
				List<GeoCoord> trailMarkers = mySession.getMapController().getAllPoints().getTrailMarkers();
				int distanceIndex = 1;
				int trailMarkerIndex = 1;
				travelDistance = 0.0;
				distances[0] = travelDistance;
				while(distanceIndex < distances.length && trailMarkerIndex < trailMarkers.size()){
					travelDistance += trailMarkers.get(trailMarkerIndex-1).calculateDistance(
							trailMarkers.get(trailMarkerIndex), DistanceUnit.KILOMETERS);
					GeoCoord currentMarker = new GeoCoord(trailMarkers.get(trailMarkerIndex).getLat(),
							trailMarkers.get(trailMarkerIndex).getLon(), 0.0);
					if(currentMarker.equals(forecastPoints.get(distanceIndex))){
						forecastPoints.set(distanceIndex, trailMarkers.get(trailMarkerIndex));
						//updates GeoCoord of forecasts to include elevation, will make comparing
						//GeoCoords easier when calculating headwind
						distances[distanceIndex] = travelDistance;
						distanceIndex++;
					}
					trailMarkerIndex++;
				}
				
				int numHours = 0;
				
				while((numHours < NUM_LINES) && ( numHours < hourlyForecasts.size()) ){
					double[][] data = new double[2][distances.length];
					for(int i = 0; i < hourlyForecasts.size(); i++){
						data[0][i] = distances[i];
						FIODataPoint currentHourForecast = hourlyForecasts.get(i).datapoint(numHours);
						if(chartType.equals(WeatherChartType.TEMPERATURE)){
							data[1][i] = currentHourForecast.temperature();
						}else if(chartType.equals(WeatherChartType.CLOUD_COVER)){
							data[1][i] = currentHourForecast.cloudCover()*100;
						}else if(chartType.equals(WeatherChartType.PRECIPITATION)){
							data[1][i] = currentHourForecast.precipProbability()*100;
						}else if(chartType.equals(WeatherChartType.WIND_SPEED)){
							data[1][i] = currentHourForecast.windSpeed();
						}
					}
					dds.addSeries("Hour " + numHours, data);
					numHours++;
				}
				
				return dds;
			}
		}
		
		private XYDataset createDataset(){
			DefaultXYDataset dds = new DefaultXYDataset();
				double[][] data = { {0.1, 0.2, 0.3}, {1, 2, 3} };
				
				dds.addSeries("series1", data);
				return dds;
		}
		
		private XYDataset createBlankDataset(){
			DefaultXYDataset dds = new DefaultXYDataset();
			double[][] data = new double[2][0];
			dds.addSeries("", data);
			return dds;
		}
		
		public double getTravelDistance(){
			return travelDistance;
		}

}
