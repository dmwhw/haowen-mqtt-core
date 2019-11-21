package com.haowen.mqtt.core.waiter.impl;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class CustomWaiter extends AbstractWaiter{
	private Integer timeOut;

	private String waitForTopic;
	
	private MqttMessage msg; 
	
	public static  interface CustomRule{
		public boolean matches(CustomWaiter waiter,MqttMessage message);
		public void setWaiter(CustomWaiter waiter);
		public CustomWaiter getWaiter();
	}
 	private CustomRule rule;
	
	/**
	 * 是否已经超时或者已经解决完了
	 */
	private boolean isDone=false;
	
	public CustomWaiter(Integer timeOut, String waitForTopic, CustomRule rule) {
		super();
		this.timeOut = timeOut;
		this.waitForTopic = waitForTopic;
		this.rule = rule;
		//不能为空
		rule.setWaiter(this);
	}


	public Integer getTimeOut() {
		return timeOut;
	}


	public String getWaitForTopic() {
		return waitForTopic;
	}






	public boolean collectData(String topic,MqttMessage message){
		synchronized (this) {
			if (isDone){
				return true;
			}
		}
		if (topic.equals(waitForTopic)){
			if (rule.matches(this,message)  ){
				this.msg=message;
				synchronized (this) {
					this.notify();	
				}
				return true;
			}
			
		}
		return false;
	}
	
	public MqttMessage getMsg( ){
			synchronized (this) {
			try {
				this.wait(timeOut);
				isDone=true;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
			
			//waiters.remove(this);
		return msg;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((timeOut == null) ? 0 : timeOut.hashCode());
		result = prime * result + ((waitForTopic == null) ? 0 : waitForTopic.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomWaiter other = (CustomWaiter) obj;
		if (timeOut == null) {
			if (other.timeOut != null)
				return false;
		} else if (!timeOut.equals(other.timeOut))
			return false;
		if (waitForTopic == null) {
			if (other.waitForTopic != null)
				return false;
		} else if (!waitForTopic.equals(other.waitForTopic))
			return false;
		return true;
	}


 


}
