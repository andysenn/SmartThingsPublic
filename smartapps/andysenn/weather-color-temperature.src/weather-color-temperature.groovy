/**
 *  Weather Color Temperature
 *
 *  Copyright 2017 Andy Senn
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

int SOLAR_CONSTANT = 128_000

definition(
	name: "Weather Color Temperature",
	namespace: "andysenn",
	author: "Andy Senn",
	description: "Adjust the color temperature of lights based on outdoor lighting and weather",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Outdoor sensor") {
		input "outdoorSensors", "capability.illuminanceMeasurement", required: true, multiple: true
	}
    /*
	section("Indoor lights") {
		input "indoorLights", "capability.colorControl", required: true, multiple: true
	}
    */
	section("Daytime color temperature") {
		input "daytimeHue", "number", title: "Hue", range: "0..100", required: true
		input "daytimeSat", "number", title: "Saturation", range: "0..100", required: true
	}
	section("Nighttime color temperature") {
		input "nighttimeHue", "number", title: "Hue", range: "0..100", required: true
		input "nighttimeSat", "number", title: "Saturation", range: "0..100", required: true
	}
	section("Geometry") {
		input "acceleration", "number", title: "Acceleration", range: "-100..100", defaultValue: "0", required: true
		input "ceiling", "number", title: "Ceiling (Lux)", range: "0..${SOLAR_CONSTANT}", defaultValue: SOLAR_CONSTANT, required: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(outdoorSensors, "illuminance", outdoorLightingChangeHandler)
}

def outdoorLightingChangeHandler(evt) {
	log.debug "Outdoor lighting changed - Updating indoor lighting"

	long luxSum = 0
	long lux = 0
    long sensors = 0;
	outdoorSensors.each { outdoorSensor ->
		lux = outdoorSensor.currentValue("illuminance").toInteger()
		log.debug "Sensor ${sensors} Lux: ${lux}"
		luxSum += lux
        sensors++
	}

	int luxAvg = luxSum / sensors
	log.debug "Average Lux: ${luxAvg}"

	double ratio = luxAvg / ceiling
	log.debug "Lux Ratio [Raw]: ${ratio}"

	log.debug "Acceleration: ${acceleration}"
	// TODO: Apply adjustments based on acceleration value
	double ratioAdj = ratio
	log.debug "Lux Ratio [Accelerated]: ${ratioAdj}"

	int hueDiff = (daytimeHue - nighttimeHue) * ratioAdj
    log.debug "Hue Offset: ${hueDiff}"
    
	int satDiff = (daytimeSat - nighttimeSat) * ratioAdj
    log.debug "Sat Offset: ${satDiff}"

	/*
	Daytime	| Nighttime	| Ratio	| Final
	100		| 100		| .5	| 100
	100		| 80		| .5	| 90
	80		| 100		| .5	| 90
	100		| 80		| .25	| 85
	80		| 100		| .25	| 95
	*/
	
	int hue = nighttimeHue + hueDiff
	int sat = nighttimeSat + satDiff

	log.debug "Updating Light Color: [hue: ${hue}, sat: ${sat}]"
}