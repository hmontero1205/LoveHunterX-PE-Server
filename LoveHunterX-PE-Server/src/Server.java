import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class Server {
	
	private static final int PORT = 8080;
	public static Database db;
	
	public void run() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
        	ServerBootstrap b = new ServerBootstrap();
        	b.group(bossGroup, workerGroup)
        	 .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                	 ChannelPipeline pipeline = ch.pipeline();
                     pipeline.addLast("decoder", new StringDecoder());
                     pipeline.addLast("encoder", new StringEncoder());
                     ch.pipeline().addLast(new Handler());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);
        	
        	ChannelFuture f = b.bind(PORT).sync();
        	f.channel().closeFuture().sync();
        } finally {
        	workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
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
