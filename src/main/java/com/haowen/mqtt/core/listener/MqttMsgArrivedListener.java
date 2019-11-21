package com.haowen.mqtt.core.listener;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.haowen.mqtt.core.MyMQTTClient;

public interface MqttMsgArrivedListener {
	public void messageArrived(MyMQTTClient client,String topic, MqttMessage message);
	
}
