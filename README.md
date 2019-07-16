# Spring Boot 整合 RocketMq

## 1.pom.xml添加RocketMq依赖

```
<dependency>  
     <groupId>com.alibaba.rocketmq</groupId>  
     <artifactId>rocketmq-client</artifactId>  
     <version>3.2.6</version>  
</dependency> 
```

## 2.application.properties中添加如下信息

```
###producer
#该应用是否启用生产者
rocketmq.producer.isOnOff=on
#发送同一类消息的设置为同一个group，保证唯一,默认不需要设置，rocketmq会使用ip@pid(pid代表jvm名字)作为唯一标示
rocketmq.producer.groupName=${spring.application.name}
#mq的nameserver地址
rocketmq.producer.namesrvAddr=127.0.0.1:9876
#消息最大长度 默认1024*4(4M)
rocketmq.producer.maxMessageSize=4096
#发送消息超时时间,默认3000
rocketmq.producer.sendMsgTimeout=3000
#发送消息失败重试次数，默认2
rocketmq.producer.retryTimesWhenSendFailed=2

###consumer
##该应用是否启用消费者
rocketmq.consumer.isOnOff=on
rocketmq.consumer.groupName=${spring.application.name}
#mq的nameserver地址
rocketmq.consumer.namesrvAddr=127.0.0.1:9876
#该消费者订阅的主题和tags("*"号表示订阅该主题下所有的tags),格式：topic~tag1||tag2||tag3;topic2~*;
rocketmq.consumer.topics=DemoTopic~*;
rocketmq.consumer.consumeThreadMin=20
rocketmq.consumer.consumeThreadMax=64
#设置一次消费消息的条数，默认为1条
rocketmq.consumer.consumeMessageBatchMaxSize=1
```

## 3.生产者Bean配置

```
package com.clouds.common.rocketmq.producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.clouds.common.rocketmq.constants.RocketMQErrorEnum;
import com.clouds.common.rocketmq.exception.RocketMQException;

/**
 * 生产者配置
 * .<br/>
 * 
 * Copyright: Copyright (c) 2017  zteits
 * 
 * @ClassName: MQProducerConfiguration
 * @Description: 
 * @version: v1.0.0
 * @author: zhaowg
 * @date: 2018年3月2日 下午11:44:36
 * Modification History:
 * Date             Author          Version            Description
 *---------------------------------------------------------*
 * 2018年3月2日      zhaowg           v1.0.0               创建
 */
@SpringBootConfiguration
public class MQProducerConfiguration {
    public static final Logger LOGGER = LoggerFactory.getLogger(MQProducerConfiguration.class);
    /**
     * 发送同一类消息的设置为同一个group，保证唯一,默认不需要设置，rocketmq会使用ip@pid(pid代表jvm名字)作为唯一标示
     */
    @Value("${rocketmq.producer.groupName}")
    private String groupName;
    @Value("${rocketmq.producer.namesrvAddr}")
    private String namesrvAddr;
    /**
     * 消息最大大小，默认4M
     */
    @Value("${rocketmq.producer.maxMessageSize}")
    private Integer maxMessageSize ;
    /**
     * 消息发送超时时间，默认3秒
     */
    @Value("${rocketmq.producer.sendMsgTimeout}")
    private Integer sendMsgTimeout;
    /**
     * 消息发送失败重试次数，默认2次
     */
    @Value("${rocketmq.producer.retryTimesWhenSendFailed}")
    private Integer retryTimesWhenSendFailed;

    @Bean
    public DefaultMQProducer getRocketMQProducer() throws RocketMQException {
        if (StringUtils.isEmpty(this.groupName)) {
            throw new RocketMQException(RocketMQErrorEnum.PARAMM_NULL,"groupName is blank",false);
        }
        if (StringUtils.isEmpty(this.namesrvAddr)) {
            throw new RocketMQException(RocketMQErrorEnum.PARAMM_NULL,"nameServerAddr is blank",false);
        }
        DefaultMQProducer producer;
        producer = new DefaultMQProducer(this.groupName);
        producer.setNamesrvAddr(this.namesrvAddr);
        //如果需要同一个jvm中不同的producer往不同的mq集群发送消息，需要设置不同的instanceName
        //producer.setInstanceName(instanceName);
        if(this.maxMessageSize!=null){
        	producer.setMaxMessageSize(this.maxMessageSize);
        }
        if(this.sendMsgTimeout!=null){
        	producer.setSendMsgTimeout(this.sendMsgTimeout);
        }
        //如果发送消息失败，设置重试次数，默认为2次
        if(this.retryTimesWhenSendFailed!=null){
        	producer.setRetryTimesWhenSendFailed(this.retryTimesWhenSendFailed);
        }
        
        try {
            producer.start();
            
            LOGGER.info(String.format("producer is start ! groupName:[%s],namesrvAddr:[%s]"
                    , this.groupName, this.namesrvAddr));
        } catch (MQClientException e) {
            LOGGER.error(String.format("producer is error {}"
                    , e.getMessage(),e));
            throw new RocketMQException(e);
        }
        return producer;
    }
}
```

## 4.消费者Bean配置

```
package com.clouds.common.rocketmq.consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.util.StringUtils;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.consumer.ConsumeFromWhere;
import com.clouds.common.rocketmq.constants.RocketMQErrorEnum;
import com.clouds.common.rocketmq.consumer.processor.MQConsumeMsgListenerProcessor;
import com.clouds.common.rocketmq.exception.RocketMQException;


/**
 * 消费者Bean配置
 * .<br/>
 * 
 * Copyright: Copyright (c) 2017  zteits
 * 
 * @ClassName: MQConsumerConfiguration
 * @Description: 
 * @version: v1.0.0
 * @author: zhaowg
 * @date: 2018年3月2日 下午11:48:32
 * Modification History:
 * Date             Author          Version            Description
 *---------------------------------------------------------*
 * 2018年3月2日      zhaowg           v1.0.0               创建
 */
@SpringBootConfiguration
public class MQConsumerConfiguration {
    public static final Logger LOGGER = LoggerFactory.getLogger(MQConsumerConfiguration.class);
    @Value("${rocketmq.consumer.namesrvAddr}")
    private String namesrvAddr;
    @Value("${rocketmq.consumer.groupName}")
    private String groupName;
    @Value("${rocketmq.consumer.consumeThreadMin}")
    private int consumeThreadMin;
    @Value("${rocketmq.consumer.consumeThreadMax}")
    private int consumeThreadMax;
    @Value("${rocketmq.consumer.topics}")
    private String topics;
    @Value("${rocketmq.consumer.consumeMessageBatchMaxSize}")
    private int consumeMessageBatchMaxSize;
    @Autowired
    private MQConsumeMsgListenerProcessor mqMessageListenerProcessor;
    
    @Bean
    public DefaultMQPushConsumer getRocketMQConsumer() throws RocketMQException {
        if (StringUtils.isEmpty(groupName)){
            throw new RocketMQException(RocketMQErrorEnum.PARAMM_NULL,"groupName is null !!!",false);
        }
        if (StringUtils.isEmpty(namesrvAddr)){
            throw new RocketMQException(RocketMQErrorEnum.PARAMM_NULL,"namesrvAddr is null !!!",false);
        }
        if(StringUtils.isEmpty(topics)){
        	throw new RocketMQException(RocketMQErrorEnum.PARAMM_NULL,"topics is null !!!",false);
        }
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(groupName);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.setConsumeThreadMin(consumeThreadMin);
        consumer.setConsumeThreadMax(consumeThreadMax);
		consumer.registerMessageListener(mqMessageListenerProcessor);
		/**
         * 设置Consumer第一次启动是从队列头部开始消费还是队列尾部开始消费
         * 如果非第一次启动，那么按照上次消费的位置继续消费
         */
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        /**
         * 设置消费模型，集群还是广播，默认为集群
         */
        //consumer.setMessageModel(MessageModel.CLUSTERING);
        /**
         * 设置一次消费消息的条数，默认为1条
         */
        consumer.setConsumeMessageBatchMaxSize(consumeMessageBatchMaxSize);
        try {
        	/**
        	 * 设置该消费者订阅的主题和tag，如果是订阅该主题下的所有tag，则tag使用*；如果需要指定订阅该主题下的某些tag，则使用||分割，例如tag1||tag2||tag3
        	 */
        	String[] topicTagsArr = topics.split(";");
        	for (String topicTags : topicTagsArr) {
        		String[] topicTag = topicTags.split("~");
        		consumer.subscribe(topicTag[0],topicTag[1]);
			}
            consumer.start();
            LOGGER.info("consumer is start !!! groupName:{},topics:{},namesrvAddr:{}",groupName,topics,namesrvAddr);
        }catch (MQClientException e){
            LOGGER.error("consumer is start !!! groupName:{},topics:{},namesrvAddr:{}",groupName,topics,namesrvAddr,e);
            throw new RocketMQException(e);
        }
        return consumer;
    }
}
```
## 5.消费者消息监听处理器

```
package com.clouds.common.rocketmq.consumer.processor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.common.message.MessageExt;
/**
 * 消费者消费消息路由
 * .<br/>
 * 
 * Copyright: Copyright (c) 2017  zteits
 * 
 * @ClassName: RocketMQMessageListenerConcurrentlyProcessor
 * @Description: 
 * @version: v1.0.0
 * @author: zhaowg
 * @date: 2018年2月28日 上午11:12:32
 * Modification History:
 * Date             Author          Version            Description
 *---------------------------------------------------------*
 * 2018年2月28日      zhaowg           v1.0.0               创建
 */
@Component
public class MQConsumeMsgListenerProcessor implements MessageListenerConcurrently{
	private static final Logger logger = LoggerFactory.getLogger(MQConsumeMsgListenerProcessor.class);
    
	/**
	 *  默认msgs里只有一条消息，可以通过设置consumeMessageBatchMaxSize参数来批量接收消息<br/>
	 *  不要抛异常，如果没有return CONSUME_SUCCESS ，consumer会重新消费该消息，直到return CONSUME_SUCCESS
	 */
	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		if(CollectionUtils.isEmpty(msgs)){
			logger.info("接受到的消息为空，不处理，直接返回成功");
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		}
		MessageExt messageExt = msgs.get(0);
		logger.info("接受到的消息为："+messageExt.toString());
		if(messageExt.getTopic().equals("你的Topic")){
			if(messageExt.getTags().equals("你的Tag")){
				//TODO 判断该消息是否重复消费（RocketMQ不保证消息不重复，如果你的业务需要保证严格的不重复消息，需要你自己在业务端去重）
				//TODO 获取该消息重试次数
				int reconsume = messageExt.getReconsumeTimes();
				if(reconsume ==3){//消息已经重试了3次，如果不需要再次消费，则返回成功
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}
				//TODO 处理对应的业务逻辑
			}
		}
		// 如果没有return success ，consumer会重新消费该消息，直到return success
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}
}

```
至此RocketMq配置已经全部完成，现在编写测试用例测试下。

## 6.生产者Test

```
package com.clouds.common.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.remoting.exception.RemotingException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultProductTest {
	private static final Logger logger = LoggerFactory.getLogger(DefaultProductTest.class);
	
	/**使用RocketMq的生产者*/
	@Autowired
	private DefaultMQProducer defaultMQProducer;
	
	/**
	 * 发送消息
	 * 
	 * 2018年3月3日 zhaowg
	 * @throws InterruptedException 
	 * @throws MQBrokerException 
	 * @throws RemotingException 
	 * @throws MQClientException 
	 */
	@Test
	public void send() throws MQClientException, RemotingException, MQBrokerException, InterruptedException{
		String msg = "demo msg test";
		logger.info("开始发送消息："+msg);
		Message sendMsg = new Message("DemoTopic","DemoTag",msg.getBytes());
		//默认3秒超时
		SendResult sendResult = defaultMQProducer.send(sendMsg);
		logger.info("消息发送响应信息："+sendResult.toString());
	}
}

```

现在启动SpringBootRocketMqApplication.java.
运行DefaultProductTest类的send方法发送消息，可以看到在消费者监听器中已经获取到消息了。
> 注意：发送的topic和tag，如果希望该应用收到消息，要现在application.properties中配置好rocketmq.consumer.topics值。

打印信息如下：

```
2018-03-03 00:52:39.785  INFO 14592 --- [           main] c.clouds.common.test.DefaultProductTest  : 开始发送消息：demo msg test
2018-03-03 00:52:39.836  INFO 14592 --- [           main] c.clouds.common.test.DefaultProductTest  : 消息发送响应信息：SendResult [sendStatus=SEND_OK, msgId=2F61081600002A9F00000000000A35F6, messageQueue=MessageQueue [topic=DemoTopic, brokerName=iZbp1g7kmh3b0jp1moarjoZ, queueId=0], queueOffset=2]
2018-03-03 00:52:39.853  INFO 14592 --- [       Thread-3] s.c.a.AnnotationConfigApplicationContext : Closing org.springframework.context.annotation.AnnotationConfigApplicationContext@3e694b3f: startup date [Sat Mar 03 00:52:37 CST 2018]; root of context hierarchy
2018-03-03 00:52:39.862  INFO 14592 --- [MessageThread_2] .c.c.r.c.p.MQConsumeMsgListenerProcessor : 接受到的消息为：MessageExt [queueId=0, storeSize=136, queueOffset=2, sysFlag=0, bornTimestamp=1520009559787, bornHost=/222.35.171.182:60888, storeTimestamp=1520009560327, storeHost=/47.97.8.22:10911, msgId=2F61081600002A9F00000000000A35F6, commitLogOffset=669174, bodyCRC=1945576729, reconsumeTimes=0, preparedTransactionOffset=0, toString()=Message [topic=DemoTopic, flag=0, properties={MIN_OFFSET=0, MAX_OFFSET=3, WAIT=true, TAGS=DemoTag}, body=13]]

```

## 7.项目源代码
https://gitee.com/zhaowg3/springboot-rocketmq/tree/simple/


#### 参考：
1. [分布式开放消息系统(RocketMQ)的原理与实践](https://www.jianshu.com/p/453c6e7ff81c)


## 后续：消息监听优化
如果一个消费者订阅了多个topic和tag,在上面的消息监听程序中将会出现大量的if..else，每次新增一个订阅topic，则需要修改消息监听器类；下一篇文章将通过自定义注解消除消息监听器中的if..else。

[下一篇：RocketMq消息监听程序消除大量的if..else](http://www.jianshu.com/writer#/notebooks/6196770/notes/24675068)
