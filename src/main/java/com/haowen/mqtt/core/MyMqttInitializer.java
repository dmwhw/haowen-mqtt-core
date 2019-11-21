package com.haowen.mqtt.core;

import java.util.List;

import com.haowen.mqtt.core.listener.MqttConnectlLostListener;
import com.haowen.mqtt.core.listener.MqttDeliveryCompleteListener;
import com.haowen.mqtt.core.listener.MqttListener;
import com.haowen.mqtt.core.listener.MqttMsgArrivedListener;

public class MyMqttInitializer {

	public static MyMQTTClient init(MyMqttConfig config, List<MqttListener> mqttListeners,
			List<MqttConnectlLostListener> mqttConnectlLostListeners,
			List<MqttDeliveryCompleteListener> mqttDeliveryCompleteListeners,
			List<MqttMsgArrivedListener> mqttMsgArrivedListeners) {
		ReceiveCallback receiveCallback = new ReceiveCallback();
		if (mqttListeners != null) {
			receiveCallback.getMqttListeners().addAll(mqttListeners);
		}
		if (mqttConnectlLostListeners != null) {
			receiveCallback.getMqttConnectlLostListeners().addAll(mqttConnectlLostListeners);
		}
		if (mqttDeliveryCompleteListeners != null) {
			receiveCallback.getMqttDeliveryCompleteListeners().addAll(mqttDeliveryCompleteListeners);
		}
		if (mqttMsgArrivedListeners != null) {
			receiveCallback.getMqttMsgArrivedListener().addAll(mqttMsgArrivedListeners);
		}
		
		MyMQTTClient mqttClient = new MyMQTTClient(receiveCallback);

		if (config.getMqttUrl()==null||config.getMqttUrl().isEmpty()) {
			throw new RuntimeException("no mqtt url specified...");
		}

		mqttClient.setConfig(config);

		if (Boolean.TRUE.equals(config.isAutoConnectWhenStarted())) {
			mqttClient.connect();
		}
		return mqttClient;
	}

}
