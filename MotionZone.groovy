definition (
    name: "Motion Zone",
    namespace: "okdale",
    author: "Dale Munn",
    description: "Aggregation for Motion / Contact devices",
    category: "Convenience",
	iconUrl: "",
	iconX2Url: ""
)
def version() { return "Motion Aggregation 0.1.0" }

preferences {page name: "mainPage", title: "", install: true, uninstall: true}
def installed() {initialize()}
def updated() {initialize()}
def initialize() {
    version()
    log.debug "Initialised with settings: ${settings}"
    log.info "There are ${childApps.size()} child apps"r
    childApps.each {child ->
    log.info "Child app: ${child.label}"
    }    
}

def mainPage() {
    dynamicPage(name: "mainPage") {   
	installCheck()
	if(state.appInstalled == 'COMPLETE'){
	section (){app(name: "Motion Aggregator", appName: "Motion Aggregation Child", namespace: "okladale", title: "<b>Add a new Motion Aggregator</b>", multiple: true)}
	}
  }
}

def installCheck(){         
	state.appInstalled = app.getInstallationState() 
	if(state.appInstalled != 'COMPLETE'){
	section{paragraph "Please hit 'Done'"}
	  }
	}

