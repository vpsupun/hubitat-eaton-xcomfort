/*
 * Eaton XComfort Switch
 *
 * Hubitat connecting to the Eaton XComfort switch using HTTP
 *
 */
metadata {
    definition(name: "Eaton XComfort Switch", namespace: "community", author: "Community", importUrl: "https://raw.githubusercontent.com/hubitat/HubitatPublic/master/examples/drivers/httpGetSwitch.groovy") {
        capability "Actuator"
        capability "Switch"
        capability "Sensor"
    }
}

preferences {
    section("xComfort Device Data") {
        input "URI", "text", title: "URI", required: true
        input "username", "text", title: "Username", required: true
        input "pass", "text", title: "Password", required: true
        input "zone", "text", title: "Zone", required: false
        input "devID", "text", title: "Device ID", required: false
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

def on() {
    if (logEnable) log.debug "Switching on the device, [${settings.devID}] on the zone, [${settings.zone}]"
    Map httpParams = prepareHttpParams("on")
    if (logEnable) log.debug "HTTP param received"
    try {
        httpPostJson(httpParams) { resp ->
            if (resp.success) {
                sendEvent(name: "switch", value: "on", isStateChange: true)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to off failed: ${e.message}"
    }
}

def off() {
    if (logEnable) log.debug "Switching off the device, [${settings.devID}] on the zone, [${settings.zone}]"
    Map httpParams = prepareHttpParams("off")
    try {
        httpPostJson(httpParams) { resp ->
            if (resp.success) {
                sendEvent(name: "switch", value: "off", isStateChange: true)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to off failed: ${e.message}"
    }
}

def prepareHttpParams(state) {
    def path = "/remote/json-rpc"
    def pair = "$username:$pass"
    def basicAuth = pair.bytes.encodeBase64();

    Map<String, Object> content = [
            "jsonrpc": "2.0",
            "method" : "StatusControlFunction/controlDevice",
            "id"     : 1
    ]
    content.params = ["${settings.zone}", "${settings.devID}", "${state}"]

    Map<String> headers = [
            "Content-Type": "application/json"
    ]
    headers.Authorization = "Basic " + basicAuth

    Map<String, Object> httpParams = [
            "uri"    : settings.URI,
            "headers": headers,
            "path"   : path,
            "body"   : content
    ]
    if (logEnable) log.debug "HTTP output: ${httpParams}"
    return httpParams
}