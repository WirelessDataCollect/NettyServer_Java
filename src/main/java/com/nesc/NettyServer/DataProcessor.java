package com.nesc.NettyServer;


import org.bson.Document;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.async.SingleResultCallback;

import io.netty.buffer.ByteBuf;

/**
* 
* 数据处理器，用于处理设备发送到服务器的ByteBuf数据
*
* @author  nesc528
* @Date    2018-9-7
* @version 0.0.1
*/
public class DataProcessor {
	private final int LENGTH_ONE_GROUP_ADC = 8;//一组ADC数据有多少个Byte
	private final int  WIFI_CLIENT_ID_MAX= 50;//模组id最大不超过
	private final byte  ADC_CHANNEL_MAX= 4;//adc数据通道最多不超过
	private final short ADC_BYTES_NUM = 2*ADC_CHANNEL_MAX;//ADC一个周期所占的bytes
	private final byte HEAD_FRAME_LENGTH = 16;//一帧的头的长度，包括yyyy_mm_dd、headtime、adc_count等
	private final short CHECK_UBYTE = 15;//校验开始的位置，和headtime的低八位相等
	
	private final int WIFI_CLIENT_ID_IDX = 12;//这里保存wifi模组的id的下标
	private final int YYYY_MM_DD_START_IDX = 0;//年月日开始的下标,下标越大，越高位
	private final int HEADTIME_START_IDX = 4;//毫秒开始的下标
	private final int ADC_COUNT_START_IDX = 8;//adc数据量开始的下标
	private final int IO1_IDX = 13;
	private final int IO2_IDX = 14;
	
	private int BytebufLength;
	private short checkUbyte;
	private short wifi_client_id;//设备的id，虽然是short，但是设备以8bits无符号传输，最大255
	private long yyyy_mm_dd;//年月日
	private long headtime;//毫秒
	private long adc_count;//数据个数（ADC数据）
	private short io1,io2;//io数字电平

	
	private MyMongoDB mongodb;
	/**
	* 数据处理obj的构造函数
	*
	* @param dbname 数据库名称
	* @return 无
	* @throws 无
	*/		
	DataProcessor(String dbname) {
		mongodb = new MyMongoDB(dbname);
	}
	SingleResultCallback<Void> callback;
	/**
	* 总的数据包的解析和存储方法
	*
	* @param msg 传入的ByteBuf数据。
	* @return none
	* @throws none
	*/	
	public void dataProcess(ByteBuf msg){
		/*更新帧头的信息*/
		if(!getFrameHead(msg)) {
			return;
		}
		/*生成document*/
		BasicDBObject bdo = getAdcVal4CH(msg,(short)(adc_count));
		Document doc = new Document("wifi_client_id",wifi_client_id)
				.append("yyyy_mm_dd", yyyy_mm_dd)
				.append("headtime",headtime)
				.append("adc_count_short",adc_count / 2 / ADC_CHANNEL_MAX)//这里的adc_count_short表示每个channel有多少个short数据 
				.append("io1",io1)
				.append("io2",io2)
				.append("adc_val",bdo );
//		System.out.println(doc);
		/*doc存入数据库*/
		mongodb.collection.insertOne(doc, new SingleResultCallback<Void>() {
		    public void onResult(final Void result, final Throwable t) {
			        System.out.println("Document inserted!");
		    }});
	}

	/**
	* 数据包的校验方法
	*
	* @param checkByte1,checkByte2： 两个校验字节，相等才能通过
	* @return true：通过;false:不通过
	* @throws 无
	*/
	private boolean isRightPkg(short checkByte1,short checkByte2){
		if(checkByte1 == checkByte2) {
			return true;
		}
		else {
			return false;
		}
	}
	/**
	* 数据包的提取前16bits帧头数据
	* 
	* YYYY_MM_DD:年月日32bits[0:3], HeadTime:毫秒32bits[4:7], count:adc数据长度32bits[8:11], 
	* 
	* wifi_client_id:模组id8bits[12],IO:数字电平[13:14],checkUbyte:校验8bits[15]
	*
	* @param msg 其中的两个校验字节msg[4]和msg[15]，相等才能通过
	* @return true：成功获取数据帧头;false:数据有问题
	* @throws 无
	*/
	private boolean getFrameHead(ByteBuf msg) {
		/*得到帧头+实际数据的Bytebuf字节长度*/
		BytebufLength = msg.readableBytes();
		if(BytebufLength<=HEAD_FRAME_LENGTH+LENGTH_ONE_GROUP_ADC)
		{
			System.out.println("Error:length = "+msg.readableBytes()+
					", <= SMALLEST LIMIT("+(HEAD_FRAME_LENGTH+LENGTH_ONE_GROUP_ADC)+")");
			return false;
		}
		/*获取设备的id*/
		wifi_client_id = msg.getUnsignedByte(WIFI_CLIENT_ID_IDX);
//		System.out.println("Client Id:"+wifi_client_id);
		
		/*校验设备的id*/
		if((wifi_client_id<0) ||(wifi_client_id>WIFI_CLIENT_ID_MAX)) {
			System.out.println("id error:abandoned");
			return false;
		}
		
		/*获取headtime/微秒*/
		headtime = (long)(msg.getUnsignedByte(HEADTIME_START_IDX)|
				(msg.getUnsignedByte(HEADTIME_START_IDX+1)<<8)|
				(msg.getUnsignedByte(HEADTIME_START_IDX+2)<<16)|
				(msg.getUnsignedByte(HEADTIME_START_IDX+3)<<24));
		
		/*获取校验byte*/
		checkUbyte = msg.getUnsignedByte(CHECK_UBYTE);
		/*校验位校验，headtime的最低8bits需要和帧头校验位相同*/
		if(!isRightPkg((short)(headtime&0xff),(short)checkUbyte)){
			System.out.println("checkUbyte error:abandoned");
			return false;
		}
		
		/*获取年月日*/
		yyyy_mm_dd = (long)(msg.getUnsignedByte(YYYY_MM_DD_START_IDX)|
				(msg.getUnsignedByte(YYYY_MM_DD_START_IDX+1)<<8)|
				(msg.getUnsignedByte(YYYY_MM_DD_START_IDX+2)<<16)|
				(msg.getUnsignedByte(YYYY_MM_DD_START_IDX+3)<<24));
		/*获取adc数据的byte数目*/
		adc_count = (long)(msg.getUnsignedByte(ADC_COUNT_START_IDX)|
				(msg.getUnsignedByte(ADC_COUNT_START_IDX+1)<<8)|
				(msg.getUnsignedByte(ADC_COUNT_START_IDX+2)<<16)|
				(msg.getUnsignedByte(ADC_COUNT_START_IDX+3)<<24));
		/*adc数据个数的校验*/
		if((adc_count<0)||adc_count!=BytebufLength - HEAD_FRAME_LENGTH) {
				System.out.println("count error:abandoned");
			return false;
		}
//		System.out.println("adc data len:"+adc_count);
		/*获取io电平*/
		io1 = msg.getUnsignedByte(IO1_IDX);
		io2 = msg.getUnsignedByte(IO2_IDX);		
//		System.out.printf("io1:%d  io2:%d\n",io1,io2);
		return true;
	}	

	/**
	* 数据包的提取,除了前16bits帧头外的adc数据，adc数据以[channel1低八位,channel1高八位,channel2低八位,channel2高八位...]传输
	* 
	* channle_num是通道数，adc_count_short是每个通道包含多少个数据（数据12bit，用hsort）
	* ----!!必须在getFrameHead更新后才能调用!!------
	*
	* @param msg 包括帧头在内的所有数据
	* @param adc_count adc数据的byte位数
	* @return BasicDBObject
	* @throws 无
	*/
	private BasicDBObject getAdcVal4CH(ByteBuf msg,int adc_count) {
		int adc_count_short = adc_count/2/ADC_CHANNEL_MAX;
		BasicDBList ch1 = new BasicDBList();
		BasicDBList ch2 = new BasicDBList();
		BasicDBList ch3 = new BasicDBList();
		BasicDBList ch4 = new BasicDBList();
		for(int idx = 0,idx_start; idx<adc_count_short ; idx++) {
			idx_start = idx * 8 + HEAD_FRAME_LENGTH;
			ch1.add((short)( (msg.getUnsignedByte(idx_start)<<4) | 
							(msg.getUnsignedByte(idx_start + 1)>>4) ));
			ch2.add((short)( (msg.getUnsignedByte(idx_start + 2)<<4) |   
							(msg.getUnsignedByte(idx_start + 3)>>4) ));
			ch3.add((short)( (msg.getUnsignedByte(idx_start + 4)<<4) | 
							(msg.getUnsignedByte(idx_start + 5)>>4) ));
			ch4.add((short)( (msg.getUnsignedByte(idx_start + 6)<<4) | 
							(msg.getUnsignedByte(idx_start + 7)>>4) ));
		}
		return (new BasicDBObject()).append("ch1", ch1).append("ch2", ch2).append("ch3", ch3).append("ch4", ch4);
	}
	/**
	* 打印每个通道的两个电压数值，用来检查。以电压形式展示。
	* 
	*
	* @param buf 以short格式（每个short对因一个12位adc数据）存储的adc数据
	* @param idx1 第一个数据下标
	* @param idx2 第二个数据下标
	* @return 无
	* @throws 无
	*/	
	private void printAdcBuf_4Ch(short[][] buf,short idx1,short idx2){
		try {
			System.out.printf("ch1: %.5f V   %.5f V\n", 5.0*buf[(byte)0][(short)idx1]/4096.0,5.0*buf[(byte)0][(short)idx2]/4096.0);
			System.out.printf("ch2: %.5f V   %.5f V\n", 5.0*buf[(byte)1][(short)idx1]/4096.0,5.0*buf[(byte)1][(short)idx2]/4096.0);
			System.out.printf("ch3: %.5f V   %.5f V\n", 5.0*buf[(byte)2][(short)idx1]/4096.0,5.0*buf[(byte)2][(short)idx2]/4096.0);
			System.out.printf("ch4: %.5f V   %.5f V\n", 5.0*buf[(byte)3][(short)idx1]/4096.0,5.0*buf[(byte)3][(short)idx2]/4096.0);				
		}catch(ArrayIndexOutOfBoundsException a) {  //数组下标越界
			System.out.println("Exception: ArrayIndexOutOfBoundsException at printAdcBuf_4Ch()");
			return;
		}
	
	}
	/**
	* 数据包的提取,除了前16bits帧头外的adc数据，adc数据以[channel1低八位,channel1高八位,channel2低八位,channel2高八位...]传输
	* 
	* channle_num是通道数，adc_count_short是每个通道包含多少个数据（数据12bit，用hsort）
	* ----!!必须在getFrameHead更新后才能调用!!------
	*
	* @param ByteBuf 包括帧头的数据
	* @param adc_count adc数据的字节数
	* @return short[][]
	* @throws 无
	*/
	private short[][] getAdcVal(ByteBuf msg,int adc_count) {
		int adc_count_short = adc_count/2/ADC_CHANNEL_MAX;
		short[][] buf = new short[ADC_CHANNEL_MAX][adc_count_short];
		for(int idx = 0; idx<adc_count ; idx += 2) {
			buf[(byte)(idx % ADC_BYTES_NUM)/2][(short)(idx / ADC_BYTES_NUM)] = 
					(short)(  (msg.getUnsignedByte(idx + HEAD_FRAME_LENGTH)<<4) | 
							(msg.getUnsignedByte(idx+HEAD_FRAME_LENGTH+1)>>4) );//>>有符号右移，>>>也可以
			
		}
		return buf;	
	}
}



