#!/bin/bash
name=${PWD##*/}
deployPath="../$name-deploy/"
cp -r target/docker/* $deployPath
cd $deployPath
