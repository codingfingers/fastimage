package com.codingfingers.fastimage.examples.flicr.remote;


import java.io.IOException;
import java.text.ParseException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;



public class EntryReader {

	
	public EntryVO readContent(XmlPullParser parser)  throws XmlPullParserException, IOException, ParseException
	{
		EntryVO entry = new EntryVO();
		
		boolean done = false;
		int parserDepth = parser.getDepth();	
		while(!done)
		{
			int eventType = parser.next();
			if(eventType == XmlPullParser.START_TAG)
			{
				String elementName = parser.getName();
				if(elementName.equals("media:thumbnail"))
				{
					entry.setUrl(parser.getAttributeValue(0));
				}
			}
			else if(eventType == XmlPullParser.END_TAG && parser.getName().equals("item"))
				done = true;
			else if(parserDepth > parser.getDepth())
				throw new ParseException("Xml content reader error", 0);			
		}		
	
		return entry;
	}
	
}
