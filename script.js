// Encoding stuff for how to send data with parity and checksums
function addParity(C) {
  C |= 0x80;
  for (var i = 0; i < 7; i++) {
    if ((C & (1 << i)) != 0) {
      C ^= 0x80;
    }
  }
  return C;
}

function checksum(s) {
  var par = 0x00;
  for (var i = 0; i < s.length; ++i) {
    par = par ^ s.charCodeAt(i);
  }
  return par.toString(16).toUpperCase();
}

// This encodes the message s with parity and checksum for transmission
function encode(s) {
  var msg = s + checksum(s);
  var res = [];
  for (var i = 0; i < msg.length; ++i) {
    res.push(addParity(msg.charCodeAt(i)));
  }
  return res;
}

var fragment = 200;
// This sends a command "m" to the board, handling encoding and chunking
function send(m) {
  console.log('Sending ' + m);
  var message = encode(m);
  sendChunk(message, 0);
}

function sendChunk(message, i) {
  if (i < message.length) {
    var rest = message.length - i;
    var chunk = rest < fragment ? rest : fragment;
    // console.log("Sending chunk of " + chunk);

    const buffer = new ArrayBuffer(chunk);
    var data = new Uint8Array(buffer);
    for (var j = 0; j < chunk; ++j) {
      data[j] = message[i + j];
    }
    if (writeCharacteristic != null) {
      writeCharacteristic
        .writeValue(data)
        .then(val => {
          // console.log("Sent chunk " + i);
          // console.log(data);
          sendChunk(message, i + fragment);
        })
        .catch(e => {
          console.log('Could not send ' + message + ': ' + e);
        });
    } else if (port != null) {
      console.log('Trying to send to serial');
      const writer = port.writable.getWriter();
      writer
        .write(data)
        .then(val => {
          console.log('Sent chunk ' + i);
          writer.releaseLock();
          sendChunk(message, i + fragment);
        })
        .catch(e => {
          console.log('Could not send ' + message + ': ' + e);
        });
    } else {
      console.log('ERROR: Connect to board through Bluetooth or USB first');
    }
  } else {
    console.log('Sent it');
  }
}

function dataView2UInt(dataview) {
  var data = new Uint8Array(dataview.byteLength);
  for (var i = 0; i < dataview.byteLength; ++i) {
    var c = dataview.getUint8(i);
    data[i] = c;
  }
  return data;
}

// This receives the board state from the board
function handleBoardData(data) {
  var print = function (message) {
    var row = '';
    for (var i = 0; i < data.length; ++i) {
      var c = data[i] & 0x7f;
      row += String.fromCharCode(c);
    }
    console.log(message + ': ' + row);
  };

  var response = String.fromCharCode(data[0] & 0x7f);
  if (response == 'l') {
    // For leds
    print('LED received');
  } else if (response == 't') {
    // For reset
    print('Reset received');
  } else if (response == 'x') {
    // For clear
    print('Clear received');
  } else if (response == 's') {
    // This is board state
    var boardIndex = 0;
    var board = [];
    for (var i = 0; i < data.length; ++i) {
      var c = data[i] & 0x7f;
      if (c < 0x20 || c > 0x7a) {
      } else if (c == 's'.charCodeAt(0)) {
        boardIndex = 0;
      } else if (boardIndex < 64) {
        board[boardIndex] = c;
        boardIndex++;
      } else {
        // Other stuff comes here,
        // but that is for the parity, which
        // we will just ignore
      }
    }

    // Now, we have the board in board. Print it
    console.log('Received board');
    boardIndex = 0;
    for (var y = 0; y < 8; ++y) {
      var row = '' + (y + 1) + ' ';
      for (var x = 0; x < 8; ++x) {
        row += String.fromCharCode(board[boardIndex]);
        boardIndex++;
      }
      console.log(row);
    }
  } else {
    console.log('Unknown response');
    console.log(dataview);
  }
}

function monitorCharacteristic(name, characteristic) {
  console.log('Got ' + name + ' characteristic');

  characteristic.addEventListener('characteristicvaluechanged', event => {
    var dataview = event.currentTarget.value;
    var data = dataView2UInt(dataview);
    handleBoardData(data);
  });
}

function read(name, characteristic) {
  characteristic
    .readValue()
    .then(val => {
      console.log('Read from ' + name);
      console.log(val);
    })
    .catch(e => {
      console.log('Could not read from ' + name + ': ' + e);
    });
}

// State for Bluetooth connection
var device = null;
var service = null;
var notifyCharacteristic = null; // Called TX
var writeCharacteristic = null; // Called RX

// This is what we run when we click for bluetooth
function discover() {
  let options = {
    filters: [{ name: 'MILLENNIUM CHESS' }],
    optionalServices: ['49535343-fe7d-4ae5-8fa9-9fafd205e455'],
  };
  navigator.bluetooth
    .requestDevice(options)
    .then(function (d) {
      device = d;
      console.log('Found ' + device.name + ' with id ' + device.id);
      // Do something with the device.
      return device.gatt.connect();
    })
    .then(server => {
      console.log('Got connection');
      return device.gatt.getPrimaryService('49535343-fe7d-4ae5-8fa9-9fafd205e455');
    })
    .then(s => {
      service = s;
      console.log('Got service');
      // console.log(s);
      // Find the notify characteristics
      return service.getCharacteristic('49535343-1e4d-4bd9-ba61-23c647249616');
    })
    .then(wc => {
      notifyCharacteristic = wc;
      monitorCharacteristic('notify (TX)', notifyCharacteristic);

      return notifyCharacteristic.startNotifications();
    })
    .then(char => {
      console.log('Started notifications. Make a move');
      return service.getCharacteristic('49535343-8841-43f4-a8d4-ecbe34729bb3');
    })
    .then(characteristic => {
      writeCharacteristic = characteristic;
      monitorCharacteristic('write (RX)', writeCharacteristic);
    })
    .catch(function (error) {
      console.log('Something went wrong. ' + error);
    });
}

// State for serial connection
var port = null;
async function discoverSerial() {
  navigator.serial.addEventListener('connect', event => {
    console.log('Serial connected');
    console.log(event);
  });
  navigator.serial.addEventListener('disconnect', event => {
    console.log('Serial disconnected');
    console.log(event);
  });

  if (port != null) {
    console.log('Already has a port');
    if (!port.readable) {
      console.log('Stream is not readable');
    }
  } else {
    // Prompt user to select any serial port.
    port = await navigator.serial.requestPort([{ usbVendorId: 0x0403 }]);
    await port.open({ baudRate: 38400 });
    if (port.readable) {
      const reader = port.readable.getReader();
      startSerialListening(reader);
    } else {
      console.log('Stream is not readable');
    }
  }
}

// Called when we get data from the serial port
function startSerialListening(reader) {
  reader
    .read()
    .then(data => {
      const { value, done } = data;
      console.log('Received serial data');
      if (done) {
        console.log('Done');
        reader.releaseLock();
      } else {
        handleBoardData(value);
      }
      startSerialListening(reader);
    })
    .catch(function (error) {
      console.log('Could not read serial');
      console.log(error);
    });
}

function readall() {
  read('notify', notifyCharacteristic);
}

// This blinks all LEDs fast. The 55 is a bit-pattern in upper-case hex for a 8-clock cycle of the LED pattern
function sendfast() {
  var leds = 'L32';
  for (var x = 0; x < 9; ++x) {
    for (var y = 0; y < 9; ++y) {
      leds += '55';
    }
  }
  send(leds);
}

// Reset the board
function sendreset() {
  send('T');
}

// Clear the LEDs
function sendclear() {
  send('X');
}

function sendLetter(l) {
  var leds = 'L32';
  for (var y = 0; y < 9; ++y) {
    var row = l[y];
    for (var x = 0; x < 9; ++x) {
      var char = row.charAt(8 - x);
      if (char == '0') {
        leds += '00';
      } else if (char == '1') {
        leds += '55';
      } else if (char == '2') {
        leds += 'FF';
      } else {
        leds += char + char;
      }
    }
  }
  send(leds);
}

function sendlove() {
  sendLetter([
    '000020000',
    '000222000',
    '000222000',
    '002202200',
    '002202200',
    '002222200',
    '022000220',
    '022000220',
    '220000022',
  ]);

  setTimeout(function () {
    sendLetter([
      '000000000',
      '002000200',
      '022202220',
      '222222222',
      '222222222',
      '022222220',
      '002222200',
      '000222000',
      '000020000',
    ]);

    setTimeout(function () {
      sendLetter([
        '000222200',
        '002222220',
        '022000022',
        '220000000',
        '220000000',
        '220000000',
        '022000022',
        '002222220',
        '000222200',
      ]);
    }, 2000);
  }, 2000);
}
