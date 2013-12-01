RPi::omx Remote
===============

Android remote for omxplayer on a Raspberry Pi. To achieve this it uses 
a Python 'bridge' module to forward commands to the omxplayer and receive
status updates from it. It is also used to query additional information
about the video file in playback like poster, title, etc.

This module needs a modified version of the omxplayer which can be found here:

    https://github.com/rycus86/omxplayer

The Python module - acting as a bridge between the client and the omxplayer -
can be found here:

    https://github.com/rycus86/omxremote-py

Video
-----

Here is a small video to demonstrate the client UI:

[![screencast](https://i.ytimg.com/vi/wfhlWqQlv28/3.jpg?1385905603632)](http://www.youtube.com/watch?v=wfhlWqQlv28)

Features
--------

The client is only working on Wi-Fi network at the moment and it searches for
the omxremote (Python bridge) on a predefined multicast address 
(224.1.1.7:42001). After this initial message the communication continues on
this port but by using unicast UDP messages.
There is very basic support to set some parameters of the omxplayers like 
initial volume, subtitle font size, subtitle alignment, etc.
You can browse files stored on the Pi and start the playback of a video file 
(optionally along with a subtitle). After the player starts the Python bridge
module start sending status update for the playback. It also tries to guess 
video information and fetch some information from TVDB if the video is probably
an episode of a TV show. The player can display these informations on the client,
provides a status bar notification and lock screen controls for it (if the system
supports this functions).

Download
--------

You can download the APK from here (for now):

    https://github.com/rycus86/omxremote-android/raw/master/RPi_omx_remote/RPi_omx_remote.apk
