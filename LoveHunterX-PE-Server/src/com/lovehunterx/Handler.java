package com.lovehunterx;

import java.net.InetSocketAddress;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

public class Handler extends SimpleChannelInboundHandler<DatagramPacket> {
	private static final ConcurrentHashMap<InetSocketAddress, Client> clients = new ConcurrentHashMap<InetSocketAddress, Client>();
	private ChannelHandlerContext ctx;
	private InetSocketAddress sender;

	private static DatagramPacket createDatagramPacket(Packet p, InetSocketAddress addr) {
		return new DatagramPacket(Unpooled.copiedBuffer(p.toJSON(), CharsetUtil.UTF_8), addr);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
		this.ctx = ctx;
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
		case "disconnect":
			handleLeave();
			break;
		}
	}

	private void handleRegistration(Packet p) {
		boolean success = Server.db.register(p.getData("user"), p.getData("pass"));
		Packet regPacket = Packet.createRegPacket(success);
		ctx.writeAndFlush(createDatagramPacket(regPacket, sender));
	}

	private void handleAuthentication(Packet p) {
		boolean success = Server.db.authenticate(p.getData("user"), p.getData("pass"))
				&& !isLoggedIn(p.getData("user"));

		Packet authPacket = Packet.createAuthPacket(success);
		ctx.writeAndFlush(createDatagramPacket(authPacket, sender));

		if (success) {
			Client cli = new Client(sender);
			cli.setUsername(p.getData("user"));

			clients.put(sender, cli);
			System.out.println(clients.size());
		}
	}

	public boolean isLoggedIn(String username) {
		for (Entry<InetSocketAddress, Client> entry : clients.entrySet()) {
			Client cli = entry.getValue();

			if (cli.getUsername().equals(username)) {
				return true;
			}
		}

		return false;
	}

	private void handleJoin(Packet p) {
		String room = p.getData("room");
		Client c = clients.get(sender);
		c.joinRoom(room);

		Packet update = Packet.createJoinPacket(c.getUsername(), room, 0, 0);
		for (Entry<InetSocketAddress, Client> entry : clients.entrySet()) {
			Client other = entry.getValue();
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
				ctx.writeAndFlush(createDatagramPacket(update, entry.getKey()));
			}
		}
	}

	private void handleMovement(Packet p) {
		Client c = clients.get(sender);
		c.setVelocityX(Float.valueOf(p.getData("vel_x")));
		c.setVelocityY(Float.valueOf(p.getData("vel_y")));
		p.addData("user", c.getUsername());

		for (Entry<InetSocketAddress, Client> entry : clients.entrySet()) {
			Client other = entry.getValue();
			if (!other.isInRoom(c.getRoom())) {
				continue;
			}

			ctx.writeAndFlush(createDatagramPacket(p, other.getAddress()));
		}
	}

	public void handleLeave() {
		clients.remove(sender);

		Client c = clients.get(sender);
		if (c == null) {
			return;
		}

		Packet leave = Packet.createLeavePacket(c.getUsername(), c.getRoom());
		for (Entry<InetSocketAddress, Client> entry : clients.entrySet()) {
			Client other = entry.getValue();
			if (!other.isInRoom(c.getRoom())) {
				continue;
			}

			ctx.writeAndFlush(createDatagramPacket(leave, entry.getKey()));
		}
	}
}
