# Eventor-skript

Här är ett skript för att extrahera ut data från Eventor. För att göra det behöver man en API-nyckel. 

## Användning
Man kan köra skript direkt med hjälp av Groovy eller med hjälp av Docker (då t.ex. Java-version blir korrekt automatiskt).
Börja med att hämta hem detta repo genom (kräver Git installerat: https://git-scm.com/downloads):

    git clone https://github.com/callebokedal/eventor-scripts.git

Sedan väljer du om du vill köra via groovy direkt eller indirekt med hjälp av Docker.

### Via Groovy (direkt)

    $ groovy eventData.groovy --keyfile <apikey.txt> --config <config.json>

    $ groovy eventData.groovy --keyfile apikey.txt --config config/events/allt.json

### Via Docker
(Se installation nedan först).

    docker run -t --rm -v $(pwd -P):/home/groovy eventor-app groovy eventData.groovy --keyfile apikey.txt --config config/events/allt.json

## API-fil

En obligatorisk textfil som innehåller aktuell API-nyckel.

**Tips:** Skydda filen genom `chmod 600 apikey.txt`.

## Konfigurationsfil

En obligatorisk textfil i json-format som innehåller den konfiguration du vill exekvera.

Exempel:

    {
        "fromDate": "2017-10-10",  
        "duration": 10,
        "classificationIds": [1,2,3,4],
        "organisationIds": [321,3,6,12,13]
    }

Genom att ange konfigurationsfil som parameter till skriptet, kan man enkelt skapa olika konfigurationsfiler för olika önskemål.

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

## Sjövallaspecifik dokumentation


### Sjövalla FK

- Sjövalla FK (id=321) har Göteborg OF (id=13) som ParentOrganisation - se OrganisationList / https://eventor.orienteering.org/api/organisations/iofxml

### Närliggande organisationer

Id-mappning närliggande organisationer

- 3  = Västergötlands OF
- 6  = Bohuslän-Dals OF
- 12 = Hallands OF
- 13 = Göteborg GOF


