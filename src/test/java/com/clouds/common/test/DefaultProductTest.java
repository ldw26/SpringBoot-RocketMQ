package com.clouds.common.test;

import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.clouds.common.rocketmq.producer.MQProducerConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.remoting.exception.RemotingException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultProductTest {
    private static final Logger logger = LoggerFactory.getLogger(DefaultProductTest.class);

    /**
     * 使用RocketMq的生产者
     */
    @Autowired
    private DefaultMQProducer defaultMQProducer;

    /**
     * 发送消息
     * <p>
     * 2018年3月3日 zhaowg
     *
     * @throws InterruptedException
     * @throws MQBrokerException
     * @throws RemotingException
     * @throws MQClientException
     */
    @Test
    public void send() throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        String msg = "这是一个简单的信息";

        logger.info("开始发送消息：" + msg);
        Message sendMsg = new Message("DemoTopic", "tag1", msg.getBytes());

        //默认3秒超时
        SendResult sendResult = defaultMQProducer.send(sendMsg);

        System.out.println("生产者准备发送消息了");
        logger.info("消息发送响应信息：" + sendResult.toString());
    }
}











