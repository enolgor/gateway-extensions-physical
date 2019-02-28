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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.interiot.gateway.commons.api.device.Attribute;
import eu.interiot.gateway.commons.api.device.DeviceDefinition;
import eu.interiot.gateway.commons.physical.api.dmanager.DeviceController;
import eu.interiot.gateway.commons.physical.api.dmanager.DeviceState;
import eu.interiot.gateway.commons.physical.api.dmanager.Device.State;
import eu.interiot.gateway.devicecontroller.panstamp._new.EndpointListenerImpl;
import me.legrange.panstamp.PanStamp;
import me.legrange.panstamp.Register;
import me.legrange.panstamp.definition.EndpointDefinition;
import me.legrange.panstamp.definition.RegisterDefinition;

public class PanStampDeviceController extends DeviceController{
	
	private static Logger log = LogManager.getLogger(PanStampDeviceController.class);

	private boolean isDetected;
	private PanStamp psDevice;
	private me.legrange.panstamp.definition.DeviceDefinition psDevDef;
	private final Map<Integer, Map<String, EndpointListenerImpl<?>>> registerEndpoints;
	
	public PanStampDeviceController(DeviceDefinition deviceDefinition, DeviceState deviceState) {
		super(deviceDefinition, deviceState);
		this.isDetected = false;
		this.registerEndpoints = new HashMap<>();
	}

	public void onDetect(me.legrange.panstamp.definition.DeviceDefinition psDevDef, PanStamp panStampDev) {
		this.isDetected = true;
		super.deviceState.updateState(State.READY);
		this.psDevice = panStampDev;
		this.psDevDef = psDevDef;
	}
	
	public void onRemove() {
		this.isDetected = false;
	}

	@Override
	public void connect() throws Exception {
		if(this.isDetected) {
			super.deviceState.updateState(State.CONNECTED);
			this.loadListeners();
		} else {
			log.error(String.format("Can't connect device %s, device not detected in PanStamp network", super.deviceDefinition.getId()));
		}
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void disconnect() throws Exception {
		for(Integer registerId : this.registerEndpoints.keySet()) {
			Register register = this.psDevice.getRegister(registerId);
			Map<String, EndpointListenerImpl<?>> endpointListeners = registerEndpoints.get(registerId);
			for(String endpointName : endpointListeners.keySet()) {
				register.getEndpoint(endpointName).removeListener(endpointListeners.get(endpointName));
			}
		}
		super.deviceState.updateState(State.DISCONNECTED);
	}
	
	private void addEndpointListener(int register, String endpoint, EndpointListenerImpl<?> endpointListenerImpl) {
		if(!registerEndpoints.containsKey(register)) registerEndpoints.put(register, new HashMap<>());
		this.registerEndpoints.get(register).put(endpoint, endpointListenerImpl);
	}
	
	private void loadListeners() {
		Set<Attribute> attributes = super.deviceDefinition.getDeviceIOs().stream().map(devio -> {
			Attribute attr = devio.getAttribute();
			attr.setName(attr.getName().trim().toLowerCase());
			return attr;
		}).collect(Collectors.toSet());
		for(RegisterDefinition register : this.psDevDef.getRegisters()) {
			for(EndpointDefinition ep : register.getEndpoints()) {
				String epName = ep.getName().trim().toLowerCase();
				Optional<Attribute> attrOpt = attributes.stream().filter(attr -> attr.getName().equals(epName)).findFirst();
				if(attrOpt.isPresent()) 
					this.addEndpointListener(register.getId(), ep.getName(), EndpointListenerImpl.getEndpointListenerImpl(ep.getType(), super.deviceState, attrOpt.get()));                              
			}
		}
	}
	

	@Override
	public void update() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void action(Attribute attribute, String value) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
}
