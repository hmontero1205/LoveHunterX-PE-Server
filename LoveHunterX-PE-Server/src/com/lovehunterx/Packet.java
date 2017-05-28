package com.lovehunterx;
import java.util.HashMap;

import org.json.simple.JSONObject;

public class Packet {
	private String action;
	private HashMap<String, String> data;

	@SuppressWarnings("unchecked")
	public Packet(JSONObject j) {
		this.action = (String) j.get("action");
		this.data = (HashMap<String, String>) j.get("data");
	}

	public Packet(String a, HashMap<String, String> d) {
		this.action = a;
		this.data = d;
	}

	public String getAction() {
		return this.action;
	}

	public String getData(String key) {
		return this.data.get(key);
	}

	public void addData(String key, String value) {
		this.data.put(key, value);
	}

	public void removeData(String key) {
		this.data.remove(key);
	}

	public static Packet createAuthPacket(boolean success) {
		HashMap<String, String> authData = new HashMap<String, String>();
		authData.put("success", String.valueOf(success));
		return new Packet("auth", authData);
	}

	public static Packet createRegPacket(boolean success) {
		HashMap<String, String> regData = new HashMap<String, String>();
		regData.put("success", String.valueOf(success));
		return new Packet("reg", regData);
	}

	public static Packet createJoinPacket(String user, String room, float x, float y) {
		HashMap<String, String> joinData = new HashMap<String, String>();
		joinData.put("user", user);
		joinData.put("room", room);
		joinData.put("x", String.valueOf(x));
		joinData.put("y", String.valueOf(y));
		return new Packet("join", joinData);
	}

	public static Packet createLeavePacket(String user, String room) {
		HashMap<String, String> leaveData = new HashMap<String, String>();
		leaveData.put("user", user);
		leaveData.put("room", room);
		return new Packet("leave", leaveData);
	}

	public String toJSON() {
		HashMap<String, Object> obj = new HashMap<String, Object>();
		obj.put("action", this.action);
		obj.put("data", this.data);

		return new JSONObject(obj).toJSONString();
	}
}
