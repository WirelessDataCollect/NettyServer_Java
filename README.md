# NettyServer
## 介绍
服务器主要包括三个方面的功能——从设备端接收原始数据并进行解析；将解析后的数据转发给PC上位机；将解析后的数据存储在服务器的MongoDB数据库中。

* 数据接收

设备通过TCP或者UDP（可手动设置）连接服务器，并传输数据到服务器。

* 数据转发

PC上位机通过TCP连接服务器8080端口，实施接受经过服务器转发的设备采集数据。

* 数据存储

服务器接受设备数据，并存储在MongoDB数据库中。

存储格式：

```Json
{
	"_id" : ObjectId("5b90c70aa7986c262ff73c34"),
	"wifi_client_id" : 48,
	"yyyy_mm_dd" : NumberLong(842084913),
	"headtime" : NumberLong(842084913),
	"adc_count_short" : NumberLong(2),
	"io1" : 48,
	"io2" : 48,
	"adc_val" : {
		"ch1" : [
			787,
			771
		],
		"ch2" : [
			787,
			771
		],
		"ch3" : [
			787,
			803
		],
		"ch4" : [
			771,
			803
		]
	}
}
```

# 参考

[Netty实战精髓](https://www.w3cschool.cn/essential_netty_in_action/ "Netty实战精髓")

[Netty实战](https://book.douban.com/subject/27038538/ "Netty实战-何平")

[Java菜鸟教程](http://www.runoob.com/java/java-tutorial.html "Java菜鸟教程")

[MongoDB菜鸟教程](http://www.runoob.com/mongodb/mongodb-tutorial.html "MongoDB菜鸟教程")

[Java笔记](http://neyzoter.cn/2018/09/07/Netty-EventLoopGroup-EventLoop-Channel-Channle-ChannlePipeline-et/ "Java笔记")

[对Netty组件的理解（Channel、Pipeline、EventLoop等）](http://neyzoter.cn/wiki/Java/ "对Netty组件的理解（Channel、Pipeline、EventLoop等）")

[Maven笔记](http://neyzoter.cn/wiki/MAVEN/ "Maven笔记")

[Netty笔记](http://neyzoter.cn/wiki/Netty/ "Netty笔记")

[MongoDB笔记](http://neyzoter.cn/wiki/MongoDB/ "MongoDB笔记")


