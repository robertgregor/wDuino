#include <EEPROM.h>
#include <avr/wdt.h>
#include <RemoteHomeWifi.h>
#include <SoftwareSerial.h>

/* the EEPROM positions 1013 - 1024 are reserved by the library */
#define EEPROM_POSITION_PIN 0

RemoteHomeWifi remoteHome(Serial);
const char SWITCH_ON_TITLE[] PROGMEM = "Switch  ON";
const char SWITCH_ON_ACTION[] PROGMEM = "o";
const char SWITCH_OFF_TITLE[] PROGMEM = "Switch OFF";
const char SWITCH_OFF_ACTION[] PROGMEM = "p";
const char PIN_CONTROL[] PROGMEM = "Pin to control:";
const char ACTION_F[] PROGMEM = "f";
const char MAXSIZE_3[] PROGMEM = "3";
byte pinToControl = 13;

void appendConfigTable() {
  /*
  Reserved values by the library:
      const char ACTION_S[] PROGMEM = "s";
      const char ACTION_P[] PROGMEM = "p";
      const char ACTION_R[] PROGMEM = "r";
      const char ACTION_D[] PROGMEM = "d";
  */
  remoteHome.createTextBoxTableRow(PIN_CONTROL, ACTION_F, remoteHome.readByteFromEEPROM(EEPROM_POSITION_PIN), MAXSIZE_3);
}
void saveConfigValues() {
  remoteHome.saveByteToEEPROM(EEPROM_POSITION_PIN);
  pinToControl = EEPROM.read(EEPROM_POSITION_PIN);
}

void setup() {
  Serial.begin(115200);
  remoteHome.enable();
  /* Setup the ESP module and if configured, it connects to network. If not configured, the add hoc network will be started. ) */
  remoteHome.setup();
  /* Set the sketch version. */
  remoteHome.version = F("1.0.0");
  wdt_enable(WDTO_8S);
  /* Append the menu with the specific sketch functions */
  remoteHome.menuString = F("&nbsp;|&nbsp;<a href='t'>Status</a>&nbsp;|&nbsp;<a href='s'>Control</a>");
  /* Register the function to add fields to configure page. */
  remoteHome.registerAppendConfigTable(appendConfigTable);
  /* Register the function to process the added configure menu. */
  remoteHome.registerSaveConfigValues(saveConfigValues);
  pinToControl = EEPROM.read(EEPROM_POSITION_PIN);
}

void loop() {
  wdt_reset();
  /* let library to process its commands. If remoteHome.processCommonData() return true, this means that no command for the library, so it's sketch command. */ 
  if (remoteHome.processCommonData()) {
    /* This means that the HTTP GET has been fully read to the inputString so it's ready for processing. The library will skip the "GET /" string so the inputString starts with the character after the / - so in our case t */ 
    if (remoteHome.stringComplete) {
      if (remoteHome.inputString.startsWith("t")) {
        remoteHome.outputString = F("<p>D");
        remoteHome.outputString += String(pinToControl, DEC);
        remoteHome.outputString += F(" status: ");
        remoteHome.outputString += String(digitalRead(pinToControl), DEC) + F("</p>");
      } else if (remoteHome.inputString.startsWith("s")) {
        remoteHome.createTableWithForm(SWITCH_ON_TITLE, SWITCH_ON_ACTION);
        remoteHome.createSubmitButton();
        remoteHome.endTableWithForm();
        remoteHome.createTableWithForm(SWITCH_OFF_TITLE, SWITCH_OFF_ACTION);
        remoteHome.createSubmitButton();
        remoteHome.endTableWithForm();
      } else if (remoteHome.inputString.startsWith("o")) {
        remoteHome.outputString = F("<p>Switched on.</p>");
        digitalWrite(pinToControl, HIGH);
      } else if (remoteHome.inputString.startsWith("p")) {
        remoteHome.outputString = F("<p>Switched off.</p>");
        digitalWrite(pinToControl, LOW);
      } else {
        //Invalid data received, no handler to process this, send 404 NOT found
        remoteHome.sendDataNotFound();
        remoteHome.inputString = "";
        remoteHome.stringComplete = false;
        return;
      }
      //This will process the outputString - it will send the http answer with outputString.
      remoteHome.sendPageWithMenuAndHeaderResponse();
      //This will do cleanup of all variables - inputString, outputString stringComplete and serial buffer is also cleaned.
      remoteHome.cleanVariablesAfterProcessing();
    }
  }
}
void serialEvent() {
  /* OK, let the library to process the serial data. */
  remoteHome.manageSerialEvent();
}
