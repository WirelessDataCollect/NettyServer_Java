package com.nesc.NettyServer;

/**
* 
* 基于WiFi的物联网数据采集服务器程序
*
* @author  nesc418
* @Date    2018-10-23
* @version 0.0.1
*/
public class App{
    public static void main(String[] args) { 
    	TestTools test = new TestTools();
    	test.start();
    	RunPcServer pc_server = new RunPcServer("PC-Thread",8080);
    	pc_server.start();
    	RunDeviceServer device_server = new RunDeviceServer("Device-Thread");
    	device_server.start();   	
    }
}