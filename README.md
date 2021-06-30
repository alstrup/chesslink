# Chesslink

This is the start of a project to communicate with the chess link.

For inspiration, see

https://github.com/domschl/python-mchess

which is a Python library for communication.

Now that JS has Bluetooth and USB APIs, it is relevant to make a JS
implementation of the communication protocol. This project lays the
fundation for this.

## Usage

Open bluetooth.html in Chrome or Firefox or another browser, which
implements the Bluetooth API, and follow the instructions.

## USB Note

On Windows, the only drivers that work are the once from Zadig:
https://zadig.akeo.ie/

The official drivers are these, and they work with Python MChess,
but not in the browser for some reason:
https://www.hiarcs.com/eboard/ChessLinkDrivers.htm


## Protocol documentation

https://github.com/domschl/python-mchess/blob/master/mchess/magic-board.md

has documentation on the communication protocol. The core parts have been
implemented in this JS code.

## General bluetooth documentation

Introduction to bluetooth GATT
https://www.youtube.com/watch?v=eHqtiCMe4NA&list=PLYj4Cw17Aw7ypuXt7mDFWAyy6P661TD48&index=5

## Background info

There is a Java implementation for Android provided by Millenium, which I used
as a reference in addition to the Magic-Board code.

### Device specific characteristics


List of characteristics: (https://www.microchip.com/forums/m893253.aspx)

	// Write Notify
	"49535343-026e-3a9b-954c-97daef17e26e"

	// Notify. Called TX in Python mchess. Read on this gives 0, 0, 0, 0, 0.
	"49535343-1e4d-4bd9-ba61-23c647249616"

	// Read. Initial read gives:
	// 136, 102, 0, 0, 0, 0, 0, 0, 0   	^B
	"49535343-6daa-4d02-abf6-19569aca69fe"

	// Write. Called RX in Python mchess
	"49535343-8841-43f4-a8d4-ecbe34729bb3"

	// Write Notify
	"49535343-aca3-481c-91ec-d85e28a60318"

    public static String UUIDSTR_ISSC_TRANS_TX = "49535343-1e4d-4bd9-ba61-23c647249616";
    public static String UUIDSTR_ISSC_TRANS_RX = "49535343-8841-43f4-a8d4-ecbe34729bb3";
