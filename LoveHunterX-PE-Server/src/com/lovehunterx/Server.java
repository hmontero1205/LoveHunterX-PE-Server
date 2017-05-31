package com.lovehunterx;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

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
	
	public static void send(DatagramPacket packet) {
		connection.writeAndFlush(packet);
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
