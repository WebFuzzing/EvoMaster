#!/usr/bin/env bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

VERSION=3.0.1
SOURCE=https://api.nuget.org/v3/index.json

APIKEY=$1

echo Going to publish libraries to NuGet

dotnet nuget push $SCRIPT_DIR/src/EvoMaster.Controller/bin/Debug/EvoMaster.Controller.$VERSION.nupkg  --api-key $APIKEY  --source $SOURCE
if [ $? -ne 0 ] ; then
   echo $(date) "UPLOAD ERROR. Exist status " $?
   exit 1
fi

dotnet nuget push $SCRIPT_DIR/src/EvoMaster.Client.Util/bin/Debug/EvoMaster.Client.Util.$VERSION.nupkg  --api-key $APIKEY  --source $SOURCE
if [ $? -ne 0 ] ; then
   echo $(date) "UPLOAD ERROR. Exist status " $?
   exit 1
fi

dotnet nuget push $SCRIPT_DIR/src/EvoMaster.Controller.Api/bin/Debug/EvoMaster.Controller.Api.$VERSION.nupkg  --api-key $APIKEY  --source $SOURCE
if [ $? -ne 0 ] ; then
   echo $(date) "UPLOAD ERROR. Exist status " $?
   exit 1
fi


dotnet nuget push $SCRIPT_DIR/src/EvoMaster.DatabaseController/bin/Debug/EvoMaster.DatabaseController.$VERSION.nupkg  --api-key $APIKEY  --source $SOURCE
if [ $? -ne 0 ] ; then
   echo $(date) "UPLOAD ERROR. Exist status " $?
   exit 1
fi

dotnet nuget push $SCRIPT_DIR/src/EvoMaster.DatabaseController.Abstractions/bin/Debug/EvoMaster.DatabaseController.Abstractions.$VERSION.nupkg  --api-key $APIKEY  --source $SOURCE
if [ $? -ne 0 ] ; then
   echo $(date) "UPLOAD ERROR. Exist status " $?
   exit 1
fi


dotnet nuget push $SCRIPT_DIR/src/EvoMaster.Instrumentation/bin/Debug/EvoMaster.Instrumentation.$VERSION.nupkg  --api-key $APIKEY  --source $SOURCE
if [ $? -ne 0 ] ; then
   echo $(date) "UPLOAD ERROR. Exist status " $?
   exit 1
fi


dotnet nuget push $SCRIPT_DIR/src/EvoMaster.Instrumentation_Shared/bin/Debug/EvoMaster.Instrumentation_Shared.$VERSION.nupkg  --api-key $APIKEY  --source $SOURCE
if [ $? -ne 0 ] ; then
   echo $(date) "UPLOAD ERROR. Exist status " $?
   exit 1
fi


echo UPLOAD COMPLETED SUCCESSFULLY