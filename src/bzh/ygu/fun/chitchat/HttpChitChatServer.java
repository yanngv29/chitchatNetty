package bzh.ygu.fun.chitchat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.net.UnknownHostException;

import bzh.ygu.fun.chitchat.model.Root;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.logging.slf4j.SLF4JLogrImplFactory;
import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * An Chichat HTTP server modified from {@link HttpSnoopServer} that received chichat Rest actions.
 */
public class HttpChitChatServer {



    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));
    private static Mongo m = null;

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
        } else {
            sslCtx = null;
        }

        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new HttpChitChatServerInitializer(sslCtx));

            Channel ch = b.bind(PORT).sync().channel();
            initDb(args);
            System.err.println("Open your web browser and navigate to " +
                    (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            if (m != null)
            	m.close();
        }
    }

	private static void initDb(String[] args) {
		try {
			boolean reset = false;
			if (args.length >= 1) {
				reset = args[0].equalsIgnoreCase("reset");
			}
			// connect to mongo
			m = new Mongo("localhost");
			
			DB db = m.getDB("chitchat");
			if (reset) {
				if (db.collectionExists("heros")) {
					db.getCollection("heros").drop();
				}
				if (db.collectionExists("messageThreads")) {
					db.getCollection("messageThreads").drop();
				}
				if (db.collectionExists("messages")) {
					db.getCollection("messages").drop();
				}
				if (db.collectionExists("threads")) {
					db.getCollection("threads").drop();
				}	
				db.dropDatabase();
				db = m.getDB("chitchat");
				
			}
			MorphiaLoggerFactory.registerLogger(SLF4JLogrImplFactory.class);
			Morphia morphia = new Morphia();
			morphia.mapPackage("bzh.ygu.fun.chitchat.model");
			Datastore ds = morphia.createDatastore(m, "chitchat");
			ds.ensureIndexes();
			Root.setDS(ds);

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
