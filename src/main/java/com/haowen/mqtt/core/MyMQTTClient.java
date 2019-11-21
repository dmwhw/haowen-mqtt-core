package com.haowen.mqtt.core;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.haowen.mqtt.core.io.ByteReader;
import com.haowen.mqtt.core.io.impl.FileReader;
import com.haowen.mqtt.core.listener.impl.DeliverAndWaitForResponseFactoryImpl;
import com.haowen.mqtt.core.waiter.Waiter;
import com.haowen.mqtt.core.waiter.impl.RegexTimeOutWaiter;
import com.haowen.mqtt.utils.SslUtil;

public class MyMQTTClient {
//	String mqttUrl;
//	String mqttUser;
//	String mqttpw;
//	boolean useAuth = true;
//	boolean useSSL = false;
//	String caCrt;
//	String clientCrt;
//	String clientKey;
//	boolean cleanSession = true;
//	int connectTimeOut = 30;
//	int connectReTryMaxTimes = 5;
//	int connectReTryInterval = 10000;
//	String[] onStartedSubcribeTopics;
//
//	boolean isReConnect = true;
//	Integer onStartedSubcribeTopicsQos[] = { 1 };
//	int keepAliveTime = 60000;
//	boolean isLogMessageArrived = false;
//	boolean isLogMessageArrivedBody = false;
//	boolean isSslStrict=false;
//	String clientId;
//	
	private MyMqttConfig config;
	

	private ReceiveCallback receiveCallback;

	private Map<String, Integer> topics = new ConcurrentHashMap<>();
	private MqttClient client;
	private final ExecutorService es = Executors.newCachedThreadPool();
	private final static Logger log = LoggerFactory.getLogger(MyMQTTClient.class);
	private boolean destory = false;
	private int retryTimes = 0;

	
	// first,
	{
		// if ( StringUtils.isEmpty(clientId)){
		// clientId=UUID.randomUUID().toString();
		// }

	}

	private DeliverAndWaitForResponseFactoryImpl deliverAndWaitForResponseFactoryImpl=new DeliverAndWaitForResponseFactoryImpl();
	
	
	
	public MyMQTTClient(ReceiveCallback receiveCallback) {
		super();
		this.receiveCallback = receiveCallback;
	}

	public void connect() {
		if (destory) {
			return;
		}
		if ( config.getClientId()==null||config.getClientId().isEmpty()) {
			config.setClientId( UUID.randomUUID().toString());
		}
		log.info("MQTT start to connect,user is {},clientid is {} ",config.getMqttUser(),config.getClientId());
		try {

			// host为主机名，clientid即连接MQTT的客户端ID，一般以唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
			client = new MqttClient(config.getMqttUrl(), config.getClientId(), new MemoryPersistence());

			// MQTT的连接设置
			MqttConnectOptions options = new MqttConnectOptions();
			// 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
			options.setCleanSession(config.isCleanSession());
			if (config.isUseAuth()) {
				// 设置连接的用户名
				options.setUserName(config.getMqttUser());
				// 设置连接的密码
				options.setPassword(config.getMqttpw().toCharArray());

			}
			if (config.getBytereaderClassimpl()==null){
				config.setBytereaderClassimpl("com.haowen.mqtt.core.io.impl.FileReader");
			}
			ByteReader byteReader=null;
			try {
				Class<?> clazz = Class.forName(config.getBytereaderClassimpl());
				byteReader=(ByteReader) clazz.newInstance();
				log.debug("use {} for byte reader",byteReader);
			} catch (Exception e) {
				byteReader=new FileReader();
				log.warn("fail to load {} ,byte reader use {},reason is {}",config.getBytereaderClassimpl(),FileReader.class.getName(),e.getMessage());
 			}
			 
			if (config.isUseSsl()) {
				if (config.isSslStrict()){
					options.setSocketFactory(SslUtil.getSocketFactory(config.getCaCrt(), config.getClientCrt(), config.getClientKey(), "",byteReader));
					
				}else{
					options.setSocketFactory(SslUtil.getSocketFactory2(config.getCaCrt(), config.getClientCrt(), config.getClientKey(), "",byteReader));

				}

			}
			
			// 设置超时时间 单位为秒
			options.setConnectionTimeout(config.getConnectTimeOut());
			// 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
			options.setKeepAliveInterval(config.getKeepAliveTime());

			if (receiveCallback == null) {
				// 设置回调
				receiveCallback = new ReceiveCallback();
			}
			client.setCallback(receiveCallback);
			receiveCallback.client = this;

			//注册回复拦截器类
			receiveCallback.getMqttListeners().add(deliverAndWaitForResponseFactoryImpl);
			if(config.getLastWillTopic()!=null&&!config.getLastWillTopic().isEmpty()&&config.getLastWillTopicContent()!=null){
				// setWill方法，如果项目中需要知道客户端是否掉线可以调用该方法。设置最终端口的通知消息
				options.setWill(config.getLastWillTopic(), config.getLastWillTopicContent().getBytes("UTF-8"), config.getLastWillTopicQos()==null?1:config.getLastWillTopicQos(), config.getLastWillTopicRetain()==null? false:config.getLastWillTopicRetain());
			}

			client.connect(options);
			log.info("user:{} MQTT started...",config.getMqttUser());
			retryTimes = 0;
			onConnected();
		} catch (MqttException e) {
			if (config.isReConnect()) {
				// e.printStackTrace();
				log.info("user:{} 尝试重连..{}...{},exception is {}",config.getMqttUser(), ++retryTimes, config.getConnectReTryMaxTimes(),e.getMessage(),e.toString());
				// 连接不上，重新连接几次
				try {
					Thread.sleep(config.getConnectReTryInterval());
				} catch (Exception e1) {
					// e1.printStackTrace();
				}
				if (retryTimes < config.getConnectReTryMaxTimes()) {
					connect();
				} else {
					log.error(" MQTT connectFail--{}", config.getMqttUser());
				}

			} else {
				log.error(" MQTT连接失败{}", config.getMqttUser());
				log.error("MQTT连接失败{}", e.toString());
			}
		} catch (Exception e) {
			log.error("MQTT连接失败{}",e);

		}

	}

	private void onConnected() {
		try {
			if (config.getOnStartedSubcribeTopics() == null || config.getOnStartedSubcribeTopics() .length == 0) {
				return;
			}
			for (int i = 0; i < config.getOnStartedSubcribeTopics().length; i++) {
				String topic = config.getOnStartedSubcribeTopics()[i];
				if (config.getOnStartedSubcribeTopicsQos() == null || i >= config.getOnStartedSubcribeTopicsQos().length) {
					subscribe(topic, 0);
				} else {
					subscribe(topic, config.getOnStartedSubcribeTopicsQos()[i]);
				}
			}
		} catch (Exception e) {
			log.error("{}", e);
		}
	}

	public void subscribe(final String topic, final Integer Qos) {
		es.execute(new Runnable() {
			@Override
			public void run() {
				Set<String> keySet = topics.keySet();
				if (keySet.contains(topic))
					return;
				try {
					client.subscribe(new String[] { topic }, new int[] { Qos });
					topics.put(topic, Qos);
					log.info("user:{}新增了话题监听...{} Qos{}",config.getMqttUser(), topic, Qos);
				} catch (Exception e) {
					log.error("", e);
				}
			}
		});

	}

	public void subscribeAll() {
		es.execute(new Runnable() {

			@Override
			public void run() {
				while (topics.isEmpty()) {
					return;
				}
				Set<String> keySet = topics.keySet();
				String[] topic1 = new String[topics.size()];
				keySet.toArray(topic1);

				// 订阅消息
				int[] Qos = new int[topics.size()];
				for (int i = 0; i < Qos.length; i++) {
					Qos[i] = topics.get(topic1[i]);
				}

				try {
					client.subscribe(topic1, Qos);
					log.info("user:{} 话题监听全加载...{}",config.getMqttUser(), Arrays.toString(topic1));
				} catch (Exception e) {
					log.error("", e);

				}
			}
		});
	}


	/**
	 *  发布非阻塞
	 * @author haowen
	 * @time 2018年10月29日下午6:20:59
	 * @Description 
	 * @param topic
	 * @param Qos
	 * @param payload
	 */
	public void publishAsyn(final String topic, int Qos, final String payload) {
		try {
			publishAsyn(topic, Qos, payload.getBytes("UTF-8"));
		} catch (Exception e) {
			log.info("{}", e);
		}
	}
	
	
	/**
	 *  发布阻塞,注意要订阅对应的主题
	 * @author haowen
	 * @time 2018年10月29日下午6:20:48
	 * @Description  
	 * @param topic
	 * @param Qos
	 * @param payload
	 * @return 
	 */
	public MqttMessage publishAndReturnResult(String topic, int Qos, String payload,Integer timeOut,String waitForTopic,boolean listen,String waitForContentRegex) {
		try {
			return  publishAndReturnResult( topic,  Qos,  payload.getBytes("UTF-8"),timeOut,waitForTopic, listen, waitForContentRegex);
		} catch (Exception e) {
			log.error("{}", e);
		}
		return null;
	}


	/**
	 *  发布阻塞,注意要订阅对应的主题
	 * @author haowen
	 * @time 2018年10月29日下午6:20:48
	 * @Description  
	 * @param topic
	 * @param Qos
	 * @param payload
	 * @return 
	 */
	public MqttMessage publishAndReturnResult(String topic, int Qos, byte[] payload,Integer timeOut,String waitForTopic,boolean listen,String waitForContentRegex) {
		try {
			//	
			if (!topics.keySet().contains(waitForTopic)){
				client.subscribe(new String[] { waitForTopic }, new int[] { Qos });
				log.debug("subscribe... {}",waitForTopic   );
			}
			RegexTimeOutWaiter waiter=new RegexTimeOutWaiter(timeOut, waitForTopic, waitForContentRegex);
			deliverAndWaitForResponseFactoryImpl.addWaiter(waiter);
			publish(topic,Qos,payload);
			MqttMessage msg = waiter.getMsg();
			return msg; 
		} catch (Exception e) {
			log.info("{}", e);
		}finally {
			if (!topics.keySet().contains(waitForTopic)){
				try {
					client.unsubscribe(new String[] { waitForTopic } );
					log.debug("unsubscribe... {}",waitForTopic   );

				} catch ( Exception e) {
					log.warn("unsubscribe not successful{}", e);

				}
			}
		}
		return null;
	}
	
	public void publishAndWaitForAck(String topic,byte payload,Integer timeOut){
		
	}
	
	/**
	 *  发布阻塞,注意要订阅对应的主题
	 * @author haowen
	 * @time 2018年10月29日下午6:20:48
	 * @Description  
	 * @param topic
	 * @param Qos
	 * @param payload
	 * @return 
	 */
	public Object publishWithWaiter(String topic, int Qos, byte[] payload,Integer timeOut,String waitForTopic,boolean listen,Waiter waiter) {
		try {
			//	
			if (!topics.keySet().contains(waitForTopic)){
				log.debug("subscribe... {}",waitForTopic   );
				client.subscribe(new String[] { waitForTopic }, new int[] { Qos });
			}
 			deliverAndWaitForResponseFactoryImpl.addWaiter(waiter);
			publish(topic,Qos,payload );
			return waiter.getMsg();
		} catch (Exception e) {
			log.error("{}", e);
		}finally {
			if (!topics.keySet().contains(waitForTopic)){
				try {
					client.unsubscribe(new String[] { waitForTopic } );
					log.debug("unsubscribe... {}",waitForTopic   );
				} catch ( Exception e) {
					log.warn("unsubscribe not successful{}", e);

				}
			}
		}
		return null;
	}
	
	/**
	 *  发布阻塞,注意要订阅对应的主题
	 * @author haowen
	 * @time 2018年10月29日下午6:20:48
	 * @Description  
	 * @param topic
	 * @param Qos
	 * @param payload
	 * @return 
	 */
	public Object publishWithWaiter(String topic, int Qos, String payload,Integer timeOut,String waitForTopic,boolean listen,Waiter waiter) {
		try {
			return publishWithWaiter( topic,  Qos,  payload.getBytes("UTF-8"), timeOut, waitForTopic, listen, waiter);
		} catch (Exception e) {
			log.error("{}", e);
		}
		return null;
	}
	
	  
	
	/**
	 *  发布阻塞
	 * @author haowen
	 * @time 2018年10月29日下午6:20:48
	 * @Description  
	 * @param topic
	 * @param Qos
	 * @param payload
	 */
	public void publish(String topic, int Qos, String payload) {
		try {
			publish(topic,Qos,payload.getBytes("UTF-8"));
		} catch (Exception e) {
			log.info("{}", e);
		}
	}
	

	/**
	 * 阻塞发布
	 * @author haowen
	 * @time 2018年11月28日下午1:30:30
	 * @Description  
	 * @param topic
	 * @param Qos
	 * @param payload
	 */
	public void publish(String topic, int Qos, byte[] payload) {
		try {
			MqttTopic mqttTopic = client.getTopic(topic);
			mqttTopic.publish(payload, Qos, false);
		} catch (Exception e) {
			log.info("{}", e);
		}
	}

	/**
	 * 异步发布
	 * @author haowen
	 * @time 2018年11月28日下午1:30:07
	 * @Description  
	 * @param topic
	 * @param Qos
	 * @param payload
	 */
	public void publishAsyn(final String topic, final int Qos, final byte[] payload) {
		es.execute(new Runnable() {
			@Override
			public void run() {
				try {
					MqttTopic mqttTopic = client.getTopic(topic);
					mqttTopic.publish(payload, Qos, false);
				} catch (Exception e) {
					log.info("{}", e);
				}
			}
		});
	}
	
	
	

	public void reconnect() {
		if (destory) {
			return;
		}
		if (client != null) {
			try {
				client.close();
			} catch (Exception e) {
			}
			client = null;
		}
		try {
			connect();
			subscribeAll();
		} catch (Exception e) {
			log.error("", e);

		}
	}

	@PreDestroy
	public void destory() {
		destory = true;
		try {
			client.close();
		} catch (Exception e) {

		}
		try {
			es.shutdown();
		} catch (Exception e) {
			// e.printStackTrace();
		}
		receiveCallback.shutdown();
		log.info("close MQTT...");
	}

	
	
	
	public MyMqttConfig getConfig() {
		return config;
	}

	public void setConfig(MyMqttConfig config) {
		this.config = config;

	}

 


}