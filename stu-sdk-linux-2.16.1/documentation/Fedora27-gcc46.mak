# gcc46.mak
# Copyright (c) 2015 Wacom Company Limited
# 2012-03-09 mholden
#
# tested on: g++ (GCC) 4.6.2 20111027 (Red Hat 4.6.2-1)  --  3.2.9-1.fc16.x86_64
#
# Dependencies:
#  openssl
#  libusb-1.0
#

ifndef MACHTYPE
$(error MACHTYPE not defined)
endif

OutDir=Linux/$(MACHTYPE)/
IntDir=Linux/$(MACHTYPE)/i/

ifndef SrcDir
SrcDir=../src/
endif

ifndef SamplesDir
SamplesDir=../samples/
endif

ifndef INCLUDEPATH
INCLUDEPATH+= -I../include
endif

INCLUDEPATH+= -I/usr/include/libusb-1.0

CPP=g++
CPPFLAGS=-c -Wall -Wno-unknown-pragmas $(INCLUDEPATH) -std=c++11 -fPIC -shared -fvisibility=hidden -fvisibility-inlines-hidden -g -ggdb -O0

ifeq ($(MACHTYPE),i686)
LIBPATH=/lib/
LIBBOOSTPATH=/usr/lib/
CPPFLAGS+= -m32
endif
ifeq ($(MACHTYPE),x86_64)
LIBPATH=/lib64/
LIBBOOSTPATH=/usr/lib/x86_64-linux-gnu/
endif


ifndef LIBPATH
$(error LIBPATH not defined)
endif

#	---------------------------------------------------------------------------

build	:	$(OutDir) $(IntDir)	$(OutDir)WacomGSS.a $(OutDir)getUsbDevices $(OutDir)simpleInterface $(OutDir)simpleTablet $(OutDir)query

.PHONY: build clean rebuild

clean	:
	-rm -Rf $(OutDir)

rebuild	:	clean	build

#	---------------------------------------------------------------------------

WacomGSS_Linux=\
	$(IntDir)libusb.a	\
	$(IntDir)libusbHelper.a

WacomGSS_utility=\
	$(IntDir)setThreadName.a	\
	$(IntDir)enumUsbDevices_libusb.a

WacomGSS_STU=\
	$(IntDir)Error.a	\
	$(IntDir)getUsbDevices.a	\
	$(IntDir)getTlsDevices.a	\
	$(IntDir)getUsbDevices_libusb.a	\
	$(IntDir)getTlsDevices_libusb.a	\
	$(IntDir)Interface.a	\
	$(IntDir)InterfaceImpl.a	\
	$(IntDir)InterfaceQueue.a	\
	$(IntDir)Protocol.a	\
	$(IntDir)ProtocolHelper.a	\
	$(IntDir)ReportHandler.a	\
	$(IntDir)SerialInterface.a	\
	$(IntDir)SerialProtocol.a	\
	$(IntDir)Tablet.a	\
	$(IntDir)TlsInterface.a	\
	$(IntDir)TlsInterfaceImpl.a	\
	$(IntDir)TlsInterface_Debug.a	\
	$(IntDir)TlsProtocol.a	\
	$(IntDir)TlsProtocolOOB.a	\
	$(IntDir)UsbInterface.a

WacomGSS_OpenSSL=\
	$(IntDir)OpenSSL_Loader.a		\
	$(IntDir)OpenSSL_eay.a		\
	$(IntDir)OpenSSL_ssl.a		\
	$(IntDir)Tablet_OpenSSL.a

WacomGSS_all=\
	$(WacomGSS_STU)	\
	$(WacomGSS_OpenSSL)	\
	$(WacomGSS_Linux)	\
	$(WacomGSS_utility)	

#	---------------------------------------------------------------------------

$(OutDir)	:
	mkdir -p $(OutDir)

$(IntDir)	:
	mkdir -p $(IntDir)

#	---------------------------------------------------------------------------

$(OutDir)WacomGSS.a : $(WacomGSS_all)
	ar r $@ $^

#	---------------------------------------------------------------------------

$(IntDir)setThreadName.a	:	$(SrcDir)utility/cpp/Linux/setThreadName.cpp
	$(CPP) $(CPPFLAGS) -o $@ $< 

$(IntDir)enumUsbDevices_libusb.a	:	$(SrcDir)utility/cpp/Linux/enumUsbDevices_libusb.cpp
	$(CPP) $(CPPFLAGS) -o $@ $< 

$(IntDir)OpenSSL_applink.a	:	$(SrcDir)utility/cpp/OpenSSL_applink.cpp
	$(CPP) $(CPPFLAGS) -o $@ $< 

$(IntDir)OpenSSL_Loader.a	:	$(SrcDir)utility/cpp/Linux/OpenSSL_Loader.cpp
	$(CPP) $(CPPFLAGS) -o $@ $< 

$(IntDir)OpenSSL_eay.a	:	$(SrcDir)utility/cpp/OpenSSL_eay.cpp
	$(CPP) $(CPPFLAGS) -o $@ $< 

$(IntDir)OpenSSL_ssl.a	:	$(SrcDir)utility/cpp/OpenSSL_ssl.cpp
	$(CPP) $(CPPFLAGS) -o $@ $< 

$(IntDir)libusb.a	:	$(SrcDir)Linux/cpp/libusb.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)libusbHelper.a	:	$(SrcDir)Linux/cpp/libusbHelper.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)Error.a	:	$(SrcDir)STU/cpp/Error.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)getUsbDevices.a	:	$(SrcDir)STU/cpp/getUsbDevices.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)getTlsDevices.a	:	$(SrcDir)STU/cpp/getTlsDevices.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)Interface.a	:	$(SrcDir)STU/cpp/Interface.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)InterfaceImpl.a	:	$(SrcDir)STU/cpp/InterfaceImpl.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)InterfaceQueue.a	:	$(SrcDir)STU/cpp/InterfaceQueue.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)Protocol.a	:	$(SrcDir)STU/cpp/Protocol.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)ProtocolHelper.a	:	$(SrcDir)STU/cpp/ProtocolHelper.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)ReportHandler.a	:	$(SrcDir)STU/cpp/ReportHandler.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)SerialProtocol.a	:	$(SrcDir)STU/cpp/SerialProtocol.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)Tablet.a	:	$(SrcDir)STU/cpp/Tablet.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)Tablet_OpenSSL.a	:	$(SrcDir)STU/cpp/Tablet_OpenSSL.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)SerialInterface.a	:	$(SrcDir)STU/cpp/Linux/SerialInterface.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)UsbInterface.a	:	$(SrcDir)STU/cpp/Linux/UsbInterface.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)TlsInterface.a	:	$(SrcDir)STU/cpp/Linux/TlsInterface.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)TlsInterfaceImpl.a	:	$(SrcDir)STU/cpp/TlsInterfaceImpl.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)TlsInterface_Debug.a	:	$(SrcDir)STU/cpp/TlsInterface_Debug.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)TlsProtocol.a	:	$(SrcDir)STU/cpp/TlsProtocol.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)TlsProtocolOOB.a	:	$(SrcDir)STU/cpp/TlsProtocolOOB.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)getUsbDevices_libusb.a	:	$(SrcDir)STU/cpp/Linux/getUsbDevices_libusb.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(IntDir)getTlsDevices_libusb.a	:	$(SrcDir)STU/cpp/Linux/getTlsDevices_libusb.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

$(OutDir)getUsbDevices	:	$(IntDir)sample_getUsbDevices.a $(OutDir)WacomGSS.a
	g++ -o $@ $^ -lusb-1.0 $(BOOSTLIBS)

$(IntDir)sample_getUsbDevices.a	:	$(SamplesDir)getUsbDevices.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

#	---------------------------------------------------------------------------

$(OutDir)simpleInterface	:	$(IntDir)sample_simpleInterface.a $(OutDir)WacomGSS.a
	g++ -o $@ $^ -lusb-1.0 -lpthread -lrt -ldl -lssl -lcrypto  $(BOOSTLIBS)

$(IntDir)sample_simpleInterface.a	:	$(SamplesDir)simpleInterface.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

#	---------------------------------------------------------------------------

$(OutDir)simpleTablet	:	$(IntDir)sample_simpleTablet.a $(OutDir)WacomGSS.a
	g++ -o $@ $^   -lusb-1.0 -lssl -lcrypto  -ldl -lpthread -lrt $(BOOSTLIBS)

$(IntDir)sample_simpleTablet.a	:	$(SamplesDir)simpleTablet.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

#	---------------------------------------------------------------------------

$(OutDir)query	:	$(IntDir)sample_query.a $(OutDir)WacomGSS.a
	g++ -o $@ $^  -lusb-1.0 -lssl -lcrypto  -ldl -lpthread -lrt $(BOOSTLIBS)

$(IntDir)sample_query.a	:	$(SamplesDir)query.cpp
	$(CPP) $(CPPFLAGS) -o $@ $<

#	---------------------------------------------------------------------------