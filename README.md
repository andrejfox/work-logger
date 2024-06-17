# Work Logger

A simple system for tracking work hours and managing financial transactions efficiently.

## Description

This project aims to create a robust system for managing work hours and payments.
It allows users to track their work and finances effectively. This system is designed
to meet the needs of people who work on a per hourly baisis.

## Usage

### Requirements

* Server / VPS
* Java 21 or higher

### Installing

* Check that you have java 21 or higher `java -version`
* Download the latest release from the [release page](https://github.com/andrejfox/work-logger/releases)

### Executing program

* run:
  ```
  java -jar work-logger.jar
  ```
  Tihs shuld crash and create a config.toml and a mail.txt file.
  ```sh
  ❯ tree
  .
  ├── config.toml
  ├── mail.txt
  └── work-logger.jar
  ```

* edit the contence of those two files to look something like this:<br>

  config.toml
  ```toml
  botToken = "MTA2ODkzNjQ2SDLKFJFQyNTA1NA.GiOhMa.EWRW.dJIAaA3RBJpvXwLqxSSE4AFDFSD23423UqJ4hR2uQ"
  channelID = 1068986585116292096
  messageID = 1249163115900523457
  languageTag = "en"
  currency = "€"
  paymentTypes = [
    { tag = "Teaching", type = 15 },
    { tag = "Prep/Cleanup", type = 7 },
  ]
  ```
  mail.txt
  ```
  Dear Mr. Employer,
  Sending hours for {MONTH}.

  {WORK_DATA}

  In total:
  {DATA_SUM}

  Regards,
  Andrej Vencelj
  ```
* Than just run the start command again and you are done.
  ```
  java -jar work-logger.jar
  ```
