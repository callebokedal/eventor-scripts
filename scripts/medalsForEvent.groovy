import groovy.xml.XmlUtil
import static groovy.io.FileType.* // To get FILES to work
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Script to extract event data. Using the API will only retreive persons from Sjövalla FK (id=321). Probably due to GDPR.
 *
 * Usage: 
 * 		groovy medalsForEvent.groovy -f event-22536.xml
 *
 * Results can be retreived by:
 * 		curl -K api-config.txt 'https://eventor.orientering.se/api/results/organisation?organisationIds=321&eventId=22536' | xmllint --format - >> event-22536.xml
 *
 * Where api-config.txt contains info about the API-key
 * 		-H "ApiKey:...secret-key-here..."
 */

// --------------------------- Arguments ----------------------------
def cli = new CliBuilder(usage: '<this script> -[h] -[v] -a <api-file> -id EventId of Event')
cli.with {
    h longOpt: 'help', 'Show usage information'
    a longOpt: 'apiFile', 'File containing API-key', type: String, args: 1, required: true
    id longOpt: 'eventid', 'EventId for Event', type: String, args: 1, required: true
    v longOpt: 'verbose', 'Verbose output', type: boolean, args: 0, required: false
}

def options = cli.parse(args)
if (!options) {
    return
}

// Show usage text when -h or --help option is used.
if (options.h) {
    cli.usage()
    return
}

def eventId = options.id 
def verbose = options.v || false

// Get required and secret API-key
if(!new File(options.a).exists()) {
	println "File with API-key not found. Aborting."
	System.exit(1)
}
def apiKey = new File(options.a).text

// To make SSL/TLS connections work
// Ref: https://sites.google.com/a/athaydes.com/renato-athaydes/code/groovy---rest-client-without-using-libraries
def sc = SSLContext.getInstance("SSL")
def trustAll = [getAcceptedIssuers: {}, checkClientTrusted: { a, b -> }, checkServerTrusted: { a, b -> }]
sc.init(null, [trustAll as X509TrustManager] as TrustManager[], new SecureRandom())
HttpsURLConnection.defaultSSLSocketFactory = sc.socketFactory

// --------------- Variables --------------------
def baseApiUrl = "https://eventor.orientering.se"
def sjovallaOrgId = 321
def cacheDir = "cache/"

// ---------------- Utility ---------------------
public void writeToFile(def fullFileName, def content) {
	new File("$fullFileName").withWriter { out ->
		out.println content
	}
}
def getCacheFileName = { endPoint, params ->
	def fileName = cacheDir + "cache" + endPoint.replaceAll("/",".") + "_" + params.replaceAll("=","--").replaceAll("&","_") + ".xml"
}
// Test if file is cached = exists and is not older than 1 day
def isCached = { fileName ->
	def file = new File(fileName)
	if(!file.exists()) { return false }
	def difference = new Date().time - file.lastModified()
	if (difference < TimeUnit.DAYS.toMillis(1)) {
		return true
	}
	return false
}
// endPoint, ex: "/api/activities"
// params, ex: "organisationId=321&from=2018-10-01&to=2018-11-01"
// useCache: true/false, default true
def loadData = { endPoint, params, useCache=true -> 

	def cacheFileName = getCacheFileName(endPoint, params)
	if(isCached(cacheFileName)) {
		return new File(cacheFileName).text
	}

	def connection = new URL( baseApiUrl + endPoint).openConnection() as HttpURLConnection
	if(params != "")
		connection = new URL( baseApiUrl + endPoint + "?" + params)
			.openConnection() as HttpURLConnection
	
	// Set required ApiKey
	connection.setRequestProperty( 'ApiKey', apiKey)

	// get the response code - automatically sends the request
	if(connection.responseCode == 200) {
		def result = XmlUtil.serialize(connection.inputStream.text)
		if(useCache) {
			writeToFile(cacheFileName, result)
		}
		return result
	} else
		return "Could not load resource. " + connection.responseCode + ": " + connection.inputStream.text
}

// Load event data
def data = loadData("/api/results/organisation", "organisationIds=321&eventId=" + eventId, true)
def event = new XmlSlurper().parseText(data)

// Convert position in to medal info. Output also depends on event class.
def medalInfo = { eventclass, idx ->
	if(eventclass.contains("15-49")) {
		// Assume adult class/medal
		switch(idx) {
			case 1:
				return "Guld"
				break
			default:
				return ""
				break
		}	
	} else {
		// Assume youth medal
		switch(idx) {
			case 1:
				return "Guld"
				break
			case 2:
				return "Silver"
				break
			case 3:
				return "Brons"
				break
			default:
				return ""
				break
		}	
	}
}

// Convenience function
def sjovallaPosition = { idx -> idx +1 }

// Print result
def eventName = event.Event.Name.text()
println ((" " + eventName + " ").center(45, "-"))

event.ClassResult.each { ec ->
	println "\n" + ec.EventClass.Name.text() 

	ec.PersonResult.eachWithIndex { pr, idx -> 
		if( pr.Result.CompetitorStatus.@value == "OK" ) { // Check if valid status for current competitor 
			if(verbose) {
				println sjovallaPosition(idx).toString().padLeft(5) + ":" + pr.Result.ResultPosition.text().padRight(4) \
							+ pr.Person.PersonName.Given.text() + " " + pr.Person.PersonName.Family.text() + " " + medalInfo(ec.EventClass.Name.text(), sjovallaPosition(idx))
			} else {
				def medalText = medalInfo(ec.EventClass.Name.text(), sjovallaPosition(idx))
				if(medalText != "") {
					println sjovallaPosition(idx).toString().padLeft(5) + " " + (pr.Person.PersonName.Given.text() + " " \
						+ pr.Person.PersonName.Family.text()).padRight(25) + " " + "[ " + medalText.center(7) + " ]"
				}
			}
		}
		
	}
}

