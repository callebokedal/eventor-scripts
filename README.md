# Eventor-skript

Här är skript för att extrahera ut data från Eventor. För att göra det behöver man en API-nyckel. 

## Användning
Man kan köra skript direkt med hjälp av Groovy eller med hjälp av Docker (då t.ex. Java-version blir korrekt automatiskt).
Börja med att hämta hem detta repo genom (kräver Git installerat: https://git-scm.com/downloads):

Första gången:

    git clone https://github.com/callebokedal/eventor-scripts.git

För att senare hämta eventuella uppateringar i koden

    git pull

Sedan väljer du om du vill köra via groovy direkt eller indirekt med hjälp av Docker.

### Via Groovy (direkt)

#### Visa översikt för utmärkelser enligt SOFT och Sjövallas riktlinjer
    $ groovy scripts/softMedals.groovy -a secret/apikey.txt

#### Hämta data för ett event
    $ groovy scripts/eventData.groovy --keyfile <apikey.txt> --config <config.json>
    $ groovy scripts/eventData.groovy --keyfile secret/apikey.txt --config config/events/allt.json

### Via Docker
(Se installation nedan först).

    $ docker run -t --rm -v $(pwd -P):/home/groovy eventor-app groovy scripts/softMedals.groovy -a secret/apikey.txt
    $ docker run -t --rm -v $(pwd -P):/home/groovy eventor-app groovy scripts/eventData.groovy --keyfile secret/apikey.txt --config config/events/allt.json

## API-fil

En obligatorisk textfil som innehåller aktuell API-nyckel.

**Tips:** Skydda filen genom `chmod 600 apikey.txt`.

## Konfigurationsfil

En obligatorisk textfil i json-format som innehåller den konfiguration du vill exekvera.

Exempel - [config/events/allt.json](config/events/allt.json):

    {
        "fromDate": "2017-10-10",  
        "duration": 10,
        "classificationIds": [1,2,3,4],
        "organisationIds": [321,3,6,12,13]
    }

Genom att ange konfigurationsfil som parameter till skriptet, kan man enkelt skapa olika konfigurationsfiler för olika önskemål.

### Parametrar

| Parameter | Example | Type | Description | 
|---|---|---|---|
| clubId | 321 | int | Id for organisation. 321 = Sjövalla FK |
| fromDate | "2017-10-10" | String | Start date. Format: "yyyy-mm-dd". Default: <current date>. |
| duration | 5 | int | Number of days after 'fromDate'. Default: 14. |
| classificationIds | [1,2,3,4] | [int] | Type of events to include. Format: List of integers. |
| organisationIds | [6,13,12,3] | [int] | List of organisation id's to include. Format: List of integers. Närliggande enligt test  |
| maxDistance | 100 | int | Max distance (in km) between TC and Finnsjöråden |
| output | "html" | String | Output: "text"|"html". Default: "text" |
| googleMapLink | true | boolean | Link to Google maps or not. Default: false |
| eventorMessage | false | boolean | Include Eventor Message - if exists. Default: false |
| verbose | false | boolean | Verbose or not. Default: false. |

# Förutsättning Groovy

Skripten ska fungera för följande uppsättning av groovy:

    groovy -v
    Groovy Version: 2.4.11 JVM: 1.7.0_17 Vendor: Oracle Corporation OS: Mac OS X

# Installation Docker
Installera Docker enligt till exempel:

  * https://store.docker.com/editions/community/docker-ce-server-ubuntu
  * https://www.docker.com/docker-mac
  * https://www.docker.com/docker-windows

Via https://www.docker.com finns det fler alternativ.

### Skapa image och volym för data
När Docker är installerat på din dator fortsätter du med följande (en gång):

    docker build -t eventor-app .
    docker volume create eventor-data

Klart! Nu kan du följa instruktionerna enligt "Via Docker" ovan.

# Dokumentation

Eftersom del dokumentation saknas gällande Eventors API, finns här en del samlade uppgifter och exempel.

Dokumentation som jag hittat:

* https://eventor.orienteering.org/api/documentation
* https://eventor.orientering.se/api/documentation

Dokumentationen skiljer sig lite på dessa två sidor - så läs båda.

## ClassificationIds

URL i webbläsaren stämmer inte överens med benämningar i API-dokumentationen

**Exempel:**

Oinloggad + Göteborg:

    https://eventor.orientering.se/Events?organisations=13&classifications=International,Championship,National,Regional,Local,Club&mode=List&startDate=2017-10-01&endDate=2017-10-31

Inloggad + mitt och angränsande distrikt + inkludera närtävlingar + inkludera klubbtävlingar

    https://eventor.orientering.se/Events?organisations=6,13,12,3&classifications=International,Championship,National,Regional,Local,Club&mode=List&startDate=2017-10-01&endDate=2017-10-31

Men API-docs:
    
    classificationIds           
        Comma-separated list of event classification IDs, where 
        1=championship event, 
        2=national event, 
        3=state event, 
        4=local event, 
        5=club event. 
        Omit to include all events.

## EventStatusId-värden

    1 Applied
    2 ApprovedByRegion
    3 Approved
    4 Created
    5 EntryOpened
    6 EntryPaused
    7 EntryClosed
    8 Live
    9 Completed
    10 Canceled
    11 Reported

## Exempel: Tävlingar

Oinloggad + Göteborg:

    https://eventor.orientering.se/Events?organisations=13&classifications=International,Championship,National,Regional,Local,Club&mode=List&startDate=2017-10-01&endDate=2017-10-31

Inloggad + mitt och angränsande distrikt + inkludera närtävlingar + inkludera klubbtävlingar

    https://eventor.orientering.se/Events?organisations=6,13,12,3&classifications=International,Championship,National,Regional,Local,Club&mode=List&startDate=2017-10-01&endDate=2017-10-31

## Anmälningsdatum

Verkar som att första EntryBreak är "Ordinarie anmälningsdatum", och andra EntryBreak är "Efteranmälningsdata". Exempel:

      <EntryBreak>
         <ValidToDate>
            <Date>2017-11-07</Date>
            <Clock>23:59:59</Clock>
         </ValidToDate>
      </EntryBreak>
      <EntryBreak>
         <ValidFromDate>
            <Date>2017-11-08</Date>
            <Clock>00:00:00</Clock>
         </ValidFromDate>
         <ValidToDate>
            <Date>2017-11-08</Date>
            <Clock>23:59:59</Clock>
         </ValidToDate>
      </EntryBreak>

## Sjövallaspecifik dokumentation

Koordinater för Finnsjögården som används för att beräkna avstånd till olika tävlingar:
Lat:    57.640542
Long:   12.135172

### Sjövalla FK

- Sjövalla FK (id=321) har Göteborg OF (id=13) som ParentOrganisation - se OrganisationList / https://eventor.orienteering.org/api/organisations/iofxml

### Närliggande organisationer

Id-mappning närliggande organisationer

- 3  = Västergötlands OF
- 6  = Bohuslän-Dals OF
- 12 = Hallands OF
- 13 = Göteborg GOF


