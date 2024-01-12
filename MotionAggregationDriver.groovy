/*
 *	Copyright 2023 Dale Munn
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *	use this file except in compliance with the License. You may obtain a copy
 *	of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *	License for the specific language governing permissions and limitations
 *	under the License.
 *
 *
 */
def getDriverVersion() {[platform: "Universal", major: 1, minor: 0, build: 0]}

metadata
{
	definition(name: "Motion Aggregation Motion Sensor", namespace: "okdale", author: "Dale Munn", importUrl: "https://raw.githubusercontent.com/HubitatCommunity/HubConnect/master/UniversalDrivers/HubConnect-Motion-Sensor.groovy")
	{
		capability "Motion Sensor"
		capability "Temperature Measurement"
//	capability "Refresh"

//		attribute "version", "string"

//		command "sync"
		}
    preferences {
      section { // General
  //      input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
        input name: "logDescText", title: "Log Description Text", type: "bool", defaultValue: true, required: false
     }
	}
}


/*
	installed

	Doesn't do much other than call initialize().
*/
def installed()
{
	initialize()
}


/*
	updated

	Doesn't do much other than call initialize().
*/
def updated()
{
	initialize()
}


/*
	initialize

	Doesn't do much other than call refresh().
*/
def initialize()
{
//	refresh()
	state.version = getDriverVersion()
}


/*
	uninstalled

	Reports to the remote that this device is being uninstalled.
*/
def uninstalled()
{
	// Report
	parent?.sendDeviceEvent(device.deviceNetworkId, "uninstalled")
}


/*
	parse

	In a virtual world this should never be called.
*/
def parse(String description)
{
	log.trace "Msg: Description is $description"
}


/*
	refresh

	Refreshes the device by requesting an update from the client hub.
*/
def refresh()
{
	// The server will update motion status
	parent.sendDeviceEvent(device.deviceNetworkId, "refresh")
}


/*
	sync

	Synchronizes the device details with the parent.
*/
def sync()
{
	// The server will respond with updated status and details
	parent.syncDevice(device.deviceNetworkId, "motion")
	sendEvent([name: "version", value: "v${driverVersion.major}.${driverVersion.minor}.${driverVersion.build}"])
}
def active() {
     sendEvent(name: "motion", value: "active", descriptionText: "${device.displayName} motion is active")
		if(logDescText) log.info "${device.displayName} motion is active"
}

def inactive() {
     sendEvent(name: "motion", value: "inactive", descriptionText: "${device.displayName} motion is inactive")
		if(logDescText) log.info "${device.displayName} motion is inactive"
}

def setTemperature(value) {
     sendEvent(name: "temperature", value: value, descriptionText: "${device.displayName} temperature is $value")
		if(logDescText) log.info "${device.displayName} temperature is $value"
}
