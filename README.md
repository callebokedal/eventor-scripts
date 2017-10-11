# Eventor-skript

Här är ett skript för att extrahera ut data från Eventor. För att göra det behöver man en API-nyckel.

## Användning

    $ groovy eventData.groovy --keyfile <apikey.txt> --config <config.json>

    $ groovy eventData.groovy --keyfile apikey.txt --config config/events/allt.json

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


