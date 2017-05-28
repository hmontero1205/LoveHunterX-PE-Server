import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

public class Handler extends ChannelInboundHandlerAdapter {
	private static final AttributeKey<Client> CLIENT_INFO = AttributeKey.valueOf("info");
	private static final ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	static {
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Started");
				while (true) {
					for (Channel channel : clients) {
						Client cli = channel.attr(CLIENT_INFO).get();
						if (cli.getDirection() != 0) {
							cli.move(1 / 3f);
						}
					}

					try {
						Thread.sleep((int) ((1 / 3f) * 1000));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		String message = (String) msg;
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
			handleAuthentication(ctx, p);
			break;
		case "reg":
			handleRegistration(ctx, p);
			break;
		case "join":
			handleJoin(ctx, p);
			break;
		case "move":
			handleMovement(ctx, p);
		}
	}

	private void handleRegistration(ChannelHandlerContext ctx, Packet p) {
		boolean success = Server.db.register(p.getData("user"), p.getData("pass"));
		Packet regPacket = Packet.createRegPacket(success);
		ctx.writeAndFlush(regPacket.toJSON());
	}

	private void handleAuthentication(ChannelHandlerContext ctx, Packet p) {
		boolean success = Server.db.authenticate(p.getData("user"), p.getData("pass")) && !isLoggedIn(p.getData("user"));
		Packet authPacket = Packet.createAuthPacket(success);
		ctx.writeAndFlush(authPacket.toJSON());

		if (success) {
			Client cli = new Client(ctx.channel());
			cli.login(p.getData("user"));
			ctx.channel().attr(CLIENT_INFO).set(cli);

			clients.add(ctx.channel());
			System.out.println(clients.size());
		}
	}
	
	public boolean isLoggedIn(String username) {
		for (Channel channel : clients) {
			Client cli = channel.attr(CLIENT_INFO).get();
			
			if (cli.getUsername().equals(username)) {
				return true;
			}
		}
		
		return false;
	}

	private void handleJoin(ChannelHandlerContext ctx, Packet p) {
		String room = p.getData("room");
		Client c = ctx.channel().attr(CLIENT_INFO).get();
		c.joinRoom(room);

		Packet update = Packet.createJoinPacket(c.getUsername(), room, 0, 0);
		for (Channel channel : clients) {
			Client other = channel.attr(CLIENT_INFO).get();
			if (!other.isInRoom(room)) {
				continue;
			}

			p.addData("user", other.getUsername());
			p.addData("x", String.valueOf(other.getX()));
			p.addData("y", String.valueOf(other.getY()));
			c.getChannel().writeAndFlush(p.toJSON());

			if (!other.getUsername().equals(c.getUsername())) {
				other.getChannel().writeAndFlush(update.toJSON());
			}
		}
	}

	private void handleMovement(ChannelHandlerContext ctx, Packet p) {
		Client c = ctx.channel().attr(CLIENT_INFO).get();
		c.setDirection(Integer.valueOf(p.getData("direction")));
		p.addData("user", c.getUsername());

		for (Channel cli : clients) {
			Client other = cli.attr(CLIENT_INFO).get();
			if (!other.isInRoom(c.getRoom())) {
				continue;
			}

			other.getChannel().writeAndFlush(p.toJSON());
		}
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) {
		System.out.println("Someone connected to the server");
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		System.out.println("Someone disconnected to the server");
		clients.remove(ctx.channel());
		
		Client c = ctx.channel().attr(CLIENT_INFO).get();
		if (c == null) {
			return;
		}
		
		Packet leave = Packet.createLeavePacket(c.getUsername(), c.getRoom());
		for (Channel cli : clients) {
			Client other = cli.attr(CLIENT_INFO).get();
			if (!other.isInRoom(c.getRoom())) {
				continue;
			}

			other.getChannel().writeAndFlush(leave.toJSON());
		}
	}

}
