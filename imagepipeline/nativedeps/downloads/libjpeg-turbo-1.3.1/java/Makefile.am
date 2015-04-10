JAVAROOT = .

org/libjpegturbo/turbojpeg/TJLoader.java: $(srcdir)/org/libjpegturbo/turbojpeg/TJLoader.java.tmpl
	mkdir -p org/libjpegturbo/turbojpeg; \
	cat $(srcdir)/org/libjpegturbo/turbojpeg/TJLoader.java.tmpl | \
		sed s@%{__libdir}@$(libdir)@g > org/libjpegturbo/turbojpeg/TJLoader.java


JAVASOURCES = org/libjpegturbo/turbojpeg/TJ.java \
	org/libjpegturbo/turbojpeg/TJCompressor.java \
	org/libjpegturbo/turbojpeg/TJCustomFilter.java \
	org/libjpegturbo/turbojpeg/TJDecompressor.java \
	org/libjpegturbo/turbojpeg/TJScalingFactor.java \
	org/libjpegturbo/turbojpeg/TJTransform.java \
	org/libjpegturbo/turbojpeg/TJTransformer.java \
	TJExample.java \
	TJUnitTest.java \
	TJBench.java

JNIHEADERS = org_libjpegturbo_turbojpeg_TJ.h \
	org_libjpegturbo_turbojpeg_TJCompressor.h \
	org_libjpegturbo_turbojpeg_TJDecompressor.h \
	org_libjpegturbo_turbojpeg_TJTransformer.h

if WITH_JAVA

nodist_noinst_JAVA = ${JAVASOURCES} org/libjpegturbo/turbojpeg/TJLoader.java

JAVA_CLASSES = org/libjpegturbo/turbojpeg/TJ.class \
	org/libjpegturbo/turbojpeg/TJCompressor.class \
	org/libjpegturbo/turbojpeg/TJCustomFilter.class \
	org/libjpegturbo/turbojpeg/TJDecompressor.class \
	org/libjpegturbo/turbojpeg/TJLoader.class \
	org/libjpegturbo/turbojpeg/TJScalingFactor.class \
	org/libjpegturbo/turbojpeg/TJTransform.class \
	org/libjpegturbo/turbojpeg/TJTransformer.class \
	TJExample.class \
	TJUnitTest.class \
	TJBench.class

all: all-am turbojpeg.jar

turbojpeg.jar: $(JAVA_CLASSES) ${srcdir}/MANIFEST.MF
	$(JAR) cfm turbojpeg.jar ${srcdir}/MANIFEST.MF $(JAVA_CLASSES)

clean-local:
	rm -f turbojpeg.jar

install-exec-local: turbojpeg.jar
	mkdir -p $(DESTDIR)/$(datadir)/classes
	$(INSTALL) -m 644 turbojpeg.jar $(DESTDIR)/$(datadir)/classes/

uninstall-local:
	rm -f $(DESTDIR)/$(datadir)/classes/turbojpeg.jar
	if [ -d $(DESTDIR)/$(datadir)/classes ]; then rmdir $(DESTDIR)/$(datadir)/classes; fi

headers: all
	javah -d ${srcdir} org.libjpegturbo.turbojpeg.TJ; \
	javah -d ${srcdir} org.libjpegturbo.turbojpeg.TJCompressor; \
	javah -d ${srcdir} org.libjpegturbo.turbojpeg.TJDecompressor; \
	javah -d ${srcdir} org.libjpegturbo.turbojpeg.TJTransformer

docs: all
	mkdir -p ${srcdir}/doc; \
	javadoc -notimestamp -d ${srcdir}/doc -sourcepath ${srcdir} org.libjpegturbo.turbojpeg

endif

EXTRA_DIST = MANIFEST.MF ${JAVASOURCES} ${JNIHEADERS} doc CMakeLists.txt \
	org/libjpegturbo/turbojpeg/TJLoader.java.tmpl \
	org/libjpegturbo/turbojpeg/TJLoader.java.in
