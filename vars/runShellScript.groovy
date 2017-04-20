
def call(SCRIPT_FILE_NAME, args=null ) {
  //Modify Variable to ensure path starts with "./"
  if(SCRIPT_FILE_NAME.charAt(0) == "/"){
    SCRIPT_FILE_NAME = "." + SCRIPT_FILE_NAME
  }
  if(SCRIPT_FILE_NAME.charAt(0) != "."){
    SCRIPT_FILE_NAME = "./" + SCRIPT_FILE_NAME
  }

  println "Running Shell Script ${SCRIPT_FILE_NAME}"
  println "Cmd: ${WORKSPACE}/${SCRIPT_FILE_NAME}"

  sh """#!/bin/bash
    if [ -f "${WORKSPACE}/${SCRIPT_FILE_NAME}" ] ; then
      /bin/bash ${WORKSPACE}/${SCRIPT_FILE_NAME} ${args ? args.join(' '): ''}
    else
      echo "'${WORKSPACE}/${SCRIPT_FILE_NAME}' does not exist!"
      exit 0
    fi
  """
}
