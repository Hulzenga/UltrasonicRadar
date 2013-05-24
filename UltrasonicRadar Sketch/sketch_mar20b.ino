#include <NewPing.h>

#define TRIGGER_PIN_R 14
#define ECHO_PIN_R 15

#define TRIGGER_PIN_L 8
#define ECHO_PIN_L 9

#define BOUNCE_BACK 40 
int maxDistance = 200;
NewPing sonar[2] = { NewPing(TRIGGER_PIN_R, ECHO_PIN_R, maxDistance), NewPing(TRIGGER_PIN_L, ECHO_PIN_L, maxDistance)};

float triggerScale = 0.886f;

String buffer = "";

char control_mode = 'c';
int direction = 0;
int angle = 0;
int step = 0;
int stepInterval = 4; 
int sampleInterval = 10;

int sweepRange = 265;

float intensity = 0.0f;
float maxIntensity = 0.0f;
float lastMaxIntensity = 0.0f;
float lastIntensity = 0.0f;
float triggerLevel = 0.0f;

boolean triggered = false;
boolean triggerLevelCrossed = false;
boolean triggerFired = false;
int triggerAngle = 0;

unsigned int measurement = 0;
String response = "";
unsigned long last;
volatile long cnt = 0;


void setup() {
  DDRH = 255;
  PORTH = 3;
  DDRA = 255;
  PORTA = 253;
  Serial3.begin(9600);    
  
  TCCR5A = 0;
  TCCR5B = 0;
  TCCR5B |= (1 << CS50);
  TCCR5B |= (1 << CS51);
  TCCR5B |= (1 << CS52);
  TIMSK5 |= (1 << TOIE5);
  
  last = millis(); 
  sendState();
}

void loop() {
  switch(control_mode) {
    case 'c':
      delay(8);
      if (step == 0)  {
        //lfreq();
      }
      break;
      
    case 'm':
      delay(8);
      angle += direction;
      if (step == 0)  {
        sample();
      }
      break;
      
    case 'w':
      delay(5);
      if (step == 0)  {
        sample();
      }      
      if (angle <= -sweepRange && direction == -1) {
        bounce();
        newSweep();        
      } else if (angle >= sweepRange && direction == 1) {
        bounce();
      }
      angle += direction;      
      break;      
  }
  
  /*
  if (direction == -1) {
    if ((step & 1) == 0) {
      sc = (sc << 4 | sc >> 1);
      PORTH = sc;
    } else {
      dc = (dc << 4 | dc >> 1);
      PORTH = dc;
    }
  } else if (direction == 1) {
    if ((step & 1) == 0) {
      sc = (sc >> 4 | sc << 1);
      PORTH = sc;
    } else {
      dc = (dc >> 4 | dc << 1);
      PORTH = dc;
    }
  }*/
  
  move();

  step++;
  step = step % stepInterval;
}

void move() {
  if (direction == -1) {
    PORTH = (PORTH << 4 | PORTH >> 1);
  } else if (direction == 1) {
    PORTH = (PORTH >> 4 | PORTH << 1);
  }  
}

//called after each full sweep, used to set up trigger variables
void newSweep() {
  lastMaxIntensity = maxIntensity;
  triggerLevel = maxIntensity * triggerScale;
  maxIntensity = 0.0f;
  
  char test[10];  
  dtostrf(triggerLevel, 1, 2, test);
  String response = "L,";
  response += test;
  Serial3.println(response);
  delay(10);
  
  triggered = true;
}

void bounce() {
  triggerFired = false;
  triggerLevelCrossed = false;
  //angle += direction*BOUNCE_BACK;
  direction = -direction;
  
  for(int i = 0; i < BOUNCE_BACK; i++) {
    delay(8);
    move();
  }
}

//checks the intensity against the trigger filter
void checkTriggerPoint() {
  if (intensity > maxIntensity) {
    maxIntensity = intensity;
  }
  
  if (triggered) {
    if (!triggerLevelCrossed && (intensity > triggerLevel)) {
      triggerLevelCrossed = true;
    }
    if (!triggerFired && triggerLevelCrossed && (intensity < lastIntensity)) {
      angle = 0 - direction * stepInterval;
      triggerFired = true;      
      Serial3.println("T");
    }    
  }
  
  lastIntensity = intensity;  
}

//sample all of the sensors
void sample() {
  
  intensity = ((cnt << 16) + TCNT5) / ((float) millis() - last + 1);
  checkTriggerPoint();
  
  delay(sampleInterval);
  measurement = sonar[0].ping_cm();
  delay(sampleInterval);
  response = "M,";
  response += String(angle);
  response += ",";
  response += String(measurement);  
  measurement = sonar[1].ping_cm();
  response += ",";
  response += String(measurement);
  response += ",";
  
  char test[10];  
  dtostrf(intensity, 1, 2, test);
  response += test;
  Serial3.println(response);
  TCNT5 = 0;
  cnt = 0;
  last = millis();  
}

void setMaxDistance(int distance) {
  maxDistance = distance;
  sonar[0] = NewPing(TRIGGER_PIN_R, ECHO_PIN_R, maxDistance);
  sonar[1] = NewPing(TRIGGER_PIN_L, ECHO_PIN_L, maxDistance);
}

ISR(TIMER5_OVF_vect) {
  cnt++;
}

void serialEvent3() {
  //PORTA = (PORTA << 7 | PORTA >> 1);
  while (Serial3.available()) {
    char inChar = (char)Serial3.read();    
    
    if (inChar == '\n') {
      handleCommand(buffer);
      PORTA = (PORTA << 7 | PORTA >> 1);
      buffer = "";
      break;
    } else {
      buffer += inChar;
    }

  }
}

int str2int(String str) {
  char buffer[4];
  str.toCharArray(buffer, 4);
  return atoi(buffer);
}

void sendState() {
  Serial3.println("C");
  
  Serial3.println(control_mode);
  
  String response = "W,";
  response += String(sweepRange);
  Serial3.println(response);
  
  char test[10];  
  dtostrf(triggerLevel, 1, 2, test);
  response = "L,";
  response += test;
  Serial3.println(response);

  response = "d,";
  response += String(maxDistance);
  Serial3.println(response);
  
  response = "p,";
  response += String(stepInterval);
  Serial3.println(response);

  response = "a,";
  response += String(sampleInterval);  
  Serial3.println(response);
}

void handleCommand(String cmd) {
  switch(cmd.charAt(0)){
      case 'l':
        direction = -1;
        break;
      case 'r':
        direction = 1;
        break;
      case 's':
        direction = 0;
        break;
      case 'c':
        direction = 0;
        angle = 0;
        triggered = false;
        control_mode = 'c';
        break;
      case 'm':
        direction = 0;
        control_mode = 'm';
        break;
      case 'w':
        direction = 1;
        control_mode = 'w';
        break;
      case 't':   
        sendState();
        break;
      case 'S':
        sweepRange = str2int(cmd.substring(2));
        break;
      case 'd':
        setMaxDistance(str2int(cmd.substring(2)));
        break;
      case 'p':
        stepInterval = str2int(cmd.substring(2));
        triggerScale = 0.95f - stepInterval*0.016f;
        break;
      case 'a':
        sampleInterval = str2int(cmd.substring(2));
        break;
    }
}

