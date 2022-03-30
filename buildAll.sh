#!/bin/bash


# Build Core and JVM Driver
mvn clean package -DskipTests

# Build .Net Driver
dotnet build

# Build JavaScript Driver
cd client-js/evomaster-client-js
npm install
npm run build

# Build Python Driver
cd client-python
pip install -r requirements-dev.txt
