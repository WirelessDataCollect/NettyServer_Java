package com.nesc.NettyServer;

import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ReferenceCountUtil;
/**
* 
* 运行UDP/TCP服务器，用于连接硬件设备
*
* 设备参考输入：12121212000000011212120102012022（只适用于固定adc_length为16）
* @author  nesc528
* @Date    2018-9-7
* @version 0.0.1
*/
public class RunDeviceServer implements Runnable{
	private String protocol = "UDP";
	private int listen_port = 5001;
	private Thread t;
	private String threadName;
	private volatile static int packsNum = 0;
	
	/**
	* 构造方法。
	*
	* @param name 线程名称 
	* @param port 面向设备的端口
	* @throws none
	*/
	RunDeviceServer( String name) {
		threadName = name;
		System.out.println("Creating thread:" +  threadName );
//		getProtocolInfo();
	}
	/**
	* 清除packsNum1s。
	*
	* @throws none
	*/	
	public static void resetPacksNum() {
		packsNum = 0;
	}
	/**
	* 增加packsNum1s。
	*
	* @throws none
	*/	
	public static void incPacksNum() {
		packsNum ++;
	}	
	/**
	* 获取packsNum的数值。
	*
	* @throws none
	*/	
	public static int getPacksNum() {
		return packsNum;
	}	
	/**
	* 获取UDP还是TCP协议，获取端口号。
	*
	* @throws none
	*/	
	public void getProtocolInfo(){
		System.out.println("Choose TCP or UDP?");
		Scanner scan = new Scanner(System.in);
        while (scan.hasNextLine()) {
        	protocol = scan.nextLine().toUpperCase();//支持大小写混写
            if(protocol.equals("TCP")||protocol.equals("UDP")) {
            	System.out.println("Choose port from 5000~9000...");
            	while (scan.hasNextLine()) {
            		try{
            			listen_port = Integer.parseInt(scan.nextLine());
            			if(listen_port<5000||listen_port>9000) {//如果超出了这个port界限，则要重新输入
            				System.out.println("Error:Please input port number from 5000~9000!");
            				continue;
            			}
            			else {
            				System.out.println("面向设备运行端口：" + this.listen_port);
            				break;//退出while(得到port)
            			}		
            		}catch(NumberFormatException nfe) {
            			System.out.println("Error:Please input port number from 5000~9000!");
            		}
            	}
                if(scan!=null) {
                	scan.close();//关闭scanner
                }
            	return;//getProtocolInfo结束
            }
            else {
            	System.out.println("Error:Please input \"TCP\" or \"UDP\"!");
            }

        }
        if(scan!=null) {
        	scan.close();//关闭scanner
        }
	}
	/**
	* 运行UDP连接设备。
	*
	* @param port 面向设备的端口
	* @throws none
	*/
	private void runUdp(int port){
		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		try {
			
			Bootstrap bootstrap = new Bootstrap();//引导启动 
			bootstrap.group(eventLoopGroup)
			.channel(NioDatagramChannel.class)
			.option(ChannelOption.SO_BROADCAST, true)
			.handler(new ChannelInitializer<DatagramChannel>() {
			@Override
			public void initChannel(DatagramChannel ch) throws Exception {
				ch.pipeline().addLast(new UDP_ServerHandler());
			}
		});//业务处理类,其中包含一些回调函数
			 
		ChannelFuture cf= bootstrap.bind(port).sync();
			cf.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			eventLoopGroup.shutdownGracefully();//最后一定要释放掉所有资源,并关闭channle
		}
	}
	/**
	* 运行TCP连接设备。
	*
	* @param port 面向设备的端口
	* @throws none
	*/
	private void runTcp(int port){
	    EventLoopGroup bossGroup = new NioEventLoopGroup();        // 用来接收进来的连接，这个函数可以设置多少个线程
	    EventLoopGroup workerGroup = new NioEventLoopGroup();    // 用来处理已经被接收的连接
	
	    try {
	    	ServerBootstrap b = new ServerBootstrap();
	    	b.group(bossGroup, workerGroup)
	    	 .channel(NioServerSocketChannel.class)            // 这里告诉Channel如何接收新的连接
	    	 .childHandler( new ChannelInitializer<SocketChannel>() {
		    	 @Override
		    	 protected void initChannel(SocketChannel ch) throws Exception {
		        // 自定义处理类
		    		 ch.pipeline().addLast(new TCP_ServerHandler());//如果需要继续添加与之链接的handler，则再次调用addLast即可
		    	 }	
	    	 })
		    .option(ChannelOption.SO_BACKLOG, 128)
		    .childOption(ChannelOption.SO_KEEPALIVE, true);
	   
	     
	    	// 绑定端口，开始接收进来的连接
	    	ChannelFuture cf = b.bind(port).sync();//在bind后，创建一个ServerChannel，并且该ServerChannel管理了多个子Channel 
	    	// 等待服务器socket关闭
	        cf.channel().closeFuture().sync();              
	    } catch (Exception e) {
	        workerGroup.shutdownGracefully();
	        bossGroup.shutdownGracefully();
	    }
	    finally {
	    	 workerGroup.shutdownGracefully();
	         bossGroup.shutdownGracefully();      	
	    }
	}  
	@Override
	public void run() {	
    	System.out.println("server protocol = "+protocol);
    	System.out.println("server listen port = "+listen_port);
        switch(protocol) {
        case "UDP":
        	runUdp(listen_port);
        	break;
        case "TCP":
        	runTcp(listen_port);
        	break;
        default:
        	System.out.println("Error:Bad protocal!");
        }  			
	}
	public void start () {
		System.out.println("Starting " +  threadName );
		if (t == null) {
			t = new Thread (this, threadName);
			t.start ();
		}
	}
	
}

/**
* 
* UDP服务器的输入处理器函数
*
* @author  nesc528
* @Date    2018-9-7
* @version 0.0.1
*/
class UDP_ServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	private DataProcessor processor;
	UDP_ServerHandler()
	{
		 processor = new DataProcessor("udp");
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {  //channelRead0在退出前，后面的不会打断
		RunDeviceServer.incPacksNum();  //每次进入数据接受，都要更新包裹数目
		//如果数字超过了127,则会变成负数为了解决这个问题需要用getUnsignedByte
		ByteBuf temp =msg.content();
	
		//作为实参，cnt不会减小到0
		//SimpleChannelInboundHandler父类包含了release对象的方法，不需要再release
		DeviceServerTools.send2Pc(temp);
		processor.dataProcess(temp);
		
	}
	/**
	 * 当channel建立的时候回调（不面向连接，也无法返回数据回去），不同于TCP
	 * 
	 * 在UDPbind的时候，服务器也不会进入channelActive。
	 * 
	 * channelActive是自行创建的时候，进入的。
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(
                "Device UDP channel " + ctx.channel().toString() + " create");
    }
    /**
     * 当Netty由于IO错误或者处理器在处理事件时抛出异常时调用
     */	
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.close();
        cause.printStackTrace();
    }        
}

/**
* 
* TCP服务器的输入处理器函数
*
* @author  nesc528
* @Date    2018-9-7
* @version 0.0.1
*/
class TCP_ServerHandler extends ChannelInboundHandlerAdapter {
	private DataProcessor processor;
	TCP_ServerHandler()
	{
		 processor = new DataProcessor("udp");
	}
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
        	RunDeviceServer.incPacksNum();//1秒钟内的包++
    		ByteBuf temp = (ByteBuf)msg;
    		DeviceServerTools.send2Pc(temp);
    		processor.dataProcess(temp);
    		
        } finally {
            // 抛弃收到的数据
            ReferenceCountUtil.release(msg);//如果不是继承的SimpleChannel...则需要自行释放msg
        }
    }
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {   //1
        System.out.println(
                "Device TCP channel " + ctx.channel().toString() + " create");
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 当出现异常就关闭连接
        cause.printStackTrace();
        ctx.close();
    }
}
/**
* 
* 设备服务器工具，包括转发数据给PC上位机。在此之前，需要有TCP连接，即MAP中有上位机同服务器连接的Channel。
*
* @author  nesc528
* @Date    2018-9-7
* @version 0.0.1
*/
class DeviceServerTools{
	/**
	 * 转发设备信息至PC端上位机
	 * @param temp
	 */
	protected static void send2Pc(ByteBuf temp) {   //这里需要是静态的，非静态依赖对象
		
		//如果上位机在发送数据时断开连接，那么就会抛出异常
		//IOException:连接被对方重置
		synchronized(RunPcServer.getChMap()) {
			for(Iterator<Map.Entry<String,Channel>> item = RunPcServer.getChMap().entrySet().iterator();item.hasNext();) {
				Map.Entry<String,Channel> entry = item.next();
				ByteBuf temp1 = temp.copy();
				ChannelFuture future = entry.getValue().pipeline().writeAndFlush(temp1);
				future.addListener(new ChannelFutureListener(){
					@Override
					public void operationComplete(ChannelFuture f) {
						if(!f.isSuccess()) {
							f.cause().printStackTrace();
						}
					}
				});
			}	
		}
	}
}