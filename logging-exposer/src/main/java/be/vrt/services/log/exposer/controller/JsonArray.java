package be.vrt.services.log.exposer.controller;

import java.util.LinkedList;

@SuppressWarnings("serial")
public class JsonArray extends LinkedList<Object> {
	
	public static JsonArray with(Object... args) {
		JsonArray jsonArray = new JsonArray();
		for (Object arg : args) {
			jsonArray.add(arg);
		}
		return jsonArray;
		
	}
}
