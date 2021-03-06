/**
 * this class forms the receiver for the data transmission from the car. 
 * */

package com.ubcsolar.car;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.CRC32;

import com.ubcsolar.common.TelemDataPacket;
import jssc.*;

public class XbeeSerialDataReceiver extends AbstractDataReceiver implements Runnable,SerialPortEventListener{ //needs to be threaded so it can listen for a response

	public final static byte BINMSG_SEPARATOR = (byte) 0xFF;
	public final static int BINMSG_LENGTH = 65;
	public final static int SERIAL_READ_BUF_SIZE = 100;
	
	// values from the last received data are cached in here...
	public TelemDataPacket lastLoadedDataPacket;

	private SerialPort serialPort;
	private byte[] serialReadBuf = new byte[SERIAL_READ_BUF_SIZE];
	private int serialReadBufPos = 0;
	private DataProcessor myDataProcessor;
	
	@Override
	void setName() {
		this.name = "Real Car";
	}
	
	private void setSerialPort(String serialPortName) throws SerialPortException{
		System.out.println(serialPortName);
		serialPort = new SerialPort(serialPortName);
		serialPort.openPort();
		serialPort.setParams(115200, 8, 1, 0);
		serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
	}
	
	public XbeeSerialDataReceiver(CarController toAdd, DataProcessor theProcessor, String serialPortName) throws SerialPortException{
		super(toAdd, theProcessor);
		myDataProcessor = theProcessor;
		setSerialPort(serialPortName);
	}
	
	/**
	 * default constructor.
	 * @param toAdd - the CarController to notify when it gets a new result
	 */ 
	public XbeeSerialDataReceiver(CarController toAdd, DataProcessor theProcessor) throws SerialPortException{
		super(toAdd, theProcessor);
		myDataProcessor = theProcessor;
		
		String[] portNames = SerialPortList.getPortNames();
		String serialPortName = "NO SERIAL PORT";
		if(portNames.length > 0)
			serialPortName = portNames[0]; //it always gets the first serial port available. 

		setSerialPort(serialPortName);
	}
	
	/**
	 * This method turns the binary data that we received into a TelemDataPacket for 
	 * transfer to the DataProcessor
	 * @param inData - the bytes up to the sync character received from the xbee serial port. 
	 */
	public void loadBinaryData(byte[] inData){
		@SuppressWarnings("unused")
		byte[] exampleData = {100, /* speed */
				44, /* total V */
				101, /* state of charge */
				40, 50, 36, 37, 38, 39, /* temperatures: bms, motor, pack1-pack4 */
				1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, /* cell V divided by 50: pack1 */
				13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
				25, 26, 27, 27, 29, 30, 31, 32, 33, 34, 35, 36,
				37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, /* cell V divided by 50: pack4 */
				'E', '0', 'D', '5', '2', 'D', 'F', '4' /* checksum for example data */
		};

		CRC32 crc32 = new CRC32();
		crc32.update(inData, 0, 57);
		
		long crc32FromData = 0;
		for(int i=0; i<8; i++){
			crc32FromData = crc32FromData << 4;
			if(inData[57+i] >= '0' && inData[57+i] <= '9')
				crc32FromData += inData[57+i] - '0';
			else
				crc32FromData += inData[57+i] - 'A' + 10;
		}
		
		if(crc32.getValue() != crc32FromData){
			System.out.println("Received a Corrupt Packet");
			System.out.printf("Expected checksum: 0x%08x Got checksum 0x%08x\n", crc32.getValue(), crc32FromData);
			// ignore corrupt packet
			return;
		}
		
		int i = 0;
		double speed = (double) (0xff & (int) inData[i++]);
		int totalVoltage = 0xff & (int) inData[i++];
		int stateOfCharge  = 0xff & (int) inData[i++];
		HashMap<String,Integer> mapForTemperatures = new HashMap<String,Integer>();
		mapForTemperatures.put("bms", 0xff & (int) inData[i++]);
		mapForTemperatures.put("motor", 0xff & (int) inData[i++]);
		for(int j = 0; j < 4; j++){
			mapForTemperatures.put("pack" + j, 0xff & (int) inData[i++]);
		}
		HashMap<Integer,ArrayList<Float>> mapForCellVoltages = new HashMap<Integer,ArrayList<Float>>();
		for(int j = 0; j < 4; j++){
			mapForCellVoltages.put(j, new ArrayList<Float>());
			for(int k = 0; k < 12; k++){
				mapForCellVoltages.get(j).add(((float) (0xff & (int) inData[i++])) / 50);
			}
		}
		
		TelemDataPacket newData = new TelemDataPacket(speed, totalVoltage, mapForTemperatures, mapForCellVoltages, stateOfCharge);
		this.lastLoadedDataPacket = newData;
		this.myDataProcessor.store(newData);
	}
	 	
	
	/**
	 * This class is made to be it's own thread so it can block waiting for a 
	 * new data packet from the car
	 */
	@Override
	public void run() {
		try {
			serialPort.addEventListener(this);
		} catch (SerialPortException e) {
			System.out.println(e);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
		
	public void stop()throws SerialPortException{
		
			serialPort.removeEventListener();
			serialPort.closePort();
		
	}

	/**
	 * 
	 * @return the name of the car loaded. 
	 */
	public String getName() {
		return name;
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		// we set event mask to SerialPort.MASK_RXCHAR so we don't check event type
		try {
			int bytesAvailable = event.getEventValue();
			while (bytesAvailable-->0) {
				serialReadBuf[serialReadBufPos] = (byte) serialPort.readBytes(1)[0];
				if(serialReadBuf[serialReadBufPos] == BINMSG_SEPARATOR){
					if(serialReadBufPos == BINMSG_LENGTH){
						loadBinaryData(serialReadBuf);
						if(this.lastLoadedDataPacket != null){
							System.out.println(this.lastLoadedDataPacket.toString());
						}
					}
					serialReadBufPos = 0;
				}else{
					serialReadBufPos = (serialReadBufPos + 1) % SERIAL_READ_BUF_SIZE;
				}
			}
		} catch (SerialPortException e) {System.out.println(e);}
	}

	/**
	 * For testing the serial code on its own 
	 */
	public static void main(String[] args) throws SerialPortException{
		XbeeSerialDataReceiver xsdr = new XbeeSerialDataReceiver(null, null);
		xsdr.run();
		return;
	}
}
