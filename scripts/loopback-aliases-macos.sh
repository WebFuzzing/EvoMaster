#!/bin/sh

# The script helps to set multiple aliases for loopback on MacOS.
# 
# Needs to be run as root to set aliases on the given network 
# interface.
# 
# Most cases lo0 will be the default. If there is a need to select
# custom network interface, can be done via passing the name of the interface
# after the action argument.
# set - assgins aliases from 2 to 20
# remove - removes the aliases from 2 to 20
# Setting aliases to the full range breaks the internet connectivity for some
# unknown reasons. Due to that it is set to 20.


# Validates whether executing user has root privileges
if [ $EUID -ne 0 ]
	then echo "Please run as root"
exit
fi

# It'll use lo0 as default interface unless the interface name is explicitly
# provided via argument.
if [ -z "$2" ]
then
	INTERFACE="lo0"
	echo "Setting the aliases on the default network interface: $INTERFACE"
else
	INTERFACE=$2
	echo "Setting the aliases on the custom network interface: $INTERFACE"
fi


# Function to set aliases
set_alias() {
	for i in {2..20}
	do
		ifconfig $INTERFACE alias "127.0.0.$i"
		if [ $? -eq 0 ]
		then
			echo "Successfully set 127.0.0.$i to $INTERFACE"
		else
			echo "Unable to set 127.0.0.$i to $INTERFACE"
			exit
		fi
	done
}

# Function to remove aliases
remove_alias() {
	for i in {2..20}
	do
		ifconfig $INTERFACE -alias "127.0.0.$i"
		if [ $? -eq 0 ]
		then
			echo "Successfully removed 127.0.0.$i from $INTERFACE"
		else
			echo "Unable to remove 127.0.0.$i from $INTERFACE"
			exit
		fi
	done
}

if [ -z "$1" ]
then
	echo "Select the operation to perform: s (set) or r (remove)"
	exit
fi

if [[ $1 == "s" ]]
then
	set_alias
elif [[ $1 == "r" ]]
then
	remove_alias
else
	echo "No operation selected to execute"
fi
