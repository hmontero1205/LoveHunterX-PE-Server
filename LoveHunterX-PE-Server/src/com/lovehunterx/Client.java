package com.lovehunterx;

import java.net.InetSocketAddress;

public class Client {
	private InetSocketAddress addr;
	private String username;
	private String room;
	private float x, y;
	private float velX, velY;
	private byte delta = 0b0000;

	public Client(InetSocketAddress addr) {
		this.addr = addr;
	}

	public InetSocketAddress getAddress() {
		return addr;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
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

	public void update(float delta) {
		this.setX(this.getX() + this.getVelocityX() * 100 * delta);
		this.setY(Math.max(0, this.getY() + this.getVelocityY() * 200 * delta));
		this.setVelocityY(this.y != 0 ? this.getVelocityY() - (2F * delta) : 0);
	}

	public Packet getDeltaUpdate() {
		Packet packet = new Packet("move");
		packet.addData("user", username);
		packet.addData("room", room);

		if (hasDelta(3)) {
			packet.addData("x", String.valueOf(x));
		}

		if (hasDelta(1)) {
			packet.addData("vel_x", String.valueOf(velX));
		}

		if (hasDelta(2)) {
			packet.addData("y", String.valueOf(y));
		}

		if (hasDelta(0)) {
			packet.addData("vel_y", String.valueOf(velY));
		}

		return delta == 0 ? null : packet;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getVelocityX() {
		return velX;
	}

	public float getVelocityY() {
		return velY;
	}

	public boolean hasDelta(int index) {
		return (delta >> index & 0b0001) == 0b0001;
	}

	public void setX(float x) {
		if (this.x != x) {
			delta |= 0b1000;
		}

		this.x = x;
	}

	public void setY(float y) {
		if (this.y != y) {
			delta |= 0b0100;
		}

		this.y = y;
	}

	public void setVelocityX(float velX) {
		if (this.velX != velX) {
			delta |= 0b0010;
		}

		this.velX = velX;
	}

	public void setVelocityY(float velY) {
		if (this.y == 0 && this.velY != velY) {
			delta |= 0b0001;
		}

		this.velY = velY;
	}

	public void clearDelta() {
		delta = 0;
	}

}