# spring-xmpp-websocket-reactjs

![IM System Diagram](im-system-diagram.jpg)

## Tech Stack

- **Spring Boot**
- [Smack](https://www.igniterealtime.org/projects/smack/)
- **Websocket**
- **[MySQL](https://sergiomartinrubio.com/articles/mysql-guide/)**
- **[Liquibase](https://sergiomartinrubio.com/articles/getting-started-with-liquibase-and-spring-boot/)**
- **[BCrypt](https://sergiomartinrubio.com/articles/storing-passwords-securely-with-bcrypt-and-java/)**

## Installation

0. Install jdk.16 and maven
   ```shell
   sudo apt update
   sudo apt search openjdk
   sudo apt install openjdk-16-jdk
   sudo apt install maven
   ```
1. Build project
   ```shell
   cd spring-xmpp-websocket-server
   mvn clean install -DskipTests
   docker build . -t smartinrub/spring-xmpp-websocket-server:latest
   ```

2. Run backend services:
    ```shell
    docker-compose up
    ```
3. Go to `http://localhost:9090` and setup openfire XMPP server:
   - Server settings:
      - Set "XMPP Domain Name" to `localhost`
      - Set "Server Host Name (FQDN)" to `localhost`
      - Leave the rest as it is.
   - Database Settings:
      - Select "Standard Database Connection"
      - Select "MySQL"
      - Replace on the "Database URL" `HOSTNAME` with `mysql` and `DATABASENAME` with `openfire`, then fill in the username( openfireuser ) and password (openfirepasswd).
        Connection URL: `jdbc:mysql://mysql:3306/openfire?rewriteBatchedStatements=true&characterEncoding=UTF-8&characterSetResults=UTF-8&serverTimezone=UTC`
   - Continue and ignore the rest of the steps.
4. Now you can use a websocket client to try out the backend application.
   - Endpoint: ws://localhost:8080/chat/sergio/pass
   - Connect will return `{"messageType":"JOIN_SUCCESS"}`
   - Send new message with body:
       ```
       {
           "from": "sergio",
           "to": "jose",+
           "content": "hello world",
           "messageType": "NEW_MESSAGE"
       }
       ```
     will return `{"from":"sergio","to":"jose","content":"hello world","messageType":"NEW_MESSAGE"}`

5. Run ReactJS App

```shell
npm install
npm start
```

## Running Tests

To run tests, run the following command

```bash
  mvn clean install
```


## Setup local system:

### Ubuntu
#### MYSQL
```shell
sudo apt-get install mysql-server
sudo /usr/sbin/mysqld stop
sudo /usr/sbin/mysqld start
sudo mysql -u root
```

__Create new user for mysql__
```sql
CREATE USER ''@'localhost' IDENTIFIED BY 'openfirepasswd';
GRANT ALL PRIVILEGES ON *.* TO 'openfireuser'@'localhost';
```
__Connect to mysql__
```shell
mysql -u openfireuser -popenfirepasswd
```


#### postgres:
```shell
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql.service
sudo -i -u postgres
psql
```

__create new user and database__
```sql 
CREATE USER xmpp WITH PASSWORD 'password';
create database chat;
```

__create new user called xmpp to login to postgres
```shell
sudo adduser xmpp
sudo su xmpp

```