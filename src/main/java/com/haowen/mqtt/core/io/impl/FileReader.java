package com.haowen.mqtt.core.io.impl;

import com.haowen.mqtt.core.io.ByteReader;
import com.haowen.mqtt.utils.FileUtils;

public class FileReader implements ByteReader {

	@Override
	public byte[] readAsByte(String name) {
		return FileUtils.getResourceFile(name);
	}

}
