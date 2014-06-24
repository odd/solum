#!/bin/bash -x
name=${PWD##*/}
deployPath="../$name-deploy/"
cp -r .ebextensions $deployPath
cp -r target/docker/* $deployPath
