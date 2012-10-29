package com.codingfingers.fastimage.examples.flicr.remote;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;



public class EntryListReader{

	public ArrayList<EntryVO> readContent(XmlPullParser parser)  throws XmlPullParserException, IOException, ParseException
	{
		ArrayList<EntryVO> entryList = new ArrayList<EntryVO>();
		EntryReader entryReader = new EntryReader();
		
		boolean done = false;
		int parserDepth = parser.getDepth();		
		while(!done)
		{
			int eventType = parser.next();
			if(eventType == XmlPullParser.START_TAG)
			{
				String elementName = parser.getName();
				if(elementName.equals("item"))
				{
					EntryVO entry = entryReader.readContent(parser);
					if(entry != null)
						entryList.add(entry);
				}
				
			}
			else if(eventType == XmlPullParser.END_TAG &&  (parser.getName().equals("rss")))
				done = true;
			else if(parserDepth > parser.getDepth())
				throw new ParseException("Xml content reader error", 0);						
		}		
		
		
		return entryList;
	}
	
}