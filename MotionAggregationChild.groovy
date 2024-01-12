/* Change Log:
v2.0 Rewrite using scheduled timers
v1.1 Make Label show status of aggregated motion.  Will require loading and saving you name again
v1.0 Initial

*/
definition (
    name: "Motion Zone Child",
    namespace: "okdale",
    author: "Dale Munn",
    description: "Aggregation for Motion / Contact devices",
    category: "Convenience",
    parent: "okdale:Motion Aggregation",
	iconUrl: "",
	iconX2Url: "",
  iconX3Url: ""
)

preferences {
	section() {
		page name: "mainPage", title: "", install: true, uninstall: true
	}
}

 def mainPage() {
	dynamicPage(name: "mainPage") {
		section(title: "") {                        
            input "myName", "string", title: "Name:", required: true, defaultValue: null, submitOnChange: true
        }
		
		section("Primary Motion") {
			input "primaryDevs", "capability.motionSensor", title: "Primary motion detectors (these can start aggregated motion and continue it)", multiple: true
           input "primaryIncr", "number", title: "If motion detected, turn the aggregated motion on for an addl X minutes", required: true, defaultValue: 5, width: 6
             input "primaryActive", "number", title: "Minimum number of active detectors", required: true, defaultValue: 1, width: 6
            input "primaryDecr", "bool", title: "Only start counting down once all primary are inactive", defaultValue: true, width: 6
            input "primaryAutoInactive", "number", title: "Auto inactive (seconds)", defaultValue: 0, width: 6
            input "primaryMax", "number", title: "Maximum countdown minutes due to primary motion sensors", defaultValue: 30, width: 6
            input "inactiveDelay", "number", title: "Inactivity delay (seconds)", defaultValue: 0, width: 6
		}
        
   		section("Secondary Motion") {
			input "secondaryDevs", "capability.motionSensor", title: "Secondary motion detectors (these can continue aggregated motion)", multiple: true
            input "secondaryIncr", "number", title: "If motion detected, keep the aggregated motion on for X minutes", required: true, defaultValue: 5
            input "secondaryDecr", "bool", title: "Only start counting down once all secondary are inactive", defaultValue: true
            input "secondaryMax", "number", title: "Maximum countdown minutes due to secondary motion sensors", defaultValue: 30
		}
		
		section("Contacts") {
			input "contactDevs", "capability.contactSensor", title: "Contact sensors", multiple: true
            input "contactOpenIncr", "number", title: "If a contact opens, initiate motion on for X minutes"
            input "contactCloseIncr", "number", title: "If a contact closes, turn the aggregated motion on for X minutes"
 //           input "contactDecr", "enum", title: "Countdown when", defaultValue: "always", options:[["always": "always"], ["closed": "all contacts are closed"], ["open": "all contacts are open"]]
//            input "contactMax", "number", title: "Maximum countdown minutes due to contact sensors", defaultValue: 30
		}

        section("Switches") {
			input "switchDevs", "capability.switch", title: "Switches", multiple: true
            input "switchOnIncr", "number", title: "If a switch turns on, turn the aggregated motion on for X minutes"
            input "switchOffIncr", "number", title: "If a switch turns off, turn the aggregated motion on for X minutes"
 //           input "switchDecr", "enum", title: "Countdown when", defaultValue: "always", options:[["always": "always"], ["off": "all switches are off"], ["on": "all switches are on"]]
            input "switchMax", "number", title: "Maximum countdown minutes due to switch changes", defaultValue: 30
		}
		
        section("Temperature") {
						input "tempDev", "capability.temperatureMeasurement", title: "Temperature Sensor", multiple: false
		}
        
        section("")  {
            input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false, width:6		
						input "logEvents", "bool", title: "Enable event logging", defaultValue: false, displayDuringSetup: false, required: false, width:6
        }
	}
}

def childDevices() {
	return getChildDevices()?.find { true }
}

def installed() {

	logDebug "Installed with settings: ${settings}"
    
	initialize()
}

def updated() {
	logDebug "Updated with settings: ${settings}"
	unsubscribe()
//	unschedule()
	initialize()
    if (settings?.debugOutput) runIn(1800,logsOff)
}

def initialize() {
		if(state.childDevice?:null)
			state.remove("childDevice")
//			state.clear()
    logDebug "initialize()"
    createChildDevices()
		if(primaryDevs) subscribe(primaryDevs, "motion", primaryHandler,  ["filterEvents": false])
    if(secondaryDevs) subscribe(secondaryDevs, "motion", secondaryHandler,  ["filterEvents": false])
    if(contactDevs) subscribe(contactDevs, "contact", contactHandler)
    if(switchDevs) subscribe(switchDevs, "switch", switchHandler)
    if(tempDev) subscribe(tempDev, "temperature", temperatureHandler)
//  log.debug("seconds=${Calendar.instance.get(Calendar.MINUTE)},${Calendar.instance.get(Calendar.SECOND)}")
		setActiveDevices()
}

def status() {
    def s = []
		if(state.primaryActivesStr) {
			s += state.primaryActivesStr
		} else {
    if (state.primaryMins?:0>0) s << "Primary ${state.primaryMins?:0}m"
    }
    if(state?.secondaryActivesStr) {
    	s += state.secondaryActivesStr
    	} else {
    if (state.secondaryMins?:0>0) s << "Secondary ${state.secondaryMins?:0}m"
    }
    if (state.contactMins?:0>0) s << "Contact ${state.contactMins?:0}m"
    if (state.switchMins?:0>0) s << "Switch ${state.switchMins?:0}m"
    return s.join(" ");
}
def refresh() {
	def isActive = getChildDevices()[0].currentValue("motion") == "active"
	state.isActive = isActive
    if (state.isActive) {
	    	if(state?.isActivePrimary || state?.isActiveSecondary ||
        	 (state.contactMins?:0)>0 || (state.switchMins?:0)>0) {
           status = status()
             logDebug "${myName} not turning off: ${status}"
            app.updateLabel("${myName} <span style=\"color:green\">${status}</span>")
            return;
        	}
        if(inactiveDelay > 0) {
            app.updateLabel("${myName} <span style=\"color:green\">delay</span>")

        	runIn(inactiveDelay, zoneinactive)
        	} else {
        	setDeviceInactive()
	      }
        
    } else {
        
        if (state.isActivePrimary || state.contactMins>0 || state.switchMins>0) {
            status = status()
            logDebug "${myName} turning ON: ${status}"
            setDeviceActive()
        }
    }        
}

def zoneinactive() {
	if(state?.isActivePrimary || state?.isActiveSecondary ||
        	 state?.contactMins>0 || state?.switchMins>0) return
	setDeviceInactive()
}
private setDeviceInactive() {
        app.updateLabel("${myName} <span style=\"color:red\">Inactive</span>")
        state.isActive = false
				if(getChildDevices()[0].currentValue("motion") == "active") {
 					getChildDevices()[0].inactive()
 						if(settings?.logEvents) sendEvent(name: "State", value: "inactive")       
            logDebug "${myName} turning off"
          }
}

private setDeviceActive() {
	app.updateLabel("${myName} <span style=\"color:green\">${status}</span>")
	isActive = getChildDevices()[0].currentValue("motion") == "active"
	if(!isActive) {
		 getChildDevices()[0].active()
 		 if(settings?.logEvents) sendEvent(name: "State", value: "active")       
     logDebug "${myName} turning on"
     }
	state.isActive = true
}
 

private void createChildDevices() {
    app.updateLabel(myName)
    def deviceName = "Mz-" + myName
		def driver =	"Motion Zone Sensor"
		childDevice = getChildDevices()[0]
	if (childDevice) {
			 childDevice.name = deviceName
    }
	if(!childDevice) {
		addChildDevice("okdale", driver, UUID.randomUUID().toString(), null, [completedSetup: true, isComponent: true, name: deviceName])
	}
}

def primaryHandler(evt) {
	state.pActives = primaryDevs?.findAll { it.currentValue("motion") == "active" }.displayName
  state.primaryActivesStr = state.pActives.size > 0 ? state.pActives.join(",") : ""
	if(settings?.logEvents) sendEvent(name:"primaryHandler", value: "$evt.value $evt.descriptionText, actives=$state.primaryActivesStr")
	if(debugOutput) log.debug("primaryHandler, value=$evt.value, desc=$evt.descriptionText, active=$state.primaryActivesStr")
//	log.debug evt.getProperties().toString()
	if(evt.value == "active") {
		if((state.pActives.size >= primaryActive) || state?.isActive) {
			state.isActivePrimary = true
		}
		if(state.isActivePrimary) {
			if((state.primaryTimer ?: 0)) {
				unschedule(primaryDelay)
				state.primaryTimer = 0
				}
			if(primaryIncr > 0) {
				state.primaryMins = (state.primaryMins ? state.primaryMins : 0) + primaryIncr
  	  		if(state.primaryMins > primaryMax) state.primaryMins = primaryMax
  	  	}
			else state.primaryMins = 0
			if(primaryAutoInactive > 0) runIn(primaryAutoInactive, setInactive)
			}
	}
	else if(evt.value == "inactive") {
//		Boolean delayTimer = !primaryDecr && (state.primaryMins ?: 0) 
		if(state.pActives.size == 0) {
			unschedule(setPrimaryInactive)
			if(primaryAutoInactive > 0) unschedule(setInactive)
			if(!(delayTimer = (state.primaryMins ?: 0))) setPrimaryInactive("primaryHandler")
			}
			else delayTimer = !primaryDecr && (state.primaryMins ?: 0)
//		log.debug "delayTimer2=$delayTimer"
		if(delayTimer) {
			state.primaryTimer = state.primaryMins > 5 ? 5 : state.primaryMins
			runIn(state.primaryTimer * 60, primaryDelay, [overwrite:false, data: [timer : state.primaryTimer]])
			}
		}
		runInMillis(50, 'refresh')
}

def setInactive() {
	setPrimaryInactive("setInactive")
	setSecondaryInactive()
	runInMillis(50, 'refresh')
	}

def setPrimaryInactive(source) {
	if(state.isActivePrimary) {
		state.isActivePrimary = false
		if(settings?.logEvents) sendEvent(name:source, value: "Primary Inactive")
		runInMillis(50, 'refresh')
		}
	if((state.primaryTimer ?: 0) != 0) {
		state.primaryTimer = 0
		unschedule(primaryDelay)
		}
	}

def primaryDelay(data) {
//log.debug("date.timer=$data.timer")
	if(state.primaryMins >= data.timer) state.primaryMins -= data.timer
	else state.primaryMins = 0
	state.primaryTimer = state.primaryMins > 5 ? 5 : state.primaryMins
	if(state.primaryTimer > 0) {
		runIn(state.primaryTimer * 60, primaryDelay, [data:[timer: state.primaryTimer]])
		runInMillis(50, 'refresh')
		}
	else if(!state.primaryActivesStr) setPrimaryInactive("primaryDelay")
	}

def secondaryHandler(evt) {
  activeDevs = secondaryDevs?.findAll { it.currentValue("motion") == "active" }
  state.secondaryActivesStr = activeDevs.size > 0 ? activeDevs.join(",") : ""
	if(debugOutput) log.debug("secondaryHandler, value=$evt.value, desc=$evt.descriptionText, active=$state.secondaryActivesStr")
	if(settings?.logEvents) sendEvent(name:"secondaryHandler", value: "$evt.descriptionText, active=$state.secondaryActivesStr")
	if(evt.value == "active") {
			if(!state.isActiveSecondary) {
				state.secondaryMins = 0
				state.secondaryTimer = 0
				if(settings?.logEvents) sendEvent(name:"setSecondaryActive", value: "Secondary Active")
				state.isActiveSecondary = true
				}
		if(secondaryIncr>0) {
			state.secondaryMins = (state.secondaryMins?:0)+secondaryIncr
			if(secondaryMax > 0) {
				max = (state.secondaryTimer ?: 0) + secondaryMax
  	  	if(state.secondaryMins > max) state.secondaryMins = max
  	  	}
  	  }
		} else if(evt.value == "inactive") {
				Boolean delayTimer = (state.secondaryMins ?: 0) && (!secondaryDecr || activeDevs.size == 0)
				if(delayTimer) {
					state.secondaryTimer = state.secondaryMins > 5 ? 5 : state.secondaryMins
					runIn(state.secondaryTimer * 60, secondaryDelay, [overwrite:false, data: [timer : state.secondaryTimer]])
				}
				else if(activeDevs.size == 0) setSecondaryInactive()
		}
		runInMillis(50, 'refresh')
}

def setSecondaryInactive() {
	if(state.isActiveSecondary) {
		state.isActiveSecondary = false
		if(settings?.logEvents) sendEvent(name:"setSecondaryInactive", value: "Secondary Inactive")
		}
	if((state.SecondaryTimer ?: 0) != 0) {
		state.SecondaryTimer = 0
		unschedule(SecondaryDelay)
		}
	zoneinactive()
	}
	
def secondaryDelay(data) {
	if(state.secondaryMins >= data.timer) state.secondaryMins -= data.timer
	else state.secondaryMins = 0
	state.secondaryTimer = state.secondaryMins > 5 ? 5 : state.secondaryMins
	if(state.secondaryTimer > 0) {
		runIn(state.secondaryTimer * 60, secondaryDelay, [data:[timer: state.secondaryTimer]])
		}
	else setSecondaryInactive()
	runInMillis(50, 'refresh')
	}
	

def contactHandler(evt) {
	if(settings?.logEvents) sendEvent(name: "contactHandler", value: "$evt.descriptionText")
	if(evt.value == "open" && contactOpenIncr>0) {
		state.contactMins = contactOpenIncr
		}
	else if(evt.value == "close" && contactCloseIncr > 0) {
		state.contactMins = contactCloseIncr
		}
	if(state.contactMins > 0) runIn(state.contactMins * 60, delayContact)
	runInMillis(50, 'refresh')
}

def delayContact() {
	state.contactMins = 0
	runInMillis(50, 'refresh')
}

def switchHandler(evt) {
    if(evt.value == "on" && switchOnIncr>0) {
        state.switchMins = (state.switchMins?:0)+switchOnIncr
    }
    if(evt.value == "off" && contactOffIncr>0) {
        state.switchMins = (state.switchMins?:0)+switchOffIncr
    }
    if (switchMax>0 && state.switchMins>switchMax) state.switchMins=switchMax
    if(state.switchMins > 0) {
    	if((state.switchTimer ?: 0) <= 0) {
    		state.switchTimer = state.switchMins
    		runIn(state.switchTimer * 60, delaySwitch)
    		}
    	}
    runInMillis(50, 'refresh')
}

def delaySwitch() {
	if(state.switchMins >= state.switchTimer) state.switchMins -= state.switchTimer
	else state.switchMins = 0
	if((state.switchTimer = state.switchMins)) runIn(state.switchTimer * 60, delaySwitch)
	runInMillis(50, 'refresh')
}

def temperatureHandler(evt) {
	if(debugOutput) log.debug("value=$evt.value, desc=$evt.descriptionText")
  getChildDevices()[0].setTemperature(evt.value)
}

def setActiveDevices() {
	state.isActive = getChildDevices()[0].currentValue("motion") == "active"
	state.pActives = primaryDevs?.findAll { it.currentValue("motion") == "active" }.displayName
  state.primaryActivesStr = state.pActives.size > 0 ? state.pActives.join(",") : ""
	if((state.isActivePrimary = state.pActives.size >= primaryActive)) {
	  activeDevs = secondaryDevs?.findAll { it.currentValue("motion") == "active" }
	  state.secondaryActivesStr = activeDevs.size > 0 ? activeDevs.join(",") : ""
	  state.isActiveSecondary = activeDevs.size > 0
	  }
	else {
		state.isActivePrimary = false
		state.isActiveSecondary = false
		}
	runInMillis(50, 'refresh')
}

def getIsActive() {
    return (childDevices?.any { sensor -> sensor.currentValue("motion") == "active" });
}

def logsOff(){
    log.warn "debug logging disabled..."
    app.updateSetting("debugOutput",[value:"false",type:"bool"])
}

private logDebug(msg) {
	if (settings?.debugOutput) {
		log.debug msg
	}
}
