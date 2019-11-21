package com.haowen.mqtt.core.waiter.impl;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * 
 * 正则超时等待器
 * 
 * 等待指定主题的指定正则内容。
 * 超时后返回null
 * @author haowen
 *
 */
public class RegexTimeOutWaiter extends AbstractWaiter {
	
	private Integer timeOut;

	private String waitForTopic;
	
	private MqttMessage msg; 
	
	private String contentRegex;
	/**
	 * 是否已经超时或者已经解决完了
	 */
	private boolean isDone=false;
	
	public RegexTimeOutWaiter(Integer timeOut, String waitForTopic, String contentRegex) {
		super();
		this.timeOut = timeOut;
		this.waitForTopic = waitForTopic;
		this.contentRegex = contentRegex;
	}


	public Integer getTimeOut() {
		return timeOut;
	}


	public String getWaitForTopic() {
		return waitForTopic;
	}




	public String getContentRegex() {
		return contentRegex;
	}


	public boolean collectData(String topic,MqttMessage message){
		synchronized (this) {
			if (isDone){
				return true;
			}
		}
		if (topic.equals(waitForTopic)&&contentRegex==null ){
			this.msg=message;
			synchronized (this) {
				this.notify();	
			}
			return true;
		}else if (topic.equals(waitForTopic)&&contentRegex!=null ){
			byte[] payload = message.getPayload(); 
			if ( payload!=null && new String(payload).matches(contentRegex)  ){
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
		result = prime * result + ((contentRegex == null) ? 0 : contentRegex.hashCode());
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
		RegexTimeOutWaiter other = (RegexTimeOutWaiter) obj;
		if (contentRegex == null) {
			if (other.contentRegex != null)
				return false;
		} else if (!contentRegex.equals(other.contentRegex))
			return false;

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
