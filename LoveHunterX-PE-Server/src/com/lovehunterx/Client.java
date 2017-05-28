package com.lovehunterx;
import java.net.InetSocketAddress;

public class Client {
	private InetSocketAddress addr;
	
	private String username;
	private String room;

	private float x, y;
	private int direction;

	public Client(InetSocketAddress addr) {
		this.addr = addr;
	}
	
	public InetSocketAddress getAddress() {
		return addr;
	}

	public void login(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
	
	public int getDirection() {
		return direction;
	}

	public void setDirection(int dir) {
		this.direction = dir;
	}

	public void move(float deltaTime) {
		if (direction == 0) {
			return;
		}

		x += direction * 30 * (deltaTime / 1);
	}

	public void joinRoom(String room) {
		this.room = room;
	}

	public String getRoom() {
		return room;
	}

	public boolean isInRoom(String room) {
		return this.room != null && this.room.equals(room);
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

}
