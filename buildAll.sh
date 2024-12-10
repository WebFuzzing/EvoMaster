#!/bin/bash


# Build Core and JVM Driver
mvn clean install -DskipTests

# Build .Net Driver
#dotnet build

# Build JavaScript Driver
cd client-js/evomaster-client-js
npm install
npm run build


