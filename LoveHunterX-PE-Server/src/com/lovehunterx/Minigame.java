package com.lovehunterx;

public abstract class Minigame {
	private Client playerOne;
	private Client playerTwo;
	
	public Minigame(Client one, Client two) {
		this.playerOne = one;
		this.playerTwo = two;
	}
	
	public Client getPlayerOne() {
		return playerOne;
	}
	
	public Client getPlayerTwo() {
		return playerTwo;
	}
	
	public abstract void handle(Packet p, Client sender);
	
	public abstract void init();
	
	public abstract void disconnect(Client cli);
	
	public Client getOpponent(Client player) {
		return playerOne.equals(player) ? playerTwo : playerOne;
	}
}
