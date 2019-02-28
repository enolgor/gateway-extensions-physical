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

import eu.interiot.gateway.commons.api.device.Attribute;

public class TypeConverter {
	public static Object arduino2iot(Attribute.Type type, long pinValue) {
		Number value = pinValue;
		switch(type) {
			case BOOLEAN:
				return value.intValue() == 0 ? false : true;
			case FLOAT:
				return value.floatValue();
			case INTEGER:
				return value.intValue();
			case STRING:
			default:
				return value.toString();
		}
	}
	
	public static long iot2arduino(Attribute.Type type, String value) {
		switch(type) {
			case BOOLEAN:
				return Boolean.parseBoolean(value) ? 1 : 0;
			case FLOAT: {
				Float floatValue = Float.parseFloat(value);
				if(floatValue < 0) floatValue = 0f;
				else if(floatValue > 5) floatValue = 5f;
				floatValue = 255f * (floatValue / 5f);
				return (long) Math.round(floatValue);
			}
			case INTEGER: {
				Number number =Integer.parseInt(value);
				return number.longValue();
			}
			case STRING:
			default: {
				long val = 0;
				try {
					if(Boolean.parseBoolean(value)) val = 1;
				}catch(Exception ex) {}
				try {
					if(((Number)(Float.parseFloat(value))).intValue() != 0) val = 1;
				}catch(Exception ex) {}
				return val;
			}
		}
	}
	
	
}
