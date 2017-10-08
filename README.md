# Eventor-skript

Här är ett skript för att extrahera ut data från Eventor. För att göra det behöver man en API-nyckel.

## Användning

    $ groovy eventorData.groovy --keyfile <apikey.txt> --config <config.json>

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

# Dokumentation

Eftersom del dokumentation saknas gällande Eventors API, finns här en del samlade uppgifter och exempel.

Dokumentation som jag hittat:

* https://eventor.orienteering.org/api/documentation
* https://eventor.orientering.se/api/documentation

Dokumentationen skiljer sig lite på dessa två sidor - så läs båda.

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




