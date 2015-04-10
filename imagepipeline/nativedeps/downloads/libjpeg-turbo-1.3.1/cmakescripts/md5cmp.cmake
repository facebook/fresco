if(NOT MD5)
  message(FATAL_ERROR "MD5 not specified")
endif()

if(NOT FILE)
  message(FATAL_ERROR "FILE not specified")
endif()

file(MD5 ${FILE} MD5FILE)

if(NOT MD5 STREQUAL MD5FILE)
	message(FATAL_ERROR "MD5 of ${FILE} should be ${MD5}, not ${MD5FILE}.")
else()
	message(STATUS "${MD5}: OK")
endif()
