import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server {
	
	private static final int PORT = 8000;
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
	
	public static void main(String[] args) throws Exception {
		db = new Database();
		System.out.println(db.authenticate("Brendan", "ilikemen69!"));
		new Server().run();
	}
	
}
