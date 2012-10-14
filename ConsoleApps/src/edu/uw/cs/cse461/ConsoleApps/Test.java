package edu.uw.cs.cse461.ConsoleApps;

import org.json.JSONException;
import org.json.JSONObject;

public class Test {
	public static void main(String[] args) throws JSONException {
		JSONObject jso = new JSONObject();
		jso.append("hi", "here");
		System.out.println(jso.toString());
		System.out.println(new JSONObject(jso.toString()));
		System.out.println(new JSONObject( new String(jso.toString().getBytes())));
		
	}
}
