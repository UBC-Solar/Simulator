package com.ubcsolar.ui;

import java.awt.Color;
import java.awt.Component;
import java.text.SimpleDateFormat;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import com.eclipsesource.json.JsonObject;
import com.ubcsolar.Main.GlobalValues;

/**
 * 
 * @author Jacob
 *
 * This renderer should only be used with JLists containing forecast datapoints
 * WITH A TIME FIELD!!! If you don't have a time field in your JsonObject, you will
 * get hella NullPointerExceptions when you try to use this renderer (also it will not
 * render successfully). JsonObject is honestly probably not the best class to be doing this
 * with, but our ForecastIO API kinda sucks, so whatcha gonna do (besides write your own
 * ForecastIO stuff)
 */

public class ForecastDatapointRenderer extends JLabel implements ListCellRenderer<JsonObject> {
	public ForecastDatapointRenderer() {
		setOpaque(true);
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends JsonObject> list, JsonObject value, 
			int index, boolean isSelected, boolean cellHasFocus) {
		
		SimpleDateFormat sdf = GlobalValues.forecastIODateParser;
		String date = sdf.format(value.get("time").asLong()*1000);//have to multiply by 1000 or date will be wrong
		
		setText(date);

		Color background;
		Color foreground;

		if (isSelected) {
			background = Color.BLUE;
			foreground = Color.WHITE;
		} else {
			background = Color.WHITE;
			foreground = Color.BLACK;
		}

		setBackground(background);
		setForeground(foreground);

		return this;
	}

}
