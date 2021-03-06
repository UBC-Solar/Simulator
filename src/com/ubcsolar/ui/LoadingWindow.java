			package com.ubcsolar.ui;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.ubcsolar.Main.GlobalController;
import com.ubcsolar.Main.GlobalValues;
import com.ubcsolar.common.Listener;
import com.ubcsolar.common.LogType;
import com.ubcsolar.common.SolarLog;
import com.ubcsolar.notification.Notification;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.HeadlessException;
import java.awt.Window.Type;
import java.awt.Dialog.ModalExclusionType;


public class LoadingWindow extends JFrame  {
		
	private GlobalController mySession ; 
	

	public LoadingWindow(GlobalController toAdd) {
		
		this.mySession = toAdd;
		
		setBounds(760, 390, 450, 50);
		setLocationRelativeTo(null);
		setTitleAndLogo();
		
	    ImageIcon loading = new ImageIcon("res/ajax-loader.gif");

	    getContentPane().add(new JLabel("Loading. Please wait for a moment... ", loading, JLabel.CENTER));
		
	    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setResizable(false);

	}
	
	private void setTitleAndLogo() {
		setIconImage(GlobalValues.iconImage.getImage()); //centrally stored image for easy update (SPOC!)
		setTitle("Loading. Please wait a moment...");
	}
}
