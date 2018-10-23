package com.nesc.NettyServer;
/**
* 
* 测试工具
*
* @author  nesc418
* @Date    2018-10-22
* @version 0.0.1
*/
public class TestTools implements Runnable{
	private Thread t;//线程
	private int packsNum;
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true) {
			packsNum = RunDeviceServer.getPacksNum();  //获取packsnums
			RunDeviceServer.resetPacksNum();  //packsnums = 0
			System.out.println("Packs num: "+packsNum);			
			try {//休息1s
				Thread.sleep(1000);//阻塞当前进程
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}
	/**
	 * 开始线程
	 */
	public void start () {
		System.out.println("Starting TestTools thread");
		if (t == null) {
			t = new Thread (this, "TestTools");
			t.start ();
		}
	}	
}
