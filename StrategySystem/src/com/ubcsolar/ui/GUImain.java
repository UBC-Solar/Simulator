package com.ubcsolar.ui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SpringLayout;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.ubcsolar.car.CarUpdateNotification;
import com.ubcsolar.common.Listener;
import com.ubcsolar.common.Notification;
import com.ubcsolar.map.NewMapLoadedNotification;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class GUImain implements Listener{

	private JFrame frame;
	private GlobalController mySession; 
	private JLabel loadedMapName;
	private JLabel carSpeed;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUImain window = new GUImain();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUImain() {
		initialize();
	}
	
	@Override
	public void notify(Notification n){
		
		if(n.getClass() == NewMapLoadedNotification.class){
			this.loadedMapName.setText(((NewMapLoadedNotification) n).getMapLoadedName());
			System.out.println("IT WORKED!!!");
			//JOptionPane.showMessageDialog(frame, "New map: " + (((NewMapLoadedNotification) n).getMapLoadedName()));
		}
		else if(n.getClass() == CarUpdateNotification.class){

			if(this.carSpeed == null){
				carSpeed = new JLabel("test");
			}
			else{
				this.carSpeed.setText("Car speed: " + ((CarUpdateNotification) n).getNewCarSpeed());
			}
			System.out.println("test");
		}
		//TODO: Do something when notified. 
		
	}
	
	private void registerListeners(){
		mySession.register(this, NewMapLoadedNotification.class);
		mySession.register(this, CarUpdateNotification.class);
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		mySession = new GlobalController(this);
		registerListeners();
		frame = new JFrame();
		frame.setBounds(100, 100, 485, 347);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new SpringLayout());
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenu mnModules = new JMenu("Modules");
		menuBar.add(mnModules);
		
		JMenuItem mntmMap = new JMenuItem("Map");
		mntmMap.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("test");
			/*	try{
				mySession.getMapController().load("res\\ASC2014ClassicMapFull.kml");
				}
				catch(IOException ex){
					JOptionPane.showMessageDialog(frame, ex.getMessage() + " Could not load map");
				}*/
				
				JFrame frame = new Map(mySession);
				frame.setVisible(true);
			}
		});
		mnModules.add(mntmMap);
		
		JMenuItem mntmSimulator = new JMenuItem("Simulation");
		mntmSimulator.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFrame frame = new Simulation();
				frame.setVisible(true);
			}
		});
		
		JMenuItem mntmWeather = new JMenuItem("Weather");
		mntmWeather.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFrame frame = new Weather();
				frame.setVisible(true);
			}
		});
		mnModules.add(mntmWeather);
		mnModules.add(mntmSimulator);
		
		JMenuItem mntmPerformance = new JMenuItem("Performance");
		mntmPerformance.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFrame frame = new Performance();
				frame.setVisible(true);
			}
		});
		mnModules.add(mntmPerformance);
		
		JMenuItem mntmStrategy = new JMenuItem("Strategy");
		mntmStrategy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frame = new Strategy();
				frame.setVisible(true);
			}
		});
		mnModules.add(mntmStrategy);
		this.loadedMapName = new JLabel("None");
		this.carSpeed = new JLabel("test");
		frame.getContentPane().add(loadedMapName);
		
		frame.getContentPane().add(carSpeed);
		
	}

	@Override
	public void register() {
		// TODO Auto-generated method stub
		
	}
}
