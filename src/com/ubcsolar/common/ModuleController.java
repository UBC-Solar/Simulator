/**
 * Every model module must have a controller, and this controller forms the backbone
 * of what they must include
 */
package com.ubcsolar.common;

import com.ubcsolar.Main.GlobalController;
import com.ubcsolar.notification.Notification;

public abstract class ModuleController implements Listener {
	
	
	protected GlobalController mySession; //reference to the Global Controller
	
	/**
	 * Constructor, calls register() and creates the Global Controller reference;
	 * @param toAdd - the GlobalController to reference/send notificatoins to
	 */
	public ModuleController(GlobalController myGlobalController){
		mySession = myGlobalController;
		register();
		
	}
	
	/**
	 * Sends notifications to the Global Controller
	 * @param n - the notification to send
	 */
	public void sendNotification(Notification n){
		mySession.sendNotification(n); 
	}
	


}
