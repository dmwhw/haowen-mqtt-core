package com.haowen.mqtt.core.waiter;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface  Waiter {
	

	/**
	 * 采集数据，返回true，下次就不再采集.
	 * 此线程是由MQTT线程调用，请勿阻塞
	 * @author haowen
	 * @time 2018年11月6日下午4:31:52
	 * @Description  
	 * @param topic
	 * @param message
	 * @return
	 */
	public boolean collectData(String topic,MqttMessage message);
	
	
	/**
	 * 阻塞获取数据
	 * @author haowen
	 * @time 2018年11月6日下午4:32:19
	 * @Description  
	 * @return
	 */
	public Object getMsg( );	
	
	
	/**
	 * 阻塞的方法
	 * @author haowen
	 * @time 2018年12月3日下午5:05:37
	 * @Description 
	 * @return
	 */
	public  boolean waitFor();
	
	/**
	 * 停止阻塞线程
	 * @author haowen
	 * @time 2018年12月3日下午5:05:50
	 * @Description 
	 * @return
	 */
	public  boolean stopWait();
	
	
}
