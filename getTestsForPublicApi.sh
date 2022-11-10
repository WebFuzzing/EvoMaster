#!/bin/bash
while getopts i:s:u: option
do
  case "${option}"
    in
    i)client_id=${OPTARG};;
    s)client_secret=${OPTARG};;
    u)url=${OPTARG};;
    *)
  esac
done

pip3 install -r requirements.txt

mvn clean install -DskipTests

res_uuid=$(python3 getResidentToken.py)

resident_token="Bearer $(python3 getAuthToken.py --client_id "$client_id" --client_secret "$client_secret" --user_id "$res_uuid")"

# shellcheck disable=SC2140
java -jar core/target/evomaster.jar --blackBox true --bbTargetUrl "$url" --outputFolder "src/resident_token" --outputFormat JAVA_TESTNG --problemType GRAPHQL --maxTime 2m --ratePerMinute 120 --header0 "Authorization":"$resident_token"

client_token="Bearer $(python3 getAuthToken.py --client_id "$client_id" --client_secret "$client_secret")"

# shellcheck disable=SC2140
java -jar core/target/evomaster.jar --blackBox true --bbTargetUrl "$url"  --outputFolder "src/client_token" --outputFormat JAVA_TESTNG --problemType GRAPHQL --maxTime 2m --ratePerMinute 120 --header0 "Authorization":"$client_token"
