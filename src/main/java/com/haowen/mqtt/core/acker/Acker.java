package com.haowen.mqtt.core.acker;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

public interface Acker {

	/**
	 * 收集情报
	 * @author haowen
	 * @time 2018年12月3日下午5:35:05
	 * @Description 
	 * @param token
	 * @return
	 */
	boolean collectMsg(IMqttDeliveryToken token);

	boolean waitFor();

	boolean stopWait();

}
