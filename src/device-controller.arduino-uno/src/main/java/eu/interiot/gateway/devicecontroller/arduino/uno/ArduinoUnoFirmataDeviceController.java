/*
 * Copyright 2016-2018 Universitat Politècnica de València
 * Copyright 2016-2018 Università della Calabria
 * Copyright 2016-2018 Prodevelop, SL
 * Copyright 2016-2018 Technische Universiteit Eindhoven
 * Copyright 2016-2018 Fundación de la Comunidad Valenciana para la
 * Investigación, Promoción y Estudios Comerciales de Valenciaport
 * Copyright 2016-2018 Rinicom Ltd
 * Copyright 2016-2018 Association pour le développement de la formation
 * professionnelle dans le transport
 * Copyright 2016-2018 Noatum Ports Valenciana, S.A.U.
 * Copyright 2016-2018 XLAB razvoj programske opreme in svetovanje d.o.o.
 * Copyright 2016-2018 Systems Research Institute Polish Academy of Sciences
 * Copyright 2016-2018 Azienda Sanitaria Locale TO5
 * Copyright 2016-2018 Alessandro Bassi Consulting SARL
 * Copyright 2016-2018 Neways Technologies B.V.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.interiot.gateway.devicecontroller.arduino.uno;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.firmata4j.IODevice;
import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.PinEventListener;
import org.firmata4j.firmata.FirmataDevice;

import eu.interiot.gateway.commons.api.device.Attribute;
import eu.interiot.gateway.commons.api.device.DeviceDefinition;
import eu.interiot.gateway.commons.api.device.DeviceIO;
import eu.interiot.gateway.commons.physical.api.dmanager.Device.State;
import eu.interiot.gateway.commons.physical.api.dmanager.DeviceController;
import eu.interiot.gateway.commons.physical.api.dmanager.DeviceState;

public class ArduinoUnoFirmataDeviceController extends DeviceController implements PinEventListener{
	
	private static Logger log = LogManager.getLogger(ArduinoUnoFirmataDeviceController.class);
	
	private IODevice iodevice;
	private final Map<String, Pin> pinMap;
	
	
	public ArduinoUnoFirmataDeviceController(DeviceDefinition deviceDefinition, DeviceState deviceState) {
		super(deviceDefinition, deviceState);
		String port = super.deviceDefinition.getConfig().get("port");
		iodevice = new FirmataDevice(port);
		this.pinMap = new HashMap<>();
	}

	@Override
	public void connect() throws Exception {
		this.iodevice.start(); // initiate communication to the device
		super.deviceState.updateState(State.CONNECTING);
		this.iodevice.ensureInitializationIsDone(); // wait for initialization is done
		
		for(DeviceIO io : super.deviceDefinition.getDeviceIOs()) {
			try {
				int pinNumber = Integer.parseInt(io.getConfig().get("pin"));
				String type = io.getConfig().get("type");
				if(type == null) type = "";
				Pin pin = this.iodevice.getPin(pinNumber);
				switch(io.getType()) {
					case ACTUATOR: {
						Pin.Mode mode = null;
						if(type.equals("servo")) mode = Pin.Mode.SERVO;
						else mode = Pin.Mode.OUTPUT;
						pin.setMode(mode);
						break;
					}
					case SENSOR: {
						Pin.Mode mode = null;
						if(type.equals("analog")) mode = Pin.Mode.ANALOG;
						else mode = Pin.Mode.INPUT;
						pin.setMode(mode);
						pin.addEventListener(this);
						break;
					}
				}
				pinMap.put(io.getAttribute().getName(), pin);
			}catch(Exception ex) {
				log.error(ex);
			}
		}
		super.deviceState.updateState(State.CONNECTED);
	}

	@Override
	public void disconnect() throws Exception {
		this.iodevice.stop();
		super.deviceState.updateState(State.DISCONNECTED);
	}
	
	@Override
	public void update() throws Exception {
		for(DeviceIO io : super.deviceDefinition.getDeviceIOs()) {
			Attribute attr = io.getAttribute();
			Pin pin = this.pinMap.get(attr.getName());
			super.deviceState.updateValue(attr, TypeConverter.arduino2iot(attr.getType(), pin.getValue()));
		}
	}

	@Override
	public void action(Attribute attribute, String value) throws Exception {
		Pin pin = this.pinMap.get(attribute.getName());
		pin.setValue(TypeConverter.iot2arduino(attribute.getType(), value));
	}

	@Override
	public void onModeChange(IOEvent arg0) {
		
	}

	@Override
	public void onValueChange(IOEvent pinEvent) {
		try {
			Pin pin = pinEvent.getPin();
			Optional<DeviceIO> deviceIO = this.pinMap.entrySet().stream().filter(e -> e.getValue().getIndex() == pin.getIndex()).map(e -> super.deviceDefinition.getDeviceIOByAttributeName(e.getKey())).findFirst();
			if(deviceIO.isPresent()) {
				Attribute attr = deviceIO.get().getAttribute();
				super.deviceState.updateValue(attr, TypeConverter.arduino2iot(attr.getType(), pinEvent.getValue()));
				super.deviceState.triggerUpdate();
			}
		}catch(Exception ex) {
			log.error(ex);
		}
	}

	

}
