
STU SDK for Linux
 
Version 2.16.1
 
----------------------------------------------------------------------------------------------------------------
About
 
The Wacom STU SDK for Linux provides a software interface to the STU series tablets.
The SDK is an extension of the STU SDK for Windows.
 
For further details on using the SDK see https://developer-docs.wacom.com
Navigate to: Wacom Ink Connectivity...STU SDK
References are included to the SDK sample code on GitHub
 
----------------------------------------------------------------------------------------------------------------
File Contents
 
documentation\
  Fedora27-gcc46.mak                    - Makefile for Fedora27 (rename to gcc46.mak before use)
  Linux-STU-SDK-Guide.pdf               - Guide for Ubuntu 16.04
    STU-SDK-Build-on-Fedora27.pdf         - Build guide for Fedora 27
    STU-SDK-Build-on-Ubuntu-12.04.05.pdf  - Build guide for Ubuntu 12.04.05
 
sdk\
  Wacom-STU-SDK-2.16.1.tar.bz2          - Installation package
 
       
----------------------------------------------------------------------------------------------------------------
Version History
 
STU SDK
    Release 2.16.1   20-July-2023
          Fix for TlsInterfaceImpl
             
    Release 2.15.4   29-May-2020
          Rebuilt zip file
             
    Release 2.15.3c  29-Oct-2019
          Added documentation for Ubuntu 12 and Fedora 27 builds
             
    Release 2.15.3b  13-Sep-2019
      Added 2.15.3 release for Linux which includes fix for the 540 and Java
             
    Release 2.15.3   23-Aug-2019
      Workaround for failure to initialize STU-540 correctly with Java
      Rebuild to counteract anti-virus false positives in STU SigCaptX
 
    Release 2.15.2   19-Jun-2019
      Added OpenSSL DLLs (Windows Java folders only) to SDK installer
   
    Release 2.15.1b  25-Mar-2019
      Incremented SigCaptX version 
      Fix for 64-bit Java when loading OpenSSL libraries
 
    Release 2.15.1   19-Feb-2019
      OpenSSL v1.1
      Fix to writeImageArea()
       
    Release 2.14.1 2018-08-21
      Fixes for 24-bit colour and getUID2()
 
    Release 2.13.6 2018-02-16
      Fixed an issue with missing symlinks for .so libraries
      Added missing functions in wgssSTU.jar
      Improved USB handling to address stability issues in Linux
 
    Release 2.13.5 2018-01-16
      Updated installed DemoButtons sample for STU-540
      Added root certificate for STU-541 certificate exchange validation
      Added mutex locking to Tablet class in case of multi-threaded calls
 
    Release 2.13.4 2017-10-04
      Added Java support for STU-541
      Made OpenSLL DLLs load on demand (wgssSTU.dll)
 
    Release 2.13.3 2017-07-04
      Rebuild for STU-SigCaptX
      Added Linux support for STU-540/541
      STU-541 C++ only
 
    Release 2.13.1  2017-03-27
      Added C support for STU-540
      Added Java support for STU-540
      Fixed issues in Linux: 32-bit build for Ubuntu 12.04.05 LTS added
 
    
Copyright © 2020 Wacom, Co., Ltd. All Rights Reserved.
