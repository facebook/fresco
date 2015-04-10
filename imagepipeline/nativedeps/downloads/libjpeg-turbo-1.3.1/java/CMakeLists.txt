set(JAR_FILE turbojpeg.jar)
set(MANIFEST_FILE ${CMAKE_CURRENT_SOURCE_DIR}/MANIFEST.MF)

set(JAVA_CLASSNAMES org/libjpegturbo/turbojpeg/TJ
  org/libjpegturbo/turbojpeg/TJCompressor
  org/libjpegturbo/turbojpeg/TJCustomFilter
  org/libjpegturbo/turbojpeg/TJDecompressor
  org/libjpegturbo/turbojpeg/TJScalingFactor
  org/libjpegturbo/turbojpeg/TJTransform
  org/libjpegturbo/turbojpeg/TJTransformer
  TJUnitTest
  TJExample
  TJBench)

if(MSVC_IDE)
  set(OBJDIR "${CMAKE_CURRENT_BINARY_DIR}/${CMAKE_CFG_INTDIR}")
else()
  set(OBJDIR ${CMAKE_CURRENT_BINARY_DIR})
endif()

set(TURBOJPEG_DLL_NAME "turbojpeg")
if(MINGW)
  set(TURBOJPEG_DLL_NAME "libturbojpeg")
endif()
configure_file(org/libjpegturbo/turbojpeg/TJLoader.java.in
  ${CMAKE_CURRENT_BINARY_DIR}/org/libjpegturbo/turbojpeg/TJLoader.java)

set(JAVA_SOURCES "")
set(JAVA_CLASSES "")
set(JAVA_CLASSES_FULL "")
foreach(class ${JAVA_CLASSNAMES})
  set(JAVA_SOURCES ${JAVA_SOURCES} ${CMAKE_CURRENT_SOURCE_DIR}/${class}.java)
  set(JAVA_CLASSES ${JAVA_CLASSES} ${class}.class)
  set(JAVA_CLASSES_FULL ${JAVA_CLASSES_FULL} ${OBJDIR}/${class}.class)
endforeach()

set(JAVA_SOURCES ${JAVA_SOURCES}
  ${CMAKE_CURRENT_BINARY_DIR}/org/libjpegturbo/turbojpeg/TJLoader.java)
set(JAVA_CLASSES ${JAVA_CLASSES}
  org/libjpegturbo/turbojpeg/TJLoader.class)
set(JAVA_CLASSES_FULL ${JAVA_CLASSES_FULL}
  ${OBJDIR}/org/libjpegturbo/turbojpeg/TJLoader.class)

string(REGEX REPLACE " " ";" JAVACFLAGS "${JAVACFLAGS}")
add_custom_command(OUTPUT ${JAVA_CLASSES_FULL} DEPENDS ${JAVA_SOURCES}
  COMMAND ${JAVA_COMPILE} ARGS ${JAVACFLAGS} -d ${OBJDIR} ${JAVA_SOURCES})

add_custom_command(OUTPUT ${JAR_FILE} DEPENDS ${JAVA_CLASSES_FULL}
  ${MANIFEST_FILE}
  COMMAND ${JAVA_ARCHIVE} cfm ${JAR_FILE} ${MANIFEST_FILE} ${JAVA_CLASSES}
  WORKING_DIRECTORY ${OBJDIR})

add_custom_target(java ALL DEPENDS ${JAR_FILE})

install(FILES ${CMAKE_CURRENT_BINARY_DIR}/${JAR_FILE} DESTINATION classes)
