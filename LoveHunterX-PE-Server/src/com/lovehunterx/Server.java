package com.lovehunterx;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class Server {

	private static final int PORT = 8080;
	public static Database db;

	public void run() throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();

		try {
			Bootstrap b = new Bootstrap();
			b.group(group)
			 .channel(NioDatagramChannel.class)
			 .option(ChannelOption.SO_BROADCAST, true)
			 .handler(new Handler());

			Channel ch = b.bind(PORT).sync().channel();
			ch.closeFuture().sync();
		} finally {
			group.shutdownGracefully();
		}
	}

	public static void main(String[] args) {
		System.out.println("Running");
		db = new Database();

		try {
			new Server().run();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

}
