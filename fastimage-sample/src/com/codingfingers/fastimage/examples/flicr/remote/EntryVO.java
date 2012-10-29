package com.codingfingers.fastimage.examples.flicr.remote;

public class EntryVO {
	
	private String url;
	
	public void setUrl(String url)
	{
		this.url = url;
	}
	
	public String getUrl()
	{
		return url;
	}

	
	public String getThumb()
	{
		return url;//.replaceAll("_b.jpg", "_m.jpg");
	}
	
}
