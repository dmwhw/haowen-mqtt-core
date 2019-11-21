package com.haowen.mqtt.core.acker.impl;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

import com.haowen.mqtt.core.acker.Acker;

import java.util.Objects;

/**
 * 发布成功后，这个会收到Acker
 * @author haowen
 *
 */
public class CustomAcker implements Acker{

	private String pubTopic;
	
	private Integer timeOut=1000;

	public CustomAcker(String pubTopic, Integer timeOut,Rule rule) {
		this.pubTopic = pubTopic;
		this.timeOut = timeOut;
		this.rule=rule;
		//不让他为空
		rule.setCustomAcker(this);
	}

	private Object obj;
	
	public Rule rule;
	
	public static interface Rule{
		public boolean isMatch(Acker acker,IMqttDeliveryToken token);
		public void setCustomAcker(CustomAcker acker);
		public CustomAcker getCustomAcker();

	};
	
	
	@Override
	public boolean collectMsg(IMqttDeliveryToken token){
		/*
		 * fixme 由于回调没有主题，没办法知道是谁的回调。
		 * */
		 
		return rule.isMatch(this,token);
	}
	
	public Object get(){
		return this.obj;
	}
	
	@Override
	public  boolean waitFor(){
		synchronized (this) {
			try {
				this.wait(timeOut);
			} catch (Exception e) {
 				e.printStackTrace();
			}
		}
		return false;
	}
	@Override
	public  boolean stopWait(){
		synchronized (this) {
			try {
				this.notifyAll();
			} catch (Exception e) {
 				e.printStackTrace();
			}
		}
		return false;
	}


	public String getPubTopic() {
		return pubTopic;
	}


	public void setPubTopic(String pubTopic) {
		this.pubTopic = pubTopic;
	}


	public Integer getTimeOut() {
		return timeOut;
	}


	public void setTimeOut(Integer timeOut) {
		this.timeOut = timeOut;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CustomAcker pubAcker = (CustomAcker) o;
		return Objects.equals(pubTopic, pubAcker.pubTopic) &&
				Objects.equals(timeOut, pubAcker.timeOut);
	}

	@Override
	public int hashCode() {
		return Objects.hash(pubTopic, timeOut);
	}
}
