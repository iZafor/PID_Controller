# PID Controller App

It is an android app which lets you control **PID** constants through
**Bluetooth** modules like HC-05. [_Download APK_](https://tinyurl.com/PID-Controller)

## Available Commands

| **PARAMETERS** | **INCREMENT** | **DECREMENT** |
|:--------------:|:-------------:|:-------------:|
|     **Kp**     |       P       |       Q       |
|     **Kd**     |       D       |       E       |
|     **Ki**     |       I       |       J       |
|   **Factor**   |       U       |       V       |

**Movement Commands**

- **Start** - S
- **Stop** - T

## App UI

<img alt="App UI" src="https://i.postimg.cc/QCcsR8rd/APP-UI.png" width="300" height="600"/>

## Sample Arduino Code

```c++
#define BAUD 9600

#define KP_INCREMENT 'P'
#define KP_DECREMENT 'Q'
#define KD_INCREMENT 'D'
#define KD_DECREMENT 'E'
#define KI_INCREMENT 'I'
#define KI_DECREMENT 'J'
#define FACTOR_UPSCALE 'U'
#define FACTOR_DOWNSCALE 'V'
#define MOVEMENT_START 'S'
#define MOVEMENT_STOP 'T'

float kp = 0.0, kd = 0.0, ki = 0.0;
float increment_factor = 0.1;
bool move = true;

void handle_pid_command(char c);

void setup() {
  // put your setup code here, to run once:
  Serial.begin(BAUD);
}

void loop() {
  // put your main code here, to run repeatedly:
  if (Serial.available()) {
    char c = Serial.read();
    handle_pid_command(c);
  }

  if (!move) {
    // handle movement
  } else {
  }
}

void handle_pid_command(char c) {
  switch (c) {
    case KP_INCREMENT:
      kp += increment_factor;
      break;
    case KP_DECREMENT:
      kp -= increment_factor;
      break;
    case KD_INCREMENT:
      kd += increment_factor;
      break;
    case KD_DECREMENT:
      kd -= increment_factor;
      break;
    case KI_INCREMENT:
      ki += increment_factor;
      break;
    case KI_DECREMENT:
      ki -= increment_factor;
      break;
    case FACTOR_UPSCALE:
      increment_factor *= 10.0;
      break;
    case FACTOR_DOWNSCALE:
      increment_factor /= 10.0;
      break;
    case MOVEMENT_START:
      move = true;
      break;
    case MOVEMENT_STOP:
      move = false;
      break;
  }

  char buf[100];
  sprintf(buf, "kp = %.6f, kd = %.6f, ki = %.6f, factor = %.6f, movement = %d", kp, kd, ki, increment_factor, move);
  Serial.println(buf);
}
```
