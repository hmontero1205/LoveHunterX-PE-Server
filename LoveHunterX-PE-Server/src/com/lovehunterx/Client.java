package com.lovehunterx;

import java.net.InetSocketAddress;

public class Client {
	private static final short AFK_TIMEOUT = 300;

	private InetSocketAddress addr;
	private String username;
	private String room;
	private Integer sprite;
	private float x, y;
	private float velX, velY;
	private byte delta = 0b0000;
	private int afk;
	
	private String invited;
	private String invitedGame;
	
	private Minigame game;

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
	
	public void setGame(Minigame game) {
		this.game = game;
	}
	
	public Minigame getGame() {
		return game;
	}
	
	public boolean isInGame() {
		return game == null;
	}

	public void invite(String username, String game) {
		this.invited = username;
		this.invitedGame = game;
	}
	
	public boolean hasInvited(String u) {
		return u.equals(invited);
	}

	public String getInvitedGame() {
		return invitedGame;
	}

	public void joinRoom(String room) {
		this.room = room;
		setX(0);
		setY(0);
		setVelocityX(0);
		setVelocityY(0);
	}

	public String getRoom() {
		return room;
	}

	public boolean isInRoom(String room) {
		return this.room != null && this.room.equals(room);
	}

	public void update(float delta) {
		if (afk < AFK_TIMEOUT) {
			afk++;
		}

		this.setX(this.getX() + this.getVelocityX() * 100 * delta);
		this.setY(Math.max(0, this.getY() + this.getVelocityY() * 200 * delta));
		this.setVelocityY(this.y != 0 ? this.getVelocityY() - (2F * delta) : 0);
	}

	public void resetAFK() {
		afk = 0;
	}

	public boolean isAFK() {
		return afk == AFK_TIMEOUT;
	}

	public void setSprite(Integer i) {
		this.sprite = i;
	}

	public Integer getSprite() {
		return sprite;
	}

	public Packet getDeltaUpdate() {
		Packet packet = new Packet("move");
		packet.addData("user", username);

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

	public void setDelta(int index) {
		byte mask = (byte) (0b0001 << index);
		delta |= mask;
	}

	public void setX(float x) {
		if (this.x != x) {
			setDelta(3);
		}

		this.x = x;
	}

	public void setY(float y) {
		if (this.y != y) {
			setDelta(2);
		}

		this.y = y;
	}

	public void setVelocityX(float velX) {
		if (this.velX != velX) {
			setDelta(1);
		}

		this.velX = velX;
	}

	public void setVelocityY(float velY) {
		if (this.y == 0 && this.velY != velY) {
			setDelta(0);
		}

		this.velY = velY;
	}

	public void clearDelta() {
		delta = 0;
	}

}