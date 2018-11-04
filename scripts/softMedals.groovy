import groovy.xml.XmlUtil
import static groovy.io.FileType.* // To get FILES to work
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

//import java.util.Calendar

/**
 * Script to calulate medals according to "Tävlingsmärken och andra utmärkelser" from SOFT.
 *
 * Usage: 
 * 		groovy softMedals.groovy 
 *
 * Results can be retreived by:
 * 		curl -K api-config.txt 'https://eventor.orientering.se/api/results/organisation?organisationIds=321&eventId=22536' | xmllint --format - >> event-22536.xml
 *
 * Where api-config.txt contains info about the API-key
 * 		-H "ApiKey:...secret-key-here..."
 */

def startTime = new Date()

// --------------------------- Arguments ----------------------------
def cli = new CliBuilder(usage: '<this script> -[h] -a <api-file> -[v]')
cli.with {
    h longOpt: 'help', 'Show usage information'
    a longOpt: 'apiFile', 'File containing API-key', type: String, args: 1, required: true
    d longOpt: 'debug', 'Debug output', type: boolean, args: 0, required: false
    v longOpt: 'verbose', 'Verbose output', type: boolean, args: 0, required: false
    c longOpt: 'colorize', 'Colorize output', type: boolean, args: 0, required: false
    y longOpt: 'year', 'Year', type: int, args: 1, required: false
    o longOpt: 'offline', 'Offline mode - try only cached files', type: boolean, args: 0, required: false
//    l longOpt: 'limit', 'Limit number of events per person', type: int, args: 1, required: false
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

// To make SSL/TLS connections work
// Ref: https://sites.google.com/a/athaydes.com/renato-athaydes/code/groovy---rest-client-without-using-libraries
def sc = SSLContext.getInstance("SSL")
def trustAll = [getAcceptedIssuers: {}, checkClientTrusted: { a, b -> }, checkServerTrusted: { a, b -> }]
sc.init(null, [trustAll as X509TrustManager] as TrustManager[], new SecureRandom())
HttpsURLConnection.defaultSSLSocketFactory = sc.socketFactory

// Get required and secret API-key
if(!new File(options.a).exists()) {
	println "File with API-key not found. Aborting."
	System.exit(1)
}
def apiKey = new File(options.a).text

// --------------- Variables --------------------
def baseApiUrl = "https://eventor.orientering.se"
def sjovallaOrgId = 321
def cacheDir = "cache/"
def isDebug = options.d || false
def isVerbose = options.v || false
def doColorize = options.c || false
def isOffline = options.o || false
int year = Calendar.getInstance().get(Calendar.YEAR).toInteger()
if (options.y && options.y.length() == 4 && year > options.y.toInteger()) {
	year = options.y.toInteger()
}

//int limit = (options.l) ? options.l : 3 
def slurper = new XmlSlurper()

/* Rules

- At least to competitors per race
- Youth - 16 years or younger (?)

*/

/*new URL("http://stackoverflow.com")
        .getText(connectTimeout: 5000, 
                readTimeout: 10000, 
                useCaches: true, 
                allowUserInteraction: false, 
                requestProperties: ['Connection': 'close'])
*/

// Debug if verbose or force is true
def debug = {str, force=false -> if(isDebug || force) println str}

public void writeToFile(def fullFileName, def content) {
	new File("$fullFileName").withWriter { out ->
		out.println content
	}
}

def getCacheFileName = { endPoint, params ->
	def fileName = cacheDir + "cache" + endPoint.replaceAll("/",".") + "_" + params.replaceAll("=","--").replaceAll("&","_") + ".xml"
}

def ageInfo = { ms ->
	TimeZone tz = TimeZone.getTimeZone("UTC")
	Calendar calendar = Calendar.getInstance()
	calendar.setTimeZone(tz)
	calendar.setTime(new Date(ms))
	return calendar.get(Calendar.HOUR) + " hours, " + calendar.get(Calendar.MINUTE) + " minutes, " + calendar.get(Calendar.SECOND) + " seconds"
}

// Test if file is cached = exists and is not older than 1 day
def isCached = { fileName ->
	def file = new File(fileName)
	if(!file.exists())
		return false
	def difference = new Date().time - file.lastModified()
	debug("File " + file.name + " is " + ageInfo(difference) + " old.")
	if (difference < TimeUnit.DAYS.toMillis(1)) {
		return true
	}
	return false
}

// endPoint, ex: "/api/activities"
// params, ex: "organisationId=321&from=2018-10-01&to=2018-11-01"
// useCache: true/false, default true
def loadData = { endPoint, params, useCache=true -> 

	debug(endPoint + params, isDebug)

	def cacheFileName = getCacheFileName(endPoint, params)
	if(isCached(cacheFileName) || isOffline) {
		debug("Using cached file: " + cacheFileName)
		debug("Last modified: " + new Date(new File(cacheFileName).lastModified()))
		return new File(cacheFileName).text
	}
	//writeToFile(cacheFileName, "test2")
	//return ""

	if(isOffline) {
		// No file found and onnline mode
		println "Offline"
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
		///def result = connection.inputStream.text
		// Cache result
		if(useCache) {
			writeToFile(cacheFileName, result)
		}
		return result
	} else {
		return "Could not load resource. " + connection.responseCode + ": " + connection.inputStream.text
		System.exit(1)
	}
}

RED = "\033[0;31m"
BLUE = "\033[0;34m"
GREEN = "\033[0;32m"
GRAY = "\033[1;30m"
/*
https://en.wikipedia.org/wiki/ANSI_escape_code
Black        0;30     Dark Gray     1;30
Blue         0;34     Light Blue    1;34
Green        0;32     Light Green   1;32
Cyan         0;36     Light Cyan    1;36
Red          0;31     Light Red     1;31
Purple       0;35     Light Purple  1;35
Brown/Orange 0;33     Yellow        1;33
Light Gray   0;37     White         1;37
 */
def colorize = { str, color ->
	def NC = "\033[0m"
	if(doColorize) {
		return color + str + NC
	} else {
		return str
	}
}

//def activities = loadData("/api/activities", "organisationId=321&from=2018-10-01&to=2018-11-01")
//println result

def sjovallaMembers = slurper.parseText(loadData("/api/persons/organisations/" + sjovallaOrgId, "includeContactDetails=false"))
///def sjovallaMembers = loadData("/api/persons/organisations/" + sjovallaOrgId, "includeContactDetails=false")
///def event = new XmlSlurper().parseText(file.text)
//debug(sjovallaMembers)


// Debug XML-tree
// TODO: Not done!
/*def xmlDebug = { it ->
	println it.name() + ", " + it.getClass()
	it.children().each { it2 ->
		println it2.name() + ", " + it2.getClass()
		if(it2.getClass() instanceof groovy.util.slurpersupport.NodeChild)
			return xmlDebug(it2)
		else
			println XmlUtil.serialize(it2)
	}
}*/

// Return birth year given a Person-object
def birthYearForPerson = { it -> it.BirthDate.Date.text().substring(0,4).toInteger() }
assert 2018 == birthYearForPerson(slurper.parseText('<Person><BirthDate><Date>2018-01-01</Date></BirthDate></Person>'))

// Get age given birth year
def getAge = { birthYear -> return (year - birthYear) }
assert (year - 16) == getAge(16)

// Is youth if age is 16 or less
def isYouthAge = { birthYear -> return (getAge(birthYear) <= 16) }
assert true == isYouthAge(year - 15)
assert true == isYouthAge(year - 16)
assert false == isYouthAge(year - 17)

// Soft Rules
// ----------
// Rule: At least two competitors for given race in ResultList
// "Klassen ska bestå av minst två startande"
def numberOfCompetitorsRule = { 

	try {
		return it.ClassResult.EventClass.ClassRaceInfo.@noOfStarts.toInteger() >= 2 
	} catch (Exception ex) {
		debug("Problem with EventId: " + it.Event.EventId, isDebug)
		debug("https://eventor.orientering.se/Events/ResultList?eventId=" + it.Event.EventId.text(), isDebug)
		debug(XmlUtil.serialize(it.ClassResult.EventClass), isDebug)
		debug(" # # # Error! " + ex, isDebug)
		return false
	}
}

// Rule: Only Organiser from Sweden for given ResultList
// "Meritering gäller endast svenska tävlingar"
// Note! Assuming that multiple organisations share the same nationality
//def swedishOrganisationRule = { it.Event.Organiser.Organisation[0].CountryId.@value == "752" }
def isSwedishOrganisation = { it.Event.Organiser.Organisation[0].CountryId.@value == "752" }

// Rule: 
// "Meritering för tävlingsmärket gäller endast vid individuell tävling i orientering"
//def onlyIndividualRule = { it.ClassResult.EventClass.@teamEntry == "N" }
def isIndividualEvent = { it.ClassResult.EventClass.@teamEntry == "N" }
assert false == isIndividualEvent(slurper.parseText('<Root><ClassResult><EventClass teamEntry="Y">Testing</EventClass></ClassResult></Root>'))
assert true == isIndividualEvent(slurper.parseText('<Root><ClassResult><EventClass teamEntry="N">Testing</EventClass></ClassResult></Root>'))

// Rule:
// No MtbO-events
def isMountainbikeEvent = { it.Event.Name.text().contains("MtbO") }
assert true == isMountainbikeEvent(slurper.parseText('<Root><Event><Name>MtbO, Svenska Cupen, # 2, medel</Name></Event></Root>'))

// TODO: Missing rules:
// "För att ta guldmärke, elitmärke och mästarmärke ska startmellanrummet vara minst en minut"
// "Banan ska ha rätt svårighet för tävlingsklassen"

// Calculation rule
// "Vid uträkning av segrartid och ”procenttid” höjs överskjutande sekunder till närmast hela minut."


// Rule: Race completed correct given race in ResultList
def raceCompleted = { 
	//debug("Race " + it.Event.Name.text() + " " + (it.ClassResult.PersonResult.Result.CompetitorStatus.@value.text() == "OK" ) , true)
	//it.ClassResult.PersonResult.RaceResult.CompetitorStatus.@value.text() == "OK" 
	it.ClassResult.PersonResult.Result.CompetitorStatus.@value.text() == "OK" 
}

// Conveniance funtion to test if given Event is a single day event or not
def isSingleDayEvent = { it.Event.@eventForm.text() == "IndSingleDay" }
assert true == isSingleDayEvent(slurper.parseText('<Root><Event eventForm="IndSingleDay">Testing</Event></Root>'))
assert false == isSingleDayEvent(slurper.parseText('<Root><Event eventForm="IndMultiDay">Testing</Event></Root>'))

public enum Medal {
    MASTER(6,'Mäster'),
    ELITE(5,'Elit'),
    GOLD(4,'Guld'),
    SILVER(3,'Silver'),
    BRONZE(2,'Brons'),
    IRON(1,'Järn'),
    NONE(0,'-'),
    UNKNOWN(-1,'?')
 
    final Integer weight;
    final String name;
 
    private Medal(Integer weight, String name) {
        this.weight = weight;
        this.name = name;
    }
    /*
    static getMedalEnum(weight) {
        Medal.grep{it.weight == weight}[0]
    } */
    public Integer getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return getName();
    }
}

// Home made representation of a Person
// Simplified 
class Person {
    String name // Full name
    Integer born
    Integer age
    Integer personId
    List eventResults = []  	// List of event results for competitor
}
// Home made representation of an Event 
// Flatten, simplified
class Event {
	String name 				// Name of event
	Integer eventId
	String classShortName
	Integer classTypeId
	Integer highAge
	Integer lowAge
	Integer position 			// Result position (if succeeded)
	String competitorStatus 	// Status of race: OK, MisPunch, DidNotStart, ...
	Integer noOfCompetitors 	
	String raceDistance 		// Type of distance event
	String raceTime				// Time for competitor. Format: hh:mm:ss
	String timeDiff 			// Time diff compared to winner. Format: hh:mm:ss
	String date 				// Dete of event
	Boolean swedishEvent 		// If swedish organiser or not
	Boolean individualEvent 	// If team or individual event
	Medal medal 				// Medal given for current event
}

// Utility method for handling time
def padTime = {
	if(it == "") {
		return "00:00:00"
	}
	if(it.length() < 5) {
		return "00:" + it.padLeft(5, "0")
	} else if(it.length() < 6) {
		return "00:" + it
	} else if(it.length() < 8) {
		return it.padLeft(8, "0")
	}
	return it
}
assert "00:00:00" == padTime("") 			// -> 00:00:00
assert "00:00:01" == padTime("0:01") 		// -> 00:0 +
assert "00:00:10" == padTime("0:10") 		// -> 00:0 +
assert "00:01:00" == padTime("1:00") 		// -> 00:0 +
assert "00:10:00" == padTime("10:00") 		// -> 00: +
assert "01:11:57" == padTime("1:11:57") 	// -> 0 +
assert "01:00:00" == padTime("01:00:00")

// Conveniance method to calculate millis from time string ("hh:mm:ss" -> milliseconds)
def timeToMilliseconds = {
	String t = padTime(it) // hh:mm:ss
	return t.substring(0,2).toInteger() * 60 * 60 * 1000 + t.substring(3,5).toInteger() * 60 * 1000 + t.substring(6,8).toInteger() * 1000
}
assert 3600000 == timeToMilliseconds("01:00:00")
assert 60000 == timeToMilliseconds("00:01:00")
assert 1000 == timeToMilliseconds("00:00:01")

// Calculate difference between competitor and winner
def calculateRelativeTime = { raceTime, diffTime ->
	long competitorMillis = timeToMilliseconds( raceTime )
	long diffMillis = timeToMilliseconds( diffTime )
	long winnerMillis = competitorMillis - diffMillis

	def percentage = 0
	if (winnerMillis > 0) {
		percentage = (competitorMillis - winnerMillis) / winnerMillis
	}
	return Math.round (percentage * 100)

	// 125%
	// |------ winner time ---> | ------ winner time ---> |
	// |- - - - - - - - - - - - - - - - - - competitor - - - - -> |
	// |                        | - - - - - - diff - - - - - - -> |
	// |------ winner time ---> |             120%                |

	// 100%
	// |------ winner time ---> | ------ winner time ---> |
	// |- - - - - - - - - - - - - - - - competitor - - -> |
	// |                        | - - - - - diff - - - -> |
	// |------ winner time ---> |           100%          |

	// 75%
	// |------ winner time ---> | ------ winner time ---> |
	// |- - - - - - - - - - - - - - competitor -> |
	// |                        | - - - diff - -> |
	// |------ winner time ---> |       75%       |

}

// Table with medal results
def resultTable = []

def hasRaceResults = { it.ClassResult.EventClass.HashTableEntry.Key.text() == "Eventor_ResultListMode" && it.ClassResult.EventClass.HashTableEntry.Value.text() == "UnorderedNoTimes" }

// Get result for given ResultList
// Return
//   -1 If race not completed (CompetitorStatus != "OK")
//    0 For races without results (like Inskolning?)
//    <position> For other
def resultPositionForSingleDayEvent = {
	// it.name() == "ResultList"

	if(!raceCompleted(it)) {
		return -1
	} 
	if( hasRaceResults(it) ) {
		return 0
	}
	return it.ClassResult.PersonResult.Result.ResultPosition
}

// Calculate position for given PersonResult
// More complex compared to single day results
// Return
//   -1 If race not completed (CompetitorStatus != "OK")
//    0 For races without results (like Inskolning?)
//    <position> For other
def resultPositionForMultiDayEvent = { 
	if(it.RaceResult.Result.ResultPosition != "") {
		return it.RaceResult.Result.ResultPosition
	} else {
		return -1
	}
}

// Conveniance method to default empty time values to "0"
def defaultTime = { time ->
	if(time == "") {
		return "0:00"
	}
	return time
}


/*

SOFT rules 2018
---------------

USM                     Järn        Brons   Silver  Guld    Elit    Mästar  Kod
---
D/H16                   Fullföljt   100%    100%    75%     50%     30%     USM_16    
D/H15                   Fullföljt   100%    100%    75%     50%     30%     USM_15

Övriga
------
D/H16                   Fullföljt   75%     50%     30%     20%     10%     DH16
D/H14, D/H16 Kort       Fullföljt   75%     50%     20%     10%             DH14_DH16K
D/H12, D/H14 Kort       Fullföljt   50%     30%     10%                     DH12_DH14K
D/H10, D/H12 Kort       Fullföljt   50%                                     DH10_DH12K
U, Inskolning, ÖM       Fullföljt                                           U_I_OPEN

Sjövalla rules 2018
-------------------
Sprint/KM 16            Fullföljt   75%                                     SPECIAL_16
Sprint/KM 14            Fullföljt   75%                                     SPECIAL_14
Sprint/KM 12            Fullföljt   50%                                     SPECIAL_12
Sprint/KM 10            Fullföljt   50%                                     SPECIAL_10
Sprint/KM Övriga        Fullföljt                                           SPECIAL_UIO

*/


// Best guess of medal according to SOFT rules and poor object model
def medalByClassification = { classification, percentage ->
	if(classification == "NONE") {
		debug("medalByClassification: NONE", isDebug)
		return Medal.NONE
	}
    switch(classification) {
        case ["USM_16", "USM_15"]:
            switch(percentage) { 
                case { it < 30 }: return Medal.MASTER; break; 
                case { it < 50 }: return Medal.ELITE; break;
                case { it < 75 }: return Medal.GOLD; break;
                case { it < 100 }: return Medal.SILVER; break;
                case { it < 100 }: return Medal.BRONZE; break;
                default: return Medal.IRON; break;
            }
            break
        case "DH16":
            switch(percentage) { 
                case { it < 10 }: return Medal.MASTER; break; 
                case { it < 20 }: return Medal.ELITE; break;
                case { it < 30 }: return Medal.GOLD; break;
                case { it < 50 }: return Medal.SILVER; break;
                case { it < 75 }: return Medal.BRONZE; break;
                default: return Medal.IRON; break;
            }
            break
        case "DH14_DK16K":
            switch(percentage) { 
                case { it < 10 }: return Medal.ELITE; break;
                case { it < 20 }: return Medal.GOLD; break;
                case { it < 50 }: return Medal.SILVER; break;
                case { it < 75 }: return Medal.BRONZE; break;
                default: return Medal.IRON; break;
            }
            break
        case "DH12_DK14K":
            switch(percentage) { 
                case { it < 10 }: return Medal.GOLD; break;
                case { it < 30 }: return Medal.SILVER; break;
                case { it < 50 }: return Medal.BRONZE; break;
                default: return Medal.IRON; break;
            }
            break
        case "DH10_DK12K":
            switch(percentage) { 
                case { it < 50 }: return Medal.BRONZE; break;
                default: return Medal.IRON; break;
            }
            break
        case ~/^U_I_OPEN.*|SPECIAL_U_I_OPEN/:
            return Medal.IRON
            break
        case ~/^SPECIAL_16_USM/:
            switch(percentage) { 
                case { it < 100 }: return Medal.BRONZE; break;
                default: return Medal.IRON; break;
            }
            break
        case ~/^SPECIAL_16|^SPECIAL_14/:
            switch(percentage) { 
                case { it < 75 }: return Medal.BRONZE; break;
                default: return Medal.IRON; break;
            }
            break
        case ~/^SPECIAL_12|^SPECIAL_10/:
            switch(percentage) { 
                case { it < 50 }: return Medal.BRONZE; break;
                default: return Medal.IRON; break;
            }
            break
        case ~/^SPECIAL_.*/:
            return Medal.IRON; break;
        default:
            println "Unknown classification: " + classification
            return Medal.UNKNOWN
            break
    }
}
assert Medal.MASTER == medalByClassification("USM_16", 23)
assert Medal.ELITE == medalByClassification("USM_16", 43)
assert Medal.GOLD == medalByClassification("USM_16", 53)
assert Medal.SILVER == medalByClassification("USM_16", 83)
assert Medal.IRON == medalByClassification("USM_16", 103)
assert Medal.BRONZE == medalByClassification("SPECIAL_16", 73)
assert Medal.IRON == medalByClassification("SPECIAL_16", 76)
assert Medal.IRON == medalByClassification("SPECIAL_U_I_OPEN", 23)
assert Medal.IRON == medalByClassification("SPECIAL_U_I_OPEN", 103)
assert Medal.NONE == medalByClassification("NONE", 103)

// Method to guess classification
// Can't be 100% sure about correct classificiation, since the is not typed
def guessClassification = {shortName, distance, eventName, status ->
	if(status != "OK") {
		debug("Race not OK - return Medal.NONE", isDebug)
		return "NONE"
	}
    def lowerCaseEventName = eventName.toLowerCase()
    shortName = shortName.trim()
    if(distance.toLowerCase() == "sprint" || lowerCaseEventName.contains("-km")) {
        // Special case:
        // - Sprint or "klubbmästerskap"
        switch(shortName) {
            case ~/.*16\w{0,1}|.*15$|.*21\w{0,1}|.*15-49\w{0,1}|[H|D]15-.*/: 
				if(lowerCaseEventName.contains("-sm")) {
					return "SPECIAL_16_USM"; 
				} else {
            		return "SPECIAL_16"; 	
				}
            	break;
            case ~/.*14\w{0,1}|[H|D]14-.*|.*13$|[H|D]13-.*/: return "SPECIAL_14"; break;
            case ~/.*12\w{0,1}|.*11$/: return "SPECIAL_12"; break;
            case ~/.*10\w{0,1}/: return "SPECIAL_10"; break;
            case ~/^ÖM\d{1}$|^DU\d{1}$|^HU\d{1}$|[Ö|U]\d{1}$|^Insk.*/: 
                return "SPECIAL_U_I_OPEN"; 
                break;
            default: return "UNKNOWN_SPECIAL"; break;
        }
    } else if(lowerCaseEventName.contains("-sm")) {
        switch(shortName) {
            case ~/.*16$/: return "USM_16"; break;  // DH16
            case ~/.*15$/: return "USM_15"; break;  // DH15
            default: return "UNKNOWN_USM"; break;   // Unknown
        }
    } else {
        switch(shortName) {
            case ~/.*16$|.*15$|.*15-\w{0,2}/: return "DH16"; break;             // D16, H16, D15, H15																								
            case ~/.*14$|.*14-\w{0,2}|.*13$|.*13-\w{0,2}|.*16K/: return "DH14_DK16K"; break; // D14, H14, D16K, H16K, DH13
            case ~/.*12$|.*12-\w{0,2}|.*11$|.*14K/: return "DH12_DK14K"; break; // D12, H12, D14K, H14K, DH11
            case ~/.*10$|.*12K/: return "DH10_DK12K"; break; // D10, H10, D12K, H12K
            case ~/^ÖM\d{1}$|^DU\d{1}$|^HU\d{1}$|[Ö|U]\d{1}$|^Insk.*/: 
                return "U_I_OPEN"; 
                break;
            case ~/B\d{0,2}$|O|V|Röd|Mellan|Korta|Kort|Svart.*|ÖM\w{0,}|ÖM .*$|Gul\/Orange/: 
                // Hardcoded cases due to poor data
                return "U_I_OPEN_S"; // U_I_OPEN_S(pecial) 
                break;
            default: return "UNKNOWN"; break;
        }
   }
}
assert "SPECIAL_U_I_OPEN" == guessClassification("U1", "Middle", "Lång-KM för OK Landehof och Sjövalla FK", "OK")
assert "SPECIAL_16_USM" == guessClassification("D16", "Sprint", "Ungdoms-SM, sprint", "OK")
assert "SPECIAL_16_USM" == guessClassification("D15", "Sprint", "Ungdoms-SM, sprint", "OK")
assert "SPECIAL_14" == guessClassification("D14", "Sprint", "Sprint-KM för Landehof och Sjövalla", "OK")
assert "SPECIAL_16_USM" == guessClassification("D16", "Sprint", "Ungdom-SM, sprint", "OK")


 // 2018-08-31 Ungdoms-SM, sprint → [pos: 46, status: OK, competitors: 98, raceDistance: Sprint, swe: true, individual: true, raceTime: 19:20, diff: 4:58, 
 //            Elit, 35 %, shortName: D16, highAge: 16, typeId: 17 https://eventor.orientering.se/Events/ResultList?eventId=16819&groupBy=EventClass ]

def getMedal = {shortName, distance, eventName, percentage, status, competitors, swe, individual ->
	debug("getMedal: " + shortName + ", " + distance + ", " + eventName, isDebug)
	if(!individual || competitors.toInteger() < 2 || !swe) {
		debug("No medal according to rules - Medal.NONE! Individual: " + individual + ", competitors < 2: " + (competitors.toInteger() < 2) + ", swe: " + swe, isDebug)
		return Medal.NONE
	}
	def classification = guessClassification(shortName, distance, eventName, status)
	if(eventName.toLowerCase().contains("sprint")) {
		debug("getMedal - classification (sprint): " + classification, isDebug)	
	}
	switch(classification) {
		case ~/^UNKNOWN.*/:
			debug("Could not match medal for " + classification + ", " + shortName + ", " + eventName + ", " + status + ", " + distance, isDebug)
			System.exit(1)
			break
	}
	return medalByClassification(classification, percentage)
}
assert Medal.NONE == getMedal("U1","Middle","Team test", 172,"OK", 10, true, false)
assert Medal.NONE == getMedal("U1","Middle","International test", 172,"OK", 10, false, true)
assert Medal.NONE == getMedal("U1","Middle","Too few competitors test", 172,"OK", 1, true, true)
assert Medal.IRON == getMedal("U1","Middle","Pepparkaksluffen", 172,"OK", 10, true, true)
assert Medal.IRON == getMedal("U1","Middle","Pepparkaksluffen", 172,"OK", 10, true, true)
assert Medal.IRON == getMedal("U1","Middle","Pepparkaksluffen", 172,"OK", 10, true, true)
assert Medal.IRON == getMedal("U1","Middle","Lång-KM för OK Landehof och Sjövalla FK", 41,"OK", 5, true, true)
assert Medal.BRONZE == getMedal("D16","Sprint","Ungdoms-SM, sprint", 35,"OK", 10, true, true)
assert Medal.SILVER == getMedal("H14","Middle","Orintos vårtävling, medel", 41,"OK", 10, true, true)
assert Medal.BRONZE == getMedal("D15","Sprint","Ungdoms-SM, lång", 24,"OK", 10, true, true)
assert Medal.NONE == getMedal("D16","Sprint","Ungdom-SM, sprint", 24,"OK", 1, true, true)
// D/H16 - Sprint
assert Medal.BRONZE == getMedal("D16","Sprint","Ungdom-SM, sprint", 34,"OK", 10, true, true)
assert Medal.BRONZE == getMedal("D16","Sprint","Ungdom-SM, sprint", 54,"OK", 10, true, true)
assert Medal.BRONZE == getMedal("D16","Sprint","Ungdom-SM, sprint", 84,"OK", 10, true, true)
assert Medal.BRONZE == getMedal("D16","Sprint","Ungdom-SM, sprint", 84,"OK", 10, true, true)
assert Medal.IRON == getMedal("D16","Sprint","Ungdom-SM, sprint", 124,"OK", 10, true, true)
// D/H15 - Sprint
assert Medal.BRONZE == getMedal("D15","Sprint","Ungdom-SM, sprint", 24,"OK", 10, true, true)
assert Medal.BRONZE == getMedal("D15","Sprint","Ungdom-SM, sprint", 34,"OK", 10, true, true)
assert Medal.BRONZE == getMedal("D15","Sprint","Ungdom-SM, sprint", 54,"OK", 10, true, true)
assert Medal.BRONZE == getMedal("D15","Sprint","Ungdom-SM, sprint", 84,"OK", 10, true, true)
assert Medal.BRONZE == getMedal("D15","Sprint","Ungdom-SM, sprint", 84,"OK", 10, true, true)
assert Medal.IRON == getMedal("D15","Sprint","Ungdom-SM, sprint", 124,"OK", 10, true, true)
// D/H16 
assert Medal.MASTER == getMedal("D16","Long","Testtävling", 4,"OK", 10, true, true)
assert Medal.ELITE == getMedal("D16","Long","Testtävling", 14,"OK", 10, true, true)
assert Medal.GOLD == getMedal("D16","Long","Testtävling", 24,"OK", 10, true, true)
assert Medal.SILVER == getMedal("D16","Long","Testtävling", 34,"OK", 10, true, true)
assert Medal.BRONZE == getMedal("D16","Long","Testtävling", 54,"OK", 10, true, true)
assert Medal.IRON == getMedal("D16","Long","Testtävling", 84,"OK", 10, true, true)
// D/H15
assert Medal.MASTER == getMedal("D15","Long","Testtävling", 4,"OK", 10, true, true)
assert Medal.ELITE == getMedal("D15","Long","Testtävling", 14,"OK", 10, true, true)
assert Medal.GOLD == getMedal("D15","Long","Testtävling", 24,"OK", 10, true, true)
assert Medal.SILVER == getMedal("D15","Long","Testtävling", 34,"OK", 10, true, true)
assert Medal.BRONZE == getMedal("D15","Long","Testtävling", 54,"OK", 10, true, true)
assert Medal.IRON == getMedal("D15","Long","Testtävling", 84,"OK", 10, true, true)
// H14
assert Medal.BRONZE == getMedal("H14","Sprint","Sprint-KM för Landehof och Sjövalla", 44,"OK", 10, true, true)
assert Medal.IRON == getMedal("H14","Sprint","Sprint-KM för Landehof och Sjövalla", 84,"OK", 10, true, true)

//Mäster 2018-09-01 Ungdoms-SM, lång [D15]  → 24% https://eventor.orientering.se/Events/ResultList?eventId=16820&groupBy=EventClass
 

// List of personId's to exclude
// For some reason Eventor returns these for Sjövalla
def eventorErrors = {
	it.PersonId == 118002 // Filip Vallentin -> Göteborg-Majorna OK
}

// Conveniance method to only test certain persons
def testPersons = { 
	true // Allow all persons, should be default when done
	//it.PersonId == 168549 || it.PersonId == 163325 || it.PersonId == 120920 || 147715
	//it.PersonId == 168549 || it.PersonId == 120920 || it.PersonId == 104599
	//it.PersonId == 23446 || it.PersonId == 71027 || it.PersonId == 70083
}

// Loop only youth memebers
sjovallaMembers.Person.findAll { isYouthAge(birthYearForPerson(it)) }.findAll{ !eventorErrors(it) }.sort{ birthYearForPerson(it) }.findAll{ testPersons(it) }.eachWithIndex { it, idx ->


	// Person object to work with for each iteration
	def person = new Person(name: it.PersonName.Given.text() + " " + it.PersonName.Family.text(), 
		born: birthYearForPerson(it), 
		age: getAge(birthYearForPerson(it)),
		personId: it.PersonId.toInteger())


	// 1. Get all year events for current person
	def personEvents = slurper.parseText(loadData("/api/results/person","personId=" + it.PersonId.text() + "&fromDate=" + year + "-01-01&toDate=" + year +"-12-31"))
	//def personEvents = new XmlSlurper().parseText(loadData("/api/results/person","personId=" + it.PersonId.text() + "&fromDate=" + year + "-01-01&toDate=" + year +"-12-31&top=1"))

	debug("" + colorize(it.PersonName.Given.text() + " " + it.PersonName.Family.text(), BLUE) + " (Born: " + birthYearForPerson(it) + ", " + getAge(birthYearForPerson(it)) 
		+ " years, PersonId: " + it.PersonId.text() + ")", isDebug)

	//return

	if(personEvents.children().size() == 0) {
		debug("  Inga tävlingar registrerade under perioden", isDebug)
	}

	// Overview of all events this year for current person
	personEvents.ResultList.each{
		def eventName = it.Event.Name.text()
		def swe = isSwedishOrganisation(it)
		def individual = isIndividualEvent(it)
		def classShortName = it.ClassResult.EventClass.ClassShortName.text()
		def classTypeId = it.ClassResult.EventClass.ClassTypeId.text()
		def highAge = it.ClassResult.EventClass.@highAge.text()
		def lowAge = it.ClassResult.EventClass.@lowAge.text()

		debug(" * " + colorize(it.Event.Name.text(), RED) + " EventClassificationId: " + it.Event.EventClassificationId + ", EventStatusId: " + it.Event.EventStatusId 
			+ " [sv: " + swe + ", individual: " + individual + ", competitors: " + numberOfCompetitorsRule(it) + "]", isDebug)

		if(!individual || isMountainbikeEvent(it) || !swe) {
			debug("Filter out: " + colorize(person.name, BLUE) + " " + colorize(eventName, RED) + ", team event: " + (!individual) + ", MtbO: " + isMountainbikeEvent(it) + ", swe: " + swe, isDebug)
			return
		}

		if( isSingleDayEvent(it) ) {
			def raceTime = defaultTime(it.ClassResult.PersonResult.Result.Time.text())
			def timeDiff = defaultTime(it.ClassResult.PersonResult.Result.TimeDiff.text())
			def status = it.ClassResult.PersonResult.Result.CompetitorStatus.@value.text()
			def distance = it.Event.EventRace.@raceDistance.text()
			def noOfCompetitors = it.ClassResult.EventClass.ClassRaceInfo.@noOfStarts 
			if(noOfCompetitors == "") {
				noOfCompetitors = 0
			}
			def medal = getMedal(classShortName, distance, eventName, calculateRelativeTime( raceTime, timeDiff), status, noOfCompetitors, swe, individual) 

			person.eventResults.add(name: eventName,
				eventId: it.Event.EventId,
				classShortName: classShortName,
				classTypeId: classTypeId,
				highAge: highAge,
				lowAge: lowAge,
				position: resultPositionForSingleDayEvent(it),
				competitorStatus: status,
				noOfCompetitors: noOfCompetitors, 
				raceDistance: distance,
				raceTime: raceTime,
				timeDiff: timeDiff,
				date: it.Event.EventRace.RaceDate.Date.text(),
				swedishEvent: swe,
				individualEvent: individual,
				medal: medal)
		} else {
			// Assume "IndMultiDay"
			
			// Different race results
			// Feature: This loop will make team events drop out automatically
			it.ClassResult.PersonResult.each{ 
				def eventRaceId = it.RaceResult.EventRaceId

				// Find matching EventRace
				def eventRace = it.parent().parent().Event.EventRace.findAll{ it.EventRaceId.text() == eventRaceId.text() }
				def raceTime = defaultTime(it.RaceResult.Result.Time.text())
				def timeDiff = defaultTime(it.RaceResult.Result.TimeDiff.text())
				def status = it.RaceResult.Result.CompetitorStatus.@value.text()
				def distance = eventRace.@raceDistance.text()
				def noOfCompetitors = it.parent().EventClass.ClassRaceInfo.@noOfStarts
				def medal = getMedal(classShortName, distance, eventName, calculateRelativeTime( raceTime, timeDiff), status, noOfCompetitors, swe, individual)

				person.eventResults.add(name: eventName + " - " + eventRace.Name,
					eventId: eventRaceId,
					classShortName: classShortName,
					classTypeId: classTypeId,
					highAge: highAge,
					lowAge: lowAge,
					position: resultPositionForMultiDayEvent(it),
					competitorStatus: status,
					noOfCompetitors: noOfCompetitors, // Correct? How to calculate otherwise?
					raceDistance: distance,
					raceTime: raceTime,
					timeDiff: timeDiff,
					date: eventRace.RaceDate.Date.text(),
					swedishEvent: swe,
					individualEvent: individual,
					medal: medal)

			}


		}
	}

	// Investigate valid events
	/*personEvents.ResultList.findAll{ swedishOrganisationRule(it) && onlyIndividualRule(it) && numberOfCompetitorsRule(it) }.each{
		if( isSingleDayEvent(it) ) {

			println " - " + it.Event.Name.text() + " [status: " + it.ClassResult.PersonResult.Result.CompetitorStatus.@value.text() + "]"
		} else {
			// Assume multiday event/"IndMultiDay"
		}
	}*/

	// Store all results
	resultTable.add(person)
} 


// Display results
println "\nResults " + year
println "------------"
def shortNames = [:]
class medalTable {
	static List Master = []
	static List Elite  = []
	static List Gold  = []
	static List Silver  = []
	static List Bronze  = []
	static List Iron = []
}
addToMedalTable = { medal, name ->
	switch(medal) {
		case Medal.MASTER:
			medalTable.Master.add(name)
			break
		case Medal.ELITE:
			medalTable.Elite.add(name)
			break
		case Medal.GOLD:
			medalTable.Gold.add(name)
			break
		case Medal.SILVER:
			medalTable.Silver.add(name)
			break
		case Medal.BRONZE:
			medalTable.Bronze.add(name)
			break
		case Medal.IRON:
			medalTable.Iron.add(name)
			break
	}
}
resultTable.eachWithIndex{ it, idx ->

	/*if(idx > 10 ) {
		return
	}*/

	println "\n" + colorize(it.name, BLUE) + " (Born: " + it.born + ", Age: " + it.age + ", PersonId: " + it.personId + ")"
	if(it.eventResults.size()==0) {
		println "  Inga tävlingar registrerade"
	} else {

		if(isVerbose){
			// Full output
			it.eventResults.sort{ item -> item.medal }.eachWithIndex{ er, idx2 ->

				if(idx2 == 1) {
					//medalTable.put(it.name, er.medal)
					addToMedalTable(er.medal, it.name)
				}

				shortNames.put(er.classShortName.trim().padLeft(14," ") + " h=" + er.highAge.trim().padLeft(2," ") \
					+ " l=" + er.lowAge.trim().padLeft(2," "), er.classShortName.trim() + "|" + er.highAge.trim() \
					+ "|" + er.lowAge.trim() + "|" + er.raceDistance.trim() \
					+ "|" + er.competitorStatus.trim() + "|" + er.name.trim() + "|" + er.eventId)

				print "  " + er.date + " " + colorize(er.name, RED)  
				println " \u2192 " + colorize("[pos: " + er.position + ", status: " + er.competitorStatus \
					+ ", competitors: " + er.noOfCompetitors + ", raceDistance: " + er.raceDistance \
					+ ", swe: " + er.swedishEvent + ", individual: " + er.individualEvent + ", raceTime: " + er.raceTime + ", diff: " + er.timeDiff \
					+ ", \n             " + colorize(er.medal, GREEN) + ", " + calculateRelativeTime(er.raceTime, er.timeDiff) \
					+ " %, shortName: " + er.classShortName + ", highAge: " + er.highAge + ", typeId: " + er.classTypeId \
					+ " https://eventor.orientering.se/Events/ResultList?eventId=" + er.eventId + "&groupBy=EventClass ]", GRAY)
			}	
		} else {
			// Only essential output
			it.eventResults.sort{ item -> item.medal }.eachWithIndex{ er, idx2 ->
				// Only print to 3 medals
				if(idx2 < 3000)  {
					if(idx2 == 0) {
						//medalTable.put(it.name, er.medal)
						addToMedalTable(er.medal, it.name)
					}
					println "  " + er.medal.toString().padRight(8) + " " + er.date + " " + er.name + " [" + er.classShortName + "," \
						+ er.competitorStatus + "," + er.raceDistance + "]  \u2192 " + calculateRelativeTime(er.raceTime, er.timeDiff)  + "%" \
						+ " https://eventor.orientering.se/Events/ResultList?eventId=" + er.eventId + "&groupBy=EventClass"	
				} else if (idx2 == 4) {
					println "  ..."
				}
			}
		}
		println "Total: " + it.eventResults.size() + " events"
	}
}

// Print medal table
//medalTable.sort{ key, value -> value}.each{
println "\nMedals " + year
println "-----------"
println "\nMäster (" + medalTable.Master.size() + ")"
medalTable.Master.sort{ a, b -> a<=>b }.each{ println "  " + it }
println "\nElit (" + medalTable.Elite.size() + ")"
medalTable.Elite.sort{ a, b -> a<=>b }.each{ println "  " + it }
println "\nGuld (" + medalTable.Gold.size() + ")"
medalTable.Gold.sort{ a, b -> a<=>b }.each{ println "  " + it }
println "\nSilver (" + medalTable.Silver.size() + ")"
medalTable.Silver.sort{ a, b -> a<=>b }.each{ println "  " + it }
println "\nBrons (" + medalTable.Bronze.size() + ")"
medalTable.Bronze.sort{ a, b -> a<=>b }.each{ println "  " + it }
println "\nJärn (" + medalTable.Iron.size() + ")"
medalTable.Iron.sort{ a, b -> a<=>b }.each{ println "  " + it }


debug("\nShortnames", isVerbose)
shortNames.sort{ it.key }.each{ key, value -> debug(value, isVerbose) }

def endTime = new Date()

// Calculate duration between two dates
def calculateDuration = { d1, d2 ->
	use(groovy.time.TimeCategory) {
		return d2 - d1
	}
}
def duration = calculateDuration(startTime, endTime)
debug("\nExecution time: " + colorize(duration, GREEN), true)

debug("\nDone.")
//System.exit(0)





