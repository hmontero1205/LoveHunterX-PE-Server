import java.util.Date;
import java.util.HashMap;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class Handler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		String message = (String) msg;
		//String message = m.toString(CharsetUtil.US_ASCII);
		// System.out.println(message);
		try {
			interpretInput(ctx, message);
		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			//message.release();
		}
	}

	private void interpretInput(ChannelHandlerContext ctx, String message) throws ParseException {
		JSONParser parser = new JSONParser();
		//System.out.println(message);
		JSONObject obj = (JSONObject) parser.parse(message);
		Packet p = new Packet(obj);
		switch (p.getAction()) {
			case "auth":
				handleAuthentication(ctx, p);
				break;
			case "reg":
				handleRegistration(ctx, p);
				break;
		}
	}

    private void handleRegistration(ChannelHandlerContext ctx, Packet p) {
    	boolean success = Server.db.register(p.getData("user"), p.getData("pass"));
		Packet regPacket = Packet.createRegPacket(success);
		//System.out.println("sending response");
		ctx.writeAndFlush(regPacket.toJSON());
		
	}

	private void handleAuthentication(ChannelHandlerContext ctx, Packet p) {
		boolean success = Server.db.authenticate(p.getData("user"), p.getData("pass"));
		Packet authPacket = Packet.createAuthPacket(success);
		//System.out.println("sending response");
		ctx.writeAndFlush(authPacket.toJSON());
		
	}
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) {
		System.out.println("Someone connected to the server");
	}

}
