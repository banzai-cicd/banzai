#!/usr/bin/env groovy

def call(err) {
  return err.message.contains("The supplied credentials are invalid to login") ? true : false
}