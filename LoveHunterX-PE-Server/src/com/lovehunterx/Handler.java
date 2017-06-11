package com.lovehunterx;

import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.netty.buffer.Unpooled;
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
			System.out.println(message);
			handleAlive();
			break;
		case "auth":
			System.out.println(message);
			handleAuthentication(p);
			break;
		case "reg":
			System.out.println(message);
			handleRegistration(p);
			break;
		case "join":
			System.out.println(message);
			handleJoin(p);
			break;
		case "chat":
			handleChat(p);
			break;
		case "move":
			handleMovement(p);
			break;
		case "choose_sprite":
			System.out.println(message);
			handleChooseSprite(p);
		case "disconnect":
			System.out.println(message);
			handleLeave();
			break;
		case "update_furniture":
			System.out.println(message);
			handleSetFurniture(p);
			break;
		case "remove_furniture":
			System.out.println(message);
			handleRemoveFurniture(p);
			break;		
		case "get_money":
			System.out.println(message);
			handleGetMoney(p);
			break;
		case "purchase":
			System.out.println(message);
			handlePurchase(p);
			break;
		case "status":
			System.out.println(message);
			handleStatusCheck();
			break;
		}
	}

	private void handlePurchase(Packet p) {
		Server.db.updateMoney(p.getData("user"),p.getData("money"));
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
		Server.send(authPacket, sender);

		if (success) {
			Client cli = new Client(sender);
			cli.setUsername(p.getData("user"));

			Server.getState().addClient(cli);
		}
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
		Server.send(p, sender);
	}
	
	public void sendDoors(Client c) {
		Packet door = Packet.createFurniturePacket(-1, 50, 115, "Door");
		door.addData("destination", c.getUsername());
		Server.send(door, sender);
		
		int uid = -1, x = 50;
		for (Client other : Server.getState().getClients()) {
			if (other.getUsername().equals(c.getUsername())) {
				continue;
			}
			
			door.addData("destination", other.getUsername());
			door.addData("uid", String.valueOf(--uid));
			door.addData("x", String.valueOf(x += 200));
			Server.send(door, sender);
		}
	}

	public void updatePlayers(Client c, Packet p) {
		Packet update = Packet.createJoinPacket(c.getUsername(), c.getRoom(), 0, 0);

		for (Client other : Server.getState().getClients()) {
			if (!other.isInRoom(c.getRoom()) || other.getUsername().equals(c.getUsername())) {
				continue;
			}

			p.addData("user", other.getUsername());
			p.addData("x", String.valueOf(other.getX()));
			p.addData("y", String.valueOf(other.getY()));
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
				Packet inventoryPacket = Packet.createInventoryPacket(userInventory.getString("type"), userInventory.getInt("amount"), c.getUsername());
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
				Packet furniturePacket = Packet.createFurniturePacket(furniture.getInt(2), furniture.getFloat(4), furniture.getFloat(5), furniture.getString(3)); 
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
			if (!c.isInRoom(other.getRoom()) && !(c.isInRoom("Hallway") && other.getUsername().equals(c.getUsername()))) {
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
		
	}

	public void handleLeave() {
		Server.disconnect(sender);
		Server.getState().removeClient(sender);
	}
}