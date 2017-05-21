import java.util.Date;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class Handler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ByteBuf m = (ByteBuf) msg;
		try {
			String message = m.toString(CharsetUtil.US_ASCII);
			System.out.println(message);
			interpretInput(message);
		} finally {
			m.release();
		}
	}
	
	private void interpretInput(String message) {
		String[] messageArgs = message.split(";");
		if(messageArgs[0].equals("auth")) {
			System.out.println(Server.db.authenticate(messageArgs[1], messageArgs[2]));
		}
		if(messageArgs[0].equals("reg")) {
			System.out.println(Server.db.register(messageArgs[1], messageArgs[2]));
		}
		
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) {
		System.out.println("Someone connected to the server");
	}

}
