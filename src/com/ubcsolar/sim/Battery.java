package com.ubcsolar.sim;

/**
This class models the battery. The important features are:
- display the battery's temperature and state of charge at set time iterations
- show error if it overheats
- show error if it overcharges
*/


public class Battery{
	
//---------------------------------------CLASS FIElDS--------------------------------------------------------------------------------------
private int temperature; /** temperature of batteries in degrees C */													
private double maxChargeCapacity; /**max charge of the battery (in amp-hours) */ 
private double batteryVoltage; /** nominal voltage of battery (in volts) */
private double storedEnergy; /** the current remaining electric energy in the battery (in watt-hours) */
private double maxStoredEnergy; /** electric energy stored in the battery when it's 100% charged */
private double time;
//-----------------------------------------------------------------------------------------------------------------------------------------


//------------------------------------SETTER & GETTER METHODS------------------------------------------------------------------------------
/** Constructs a default battery, at 50% charged state.
 *  Values for maxChargeCapacity & batteryVoltage are taken from manufacturer's product data sheet.
 *  Total stored electric energy in batteries is given by:  Ah*V = Wh		(charge capacity*voltage = energy)
 *  @todo Confirm this assumption: that the battery's voltage starts out at 40.15V
 */
public Battery(){					// TODO: add parameters to set state of charge, instead of automatic 100%
	maxChargeCapacity = 220;				 
	batteryVoltage = 40.15;
	maxStoredEnergy = maxChargeCapacity*batteryVoltage;
	storedEnergy = 0.5*maxStoredEnergy;
	Log.write("Battery created.");
}

/** @todo get properties and model from file */
public Battery(String fileName){					// TODO: add model file for battery
	maxChargeCapacity = 220;				 
	batteryVoltage = 40.15;
	maxStoredEnergy = maxChargeCapacity*batteryVoltage;
	storedEnergy = 0.5*maxStoredEnergy;
	Log.write("Battery created");
}

/** Sets the state of charge of battery. */
public void setStateOfCharge(double SOC){
	storedEnergy = (SOC/100)*maxStoredEnergy;
}

/** Returns the current state of charge of battery, as a percentage. */
public double getStateOfCharge(){
	return (storedEnergy/maxStoredEnergy)*100; 
}

/** Returns the stored energy in the battery. (watt-hours) */
public double getStoredEnergy(){
	return storedEnergy;
}


/** Returns the battery's voltage. */
public double getBatteryVoltage(){
	return batteryVoltage;
}


/** Predict the next state of the battery. 
 * @param time - the time interval (in seconds)
 * @todo need to finish this method
 */
public void nextBattery(double time, Environment worldEnviro, Boolean doLog){
}


//------------------------------------------------------------------------------------------------------------------------------------------


//-----------------------------------------CALCULATION METHODS------------------------------------------------------------------------------
/** Calculates the maximum time the battery could recharge for, before it overcharges. (in seconds)
 * Should display error if the specified charge is over the battery's max charge capacity
 * @param currentRecharge - the current the battery will be charged at, specified by the electrical controller (in amps)
 * Charging time is given by: (maxWh-currentWh)/(V*A)
 * @todo Incorporate charging efficiency, temperature, other factors that will affect the charging time
 * */
public double getMaxRechargeTime(double currentRecharge){
	double rechargeTime;
	if (currentRecharge > maxChargeCapacity){
		Log.write("Cannot exceed max charge capactiy");
		return -1.0;
	}
	if (currentRecharge <= 0){
		Log.write("Error: negative current given");
		return -1.0;				
	}
	else{
		rechargeTime =(maxStoredEnergy-storedEnergy)/(batteryVoltage*currentRecharge )*3600;
		return rechargeTime;
	}
}

/** Calculates what current the battery can provide (in amps), considering it's current state of charge.
 * @param chargeTime - time period (in seconds) for which the battery will be providing this current; specified by electrical controller
 * Confirm this assumption: Wh/(V*t) = A
 * @todo Incorporate charging efficiency, temperature, other factors that will be of influence
 * @todo Apply limitation to battery's maximum discharge capacity
 * */
public double getCurrent(double chargeTime){ 
	double currentCharge;
	currentCharge = storedEnergy/(chargeTime/3600 * batteryVoltage);
	return currentCharge;
}

/** Calculates heat produced from the charge/discharge
 * Should consider ambient temperature, battery's temperature, heat from adjacent batteries in the pack
 * @todo Need to add calculations
 */
public double heatFromCharge(double current, double chargeTime){
	double heatFromCharge=0;
	// need calculations here
	return heatFromCharge;
}

/** Stores energy (in watt-hrs)
 * @todo Incorporate efficiency
 * @todo Incorporate calculates that changes the batteryVoltage since voltage varies according to battery's state of charge
 */
public void storeEnergy(double time, double current){
	if (current <= 0){
		Log.write("Error: negative current given");
		return;
	}
	else{
	storedEnergy = storedEnergy + (current*batteryVoltage*time)/3600;
	}
}

/** Draws energy (in watt-hrs)
 * @todo Incorporate efficiency
 * @todo Incorporate calculates that changes the batteryVoltage since voltage varies according to battery's state of charge
 */
public void drawEnergy(double time, double current){
	if (current <= 0){
		Log.write("Error: negative current given");
		return;
	}
	else{
	storedEnergy = storedEnergy - (current*batteryVoltage*time)/3600;
	}
}
//-----------------------------------------------------------------------------------------------------------------------------------------


}

