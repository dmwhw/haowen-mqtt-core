package com.haowen.mqtt.core.listener.impl;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.haowen.mqtt.core.MyMQTTClient;
import com.haowen.mqtt.core.acker.Acker;
import com.haowen.mqtt.core.listener.MqttListener;
import com.haowen.mqtt.core.waiter.Waiter;

/**
 * @author haowen
 *
 * 用于等待某个主题返回内容用的。
 */
public class DeliverAndWaitForResponseFactoryImpl implements MqttListener{


	private static CopyOnWriteArraySet<Waiter> waiters=new CopyOnWriteArraySet<>();

    private final static CopyOnWriteArraySet<Acker> ackerList=new CopyOnWriteArraySet<>();

	
	private final static Logger log=LoggerFactory.getLogger(DeliverAndWaitForResponseFactoryImpl.class);
 

	@Override
	public  void messageArrived(MyMQTTClient client, String topic, MqttMessage message) {
		Iterator<Waiter> iterator = waiters.iterator();
		while(iterator.hasNext()){
			try {
				Waiter next = iterator.next();
				if (next.collectData(topic, message)){
					//iterator.remove();
					waiters.remove(next);
				}
			} catch (Exception e) {
				log.error("{}",e);
			}
		}  
	}
	 
	 

	public void addWaiter(Waiter w){
		if (w==null)
		{return;}
		waiters.add(w);
	} 


	/**
	 * 添加acker并且等待
	 * @author haowen
	 * @time 2018年12月3日下午5:53:30
	 * @Description  
	 * @param acker
	 * @return
	 */
	public boolean addAckerAndWait(Acker acker){
		if (acker==null){
			return false;
		}
		ackerList.add(acker);
		return acker.waitFor();
	}
	

	@Override
	public boolean preReconnected(MyMQTTClient client, Throwable cause) {
		return true;
	}


	@Override
	public void afterReconnected(MyMQTTClient client) {
		
	}


	@Override
	public void deliveryDone(MyMQTTClient client, IMqttDeliveryToken token) {
        try {
        	token.waitForCompletion();
            Iterator<Acker> iterator = ackerList.iterator();
            while(iterator.hasNext()){
                Acker next = iterator.next();
                if (next.collectMsg(token)){
                	ackerList.remove(next);
                    next.stopWait();
                    return ;
                }
            }

        } catch ( Exception e) {
            e.printStackTrace();
        }
	}
	


	
	
}
