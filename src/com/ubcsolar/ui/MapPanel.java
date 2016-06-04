package com.ubcsolar.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.ubcsolar.Main.GlobalController;
import com.ubcsolar.common.Listener;
import com.ubcsolar.common.LogType;
import com.ubcsolar.common.SolarLog;
import com.ubcsolar.notification.NewMapLoadedNotification;
import com.ubcsolar.notification.Notification;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.Font;

public class MapPanel extends JPanel implements Listener {
	
	protected GUImain parent;
	private GlobalController mySession;
	private JLabel label_1;

	public MapPanel(GUImain main, GlobalController session) {
	mySession = session;
	parent = main;
	this.setBorder(BorderFactory.createLineBorder(Color.black));
	setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		
		JLabel label = new JLabel("Map");
		panel.add(label);
		
		JButton button = new JButton("Advanced");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parent.launchMap();
			}
		});
		panel.add(button);

		
		JPanel panel_1 = new JPanel();
		add(panel_1, BorderLayout.CENTER);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
		gbl_panel_1.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);
		
		JLabel lblMapLoaded = new JLabel("Map Loaded: ");
		GridBagConstraints gbc_lblMapLoaded = new GridBagConstraints();
		gbc_lblMapLoaded.insets = new Insets(0, 0, 0, 5);
		gbc_lblMapLoaded.gridx = 3;
		gbc_lblMapLoaded.gridy = 6;
		panel_1.add(lblMapLoaded, gbc_lblMapLoaded);
		
		label_1 = new JLabel("");
		label_1.setFont(new Font("Tahoma", Font.BOLD, 12));
		GridBagConstraints gbc_label_1 = new GridBagConstraints();
		gbc_label_1.insets = new Insets(0, 0, 0, 5);
		gbc_label_1.gridx = 5;
		gbc_label_1.gridy = 6;
		panel_1.add(label_1, gbc_label_1);
		
		label_1.setText("NONE");
		register(); //do last, in case a notification is sent before we're done building.
	
		
	}

	private void updateMapLabel(String mapName){
		label_1.setText(mapName);
	}
	
	@Override
	public void notify(Notification n) {
		// TODO Auto-generated method stub
		if(n.getClass() == NewMapLoadedNotification.class){
		updateMapLabel(((NewMapLoadedNotification) n).getMapLoadedName());
		}
	}
	@Override
	public void register() {
		// TODO Auto-generated method stub
		mySession.register(this,  NewMapLoadedNotification.class);
		
	}
}

