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
package eu.interiot.gateway.devicecontroller.panstamp._new;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.interiot.gateway.devicecontroller.panstamp._new.PanStampDeviceController;
import me.legrange.panstamp.Network;
import me.legrange.panstamp.NetworkException;
import me.legrange.panstamp.PanStamp;
import me.legrange.panstamp.definition.DeviceDefinition;
import me.legrange.panstamp.event.AbstractNetworkListener;
import me.legrange.panstamp.xml.ClassLoaderLibrary;
import me.legrange.swap.SerialModem;

public class PanstampDiscoveryService extends AbstractNetworkListener{
	
	private static Logger log = LogManager.getLogger(PanstampDiscoveryService.class);
	
	private final Network network;
	private final Map<Integer, PanStampDeviceController> controllerMap;
	private final Map<Integer, PanStamp> panStampMap;
	private final Map<Integer, DeviceDefinition> definitionMap;
	
	public PanstampDiscoveryService(String port, int baudRate) {
		this.controllerMap = new HashMap<>();
		this.panStampMap = new HashMap<>();
		this.definitionMap = new HashMap<>();
		this.network = Network.create(new SerialModem(port, baudRate));
		this.network.setDeviceLibrary(new ClassLoaderLibrary());
		this.network.addListener(this);
	}
	
	public synchronized void addDeviceController(int address, PanStampDeviceController controller) throws NetworkException {
		this.controllerMap.put(address, controller);
		if(panStampMap.containsKey(address)) {
			DeviceDefinition panStampDeviceDefinition;
			if(!this.definitionMap.containsKey(address)) {
				int manufacturer = Integer.parseInt(controller.getDeviceDefinition().getConfig().get("manufacturer"));
				int product = Integer.parseInt(controller.getDeviceDefinition().getConfig().get("product"));
				panStampDeviceDefinition = this.network.getDeviceLibrary().getDeviceDefinition(manufacturer, product);
				this.definitionMap.put(address, panStampDeviceDefinition);
			} else {
				panStampDeviceDefinition = this.definitionMap.get(address);
			}
			controller.onDetect(panStampDeviceDefinition, panStampMap.get(address));
		}
	}
	
	public void run() {
		try {
			this.network.open();
		} catch (NetworkException e) {
			log.error(e);
		}
	}
	
	@Override
	public synchronized void deviceDetected(Network gw, PanStamp dev) {
		int address = dev.getAddress();
		try {
		log.info(String.format("Device detected in address %d: %s", address, dev.getName()));
		this.panStampMap.put(address, dev);
			if(this.controllerMap.containsKey(address)) {
				PanStampDeviceController controller = this.controllerMap.get(address);
				DeviceDefinition panStampDeviceDefinition;
				if(!this.definitionMap.containsKey(address)) {
					int manufacturer = Integer.parseInt(controller.getDeviceDefinition().getConfig().get("manufacturer"));
					int product = Integer.parseInt(controller.getDeviceDefinition().getConfig().get("product"));
					panStampDeviceDefinition = this.network.getDeviceLibrary().getDeviceDefinition(manufacturer, product);
					this.definitionMap.put(address, panStampDeviceDefinition);
				} else {
					panStampDeviceDefinition = this.definitionMap.get(address);
				}
				this.controllerMap.get(address).onDetect(panStampDeviceDefinition, dev);
			}
		}catch(NetworkException e) {
			log.error(e);
		}
	}
	
	@Override
	public synchronized void deviceRemoved(Network gw, PanStamp dev) {
		int address = dev.getAddress();
		log.info(String.format("Device in address %d removed: %s", address, dev.getName()));
		this.panStampMap.remove(address);
		if(this.controllerMap.containsKey(address)) {
			this.controllerMap.get(address).onRemove();
		}
	}

}
