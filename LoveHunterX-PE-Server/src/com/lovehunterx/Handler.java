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
	private ChannelHandlerContext ctx;
	private InetSocketAddress sender;

	public static DatagramPacket createDatagramPacket(Packet p, InetSocketAddress addr) {
		return new DatagramPacket(Unpooled.copiedBuffer(p.toJSON(), CharsetUtil.UTF_8), addr);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
		this.ctx = ctx;
		this.sender = packet.sender();

		String message = packet.content().toString(CharsetUtil.US_ASCII);
		System.out.println(message);
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
		case "auth":
			handleAuthentication(p);
			break;
		case "reg":
			handleRegistration(p);
			break;
		case "join":
			handleJoin(p);
			break;
		case "move":
			handleMovement(p);
			break;
		case "choose_sprite":
			handleChooseSprite(p);
		case "disconnect":
			handleLeave();
			break;
		case "update_furniture":
			handleSetFurniture(p);
			break;
		case "remove_furniture":
			handleRemoveFurniture(p);
			break;
		}
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
			DatagramPacket dPacket = createDatagramPacket(p, other.getAddress());
			ctx.writeAndFlush(dPacket);
		}
	}
	
	private void handleRemoveFurniture(Packet p) {
		Client client = Server.getState().getClient(sender);
		Server.db.removeFurniture(p.getData("uid"), p.getData("type"), client.getUsername());
		for (Client other : Server.getState().getClients()) {
			if (!other.isInRoom(client.getRoom())) {
				continue;
			}
			
			DatagramPacket dPacket = createDatagramPacket(p, other.getAddress());
			ctx.writeAndFlush(dPacket);
		}
	}

	private void handleRegistration(Packet p) {
		boolean success = Server.db.register(p.getData("user"), p.getData("pass"));
		Packet regPacket = Packet.createRegPacket(success);
		ctx.writeAndFlush(createDatagramPacket(regPacket, sender));
	}

	private void handleAuthentication(Packet p) {
		boolean success = Server.db.authenticate(p.getData("user"), p.getData("pass"))
				&& !Server.getState().isLoggedIn(p.getData("user"));

		Packet authPacket = Packet.createAuthPacket(success);
		ctx.writeAndFlush(createDatagramPacket(authPacket, sender));

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

		String room = p.getData("room");
		c.joinRoom(room);
		
		updateInventory(sender);
		updateFurniture(sender);
		
		Packet update = Packet.createJoinPacket(c.getUsername(), room, 0, 0);
		for (Client other : Server.getState().getClients()) {
			if (!other.isInRoom(room)) {
				continue;
			}

			p.addData("user", other.getUsername());
			p.addData("x", String.valueOf(other.getX()));
			p.addData("y", String.valueOf(other.getY()));
			p.addData("vel_x", String.valueOf(other.getVelocityX()));
			p.addData("vel_y", String.valueOf(other.getVelocityY()));
			ctx.writeAndFlush(createDatagramPacket(p, sender));

			if (!other.getUsername().equals(c.getUsername())) {
				ctx.writeAndFlush(createDatagramPacket(update, other.getAddress()));
			}
		}
	}

	public void updateInventory(InetSocketAddress client) {
		Client c = Server.getState().getClient(client);
		ResultSet userInventory = Server.db.getInventory(c.getUsername());
		try {
			while (userInventory.next()) {
				Packet inventoryPacket = Packet.createInventoryPacket(userInventory.getString("type"),
						userInventory.getInt("amount"));
				ctx.writeAndFlush(createDatagramPacket(inventoryPacket, client));
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
				ctx.writeAndFlush(createDatagramPacket(furniturePacket, client));
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

		float velX = p.getFloat("vel_x");
		float velY = p.getFloat("vel_y");

		if (c.getY() == 0 && velY > 0.30) {
			c.setVelocityY(0.8F);
			c.setVelocityX(3F * velX);
		} else {
			c.setVelocityX(velX);
		}
	}
	
	private void handleChooseSprite(Packet p) {
		
	}

	public void handleLeave() {
		disconnect(sender);
	}

	public void disconnect(InetSocketAddress cli) {
		Client c = Server.getState().getClient(cli);
		if (c == null) {
			return;
		}

		Packet leave = Packet.createLeavePacket(c.getUsername(), c.getRoom());
		for (Client other : Server.getState().getClients()) {
			if (!other.isInRoom(c.getRoom())) {
				continue;
			}

			ctx.writeAndFlush(createDatagramPacket(leave, other.getAddress()));
		}
		
		Server.getState().removeClient(cli);
	}
}