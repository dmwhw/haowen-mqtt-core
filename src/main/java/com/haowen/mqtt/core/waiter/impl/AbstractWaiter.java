package com.haowen.mqtt.core.waiter.impl;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.haowen.mqtt.core.waiter.Waiter;

public abstract class AbstractWaiter implements Waiter {
	
	protected Integer timeOut;

	protected String waitForTopic;
	
	private Object msg; 
	 
 
	public Integer getTimeOut() {
		return timeOut;
	}


	public String getWaitForTopic() {
		return waitForTopic;
	}


 

	public boolean collectData(String topic,MqttMessage message){
		 
		return false;
	}
	public Object getMsg( ){
		waitFor();
		return msg;
	}


	@Override
	public boolean waitFor() {
		synchronized (this) {
		try {
			this.wait(timeOut);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
 		return false;
	}


	@Override
	public boolean stopWait() {
		synchronized (this) {
		try {
			this.notify();
		} catch ( Exception e) {
			e.printStackTrace();
		}
	}
 		return false;
	}
}
