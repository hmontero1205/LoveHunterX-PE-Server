package com.lovehunterx;

public class TicTacToe extends Minigame {

	private byte[][] board;
	private boolean playerOneTurn;
	private boolean playerTwoTurn;

	public TicTacToe(Client one, Client two) {
		super(one, two);

		one.setGame(this);
		two.setGame(this);

		board = new byte[3][3];
	}

	@Override
	public void handle(Packet p, Client sender) {
		if (!(getPlayerOne().equals(sender) && playerOneTurn || getPlayerTwo().equals(sender) && playerTwoTurn)) {
			return;
		}

		int i = Integer.parseInt(p.getData("row"));
		int j = Integer.parseInt(p.getData("col"));

		if (getPlayerOne().equals(sender)) {
			board[i][j] = 1;
			playerOneTurn = false;
			playerTwoTurn = true;
		} else {
			board[i][j] = 2;
			playerOneTurn = true;
			playerTwoTurn = false;
		}

		Server.send(p, getOpponent(sender).getAddress());

		Packet packet = new Packet("game_end");
		if (hasWon(board[i][j], i, j)) {
			packet.addData("result", "won");
			packet.addData("opp", getOpponent(sender).getUsername());
			Server.send(packet, sender.getAddress());
			packet.addData("opp", sender.getUsername());
			packet.addData("result", "lost");
			Server.send(packet, getOpponent(sender).getAddress());

			double money = Server.db.getMoney(sender.getUsername());
			Server.db.updateMoney(sender.getUsername(), String.valueOf(money + 50));
			
			getPlayerOne().setGame(null);
			getPlayerTwo().setGame(null);
		} else if (hasTied()) {
			packet.addData("result", "tied");
			packet.addData("opp", sender.getUsername());
			Server.send(packet, getOpponent(sender).getAddress());

			packet.addData("opp", getOpponent(sender).getUsername());
			Server.send(packet, sender.getAddress());

			getPlayerOne().setGame(null);
			getPlayerTwo().setGame(null);
		} else {
			Packet turn = Packet.createNotifcationPacket("Your turn!");
			Server.send(turn, getOpponent(sender).getAddress());
			Packet wait = Packet.createNotifcationPacket("Waiting for opponent...");
			Server.send(wait, sender.getAddress());
		}
	}

	// xD don't look here
	public boolean hasWon(int mode, int row, int col) {
		int win = 0;
		for (int i = 0; i < 3; i++) {
			if (board[i][col] == mode) {
				win++;
			}
		}

		if (win == 3) {
			return true;
		}

		win = 0;
		for (int i = 0; i < 3; i++) {
			if (board[row][i] == mode) {
				win++;
			}
		}

		if (win == 3) {
			return true;
		}

		win = 0;
		for (int i = 0; i < 3; i++) {
			if (board[3 - i - 1][i] == mode) {
				win++;
			}
		}

		if (win == 3) {
			return true;
		}

		win = 0;
		for (int i = 0; i < 3; i++) {
			if (board[i][i] == mode) {
				win++;
			}
		}

		if (win == 3) {
			return true;
		}

		return false;
	}

	public boolean hasTied() {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (board[i][j] == 0) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void init() {
		Packet packet = new Packet("start_game");
		packet.addData("game", "ttt");
		packet.addData("start", "true");
		Server.send(packet, getPlayerOne().getAddress());

		packet.addData("start", "false");
		Server.send(packet, getPlayerTwo().getAddress());

		playerOneTurn = true;
		playerTwoTurn = false;
	}

	@Override
	public void disconnect(Client cli) {
		Packet packet = new Packet("game_end");
		packet.addData("opp", cli.getUsername());
		packet.addData("result", "dc");
		Server.send(packet, getOpponent(cli).getAddress());

		getPlayerOne().setGame(null);
		getPlayerTwo().setGame(null);
	}
}
