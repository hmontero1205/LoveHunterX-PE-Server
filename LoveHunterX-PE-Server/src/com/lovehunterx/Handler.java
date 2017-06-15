package com.lovehunterx;

import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

public class Handler extends SimpleChannelInboundHandler<DatagramPacket> {
	private InetSocketAddress sender;

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
		this.sender = packet.sender();

		String message = packet.content().toString(CharsetUtil.US_ASCII);
		try {
			interpretInput(ctx, message);
		} catch (ParseException e) {
		}
	}

	private void interpretInput(ChannelHandlerContext ctx, String message) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(message);
		Packet p = new Packet(obj);
		switch (p.getAction()) {
		case "alive":
			handleAlive();
			break;
		case "auth":
			handleAuthentication(p);
			break;
		case "reg":
			handleRegistration(p);
			break;
		case "join":
			handleJoin(p);
			break;
		case "chat":
			handleChat(p);
			break;
		case "move":
			handleMovement(p);
			break;
		case "choose_sprite":
			handleChooseSprite(p);
			break;
		case "disconnect":
			handleLeave();
			break;
		case "update_furniture":
			handleSetFurniture(p);
			break;
		case "remove_furniture":
			handleRemoveFurniture(p);
			break;
		case "get_money":
			handleGetMoney(p);
			break;
		case "purchase":
			handlePurchase(p);
			break;
		case "status":
			handleStatusCheck();
			break;
		case "invite":
			handleInvite(p);
			break;
		case "decision":
			handleDecision(p);
			break;
		case "choose_move":
			Client cli = Server.getState().getClient(sender);
			if (cli == null) {
				break;
			} else if (cli.getGame() != null) {
				cli.getGame().handle(p, cli);
			}

			break;
		}
		
		if (!p.getAction().equals("move")) {
			System.out.println(message);
		}
	}

	private void handleDecision(Packet p) {
		Client cli = Server.getState().getClient(sender);
		if (cli == null || cli.getGame() != null) {
			return;
		}

		Client target = Server.getState().getClient(p.getData("player"));
		if (target == null || !target.hasInvited(cli.getUsername()) || target.getGame() != null) {
			return;
		}

		if (p.getData("choice").equals("true")) {
			Minigame game = null;
			if (target.getInvitedGame().equals("ttt")) {
				game = new TicTacToe(target, cli);
			} else if (target.getInvitedGame().equals("wam")) {
				return;
			}
			game.init();
			
			cli.invite(null, null);
			target.invite(null, null);

			Packet packet = Packet.createNotifcationPacket(cli.getUsername() + " has accepted your invitation");
			Server.send(packet, target.getAddress());
		} else {
			Packet packet = Packet.createNotifcationPacket(cli.getUsername() + " has declined your invitation");
			Server.send(packet, target.getAddress());
		}
	}

	private void handleInvite(Packet p) {
		Client cli = Server.getState().getClient(sender);
		if (cli == null) {
			return;
		}
		
		Client target = Server.getState().getClient(p.getData("player"));
		if (target == null) {
			return;
		}

		cli.invite(target.getUsername(), p.getData("game"));
		p.addData("player", cli.getUsername());
		Server.send(p, target.getAddress());
	}

	private void handlePurchase(Packet p) {
		Server.db.updateMoney(p.getData("user"), p.getData("money"));
		Server.db.addToInventory(p.getData("user"), p.getData("type"));
		Server.send(p, sender);
	}

	private void handleGetMoney(Packet p) {
		double money = Server.db.getMoney(p.getData("user"));
		p.addData("money", Double.toString(money));
		Server.send(p, sender);
	}

	private void handleSetFurniture(Packet p) {
		Client client = Server.getState().getClient(sender);
		String uid = Server.db.setFurniture(p.getData("x"), p.getData("y"), p.getData("uid"), p.getData("type"),
				client.getUsername());
		p.addData("uid", uid);
		if (uid == null) {
			return;
		}

		for (Client other : Server.getState().getClients()) {
			if (!other.isInRoom(client.getRoom())) {
				continue;
			}

			Server.send(p, other.getAddress());
		}
	}

	private void handleRemoveFurniture(Packet p) {
		Client client = Server.getState().getClient(sender);
		Server.db.removeFurniture(p.getData("uid"), p.getData("type"), client.getUsername());
		for (Client other : Server.getState().getClients()) {
			if (!other.isInRoom(client.getRoom())) {
				continue;
			}

			Server.send(p, other.getAddress());
		}
	}

	private void handleRegistration(Packet p) {
		boolean success = Server.db.register(p.getData("user"), p.getData("pass"));
		Packet regPacket = Packet.createRegPacket(success);
		Server.send(regPacket, sender);
	}

	private void handleAuthentication(Packet p) {
		boolean success = Server.db.authenticate(p.getData("user"), p.getData("pass"))
				&& !Server.getState().isLoggedIn(p.getData("user"));

		Packet authPacket = Packet.createAuthPacket(success);

		if (success) {
			Client cli = new Client(sender);
			cli.setUsername(p.getData("user"));

			Integer sprite = Server.db.getSprite(cli.getUsername());
			cli.setSprite(sprite);

			if (sprite == null) {
				authPacket.addData("require_sprite", "true");
			}

			Server.getState().addClient(cli);
		}

		Server.send(authPacket, sender);
	}

	private void handleJoin(Packet p) {
		Client c = Server.getState().getClient(sender);
		if (c == null) {
			return;
		}

		Server.disconnect(sender);

		String room = p.getData("room");
		c.joinRoom(room);

		updateInventory(sender);
		updateFurniture(sender);

		if (room.equals("Hallway")) {
			sendDoors(c);
		} else {
			updatePlayers(c, p);
		}

		p.addData("user", c.getUsername());
		p.addData("x", "0");
		p.addData("y", "0");
		p.addData("sprite", String.valueOf(c.getSprite()));
		Server.send(p, sender);
	}

	public void sendDoors(Client c) {
		Packet door = Packet.createFurniturePacket(-1, 50, 115, "Door");
		door.addData("destination", c.getUsername());
		Server.send(door, sender);

		int uid = -1, x = 50;

		ArrayList<Client> clients = new ArrayList(Server.getState().getClients());
		ArrayList<Client> sent = new ArrayList<Client>();
		sent.add(c);

		for (int i = 0; i < Math.min(4, Server.getState().getClients().size() - 1); i++) {
			Client chosen;
			while (sent.contains(chosen = clients.get((int) (Math.random() * clients.size()))));
			sent.add(chosen);
			
			door.addData("destination", chosen.getUsername());
			door.addData("uid", String.valueOf(--uid));
			door.addData("x", String.valueOf(x += 200));
			Server.send(door, sender);
		}
	}

	public void updatePlayers(Client c, Packet p) {
		Packet update = Packet.createJoinPacket(c.getUsername(), c.getRoom(), 0, 0);
		update.addData("sprite", String.valueOf(c.getSprite()));

		for (Client other : Server.getState().getClients()) {
			if (!other.isInRoom(c.getRoom()) || other.getUsername().equals(c.getUsername())) {
				continue;
			}

			p.addData("user", other.getUsername());
			p.addData("x", String.valueOf(other.getX()));
			p.addData("y", String.valueOf(other.getY()));
			p.addData("sprite", String.valueOf(other.getSprite()));
			p.addData("vel_x", String.valueOf(other.getVelocityX()));
			p.addData("vel_y", String.valueOf(other.getVelocityY()));
			Server.send(p, sender);
			Server.send(update, other.getAddress());
		}
	}

	public void updateInventory(InetSocketAddress client) {
		Client c = Server.getState().getClient(client);
		ResultSet userInventory = Server.db.getInventory(c.getUsername());
		try {
			while (userInventory.next()) {
				Packet inventoryPacket = Packet.createInventoryPacket(userInventory.getString("type"),
						userInventory.getInt("amount"), c.getUsername());
				Server.send(inventoryPacket, client);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void updateFurniture(InetSocketAddress client) {
		Client c = Server.getState().getClient(client);
		ResultSet furniture = Server.db.getFurniture(c.getRoom());
		try {
			while (furniture.next()) {
				Packet furniturePacket = Packet.createFurniturePacket(furniture.getInt(2), furniture.getFloat(4),
						furniture.getFloat(5), furniture.getString(3));
				Server.send(furniturePacket, client);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void handleMovement(Packet p) {
		Client c = Server.getState().getClient(sender);
		if (c == null) {
			return;
		}

		c.resetAFK();

		float velX = p.getFloat("vel_x");
		float velY = p.getFloat("vel_y");

		if (c.getY() == 0 && velY > 0.30) {
			c.setVelocityY(0.8F);
			c.setVelocityX(3F * velX);
		} else {
			c.setVelocityX(velX);
		}
	}

	private void handleChat(Packet p) {
		Client c = Server.getState().getClient(sender);
		if (c == null) {
			return;
		}

		p.addData("user", c.getUsername());
		for (Client other : Server.getState().getClients()) {
			if (!c.isInRoom(other.getRoom())
					&& !(c.isInRoom("Hallway") && other.getUsername().equals(c.getUsername()))) {
				continue;
			}

			Server.send(p, other.getAddress());
		}
	}

	private void handleAlive() {
		Client c = Server.getState().getClient(sender);
		if (c == null) {
			return;
		}

		c.resetAFK();
	}

	private void handleStatusCheck() {
		Client c = Server.getState().getClient(sender);

		Packet p = new Packet("status");
		p.addData("state", c == null ? "false" : "true");

		Server.send(p, sender);
	}

	private void handleChooseSprite(Packet p) {
		Client c = Server.getState().getClient(sender);
		if (c == null) {
			return;
		}

		c.setSprite(Integer.valueOf(p.getData("sprite")));
		Server.db.changeSprite(c.getUsername(), Integer.valueOf(p.getData("sprite")));
	}

	public void handleLeave() {
		Server.disconnect(sender);
		Server.getState().removeClient(sender);
	}
}