package com.codingfingers.fastimage.examples.flicr.utils;


public class IOUtils {
	
	public static final String convertStreamToString(java.io.InputStream is) {
	    try {
	        return new java.util.Scanner(is).useDelimiter("\\A").next();
	    } catch (java.util.NoSuchElementException e) {
	        return "";
	    }
	}
	
	
}
