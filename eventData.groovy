import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import groovy.xml.*
import groovy.json.*

/*

Script for extracting Eventor-data for endpoint https://eventor.orientering.se/api/events

Usage:
    
    $ groovy eventorData.groovy --keyfile <apikey.txt> --config <config.json>

Source:
https://github.com/callebokedal/eventor-scripts

*/

// --------------------------- Arguments ----------------------------
def cli = new CliBuilder(usage: 'groovy eventData.groovy [options]', header: 'Options')
cli.h('print this message')
cli.keyfile(args:1 , argName: 'file', required: true, 'file containing API-key')
cli.config(args:2, argName: 'file', required: true, 'file containing configuration')

def options = cli.parse(args)
assert options

if (options.h) {
    cli.usage()
    return
}

def sc = SSLContext.getInstance("SSL")
def trustAll = [getAcceptedIssuers: {}, checkClientTrusted: { a, b -> }, checkServerTrusted: { a, b -> }]
sc.init(null, [trustAll as X509TrustManager] as TrustManager[], new SecureRandom())
HttpsURLConnection.defaultSSLSocketFactory = sc.socketFactory
def apiKey = new File(options.keyfile).text

// Helper functions
def commaSeparated(Object... args) {
    return args.join(",")
}
assert "6,7,8" == commaSeparated(6,7,8)

v = false
def verbose(String str) {
    if(v) {
        println str
    }
}

// Definitions
def endPoint = "https://eventor.orientering.se/api/events"

// Get data from config-file
def jsonSlurper = new JsonSlurper(type: JsonParserType.LAX) // Use LAX to enable comments in config-json
def cfg = jsonSlurper.parseText(new File(options.config).text)
def clubId = cfg.clubId

// Verbose or not
if(cfg.verbose) {
    v = cfg.verbose
}

//println "clubId: " + clubId
//println "ids: " + cfg.organisationIds.join(",") 

//def clubId = 321 // 321 = Sjövalla FK
//def localClubs = commaSeparated(clubId,3,6,12,13)
def localClubs = cfg.organisationIds.join(",")

// Days ahead
int daysAhead = 14 // Default value
if (cfg.duration) {
    daysAhead = cfg.duration
    //daysAhead = Integer.parseInt(cfg.duration)
}
/*if (options.d) {
    daysAhead = Integer.parseInt(options.d)
}*/
//println "daysAhead: " + daysAhead // works

verbose("daysAhead: " + daysAhead)

//System.exit(0)

def fromDate = new Date()
if(cfg.fromDate) {
    fromDate = Date.parse("yyyy-MM-dd", cfg.fromDate)
}

def toDate = fromDate + daysAhead
/*int currentDay = Calendar.instance.with {
    time = current
    get( Calendar.DAY_OF_WEEK )
}*/
//int firstDay = Calendar.instance.getFirstDayOfWeek()    // 1
//def c = Calendar.instance.get(Calendar.DAY_OF_WEEK)     // 3 (onsdag)

println "Search events between " + fromDate.format("YYYY-MM-dd") + " and " + toDate.format("YYYY-MM-dd")
//println currentDay
//println firstDay
//println c

//System.exit(0)

// Eventor:ClassificationIds
def cidChampionship = 1 // "Championship?"
def cidNational = 2     // "?"
def cidState = 3        // "Regional?"
def cidLocal = 4        // "Local?"
def cidClub = 5         // "Club?"


// Tävlingar 
/*

Oinloggad + Göteborg:
https://eventor.orientering.se/Events?organisations=13&classifications=International,Championship,National,Regional,Local,Club&mode=List&startDate=2017-10-01&endDate=2017-10-31

Inloggad + mitt och angränsande distrikt + inkludera närtävlingar + inkludera klubbtävlingar
https://eventor.orientering.se/Events?organisations=6,13,12,3&classifications=International,Championship,National,Regional,Local,Club&mode=List&startDate=2017-10-01&endDate=2017-10-31


	https://eventor.orientering.se/Events?startDate=2017-09-23
	&endDate=2017-11-29
	&organisations=13
	&classifications=International%2CChampionship%2CNational%2CRegional%2CLocal%2CClub

https://eventor.orientering.se/Events?startDate=2017-09-23&endDate=2017-11-29&organisations=13&classifications=International,Championship,National,Regional,Local,Club
https://eventor.orientering.se/Events?startDate=2017-09-23&endDate=2017-11-29&organisations=13&classifications=International%2CChampionship%2CNational%2CRegional%2CLocal%2CClub
https://eventor.orientering.se/Events?startDate=2017-09-23&endDate=2017-11-29&organisations=13&classifications=International%2CChampionship%2CNational%2CRegional%2CLocal%2CClub

*/

def url = (endPoint + "?fromDate=" + fromDate.format("YYYY-MM-dd") + "&toDate=" + toDate.format("YYYY-MM-dd")
	+ "&classificationIds=" + commaSeparated(cidNational, cidChampionship, cidState, cidClub, cidLocal)
	//+ "&classificationIds=" + commaSeparated(cidClub, cidLocal)
	// + "&includeAttributes=true"
	//+ "&organisations=13").toURL()
	+ "&organisationIds=" + localClubs).toURL()
URLConnection connection = url.openConnection()
connection.setRequestProperty("ApiKey", apiKey)

InputStream inputStream = connection.getInputStream()
def res = inputStream.text

//def data = new XmlParser().parseText(res)
def data = new XmlSlurper().parseText(res)

// Debug
//println XmlUtil.serialize(data)

//println res
//def object = new JsonSlurper().parseText(inputStream.text)
//def object = new JsonSlurper().parseText(res)
connection.disconnect()

verbose(XmlUtil.serialize(data))		// Ok
//println XmlUtil.serialize(data[0])	// Not ok

//println data
//println data[0]
//println XmlUtil.serialize(data)
//println XmlUtil.serialize(data.EventList) // Not ok
//println data.Event // Ok
//println data.EventList // Inget


def getEventURL(eventId) {
    return "https://eventor.orientering.se/Events/Show/" + eventId
}
def getEventLink(name, eventId) {
    return "<a href='" + getEventURL(eventId) + "'>" + name + "</a>"
}

def printEvent(event) {
    event.each {
        println getEventLink(it.Name, it.EventId)
    }
}
printEvent(data.Event)


println url

System.exit(0)

/*
def siteMapLocation = "https://www.telia.se/sitemap.xml".toURL().text

def urlset = new XmlSlurper().parseText(siteMapLocation)
urlset.url.each{
    println it.loc
    println it.lastmod
    println it.priority
    println "^^^^^^^^"
}
*/