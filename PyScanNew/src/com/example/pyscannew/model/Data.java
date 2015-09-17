package com.example.pyscannew.model;

public class Data {
	
	private int id;
	private String value;
	public static final String ID = "_id";
	public static final String VALUE = "value";
	
	public Data(String value) {
		this.value = value;
	}
	
	public Data() {
		
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
