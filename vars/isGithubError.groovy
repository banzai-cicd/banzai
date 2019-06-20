#!/usr/bin/env groovy

def call(err) {
  if (err == null) { return false }

  return err.message && err.message.contains("The supplied credentials are invalid to login") ? true : false
}