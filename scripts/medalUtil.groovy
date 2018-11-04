
/*

Obsolete utility script. Keept for reference. See 'softMedals.groovy' instead.

Soft rules 2018
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

public enum Medal {
    MASTER(6,'Mäster'),
    ELITE(5,'Elit'),
    GOLD(4,'Guld'),
    SILVER(3,'Silver'),
    BRONZE(2,'Brons'),
    IRON(1,'Järn'),
    NONE(0,''),
    UNKNOWN(-1,'')
 
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

// Best guess of medal according to SOFT rules and poor object model
// Assume valid race/competitorStatus
def medalByClassification = { classification, percentage ->
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
        case ~/^U_I_OPEN_.*/:
            return Medal.IRON
            break
        case ~/^SPECIAL_.*/:
            switch(percentage) { 
                case { it < 50 }: return Medal.BRONZE; break;
                default: return Medal.IRON; break;
            }
            break
        default:
            println "Unknown case for USM: " + classification
            return Medal.UNKNOWN
            break
    }
}
assert Medal.MASTER == medalByClassification("USM_16", 23)

// Method to guess classification
// Can't be 100% sure about correct classificiation, since the is not typed
def guessClassification = {shortName, distance, eventName ->
    def lowerCaseEventName = eventName.toLowerCase()
    if(lowerCaseEventName.contains("ungdoms-sm")) {
        switch(shortName) {
            case ~/.*16$/: return "USM_16"; break;  // DH16
            case ~/.*15$/: return "USM_15"; break;  // DH15
            default: return "USM_?"; break;         // Unknown
        }
    } else if(distance.toLowerCase() == "sprint" || lowerCaseEventName.contains("-km")) {
        // Special case:
        // - Sprint or "klubbmästerskap"
        switch(shortName) {
            case ~/.*16\w{0,1}|.*21\w{0,1}|.*15-49\w{0,1}/: return "SPECIAL_16"; break;
            case ~/.*14\w{0,1}/: return "SPECIAL_14"; break;
            case ~/.*12\w{0,1}/: return "SPECIAL_12"; break;
            case ~/.*10\w{0,1}/: return "SPECIAL_10"; break;
            default: return "SPECIAL_?"; break;
        }
    } else {
        switch(shortName) {
            case ~/.*16$/: return "DH16"; break;             // D16, H16
            case ~/.*14$|.*13$|.*16K/: return "DH14_DK16K"; break; // D14, H14, D16K, H16K, DH13
            case ~/.*12$|.*11$|.*14K/: return "DH12_DK14K"; break; // D12, H12, D14K, H14K, DH11
            case ~/.*10$|.*12K/: return "DH10_DK12K"; break; // D10, H10, D12K, H12K
            case ~/^ÖM\d{1}$|^DU\d{1}$|^HU\d{1}$|[Ö|U]\d{1}$|^Insk.*/: 
                return "U_I_OPEN"; 
                break;
            case ~/O|V|Röd|Mellan|Korta|Svart.*|ÖM\w{0,}|ÖM .*$|Gul\/Orange/: 
                // Hardcoded cases due to poor data
                return "U_I_OPEN_S"; // U_I_OPEN_S(pecial) 
                break;
            default: return "UNKNOWN"; break;
        }
   }
}
assert "U_I_OPEN_S ".trim() == guessClassification("O          ".trim(),"long", "SM sport vintercup #3")
assert "U_I_OPEN_S ".trim() == guessClassification("V          ".trim(),"long", "SM sport vintercup #3")
assert "U_I_OPEN   ".trim() == guessClassification("U1         ".trim(),"long", "Folskubbet")
assert "U_I_OPEN   ".trim() == guessClassification("U2         ".trim(),"long", "O-Ringen Höga Kusten - Etapp 1, lång")
assert "U_I_OPEN   ".trim() == guessClassification("U2         ".trim(),"middle", "Pepparkaksluffen")
assert "U_I_OPEN   ".trim() == guessClassification("U3         ".trim(),"middle", "Närnatt Cup, etapp 1")
assert "U_I_OPEN   ".trim() == guessClassification("Ö5         ".trim(),"long", "Vintercupen etapp 1")
assert "U_I_OPEN   ".trim() == guessClassification("Ö8         ".trim(),"long", "Vintercupen etapp 1")
assert "DH10_DK12K ".trim() == guessClassification("D10        ".trim(),"long", "Folskubbet")
assert "DH12_DK14K ".trim() == guessClassification("D12        ".trim(),"middle", "GMOK:s Höstmedel")
assert "DH14_DK16K ".trim() == guessClassification("D13        ".trim(),"long", "GM, lång")
assert "DH14_DK16K ".trim() == guessClassification("D14        ".trim(),"middle", "25mannamedeln")
assert "USM_15     ".trim() == guessClassification("D15        ".trim(),"long", "Ungdoms-SM, lång")
assert "DH16       ".trim() == guessClassification("D16        ".trim(),"middle", "Fräknefejden")
assert "U_I_OPEN   ".trim() == guessClassification("DU1        ".trim(),"long", "Vårserien Etapp 1")
assert "U_I_OPEN   ".trim() == guessClassification("DU2        ".trim(),"long", "Vårserien Etapp 5")
assert "DH10_DK12K ".trim() == guessClassification("H10        ".trim(),"long", "DM, lång, Göteborg")
assert "DH10_DK12K ".trim() == guessClassification("H10        ".trim(),"long", "O-Ringen Höga Kusten - Etapp 2, lång")
assert "DH12_DK14K ".trim() == guessClassification("H11        ".trim(),"middle", "O-Ringen Höga Kusten - Etapp 3, medel")
assert "DH12_DK14K ".trim() == guessClassification("H12        ".trim(),"middle", "GMOK:s Höstmedel")
assert "DH14_DK16K ".trim() == guessClassification("H13        ".trim(),"long", "GM, lång")
assert "DH14_DK16K ".trim() == guessClassification("H13        ".trim(),"long", "O-Ringen Höga Kusten - Etapp 5, lång, jaktstart")
assert "DH14_DK16K ".trim() == guessClassification("H14        ".trim(),"middle", "GMOK:s Höstmedel")
assert "DH14_DK16K ".trim() == guessClassification("H14        ".trim(),"long", "O-Ringen Höga Kusten - Etapp 4, lång")
assert "SPECIAL_16 ".trim() == guessClassification("H16        ".trim(),"sprint", "GM, sprint")
assert "U_I_OPEN   ".trim() == guessClassification("HU1        ".trim(),"long", "Vårserien Etapp 5")
assert "U_I_OPEN   ".trim() == guessClassification("HU2        ".trim(),"long", "Vårserien Etapp 5")
assert "U_I_OPEN_S ".trim() == guessClassification("Röd        ".trim(),"long", "GOFs Sommarserie Etapp 3")
assert "U_I_OPEN   ".trim() == guessClassification("ÖM1        ".trim(),"long", "DM, stafett, Göteborg, individuella öppna klasser")
assert "U_I_OPEN   ".trim() == guessClassification("ÖM2        ".trim(),"long", "Höststafetten, individuella öppna klasser")
assert "U_I_OPEN   ".trim() == guessClassification("ÖM3        ".trim(),"long", "Folskubbet")
assert "U_I_OPEN   ".trim() == guessClassification("ÖM5        ".trim(),"middle", "Partilletrippeln, medel")
assert "U_I_OPEN   ".trim() == guessClassification("ÖM8        ".trim(),"long", "5-kvällars OK Klyftamo etapp 4")
assert "SPECIAL_10 ".trim() == guessClassification("D10S       ".trim(),"long", "Lång-KM för OK Landehof och Sjövalla FK")
assert "SPECIAL_12 ".trim() == guessClassification("D12S       ".trim(),"long", "Lång-KM för OK Landehof och Sjövalla FK")
assert "DH12_DK14K ".trim() == guessClassification("D14K       ".trim(),"long", "DM, lång, Göteborg")
assert "SPECIAL_14 ".trim() == guessClassification("D14S       ".trim(),"long", "Lång-KM för OK Landehof och Sjövalla FK")
assert "DH14_DK16K ".trim() == guessClassification("D16K       ".trim(),"long", "Vårserien Etapp 2")
assert "SPECIAL_16 ".trim() == guessClassification("D21C       ".trim(),"sprint", "SM Sport Vintercup #4 + Landslagsträning")
assert "SPECIAL_16 ".trim() == guessClassification("DX16       ".trim(),"sprint", "Sommarlandssprinten")
assert "SPECIAL_10 ".trim() == guessClassification("H10S       ".trim(),"long", "Lång-KM för OK Landehof och Sjövalla FK")
assert "DH10_DK12K ".trim() == guessClassification("H12K       ".trim(),"middle", "GMOK:s Höstmedel")
assert "SPECIAL_12 ".trim() == guessClassification("H12S       ".trim(),"long", "Lång-KM för OK Landehof och Sjövalla FK")
assert "DH12_DK14K ".trim() == guessClassification("H14K       ".trim(),"long", "DM, lång, Göteborg")
assert "SPECIAL_14 ".trim() == guessClassification("H14S       ".trim(),"long", "Lång-KM för OK Landehof och Sjövalla FK")
assert "SPECIAL_16 ".trim() == guessClassification("H21C       ".trim(),"sprint", "SM Sport Vintercup #4 + Landslagsträning")
assert "SPECIAL_14 ".trim() == guessClassification("HX14       ".trim(),"sprint", "Sommarlandssprinten")
assert "U_I_OPEN   ".trim() == guessClassification("Insk       ".trim(),"long", "DM, lång, Göteborg")
assert "U_I_OPEN_S ".trim() == guessClassification("Korta      ".trim(),"long", "GOF NattCup Etapp 6")
assert "U_I_OPEN_S ".trim() == guessClassification("Svart      ".trim(),"long", "GOFs Sommarserie Etapp 4")
assert "SPECIAL_16 ".trim() == guessClassification("D15-49     ".trim(),"sprint", "Sprint-KM för Landehof och Sjövalla")
assert "SPECIAL_16 ".trim() == guessClassification("H15-49     ".trim(),"sprint", "Sprint-KM för Landehof och Sjövalla")
assert "U_I_OPEN_S ".trim() == guessClassification("Mellan     ".trim(),"long", "GOF NattCup Etapp 6")
assert "SPECIAL_16 ".trim() == guessClassification("D15-49S    ".trim(),"long", "Lång-KM för OK Landehof och Sjövalla FK")
assert "U_I_OPEN_S ".trim() == guessClassification("ÖMTrail    ".trim(),"long", "DM, lång, Göteborg")
assert "U_I_OPEN_S ".trim() == guessClassification("Gul/Orange ".trim(),"middle", "GOFs Sommarserie Etapp 1")
assert "U_I_OPEN   ".trim() == guessClassification("Inskolning ".trim(),"long", "O-Ringen Höga Kusten - Etapp 4, lång")
assert "U_I_OPEN_S ".trim() == guessClassification("Svart kort ".trim(),"middle", "GOFs Sommarserie Etapp 1")
assert "U_I_OPEN_S ".trim() == guessClassification("Svart lång ".trim(),"middle", "GOFs Sommarserie Etapp 1")
assert "U_I_OPEN_S ".trim() == guessClassification("ÖM Häxtrail".trim(),"long", "Häxjakten, Five-O, #1")


/*
Sprint samma regler, alltså max bronsmärke. Det innebär exv Borås O-event maximalt kan ge brons.
Men... vid klubbmästerskap har vi sagt max bronsmärke.
D16 - Vad gäller för t.ex. D15-49?
D16 - En tävling med D21C. Ska den tolkas som Övriga enligt SOFTs lista ovan?
D16 - DX16? 
D16 - D15 -> ska man tolka det som D16, D14 eller en övrigt tävling enligt SOFTs lista ovan?
*/

println "Done"

// Data from events 2018 - used as base for guessing SOFT classifications
def eventData2018 = '''\
O|||Long|OK|SM sport vintercup #3|20991
V|||Long|OK|SM sport vintercup #3|20991
U1|16||Long|OK|Folskubbet|18405
U2|||Long|OK|O-Ringen Höga Kusten - Etapp 1, lång|21098
U2|16||Middle|OK|Pepparkaksluffen|18608
U3|16||Middle|OK|Närnatt Cup, etapp 1|23714
Ö5|||Long|MisPunch|Vintercupen etapp 1|20830
Ö8|||Long|OK|Vintercupen etapp 1|20830
D10|10||Long|MisPunch|Folskubbet|18405
D12|12||Middle|DidNotStart|GMOK:s Höstmedel|18240
D13|13||Long|OK|GM, lång|17754
D14|14||Middle|OK|25mannamedeln|19882
D15|15||Long|OK|Ungdoms-SM, lång|16820
D16|16||Middle|OK|Fräknefejden|18139
DU1|16||Long|MisPunch|Vårserien Etapp 1|21298
DU2|16||Long|OK|Vårserien Etapp 5|21302
H10|10||Long|OK|DM, lång, Göteborg|18241
H10|10|10|Long|OK|O-Ringen Höga Kusten - Etapp 2, lång|21099
H11|11|11|Middle|OK|O-Ringen Höga Kusten - Etapp 3, medel|21100
H12|12||Middle|OK|GMOK:s Höstmedel|18240
H13|13||Long|OK|GM, lång|17754
H13|13|13|Long|OK|O-Ringen Höga Kusten - Etapp 5, lång, jaktstart|21102
H14|14||Middle|DidNotStart|GMOK:s Höstmedel|18240
H14|14|14|Long|OK|O-Ringen Höga Kusten - Etapp 4, lång|21101
H16|16||Sprint|OK|GM, sprint|17753
HU1|16||Long|OK|Vårserien Etapp 5|21302
HU2|16||Long|OK|Vårserien Etapp 5|21302
Röd|||Long|OK|GOFs Sommarserie Etapp 3|22822
ÖM1|||Long|MisPunch|DM, stafett, Göteborg, individuella öppna klasser|23149
ÖM2|||Long|OK|Höststafetten, individuella öppna klasser|23721
ÖM3|||Long|OK|Folskubbet|18405
ÖM5|||Middle|OK|Partilletrippeln, medel|21808
ÖM8|||Long|OK|5-kvällars OK Klyftamo etapp 4|23023
D10S|10||Long|OK|Lång-KM för OK Landehof och Sjövalla FK|23202
D12S|12||Long|OK|Lång-KM för OK Landehof och Sjövalla FK|23202
D14K|14||Long|DidNotStart|DM, lång, Göteborg|18241
D14S|14||Long|OK|Lång-KM för OK Landehof och Sjövalla FK|23202
D16K|16||Long|OK|Vårserien Etapp 2|21299
D21C|||Sprint|OK|SM Sport Vintercup #4 + Landslagsträning|20924
DX16|16||Sprint|OK|Sommarlandssprinten|18818
H10S|10||Long|OK|Lång-KM för OK Landehof och Sjövalla FK|23202
H12K|12||Middle|MisPunch|GMOK:s Höstmedel|18240
H12S|12||Long|OK|Lång-KM för OK Landehof och Sjövalla FK|23202
H14K|14||Long|OK|DM, lång, Göteborg|18241
H14S|14||Long|OK|Lång-KM för OK Landehof och Sjövalla FK|23202
H21C|||Sprint|OK|SM Sport Vintercup #4 + Landslagsträning|20924
HX14|14||Sprint|OK|Sommarlandssprinten|18818
Insk|16||Long|MisPunch|DM, lång, Göteborg|18241
Korta|||Long|OK|GOF NattCup Etapp 6|20251
Svart|||Long|OK|GOFs Sommarserie Etapp 4|22821
D15-49|||Sprint|OK|Sprint-KM för Landehof och Sjövalla|22536
H15-49|||Sprint|OK|Sprint-KM för Landehof och Sjövalla|22536
Mellan|||Long|OK|GOF NattCup Etapp 6|20251
D15-49S||15|Long|DidNotFinish|Lång-KM för OK Landehof och Sjövalla FK|23202
ÖMTrail|||Long|DidNotStart|DM, lång, Göteborg|18241
Gul/Orange|||Middle|OK|GOFs Sommarserie Etapp 1|22834
Inskolning|||Long|OK|O-Ringen Höga Kusten - Etapp 4, lång|21101
Svart kort|||Middle|OK|GOFs Sommarserie Etapp 1|22834
Svart lång|||Middle|OK|GOFs Sommarserie Etapp 1|22834
ÖM Häxtrail|||Long|DidNotStart|Häxjakten, Five-O, #1|18542'''
eventData2018.eachLine{ 
    def values = it.split("\\|")
    def shortName = values[0]
    def high = values[1]
    def low = values[2]
    def distance = values[3]
    def status = values[4]
    def eventName = values[5]
    def id = values[6]

    //println guessSOFTClassification(shortName, high, low, distance, eventName).padRight(10) + " <-> " + shortName + ", " + high +", " + low + ", " + eventName + ", " + id
    println guessClassification(shortName, distance, eventName).padRight(10) + " <-> " \
    + shortName.padRight(12) + eventName + " [" + distance.toLowerCase() + "]"
}