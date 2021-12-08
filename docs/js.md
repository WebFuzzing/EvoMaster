# Dealing With JavaScript

## Setup
### build from source code
**Build JavaScript Instrumentation**

Go to `Evomaster\client-js\evomaster-client-js`, run
- step 1 `npm install`
- step 2 `npm run build`

**Build EvoMaster**

Go to `Evomaster`, run
- step 3 `mvn clean install -DskipTests`

## Run
**Instrument SUT**

Go to a sut, e.g., `EMB-js\rest\ncs-js`, run
- step 4 `npm install`
- step 5 `npm run build`

then under the sut, you will see a new folder named `build` which contains instrumented code,
e.g., `EMB-js\rest\ncs-js\build`

**Start EM Driver**

Go to the sut, e.g., `EMB-js\rest\ncs-js`
- step 6 `npm run em`

**Start EvoMaster**

- step 7 `java -jar evomaster.jar ` or with any installer

