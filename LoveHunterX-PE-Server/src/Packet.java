import java.util.HashMap;

import org.json.simple.JSONObject;

public class Packet {
	private String action;
	private HashMap<String, String> data;
	
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
	
	public static Packet createAuthPacket(boolean success) {
		HashMap<String, String> authData = new HashMap<String, String>();
		authData.put("success", String.valueOf(success));
		return new Packet("auth", authData);
	}
	
	public String toJSON() {
		JSONObject obj = new JSONObject();
		obj.put("action", this.action);
		obj.put("data", this.data);
		return obj.toJSONString();
	}

	public static Packet createRegPacket(boolean success) {
		HashMap<String, String> regData = new HashMap<String, String>();
		regData.put("success", String.valueOf(success));
		return new Packet("reg", regData);
	}
}
