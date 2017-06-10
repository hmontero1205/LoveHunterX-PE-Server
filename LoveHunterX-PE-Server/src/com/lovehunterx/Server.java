package com.lovehunterx;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

public class Server {
	private static final int PORT = 8080;
	private static GameState state;
	private static Channel connection;
	public static Database db;

	public void run() throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();

		Bootstrap b = new Bootstrap();
		b.group(group)
		 .channel(NioDatagramChannel.class)
		 .option(ChannelOption.SO_BROADCAST, true)
		 .handler(new Handler());

		connection = b.bind(PORT).sync().channel();
		connection.closeFuture().sync();

		group.shutdownGracefully();
	}

	public static GameState getState() {
		return state;
	}

	private static DatagramPacket createDatagramPacket(Packet p, InetSocketAddress addr) {
		return new DatagramPacket(Unpooled.copiedBuffer(p.toJSON(), CharsetUtil.UTF_8), addr);
	}

	public static void send(Packet p, InetSocketAddress sender) {
		connection.writeAndFlush(createDatagramPacket(p, sender));
	}
	
	public static void disconnect(InetSocketAddress cli) {
		Client c = Server.getState().getClient(cli);
		if (c == null) {
			return;
		}

		Packet leave = Packet.createLeavePacket(c.getUsername(), c.getRoom());
		for (Client other : Server.getState().getClients()) {
			if (!other.isInRoom(c.getRoom())) {
				continue;
			}

			Server.send(leave, other.getAddress());
		}
	}

	public static void main(String[] args) {
		System.out.println("Running");
		state = new GameState();
		state.init();
		
		db = new Database();

		try {
			new Server().run();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

}
