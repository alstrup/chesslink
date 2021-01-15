# Chesslink

This is the start of a project to communicate with the chess link.

For inspiration, see

https://github.com/domschl/python-mchess

which is a Python library for communication.

Now that JS has Bluetooth and USB APIs, it is relevant to make a JS
implementation of the communication protocol.

## Usage

1. Turn on your chess link, connect it with your laptop/computer/phone
using bluetooth.

2. Open bluetooth.html in Chrome or Firefox or another browser, which
implements the Bluetooth API.

3. Open the JS development console. Ctrl+shift+j in Chrome.

4. Click Connect. It will scan for bluetooth devices, and your millenium
   chess device should appear. Pair with that, and you will get output
   about a connection, a service, and three different characteristics.

5. TODO: Figure out how to initialize the board state, & get moves.

## Protocol documentation

https://github.com/domschl/python-mchess/blob/master/mchess/magic-board.md

has documentation on the communication protocol.

## Gweneral bluetooth documentation

Introduction to bluetooth GATT
https://www.youtube.com/watch?v=eHqtiCMe4NA&list=PLYj4Cw17Aw7ypuXt7mDFWAyy6P661TD48&index=5

## Device specific characteristics

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
