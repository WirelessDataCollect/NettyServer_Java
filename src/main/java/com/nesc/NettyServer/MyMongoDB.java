package com.nesc.NettyServer;

import org.bson.Document;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
/**
* 
* MongoDB数据库类
*
* @author  nesc528
* @Date    2018-9-7
* @version 0.0.1
*/
public class MyMongoDB{
	public MongoCollection<Document> collection;
	protected MongoClient mongoClient;
	protected MongoDatabase mongoDatabase;
	private final String col_name = "data";
	
	/**
	* MongoDB数据库类的构造函数
	*
	* @param dbname 数据库名称
	* @return 无
	* @throws 无
	*/
	MyMongoDB(String dbname) {
		try{
			if(collection == null) {
				/* 连接到 mongodb 服务*/
				 mongoClient = MongoClients.create();
				   
				     /* 连接到数据库*/
				 mongoDatabase = mongoClient.getDatabase(dbname);  
				 System.out.printf("Connect to db:%s successfully\n",dbname);
				 collection = mongoDatabase.getCollection(col_name);
				 System.out.printf("Connect to col:%s successfully\n",col_name);
			}	    
		  }catch(Exception e){
			  mongoClient = null;
			  mongoDatabase = null;
			  System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		  }    	
	}
	
	
}
