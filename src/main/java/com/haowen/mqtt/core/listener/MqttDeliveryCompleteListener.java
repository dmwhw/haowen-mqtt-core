package com.haowen.mqtt.core.listener;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

import com.haowen.mqtt.core.MyMQTTClient;

public interface MqttDeliveryCompleteListener {

	public void deliveryDone(MyMQTTClient client,IMqttDeliveryToken token);
	
	
}
