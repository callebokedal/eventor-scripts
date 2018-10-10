import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import groovy.xml.*
import groovy.json.*
import java.text.SimpleDateFormat 

/*

Script for extracting Eventor-data for endpoint https://eventor.orientering.se/api/events

Usage:
    
    $ groovy medalOverview.groovy --keyfile <apikey.txt> -v

Source:
https://github.com/callebokedal/eventor-scripts

*/

// --------------------------- Arguments ----------------------------
def cli = new CliBuilder(usage: 'groovy medalOverview.groovy [options]', header: 'Options')
cli.h('print this message')
cli.v('verbose output or not', required: false)
cli.keyfile(args:1 , argName: 'file', required: true, 'file containing API-key')
//cli.config(args:2, argName: 'file', required: true, 'file containing configuration')

def options = cli.parse(args)
//assert options

if (options.h) {
    cli.usage()
    return
}

// Handle encrypted transport
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


// Verbose or not - for debugging
v = options.v
def verbose(String str) {
    if(v) {
        println str
    }
}

// Sjövalla FK
def organisationId = 321 // 321 = Sjövalla FK
def localClubs = commaSeparated(organisationId,3,6,12,13)
//localClubs = cfg.organisationIds.join(",")

// End points
def endPoint = "https://eventor.orientering.se/api/persons/organisations/"
def organisationEndPoint = "https://eventor.orientering.se/api/persons/organisations/" + organisationId

// Long and lat for Finnsjögården
latHome = 57.640542
lngHome = 12.135172

// Output types
TEXT = "text"
HTML = "html"

// Locale settings
Locale SWEDISH = new Locale('sv', 'SE')
df = new SimpleDateFormat("EEE d MMM YYYY' klockan 'HH:mm", SWEDISH);
dfshort = new SimpleDateFormat("EEE d MMM", SWEDISH);

// Get data from config-file
def jsonSlurper = new JsonSlurper(type: JsonParserType.LAX) // Use LAX to enable comments in config-json
//def cfg = jsonSlurper.parseText(new File(options.config).text)
//def clubId = cfg.clubId

/*
// Verbose or not
if(cfg.verbose) {
    v = cfg.verbose
}

// Output format
output = TEXT
if(cfg.output == HTML) {
    output = HTML
}
*/

/*
// Include Google Maps link or not
includeGoogleMapLink = false
if (cfg.googleMapLink) {
    includeGoogleMapLink = cfg.googleMapLink
}

// Include Eventor Message or not
includeEventorMessage = false
if (cfg.eventorMessage) {
    includeEventorMessage = cfg.eventorMessage
}

def clubId = 321 // 321 = Sjövalla FK
def localClubs = commaSeparated(clubId,3,6,12,13)
def localClubs = cfg.organisationIds.join(",")

// Days ahead
int daysAhead = 14 // Default value
if (cfg.duration) {
    daysAhead = cfg.duration
}
verbose("daysAhead: " + daysAhead)

def fromDate = new Date()
if(cfg.fromDate) {
    fromDate = Date.parse("yyyy-MM-dd", cfg.fromDate)
}

def toDate = fromDate + daysAhead

verbose("Search events between " + fromDate.format("YYYY-MM-dd") + " and " + toDate.format("YYYY-MM-dd"))

// Eventor:ClassificationIds
def cidChampionship = 1 // "Championship?"
def cidNational = 2     // "?"
def cidState = 3        // "Regional?"
def cidLocal = 4        // "Local?"
def cidClub = 5         // "Club?"
*/

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

def SEP = ";"

def url = (organisationEndPoint).toURL()
URLConnection connection = url.openConnection()
connection.setRequestProperty("ApiKey", apiKey)

InputStream inputStream = connection.getInputStream()
def res = inputStream.text

def personList = new XmlParser().parseText(res)
connection.disconnect()

// Get 
// <PersonId>;<Given name> <Family name>;<Birth year>
// Example: 
//   168533;John Doe;1995
int year = Calendar.getInstance().get(Calendar.YEAR);
def persons = personList.Person.each { node ->
    born = node.BirthDate.Date.text().substring(0,4).toInteger()
    age = year - born
    println node.PersonId.text() + SEP + node.PersonName.Given.text().trim() + " " + node.PersonName.Family.text().trim() + SEP + born + SEP + age + SEP
}

/*

USM                     Järn        Brons   Silver  Guld    Elit    Mästar
---
D/H16                   Fullföljt   100%    100%    75%     50%     30% 
D/H15                   Fullföljt   100%    100%    75%     50%     30% 

Övriga
------
D/H16                   Fullföljt   75%     50%     30%     20%     10% 
D/H14, D/H16 Kort       Fullföljt   75%     50%     20%     10% 
D/H12, D/H14 Kort       Fullföljt   50%     30%     10% 
D/H10, D/H12 Kort       Fullföljt   50%
U, Inskolning, ÖM       Fullföljt

*/



//println(persons)

//verbose("test")
//verbose(data.Person[0].PersonName.Given)
//verbose(XmlUtil.serialize(data))

/*

Result object

PersonList 
    Person
        PersonName
            Family
            Given
        PersonId
        BirthDate
            Date
        Nationality
            CountryId
        OrganisationId
        Role
            OrganisationId
            RoleTypeId
        ModifyDate
            Date
            Clock

*/

// 
// // Calculate distance (in km) to TC
// int distance(float lat1, float lng1, float lat2, float lng2) {
    // double earthRadius = 6371; // kilometers
    // double dLat = Math.toRadians(lat2-lat1);
    // double dLng = Math.toRadians(lng2-lng1);
    // double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
               // Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
               // Math.sin(dLng/2) * Math.sin(dLng/2);
    // double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    // float dist = (float) (earthRadius * c);
    // return Math.round(dist)
// }
// String decorateDistance(lat, lng, boolean googleMapLink = false, String suffix = "") {
    // dist = distance(lat, lng, latHome, lngHome)
    // if(googleMapLink) {
        // // Ex: https://www.google.com/maps/?q=15.623037,18.388672
        // return "<a href='https://www.google.com/maps/?q="+lat+","+lng+"'>" + dist + suffix + "</a>"
    // }
    // return dist + suffix
// }
// // Calculate distance (in km) to TC by lat/long
// String distanceTC(float lat1, float lng1, boolean googleMapLink = false, String suffix = "") {
    // return decorateDistance(lat1, lng1, googleMapLink, suffix)
// }
// 
// // Calculate distance (in km) to TC by position
// int distanceTC(position) {
    // // XmlParser style
    // def x = position.@'x'[0]
    // def y = position.@'y'[0]
// 
    // if ( x && y ) {
        // float lat = Float.parseFloat( y )
        // float lng = Float.parseFloat( x )
        // return distance(lat, lng, latHome, lngHome)
    // }
    // // Default, distance unknown
    // return 0
// }
// 
// // Calculate distance (in km) to TC by position
// String distanceTCString(position, boolean googleMapLink = false, String suffix = "") {
// 
    // // XmlParser style
    // def x = position.@'x'[0]
    // def y = position.@'y'[0]
// 
    // if ( x && y ) {
        // float lat = Float.parseFloat( y )
        // float lng = Float.parseFloat( x )
        // return distanceTC(lat, lng, googleMapLink, suffix)
    // }
    // return "-"
// }
// 
// String getEventURL(eventId) {
    // return "https://eventor.orientering.se/Events/Show/" + eventId
// }
// String getEventLink(name, eventId) {
    // return "<a href='" + getEventURL(eventId) + "'>" + name + "</a>"
// }
// 
// // Return string representing the event
// String eventInfo(event) {
    // result = getEventLink(event.Name.text(), event.EventId.text()) + 
        // " (" + distanceTCString(event.EventRace.EventCenterPosition, includeGoogleMapLink, " km") + "). " + 
        // getPrettyStartDateInfo(event)
    // if(includeEventorMessage) {
        // return result + getEventorMessage(event)   
    // }
    // return result
// }
// 
// String getEventorMessage(event) {
    // if(event.HashTableEntry) {
        // idx = event.HashTableEntry.findIndexOf { it.Key.text() == "Eventor_Message" }
        // if(idx != -1) {
            // return " " + event.HashTableEntry[idx].Value.text()
        // }
    // }
    // return ""
// }
// 
// // Utility method
// String getPrettyStartDateInfo(event) {
    // if(event.StartDate) {
        // d = Date.parse("yyyy-MM-dd HH:mm:ss", event.StartDate.Date.text() + " " + event.StartDate.Clock.text())
        // return dfshort.format(d).capitalize() + ".";
    // }
    // return ""
// }
// 
// String getPrettyEntryBreakInfo(event, pos) {
    // if(event.EntryBreak[pos]) {
        // d = Date.parse("yyyy-MM-dd HH:mm:ss", event.EntryBreak[pos].ValidToDate.Date.text() + " " + event.EntryBreak[pos].ValidToDate.Clock.text())
        // return df.format(d).capitalize() + ".";
    // }
    // return ""
// }
// 
// /*
// 
// // Ordinary event entry info
// // Ex: "Fr 3 nov 2017 klockan 18:00"
// // Note! Ordinary event break seems to be on pos 0, se README.md
// String eventEntryInfo(event, String prefix = "") {
    // return prefix + getPrettyEntryBreakInfo(event, 0)
// }
// 
// // Ordinary event entry info
// // Ex: "Fr 3 nov 2017 klockan 18:00"
// // Note! Late event break seems to be on pos 1, se README.md
// String eventLateEntryInfo(event, String prefix = "") {
    // return prefix + getPrettyEntryBreakInfo(event, 1)
// }
// 
// 
// def outputHTMLRow(c1, c2, c3) {
    // return "<tr><td>" + c1 + "</td><td>" + c2 + "</td><td>" + c3 + "</td></tr>"
// }
// 
// // Print result
// def printEvent(eventList) {
    // if(output == HTML) {
        // println "<table class='eventData'>"
        // println "<tr><th>Tävling</th><th>Ordinarie anmälan senast</th><th>Efteranmälan senast</th></tr>"
    // }
    // eventList.each {
        // / *it.each {
            // println it
        // }* /
        // if(output == HTML) {
            // println outputHTMLRow(eventInfo(it), eventEntryInfo(it), eventLateEntryInfo(it))
        // } else {
            // println eventInfo(it) + " " + eventEntryInfo(it, "Ordinarie anmälan: ") + " " + eventLateEntryInfo(it, "Efteranmälan: ")
        // }
        // //println "---"
    // }
    // if(output == HTML) {
        // println "</table>"
    // }
// }
// */
// 
// def eventList = data.Event
// if(cfg.maxDistance) {
    // // Filter by distance - if specified
    // printEvent(eventList.findAll { event ->
        // dist = distanceTC(event.EventRace.EventCenterPosition)
        // dist <= cfg.maxDistance
    // })   
// } else {
    // // Print all events
    // printEvent(eventList)    
// }
// 
// verbose(url.toString())

System.exit(0)
