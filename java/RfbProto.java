//
//  Copyright (C) 2012 Secure Mission Solutions, Inc.  All Rights Reserved.
//  Copyright (C) 2009-2010, 2012-2013 D. R. Commander.  All Rights Reserved.
//  Copyright (C) 2009 Paul Donohue.  All Rights Reserved.
//  Copyright (C) 2001-2004 HorizonLive.com, Inc.  All Rights Reserved.
//  Copyright (C) 2001-2006 Constantin Kaplinsky.  All Rights Reserved.
//  Copyright (C) 2000 Tridia Corporation.  All Rights Reserved.
//  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
//
//  This is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This software is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this software; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
//  USA.
//

//
// RfbProto.java
//

import java.io.*;
import java.awt.event.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.zip.*;

class RfbProto {

  final static String
    versionMsg_3_3 = "RFB 003.003\n",
    versionMsg_3_7 = "RFB 003.007\n",
    versionMsg_3_8 = "RFB 003.008\n";

  // Vendor signatures: standard VNC/RealVNC, TridiaVNC, and TightVNC
  final static String
    StandardVendor  = "STDV",
    TridiaVncVendor = "TRDV",
    TightVncVendor  = "TGHT",
    TurboVncVendor  = "TRBO";

  // Security types
  final static int
    SecTypeInvalid = 0,
    SecTypeNone    = 1,
    SecTypeVncAuth = 2,
    SecTypeTight   = 16;

  // Supported tunneling types
  final static int
    NoTunneling = 0;
  final static String
    SigNoTunneling = "NOTUNNEL";

  // Supported authentication types
  final static int
    AuthNone      = 1,
    AuthVNC       = 2,
    AuthUnixLogin = 129;
  final static String
    SigAuthNone      = "NOAUTH__",
    SigAuthVNC       = "VNCAUTH_",
    SigAuthUnixLogin = "ULGNAUTH";

  // VNC authentication results
  final static int
    VncAuthOK      = 0,
    VncAuthFailed  = 1,
    VncAuthTooMany = 2;

  // Standard server-to-client messages
  final static int
    FramebufferUpdate   = 0,
    SetColourMapEntries = 1,
    Bell                = 2,
    ServerCutText       = 3;

  // Non-standard server-to-client messages
  final static int
    EndOfContinuousUpdates = 150;
  final static String
    SigEndOfContinuousUpdates = "CUS_EOCU";

  // Standard client-to-server messages
  final static int
    SetPixelFormat           = 0,
    FixColourMapEntries      = 1,
    SetEncodings             = 2,
    FramebufferUpdateRequest = 3,
    KeyboardEvent            = 4,
    PointerEvent             = 5,
    ClientCutText            = 6;

  // Non-standard client-to-server messages
  final static int
    EnableContinuousUpdates = 150;
  final static String
    SigEnableContinuousUpdates = "CUC_ENCU";

  // Supported encodings and pseudo-encodings
  final static int
    EncodingRaw            = 0,
    EncodingCopyRect       = 1,
    EncodingRRE            = 2,
    EncodingCoRRE          = 4,
    EncodingHextile        = 5,
    EncodingZlib           = 6,
    EncodingTight          = 7,
    EncodingZRLE           = 16,
    EncodingSubsamp1X = 0xFFFFFD00,
    EncodingFineQualityLevel0 = 0xFFFFFE00,
    EncodingCompressLevel0 = 0xFFFFFF00,
    EncodingQualityLevel0  = 0xFFFFFFE0,
    EncodingXCursor        = 0xFFFFFF10,
    EncodingRichCursor     = 0xFFFFFF11,
    EncodingPointerPos     = 0xFFFFFF18,
    EncodingLastRect       = 0xFFFFFF20,
    EncodingNewFBSize      = 0xFFFFFF21;
  final static String
    SigEncodingRaw            = "RAW_____",
    SigEncodingCopyRect       = "COPYRECT",
    SigEncodingRRE            = "RRE_____",
    SigEncodingCoRRE          = "CORRE___",
    SigEncodingHextile        = "HEXTILE_",
    SigEncodingZlib           = "ZLIB____",
    SigEncodingTight          = "TIGHT___",
    SigEncodingZRLE           = "ZRLE____",
    SigEncodingCompressLevel0 = "COMPRLVL",
    SigEncodingQualityLevel0  = "JPEGQLVL",
    SigEncodingXCursor        = "X11CURSR",
    SigEncodingRichCursor     = "RCHCURSR",
    SigEncodingPointerPos     = "POINTPOS",
    SigEncodingLastRect       = "LASTRECT",
    SigEncodingNewFBSize      = "NEWFBSIZ",
    SigEncodingFineQualityLevel0 = "FINEQLVL",
    SigEncodingSubsamp1X      = "SSAMPLVL";

  final static int MaxNormalEncoding = 255;

  // Contstants used in the Hextile decoder
  final static int
    HextileRaw                 = 1,
    HextileBackgroundSpecified = 2,
    HextileForegroundSpecified = 4,
    HextileAnySubrects         = 8,
    HextileSubrectsColoured    = 16;

  // Contstants used in the Tight decoder
  final static int TightMinToCompress = 12;
  final static int
    TightExplicitFilter = 0x04,
    TightFill           = 0x08,
    TightJpeg           = 0x09,
    TightNoZlib         = 0x0A,
    TightMaxSubencoding = 0x09,
    TightFilterCopy     = 0x00,
    TightFilterPalette  = 0x01,
    TightFilterGradient = 0x02;


  String host;
  int port;
  Socket sock;
  OutputStream os;
  SessionRecorder rec;
  boolean inNormalProtocol = false;
  VncViewer viewer;

  // Input stream is declared private to make sure it can be accessed
  // only via RfbProto methods. We have to do this because we want to
  // count how many bytes were read.
  private DataInputStream is;
  private long numBytesRead = 0;
  public long getNumBytesRead() { return numBytesRead; }

  // Java on UNIX does not call keyPressed() on some keys, for example
  // swedish keys To prevent our workaround to produce duplicate
  // keypresses on JVMs that actually works, keep track of if
  // keyPressed() for a "broken" key was called or not. 
  boolean brokenKeyPressed = false;

  // This will be set to true on the first framebuffer update
  // containing Zlib-, ZRLE- or Tight-encoded data.
  boolean wereZlibUpdates = false;

  // This will be set to false if the startSession() was called after
  // we have received at least one Zlib-, ZRLE- or Tight-encoded
  // framebuffer update.
  boolean recordFromBeginning = true;

  // This fields are needed to show warnings about inefficiently saved
  // sessions only once per each saved session file.
  boolean zlibWarningShown;
  boolean tightWarningShown;

  // Before starting to record each saved session, we set this field
  // to 0, and increment on each framebuffer update. We don't flush
  // the SessionRecorder data into the file before the second update. 
  // This allows us to write initial framebuffer update with zero
  // timestamp, to let the player show initial desktop before
  // playback.
  int numUpdatesInSession;

  // Measuring network throughput.
  boolean timing;
  long timeWaitedIn100us;
  long timedKbits;

  // Protocol version and TightVNC-specific protocol options.
  int serverMajor, serverMinor;
  int clientMajor, clientMinor;
  boolean protocolTightVNC;
  CapsContainer tunnelCaps, authCaps;
  CapsContainer serverMsgCaps, clientMsgCaps;
  CapsContainer encodingCaps;

  // "Continuous updates" is a TightVNC-specific feature that allows
  // receiving framebuffer updates continuously, without sending update
  // requests. The variables below track the state of this feature.
  // Initially, continuous updates are disabled. They can be enabled
  // by calling tryEnableContinuousUpdates() method, and only if this
  // feature is supported by the server. To disable continuous updates,
  // tryDisableContinuousUpdates() should be called.
  private boolean continuousUpdatesActive = false;
  private boolean continuousUpdatesEnding = false;

  // If true, informs that the RFB socket was closed.
  private boolean closed;

  //
  // Constructor. Make TCP connection to RFB server.
  //

  RfbProto(String h, int p, VncViewer v) throws IOException {
    viewer = v;
    host = h;
    port = p;

    if (viewer.socketFactory == null) {
      sock = new Socket(host, port);
      sock.setTcpNoDelay(true);
    } else {
      try {
	Class factoryClass = Class.forName(viewer.socketFactory);
	SocketFactory factory = (SocketFactory)factoryClass.newInstance();
	if (viewer.inAnApplet)
	  sock = factory.createSocket(host, port, viewer);
	else
	  sock = factory.createSocket(host, port, viewer.mainArgs);
      } catch(Exception e) {
	e.printStackTrace();
	throw new IOException(e.getMessage());
      }
    }
    is = new DataInputStream(new BufferedInputStream(sock.getInputStream(),
						     16384));
    os = sock.getOutputStream();

    timing = false;
    timeWaitedIn100us = 5;
    timedKbits = 0;
  }


  synchronized void close() {
    try {
      sock.close();
      closed = true;
      System.out.println("RFB socket closed");
      if (rec != null) {
	rec.close();
	rec = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  synchronized boolean closed() {
    return closed;
  }

  //
  // Read server's protocol version message
  //

  void readVersionMsg() throws Exception {

    byte[] b = new byte[12];

    readFully(b);

    if ((b[0] != 'R') || (b[1] != 'F') || (b[2] != 'B') || (b[3] != ' ')
	|| (b[4] < '0') || (b[4] > '9') || (b[5] < '0') || (b[5] > '9')
	|| (b[6] < '0') || (b[6] > '9') || (b[7] != '.')
	|| (b[8] < '0') || (b[8] > '9') || (b[9] < '0') || (b[9] > '9')
	|| (b[10] < '0') || (b[10] > '9') || (b[11] != '\n'))
    {
      throw new Exception("Host " + host + " port " + port +
			  " is not an RFB server");
    }

    serverMajor = (b[4] - '0') * 100 + (b[5] - '0') * 10 + (b[6] - '0');
    serverMinor = (b[8] - '0') * 100 + (b[9] - '0') * 10 + (b[10] - '0');

    if (serverMajor < 3) {
      throw new Exception("RFB server does not support protocol version 3");
    }
  }


  //
  // Write our protocol version message
  //

  void writeVersionMsg() throws IOException {
    clientMajor = 3;
    if (serverMajor > 3 || serverMinor >= 8) {
      clientMinor = 8;
      os.write(versionMsg_3_8.getBytes());
    } else if (serverMinor >= 7) {
      clientMinor = 7;
      os.write(versionMsg_3_7.getBytes());
    } else {
      clientMinor = 3;
      os.write(versionMsg_3_3.getBytes());
    }
    protocolTightVNC = false;
    initCapabilities();
  }


  //
  // Negotiate the authentication scheme.
  //

  int negotiateSecurity() throws Exception {
    return (clientMinor >= 7) ?
      selectSecurityType() : readSecurityType();
  }

  //
  // Read security type from the server (protocol version 3.3).
  //

  int readSecurityType() throws Exception {
    int secType = readU32();

    switch (secType) {
    case SecTypeInvalid:
      readConnFailedReason();
      return SecTypeInvalid;	// should never be executed
    case SecTypeNone:
    case SecTypeVncAuth:
      return secType;
    default:
      throw new Exception("Unknown security type from RFB server: " + secType);
    }
  }

  //
  // Select security type from the server's list (protocol versions 3.7/3.8).
  //

  int selectSecurityType() throws Exception {
    int secType = SecTypeInvalid;

    // Read the list of secutiry types.
    int nSecTypes = readU8();
    if (nSecTypes == 0) {
      readConnFailedReason();
      return SecTypeInvalid;	// should never be executed
    }
    byte[] secTypes = new byte[nSecTypes];
    readFully(secTypes);

    // Find out if the server supports TightVNC protocol extensions
    for (int i = 0; i < nSecTypes; i++) {
      if (secTypes[i] == SecTypeTight) {
	protocolTightVNC = true;
	os.write(SecTypeTight);
	return SecTypeTight;
      }
    }

    // Find first supported security type.
    for (int i = 0; i < nSecTypes; i++) {
      if (secTypes[i] == SecTypeNone || secTypes[i] == SecTypeVncAuth) {
	secType = secTypes[i];
	break;
      }
    }

    if (secType == SecTypeInvalid) {
      throw new Exception("Server did not offer supported security type");
    } else {
      os.write(secType);
    }

    return secType;
  }

  //
  // Perform "no authentication".
  //

  void authenticateNone() throws Exception {
    if (clientMinor >= 8)
      readSecurityResult("No authentication");
  }

  //
  // Perform standard VNC Authentication.
  //

  void authenticateVNC(String pw) throws Exception {
    byte[] challenge = new byte[16];
    readFully(challenge);

    if (pw.length() > 8)
      pw = pw.substring(0, 8);	// Truncate to 8 chars

    // Truncate password on the first zero byte.
    int firstZero = pw.indexOf(0);
    if (firstZero != -1)
      pw = pw.substring(0, firstZero);

    byte[] key = {0, 0, 0, 0, 0, 0, 0, 0};
    System.arraycopy(pw.getBytes(), 0, key, 0, pw.length());

    DesCipher des = new DesCipher(key);

    des.encrypt(challenge, 0, challenge, 0);
    des.encrypt(challenge, 8, challenge, 8);

    os.write(challenge);

    readSecurityResult("VNC authentication");
  }

  //
  // Perform Unix Login Authentication.
  //

  void authenticateUnixLogin(String user, String pw) throws Exception {

    if (user.length() < 1)
      throw new Exception("Empty user name");
    writeInt(user.length());
    if (pw.length() < 1)
      throw new Exception("Empty password");
    writeInt(pw.length());
    os.write(user.getBytes());
    os.write(pw.getBytes());

    readSecurityResult("Unix login authentication");
  }
  
  // 
  // Convert a character array to a byte array with the default character set.
  //
  // An empty byte array is returned if any errors are detected during the
  // conversion.
  //

  private byte[] charArrayToByteArray(char[] chars) {
    byte[] bytes;

    try {
      Charset charset = Charset.defaultCharset();
      CharsetEncoder encoder = charset.newEncoder();
      CharBuffer cbuf = CharBuffer.wrap(chars);
      ByteBuffer bbuf = encoder.encode(cbuf);
      bytes = bbuf.array();
    }
    catch ( NullPointerException ex )          { bytes = new byte[0]; }
    catch ( CharacterCodingException ex )      { bytes = new byte[0]; }
    catch ( ReadOnlyBufferException ex )       { bytes = new byte[0]; }
    catch ( UnsupportedOperationException ex ) { bytes = new byte[0]; }

    return(bytes);
  }

  //
  // Perform Unix login authentication using a plugin.
  //

  void authenticateUnixLogin(String user, char[] pw, String authType)
    throws Exception {
    byte[] bpw = charArrayToByteArray(pw);

    if (user.length() < 1)
      throw new Exception("Empty user name");
    writeInt(user.length());
    if (bpw.length < 1)
      throw new Exception("Empty password");
    writeInt(pw.length);
    os.write(user.getBytes());
    os.write(bpw);
    Arrays.fill(bpw, (byte) 0);

    readSecurityResult("Unix login authentication (" + authType + ")");
  }

  //
  // Read security result.
  // Throws an exception on authentication failure.
  //

  void readSecurityResult(String authType) throws Exception {
    int securityResult = readU32();

    switch (securityResult) {
    case VncAuthOK:
      System.out.println(authType + ": success");
      break;
    case VncAuthFailed:
      if (clientMinor >= 8)
        readConnFailedReason();
      throw new Exception(authType + ": failed");
    case VncAuthTooMany:
      throw new Exception(authType + ": failed, too many tries");
    default:
      throw new Exception(authType + ": unknown result " + securityResult);
    }
  }

  //
  // Read the string describing the reason for a connection failure,
  // and throw an exception.
  //

  void readConnFailedReason() throws Exception {
    int reasonLen = readU32();
    byte[] reason = new byte[reasonLen];
    readFully(reason);
    throw new Exception(new String(reason));
  }

  //
  // Initialize capability lists (TightVNC protocol extensions).
  //

  void initCapabilities() {
    tunnelCaps    = new CapsContainer();
    authCaps      = new CapsContainer();
    serverMsgCaps = new CapsContainer();
    clientMsgCaps = new CapsContainer();
    encodingCaps  = new CapsContainer();

    // Supported authentication methods
    authCaps.add(AuthNone, StandardVendor, SigAuthNone,
		 "No authentication");
    authCaps.add(AuthVNC, StandardVendor, SigAuthVNC,
		 "Standard VNC authentication");
    authCaps.add(AuthUnixLogin, TightVncVendor, SigAuthUnixLogin,
		 "Unix login authentication");

    // Supported non-standard server-to-client messages
    // [NONE]

    // Supported non-standard client-to-server messages
    clientMsgCaps.add(EnableContinuousUpdates, TightVncVendor,
                      SigEnableContinuousUpdates,
                      "Enable/disable continuous updates");

    // Supported encoding types
    encodingCaps.add(EncodingCopyRect, StandardVendor,
		     SigEncodingCopyRect, "Standard CopyRect encoding");
    encodingCaps.add(EncodingHextile, StandardVendor,
		     SigEncodingHextile, "Standard Hextile encoding");
    encodingCaps.add(EncodingTight, TightVncVendor,
		     SigEncodingTight, "Tight encoding");

    // Supported pseudo-encoding types
    encodingCaps.add(EncodingCompressLevel0, TightVncVendor,
		     SigEncodingCompressLevel0, "Compression level");
    encodingCaps.add(EncodingQualityLevel0, TightVncVendor,
		     SigEncodingQualityLevel0, "JPEG quality level");
    encodingCaps.add(EncodingXCursor, TightVncVendor,
		     SigEncodingXCursor, "X-style cursor shape update");
    encodingCaps.add(EncodingRichCursor, TightVncVendor,
		     SigEncodingRichCursor, "Rich-color cursor shape update");
    encodingCaps.add(EncodingPointerPos, TightVncVendor,
		     SigEncodingPointerPos, "Pointer position update");
    encodingCaps.add(EncodingLastRect, TightVncVendor,
		     SigEncodingLastRect, "LastRect protocol extension");
    encodingCaps.add(EncodingNewFBSize, TightVncVendor,
		     SigEncodingNewFBSize, "Framebuffer size change");
    encodingCaps.add(EncodingFineQualityLevel0, TurboVncVendor,
                     SigEncodingFineQualityLevel0, "TurboJPEG fine-grained quality level");
    encodingCaps.add(EncodingSubsamp1X, TurboVncVendor,
                     SigEncodingSubsamp1X, "TurboJPEG subsampling level");
  }

  //
  // Setup tunneling (TightVNC protocol extensions)
  //

  void setupTunneling() throws IOException {
    int nTunnelTypes = readU32();
    if (nTunnelTypes != 0) {
      readCapabilityList(tunnelCaps, nTunnelTypes);

      // We don't support tunneling yet.
      writeInt(NoTunneling);
    }
  }

  //
  // Negotiate authentication scheme (TightVNC protocol extensions)
  //

  int negotiateAuthenticationTight() throws Exception {
    int a, i, nAuthTypes = readU32();
    if (nAuthTypes == 0)
      return AuthNone;

    readCapabilityList(authCaps, nAuthTypes);

    int authScheme = 0;
    if (!viewer.noUnixLogin && (viewer.userParam != null)) {
      /* Prefer Unix Login over other types */
      for (i = 0; i < authCaps.numEnabled(); i++) {
        if (authCaps.getByOrder(i) == AuthUnixLogin) {
          authScheme = AuthUnixLogin;
          break;
        }
      }
    }

    if (authScheme == 0) {
      /* Try server's preferred authentication scheme. */
      for (i = 0; (authScheme == 0) && (i < authCaps.numEnabled()); i++) {
        a = authCaps.getByOrder(i);
        switch (a) {
          case AuthVNC:
          case AuthNone:
            authScheme = a;
            break;
          case AuthUnixLogin:
            if (!viewer.noUnixLogin) authScheme = a;
            break;
          default:
            /* unknown scheme - cannot use it */
            continue;
        }
      }
    }

    if (authScheme == 0)
      throw new Exception("No suitable authentication scheme found");

	writeInt(authScheme);
    return authScheme;
  }

  //
  // Read a capability list (TightVNC protocol extensions)
  //

  void readCapabilityList(CapsContainer caps, int count) throws IOException {
    int code;
    byte[] vendor = new byte[4];
    byte[] name = new byte[8];
    for (int i = 0; i < count; i++) {
      code = readU32();
      readFully(vendor);
      readFully(name);
      caps.enable(new CapabilityInfo(code, vendor, name));
    }
  }

  //
  // Write a 32-bit integer into the output stream.
  //

  void writeInt(int value) throws IOException {
    byte[] b = new byte[4];
    b[0] = (byte) ((value >> 24) & 0xff);
    b[1] = (byte) ((value >> 16) & 0xff);
    b[2] = (byte) ((value >> 8) & 0xff);
    b[3] = (byte) (value & 0xff);
    os.write(b);
  }

  //
  // Write the client initialisation message
  //

  void writeClientInit() throws IOException {
    if (viewer.options.shareDesktop) {
      os.write(1);
    } else {
      os.write(0);
    }
    viewer.options.disableShareDesktop();
  }


  //
  // Read the server initialisation message
  //

  String desktopName;
  int framebufferWidth, framebufferHeight;
  int bitsPerPixel, depth;
  boolean bigEndian, trueColour;
  int redMax, greenMax, blueMax, redShift, greenShift, blueShift;

  void readServerInit() throws IOException {
    framebufferWidth = readU16();
    framebufferHeight = readU16();
    bitsPerPixel = readU8();
    depth = readU8();
    bigEndian = (readU8() != 0);
    trueColour = (readU8() != 0);
    redMax = readU16();
    greenMax = readU16();
    blueMax = readU16();
    redShift = readU8();
    greenShift = readU8();
    blueShift = readU8();
    byte[] pad = new byte[3];
    readFully(pad);
    int nameLength = readU32();
    byte[] name = new byte[nameLength];
    readFully(name);
    desktopName = new String(name);

    // Read interaction capabilities (TightVNC protocol extensions)
    if (protocolTightVNC) {
      int nServerMessageTypes = readU16();
      int nClientMessageTypes = readU16();
      int nEncodingTypes = readU16();
      readU16();
      readCapabilityList(serverMsgCaps, nServerMessageTypes);
      readCapabilityList(clientMsgCaps, nClientMessageTypes);
      readCapabilityList(encodingCaps, nEncodingTypes);
    }

    inNormalProtocol = true;
  }


  //
  // Create session file and write initial protocol messages into it.
  //

  void startSession(String fname) throws IOException {
    rec = new SessionRecorder(fname);
    rec.writeHeader();
    rec.write(versionMsg_3_3.getBytes());
    rec.writeIntBE(SecTypeNone);
    rec.writeShortBE(framebufferWidth);
    rec.writeShortBE(framebufferHeight);
    byte[] fbsServerInitMsg =	{
      32, 24, 0, 1, 0,
      (byte)0xFF, 0, (byte)0xFF, 0, (byte)0xFF,
      16, 8, 0, 0, 0, 0
    };
    rec.write(fbsServerInitMsg);
    rec.writeIntBE(desktopName.length());
    rec.write(desktopName.getBytes());
    numUpdatesInSession = 0;

    // FIXME: If there were e.g. ZRLE updates only, that should not
    //        affect recording of Zlib and Tight updates. So, actually
    //        we should maintain separate flags for Zlib, ZRLE and
    //        Tight, instead of one ``wereZlibUpdates'' variable.
    //
    if (wereZlibUpdates)
      recordFromBeginning = false;

    zlibWarningShown = false;
    tightWarningShown = false;
  }

  //
  // Close session file.
  //

  void closeSession() throws IOException {
    if (rec != null) {
      rec.close();
      rec = null;
    }
  }


  //
  // Set new framebuffer size
  //

  void setFramebufferSize(int width, int height) {
    framebufferWidth = width;
    framebufferHeight = height;
  }


  //
  // Read the server message type
  //

  int readServerMessageType() throws IOException {
    int msgType = readU8();

    // If the session is being recorded:
    if (rec != null) {
      if (msgType == Bell) {	// Save Bell messages in session files.
	rec.writeByte(msgType);
	if (numUpdatesInSession > 0)
	  rec.flush();
      }
    }

    return msgType;
  }


  //
  // Read a FramebufferUpdate message
  //

  int updateNRects;

  void readFramebufferUpdate() throws IOException {
    skipBytes(1);
    updateNRects = readU16();

    // If the session is being recorded:
    if (rec != null) {
      rec.writeByte(FramebufferUpdate);
      rec.writeByte(0);
      rec.writeShortBE(updateNRects);
    }

    numUpdatesInSession++;
  }

  // Read a FramebufferUpdate rectangle header

  int updateRectX, updateRectY, updateRectW, updateRectH, updateRectEncoding;

  void readFramebufferUpdateRectHdr() throws Exception {
    updateRectX = readU16();
    updateRectY = readU16();
    updateRectW = readU16();
    updateRectH = readU16();
    updateRectEncoding = readU32();

    if (updateRectEncoding == EncodingZlib ||
        updateRectEncoding == EncodingZRLE ||
	updateRectEncoding == EncodingTight)
      wereZlibUpdates = true;

    // If the session is being recorded:
    if (rec != null) {
      if (numUpdatesInSession > 1)
	rec.flush();		// Flush the output on each rectangle.
      rec.writeShortBE(updateRectX);
      rec.writeShortBE(updateRectY);
      rec.writeShortBE(updateRectW);
      rec.writeShortBE(updateRectH);
      if (updateRectEncoding == EncodingZlib && !recordFromBeginning) {
	// Here we cannot write Zlib-encoded rectangles because the
	// decoder won't be able to reproduce zlib stream state.
	if (!zlibWarningShown) {
	  System.out.println("Warning: Raw encoding will be used " +
			     "instead of Zlib in recorded session.");
	  zlibWarningShown = true;
	}
	rec.writeIntBE(EncodingRaw);
      } else {
	rec.writeIntBE(updateRectEncoding);
	if (updateRectEncoding == EncodingTight && !recordFromBeginning &&
	    !tightWarningShown) {
	  System.out.println("Warning: Re-compressing Tight-encoded " +
			     "updates for session recording.");
	  tightWarningShown = true;
	}
      }
    }

    if (updateRectEncoding < 0 || updateRectEncoding > MaxNormalEncoding)
      return;

    if (updateRectX + updateRectW > framebufferWidth ||
	updateRectY + updateRectH > framebufferHeight) {
      throw new Exception("Framebuffer update rectangle too large: " +
			  updateRectW + "x" + updateRectH + " at (" +
			  updateRectX + "," + updateRectY + ")");
    }
  }

  // Read CopyRect source X and Y.

  int copyRectSrcX, copyRectSrcY;

  void readCopyRect() throws IOException {
    copyRectSrcX = readU16();
    copyRectSrcY = readU16();

    // If the session is being recorded:
    if (rec != null) {
      rec.writeShortBE(copyRectSrcX);
      rec.writeShortBE(copyRectSrcY);
    }
  }


  //
  // Read a ServerCutText message
  //

  String readServerCutText() throws IOException {
    skipBytes(3);
    int len = readU32();
    byte[] text = new byte[len];
    readFully(text);
    return new String(text);
  }


  //
  // Read an integer in compact representation (1..3 bytes).
  // Such format is used as a part of the Tight encoding.
  // Also, this method records data if session recording is active and
  // the viewer's recordFromBeginning variable is set to true.
  //

  int readCompactLen() throws IOException {
    int[] portion = new int[3];
    portion[0] = readU8();
    int byteCount = 1;
    int len = portion[0] & 0x7F;
    if ((portion[0] & 0x80) != 0) {
      portion[1] = readU8();
      byteCount++;
      len |= (portion[1] & 0x7F) << 7;
      if ((portion[1] & 0x80) != 0) {
	portion[2] = readU8();
	byteCount++;
	len |= (portion[2] & 0xFF) << 14;
      }
    }

    if (rec != null && recordFromBeginning)
      for (int i = 0; i < byteCount; i++)
	rec.writeByte(portion[i]);

    return len;
  }


  //
  // Write a FramebufferUpdateRequest message
  //

  void writeFramebufferUpdateRequest(int x, int y, int w, int h,
				     boolean incremental)
       throws IOException
  {
    byte[] b = new byte[10];

    b[0] = (byte) FramebufferUpdateRequest;
    b[1] = (byte) (incremental ? 1 : 0);
    b[2] = (byte) ((x >> 8) & 0xff);
    b[3] = (byte) (x & 0xff);
    b[4] = (byte) ((y >> 8) & 0xff);
    b[5] = (byte) (y & 0xff);
    b[6] = (byte) ((w >> 8) & 0xff);
    b[7] = (byte) (w & 0xff);
    b[8] = (byte) ((h >> 8) & 0xff);
    b[9] = (byte) (h & 0xff);

    os.write(b);
  }


  //
  // Write a SetPixelFormat message
  //

  void writeSetPixelFormat(int bitsPerPixel, int depth, boolean bigEndian,
			   boolean trueColour,
			   int redMax, int greenMax, int blueMax,
			   int redShift, int greenShift, int blueShift)
       throws IOException
  {
    byte[] b = new byte[20];

    b[0]  = (byte) SetPixelFormat;
    b[4]  = (byte) bitsPerPixel;
    b[5]  = (byte) depth;
    b[6]  = (byte) (bigEndian ? 1 : 0);
    b[7]  = (byte) (trueColour ? 1 : 0);
    b[8]  = (byte) ((redMax >> 8) & 0xff);
    b[9]  = (byte) (redMax & 0xff);
    b[10] = (byte) ((greenMax >> 8) & 0xff);
    b[11] = (byte) (greenMax & 0xff);
    b[12] = (byte) ((blueMax >> 8) & 0xff);
    b[13] = (byte) (blueMax & 0xff);
    b[14] = (byte) redShift;
    b[15] = (byte) greenShift;
    b[16] = (byte) blueShift;

    os.write(b);
  }


  //
  // Write a FixColourMapEntries message.  The values in the red, green and
  // blue arrays are from 0 to 65535.
  //

  void writeFixColourMapEntries(int firstColour, int nColours,
				int[] red, int[] green, int[] blue)
       throws IOException
  {
    byte[] b = new byte[6 + nColours * 6];

    b[0] = (byte) FixColourMapEntries;
    b[2] = (byte) ((firstColour >> 8) & 0xff);
    b[3] = (byte) (firstColour & 0xff);
    b[4] = (byte) ((nColours >> 8) & 0xff);
    b[5] = (byte) (nColours & 0xff);

    for (int i = 0; i < nColours; i++) {
      b[6 + i * 6]     = (byte) ((red[i] >> 8) & 0xff);
      b[6 + i * 6 + 1] = (byte) (red[i] & 0xff);
      b[6 + i * 6 + 2] = (byte) ((green[i] >> 8) & 0xff);
      b[6 + i * 6 + 3] = (byte) (green[i] & 0xff);
      b[6 + i * 6 + 4] = (byte) ((blue[i] >> 8) & 0xff);
      b[6 + i * 6 + 5] = (byte) (blue[i] & 0xff);
    }
 
    os.write(b);
  }


  //
  // Write a SetEncodings message
  //

  void writeSetEncodings(int[] encs, int len) throws IOException {
    byte[] b = new byte[4 + 4 * len];

    b[0] = (byte) SetEncodings;
    b[2] = (byte) ((len >> 8) & 0xff);
    b[3] = (byte) (len & 0xff);

    for (int i = 0; i < len; i++) {
      b[4 + 4 * i] = (byte) ((encs[i] >> 24) & 0xff);
      b[5 + 4 * i] = (byte) ((encs[i] >> 16) & 0xff);
      b[6 + 4 * i] = (byte) ((encs[i] >> 8) & 0xff);
      b[7 + 4 * i] = (byte) (encs[i] & 0xff);
    }

    os.write(b);
  }


  //
  // Write a ClientCutText message
  //

  void writeClientCutText(String text) throws IOException {
    byte[] b = new byte[8 + text.length()];

    b[0] = (byte) ClientCutText;
    b[4] = (byte) ((text.length() >> 24) & 0xff);
    b[5] = (byte) ((text.length() >> 16) & 0xff);
    b[6] = (byte) ((text.length() >> 8) & 0xff);
    b[7] = (byte) (text.length() & 0xff);

    System.arraycopy(text.getBytes(), 0, b, 8, text.length());

    os.write(b);
  }


  //
  // A buffer for putting pointer and keyboard events before being sent.  This
  // is to ensure that multiple RFB events generated from a single Java Event 
  // will all be sent in a single network packet.  The maximum possible
  // length is 4 modifier down events, a single key event followed by 4
  // modifier up events i.e. 9 key events or 72 bytes.
  //

  byte[] eventBuf = new byte[72];
  int eventBufLen;


  // Useful shortcuts for modifier masks.

  final static int CTRL_MASK  = InputEvent.CTRL_MASK;
  final static int SHIFT_MASK = InputEvent.SHIFT_MASK;
  final static int META_MASK  = InputEvent.META_MASK;
  final static int ALT_MASK   = InputEvent.ALT_MASK;
  final static int ALT_GRAPH_MASK = InputEvent.ALT_GRAPH_MASK;
  final static int SUPER_MASK = 1 << 16;
  final static int MASK1      = 1 << 0;
  final static int MASK2      = 1 << 1;
  final static int MASK3      = 1 << 2;
  final static int MASK4      = 1 << 3;
  final static int MASK5      = 1 << 4;


  //
  // Write a pointer event message.  We may need to send modifier key events
  // around it to set the correct modifier state.
  //

  int pointerMask = 0;

  void writePointerEvent(MouseEvent evt) throws IOException {

    int mask2 = (viewer.options.reverseMouseButtons2And3 ? MASK3 : MASK2);
    int mask3 = (viewer.options.reverseMouseButtons2And3 ? MASK2 : MASK3);

    int wheelMask = 0, wheelClicks = 0;

    int eventId = evt.getID();
    int modifiers = evt.getModifiers();

    if (eventId == MouseEvent.MOUSE_PRESSED) {
      // One button mask will be set, indicating the button that changed state.
      if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
        pointerMask |= MASK1;
      } else if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
        pointerMask |= mask2;
      } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
        pointerMask |= mask3;
      } else {
        // In Java 1.1, AWT does not set BUTTON1_MASK on left button presses,
        // so we must assume a left button press if no button modifiers are set
        // (see Java Bug ID 4029201).
        pointerMask |= MASK1;
      }
    } else if (eventId == MouseEvent.MOUSE_RELEASED) {
      // One button mask will be set, indicating the button that changed state.
      if ((modifiers & InputEvent.BUTTON1_MASK) != 0) {
        pointerMask &= ~MASK1;
      } else if ((modifiers & InputEvent.BUTTON2_MASK) != 0) {
        pointerMask &= ~mask2;
      } else if ((modifiers & InputEvent.BUTTON3_MASK) != 0) {
        pointerMask &= ~mask3;
      } else {
        // MOUSE_RELEASED event for unhandled button.
        return;
      }
    } else {
      // 'modifiers' will represent the current state of all buttons.
      if (eventId == MouseEvent.MOUSE_WHEEL) {
        wheelClicks = ((MouseWheelEvent) evt).getWheelRotation();
        if (wheelClicks == 0) {
          return;
        } else if (wheelClicks < 0) {
          wheelMask = pointerMask | MASK4;
          wheelClicks = -wheelClicks;
        } else {
          wheelMask = pointerMask | MASK5;
        }
      } else if (eventId != MouseEvent.MOUSE_ENTERED &&
                 eventId != MouseEvent.MOUSE_MOVED &&
                 eventId != MouseEvent.MOUSE_DRAGGED) {
        // Unhandled eventId.
        return;
      }
    }

    int eventBufLen = 0;

    int x = evt.getX();
    int y = evt.getY();
    if (x < 0) x = 0;
    if (y < 0) y = 0;

    if (wheelClicks == 0) {
      eventBuf[eventBufLen++] = (byte) PointerEvent;
      eventBuf[eventBufLen++] = (byte) pointerMask;
      eventBuf[eventBufLen++] = (byte) ((x >> 8) & 0xff);
      eventBuf[eventBufLen++] = (byte) (x & 0xff);
      eventBuf[eventBufLen++] = (byte) ((y >> 8) & 0xff);
      eventBuf[eventBufLen++] = (byte) (y & 0xff);
      os.write(eventBuf, 0, eventBufLen);
    } else {
      // Send press/release events for each unit of wheel rotation.
      for (int i = 0; i < wheelClicks; i++) {
        eventBuf[eventBufLen++] = (byte) PointerEvent;
        eventBuf[eventBufLen++] = (byte) wheelMask;
        eventBuf[eventBufLen++] = (byte) ((x >> 8) & 0xff);
        eventBuf[eventBufLen++] = (byte) (x & 0xff);
        eventBuf[eventBufLen++] = (byte) ((y >> 8) & 0xff);
        eventBuf[eventBufLen++] = (byte) (y & 0xff);
        eventBuf[eventBufLen++] = (byte) PointerEvent;
        eventBuf[eventBufLen++] = (byte) pointerMask;
        eventBuf[eventBufLen++] = (byte) ((x >> 8) & 0xff);
        eventBuf[eventBufLen++] = (byte) (x & 0xff);
        eventBuf[eventBufLen++] = (byte) ((y >> 8) & 0xff);
        eventBuf[eventBufLen++] = (byte) (y & 0xff);
        os.write(eventBuf, 0, eventBufLen);
        eventBufLen = 0;
      }
    }
  }


  int lmodifiers, rmodifiers;

  // KeyEvent.getKeyModifiersText() is unfortunately broken on some platforms.
  String getKeyModifiersText(int lmodifiers, int rmodifiers) {
    String str = "";
    if ((lmodifiers & SHIFT_MASK) != 0)
      str += "LShift ";
    if ((rmodifiers & SHIFT_MASK) != 0)
      str += "RShift ";
    if ((lmodifiers & CTRL_MASK) != 0)
      str += "LCtrl ";
    if ((rmodifiers & CTRL_MASK) != 0)
      str += "RCtrl ";
    if ((lmodifiers & ALT_MASK) != 0)
      str += "LAlt ";
    if ((rmodifiers & ALT_MASK) != 0)
      str += "RAlt ";
    if ((lmodifiers & META_MASK) != 0)
      str += "LMeta ";
    if ((rmodifiers & META_MASK) != 0)
      str += "RMeta ";
    if ((lmodifiers & SUPER_MASK) != 0)
      str += "LSuper ";
    if ((rmodifiers & SUPER_MASK) != 0)
      str += "RSuper ";
    return str;
  }

  String getLocationText(int location) {
    switch (location) {
      case KeyEvent.KEY_LOCATION_LEFT:      return "LEFT";
      case KeyEvent.KEY_LOCATION_NUMPAD:    return "NUMPAD";
      case KeyEvent.KEY_LOCATION_RIGHT:     return "RIGHT";
      case KeyEvent.KEY_LOCATION_STANDARD:  return "STANDARD";
      case KeyEvent.KEY_LOCATION_UNKNOWN:   return "UNKNOWN";
      default:                              return Integer.toString(location);
    }
  }

  //
  // Write a key event message.  We may need to send modifier key events
  // around it to set the correct modifier state.  Also we need to translate
  // from the Java key values to the X keysym values used by the RFB protocol.
  //

  void writeKeyEvent(KeyEvent evt) throws IOException {

    int keyChar = evt.getKeyChar();
    int keyCode = evt.getKeyCode();
    int location = evt.getKeyLocation();

    //
    // Ignore event if only modifiers were pressed.
    //

    // Some JVMs return 0 instead of CHAR_UNDEFINED in getKeyChar().
    if (keyChar == 0)
      keyChar = KeyEvent.CHAR_UNDEFINED;

    //
    // Key press or key release?
    //

    boolean down = (evt.getID() == KeyEvent.KEY_PRESSED);

//    System.out.println((evt.isActionKey() ? "action " : "") + "key " +
//                         (down ? "PRESS" : "release") +
//                       ", code " + KeyEvent.getKeyText(keyCode) +
//                         " (" + keyCode + ")" +
//                       ", loc " + getLocationText(location) +
//                       ", char " + (keyChar >= 32 && keyChar <= 126 ?
//                         "'" + (char)keyChar + "'" : Integer.toString(keyChar)) +
//                       " " + getKeyModifiersText(lmodifiers, rmodifiers) +
//                         (evt.isAltGraphDown() ? "AltGr":""));

    int key, lFakeModifiers = 0, rFakeModifiers = 0;
    if (evt.isActionKey()) {

      //
      // An action key should be one of the following.
      // If not then just ignore the event.
      //

      switch(keyCode) {
      case KeyEvent.VK_HOME:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Home;
        else
          key = Keysyms.Home;  break;
      case KeyEvent.VK_LEFT:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Left;
        else
         key = Keysyms.Left;  break;
      case KeyEvent.VK_UP:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Up;
        else
          key = Keysyms.Up;  break;
      case KeyEvent.VK_RIGHT:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Right;
        else
          key = Keysyms.Right;  break;
      case KeyEvent.VK_DOWN:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Down;
        else
         key = Keysyms.Down;  break;
      case KeyEvent.VK_PAGE_UP:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Page_Up;
        else
          key = Keysyms.Page_Up;  break;
      case KeyEvent.VK_PAGE_DOWN:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Page_Down;
        else
          key = Keysyms.Page_Down;  break;
      case KeyEvent.VK_END:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_End;
        else
          key = Keysyms.End;  break;
      case KeyEvent.VK_INSERT:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Insert;
        else
          key = Keysyms.Insert;  break;
      case KeyEvent.VK_F1:           key = Keysyms.F1;  break;
      case KeyEvent.VK_F2:           key = Keysyms.F2;  break;
      case KeyEvent.VK_F3:           key = Keysyms.F3;  break;
      case KeyEvent.VK_F4:           key = Keysyms.F4;  break;
      case KeyEvent.VK_F5:           key = Keysyms.F5;  break;
      case KeyEvent.VK_F6:           key = Keysyms.F6;  break;
      case KeyEvent.VK_F7:           key = Keysyms.F7;  break;
      case KeyEvent.VK_F8:           key = Keysyms.F8;  break;
      case KeyEvent.VK_F9:           key = Keysyms.F9;  break;
      case KeyEvent.VK_F10:          key = Keysyms.F10;  break;
      case KeyEvent.VK_F11:          key = Keysyms.F11;  break;
      case KeyEvent.VK_F12:          key = Keysyms.F12;  break;
      case KeyEvent.VK_F13:          key = Keysyms.F13;  break;
      case KeyEvent.VK_KP_DOWN:      key = Keysyms.KP_Down;  break;
      case KeyEvent.VK_KP_LEFT:      key = Keysyms.KP_Left;  break;
      case KeyEvent.VK_KP_RIGHT:     key = Keysyms.KP_Right;  break;
      case KeyEvent.VK_KP_UP:        key = Keysyms.KP_Up;  break;
      case KeyEvent.VK_NUM_LOCK:     key = Keysyms.Num_Lock;  break;
      case KeyEvent.VK_WINDOWS:
        if (location == KeyEvent.KEY_LOCATION_RIGHT) {
          key = Keysyms.Super_R;
          if (down)
            rmodifiers |= SUPER_MASK;
          else
            rmodifiers &= ~SUPER_MASK;
        } else {
          key = Keysyms.Super_L;
          if (down)
            lmodifiers |= SUPER_MASK;
          else
            lmodifiers &= ~SUPER_MASK;
        }  break;
      case KeyEvent.VK_CONTEXT_MENU: key = Keysyms.Menu;  break;
      case KeyEvent.VK_PRINTSCREEN:  key = Keysyms.Print; break;
      case KeyEvent.VK_SCROLL_LOCK:  key = Keysyms.Scroll_Lock;  break;
      case KeyEvent.VK_CAPS_LOCK:    key = Keysyms.Caps_Lock;  break;
      case KeyEvent.VK_PAUSE:
        if (evt.isControlDown())
          key = Keysyms.Break;
        else
          key = Keysyms.Pause;
        break;
      case KeyEvent.VK_BEGIN:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Begin;
        else
          key = Keysyms.Begin;  break;
      default:
        return;
      }

    } else {

      //
      // A "normal" key press.  Ordinary ASCII characters go straight through.
      // Backspace, tab, return, escape and delete have special keysyms.
      // Anything else we ignore.
      //

      key = keyChar;
      if (evt.isControlDown() && !evt.isAltDown()) {
        // For CTRL-<letter>, CTRL is sent separately, so just send <letter>.      
        if ((key >= 1 && key <= 26 && !evt.isShiftDown()) ||
            // CTRL-{, CTRL-|, CTRL-} also map to ASCII 96-127
            (key >= 27 && key <= 29 && evt.isShiftDown()))
          key += 96;
        // For CTRL-SHIFT-<letter>, send capital <letter> to emulate behavior
        // of Linux.  For CTRL-@, send @.  For CTRL-_, send _.  For CTRL-^,
        // send ^.
        else if (key < 32)
          key += 64;
        // Windows and Mac sometimes return CHAR_UNDEFINED with CTRL-SHIFT
        // combinations, so best we can do is send the key code if it is
        // a valid ASCII symbol.
        else if (key == KeyEvent.CHAR_UNDEFINED && keyCode >= 0 &&
                 keyCode <= 127)
          key = keyCode;
      }

      switch(keyCode) {
      case KeyEvent.VK_BACK_SPACE: key = Keysyms.BackSpace; break;
      case KeyEvent.VK_TAB:        key = Keysyms.Tab; break;
      case KeyEvent.VK_ENTER:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Enter;
        else
          key = Keysyms.Return;  break;
      case KeyEvent.VK_ESCAPE:     key = Keysyms.Escape; break;
      case KeyEvent.VK_NUMPAD0:    key = Keysyms.KP_0; break;
      case KeyEvent.VK_NUMPAD1:    key = Keysyms.KP_1; break;
      case KeyEvent.VK_NUMPAD2:    key = Keysyms.KP_2; break;
      case KeyEvent.VK_NUMPAD3:    key = Keysyms.KP_3; break;
      case KeyEvent.VK_NUMPAD4:    key = Keysyms.KP_4; break;
      case KeyEvent.VK_NUMPAD5:    key = Keysyms.KP_5; break;
      case KeyEvent.VK_NUMPAD6:    key = Keysyms.KP_6; break;
      case KeyEvent.VK_NUMPAD7:    key = Keysyms.KP_7; break;
      case KeyEvent.VK_NUMPAD8:    key = Keysyms.KP_8; break;
      case KeyEvent.VK_NUMPAD9:    key = Keysyms.KP_9; break;
      case KeyEvent.VK_DECIMAL:    key = Keysyms.KP_Decimal; break;
      case KeyEvent.VK_ADD:        key = Keysyms.KP_Add; break;
      case KeyEvent.VK_SUBTRACT:   key = Keysyms.KP_Subtract; break;
      case KeyEvent.VK_MULTIPLY:   key = Keysyms.KP_Multiply; break;
      case KeyEvent.VK_DIVIDE:     key = Keysyms.KP_Divide; break;
      case KeyEvent.VK_DELETE:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Delete;
        else
          key = Keysyms.Delete;  break;
      case KeyEvent.VK_CLEAR:
        if (location == KeyEvent.KEY_LOCATION_NUMPAD)
          key = Keysyms.KP_Begin;
        else
          key = Keysyms.Clear;  break;
      case KeyEvent.VK_CONTROL:
        if (location == KeyEvent.KEY_LOCATION_RIGHT) {
          key = Keysyms.Control_R;
          if (down)
            rmodifiers |= CTRL_MASK;
          else
            rmodifiers &= ~CTRL_MASK;
        } else {
          key = Keysyms.Control_L;
          if (down)
            lmodifiers |= CTRL_MASK;
          else
            lmodifiers &= ~CTRL_MASK;
        }  break;
      case KeyEvent.VK_ALT:
        if (location == KeyEvent.KEY_LOCATION_RIGHT) {
          // Mac has no AltGr key, but the Option/Alt keys serve the same
          // purpose.  Thus, we allow RAlt to be used as AltGr and LAlt to be
          // used as a regular Alt key.
          if (VncViewer.os.startsWith("mac os x")) {
            key = Keysyms.ISO_Level3_Shift;
            if (down)
              lmodifiers |= ALT_GRAPH_MASK;
            else
              lmodifiers &= ~ALT_GRAPH_MASK;
          } else {
            key = Keysyms.Alt_R;
            if (down)
              rmodifiers |= ALT_MASK;
            else
              rmodifiers &= ~ALT_MASK;
          }
        } else {
          key = Keysyms.Alt_L;
          if (down)
            lmodifiers |= ALT_MASK;
          else
            lmodifiers &= ~ALT_MASK;
        }
        break;
      case KeyEvent.VK_SHIFT:
        if (location == KeyEvent.KEY_LOCATION_RIGHT) {
          key = Keysyms.Shift_R;
          if (down)
            rmodifiers |= SHIFT_MASK;
          else
            rmodifiers &= ~SHIFT_MASK;
        } else {
          key = Keysyms.Shift_L;
          if (down)
            lmodifiers |= SHIFT_MASK;
          else
            lmodifiers &= ~SHIFT_MASK;
        }  break;
      case KeyEvent.VK_META:
        if (location == KeyEvent.KEY_LOCATION_RIGHT) {
          key = Keysyms.Super_R;
          if (down)
            rmodifiers |= SUPER_MASK;
          else
            rmodifiers &= ~SUPER_MASK;
        } else {
          key = Keysyms.Super_L;
          if (down)
            lmodifiers |= SUPER_MASK;
          else
            lmodifiers &= ~SUPER_MASK;
        }  break;
      case KeyEvent.VK_ALT_GRAPH:
        key = Keysyms.ISO_Level3_Shift;
        if (down)
          lmodifiers |= ALT_GRAPH_MASK;
        else
          lmodifiers &= ~ALT_GRAPH_MASK;
        break;
      default:
        // On Windows, pressing AltGr has the same effect as pressing LCtrl +
        // RAlt, so we have to send fake key release events for those
        // modifiers (and any other Ctrl and Alt modifiers that are pressed),
        // then the key event for the modified key, then fake key press events
        // for the same modifiers.
        if ((rmodifiers & ALT_MASK) != 0 &&
            (lmodifiers & CTRL_MASK) != 0 &&
            VncViewer.os.startsWith("windows")) {
          rFakeModifiers = rmodifiers & (ALT_MASK | CTRL_MASK);
          lFakeModifiers = lmodifiers & (ALT_MASK | CTRL_MASK);
        } else if ((lmodifiers & ALT_MASK) != 0) {
          // This is mainly for the benefit of OS X.  Un*x and Windows servers
          // expect that, if Alt + an ASCII key is pressed, the key event for
          // the ASCII key will be the same as if Alt had not been pressed.  On
          // OS X, however, the Alt/Option keys act like AltGr keys, so if
          // Alt + an ASCII key is pressed, the key code is the ASCII key
          // symbol but the key char is the code for the alternate graphics
          // symbol.
          if (keyCode >= 32 && keyCode <= 126)
            key = keyCode;
        }
      }
    }

    // Fake keyPresses for keys that only generates keyRelease events
    if ((key == 0xe5) || (key == 0xc5) || // XK_aring / XK_Aring
	(key == 0xe4) || (key == 0xc4) || // XK_adiaeresis / XK_Adiaeresis
	(key == 0xf6) || (key == 0xd6) || // XK_odiaeresis / XK_Odiaeresis
	(key == 0xa7) || (key == 0xbd) || // XK_section / XK_onehalf
	(key == 0xa3)) {                  // XK_sterling
      // Make sure we do not send keypress events twice on platforms
      // with correct JVMs (those that actually report KeyPress for all
      // keys)	
      if (down)
	brokenKeyPressed = true;

      if (!down && !brokenKeyPressed) {
	// We've got a release event for this key, but haven't received
        // a press. Fake it. 
	writeKeyEvent(key, true);
      }

      if (!down)
	brokenKeyPressed = false;  
    }

    if ((lFakeModifiers & CTRL_MASK) != 0)
      writeKeyEvent(Keysyms.Control_L, false);
    if ((lFakeModifiers & ALT_MASK) != 0)
      writeKeyEvent(Keysyms.Alt_L, false);
    if ((rFakeModifiers & CTRL_MASK) != 0)
      writeKeyEvent(Keysyms.Control_R, false);
    if ((rFakeModifiers & ALT_MASK) != 0)
      writeKeyEvent(Keysyms.Alt_R, false);
    writeKeyEvent(key, down);
    if ((lFakeModifiers & CTRL_MASK) != 0)
      writeKeyEvent(Keysyms.Control_L, true);
    if ((lFakeModifiers & ALT_MASK) != 0)
      writeKeyEvent(Keysyms.Alt_L, true);
    if ((rFakeModifiers & CTRL_MASK) != 0)
      writeKeyEvent(Keysyms.Control_R, true);
    if ((rFakeModifiers & ALT_MASK) != 0)
      writeKeyEvent(Keysyms.Alt_R, true);
  }

  //
  // Send KeyRelease events for any modifiers that are pressed
  //

  void releaseModifiers() {
    try {
      if ((lmodifiers & SHIFT_MASK) != 0)
        writeKeyEvent(Keysyms.Shift_L, false);
      if ((rmodifiers & SHIFT_MASK) != 0)
        writeKeyEvent(Keysyms.Shift_R, false);
      if ((lmodifiers & CTRL_MASK) != 0)
        writeKeyEvent(Keysyms.Control_L, false);
      if ((rmodifiers & CTRL_MASK) != 0)
        writeKeyEvent(Keysyms.Control_R, false);
      if ((lmodifiers & ALT_MASK) != 0)
        writeKeyEvent(Keysyms.Alt_L, false);
      if ((rmodifiers & ALT_MASK) != 0)
        writeKeyEvent(Keysyms.Alt_R, false);
      if ((lmodifiers & META_MASK) != 0)
        writeKeyEvent(Keysyms.Meta_L, false);
      if ((rmodifiers & META_MASK) != 0)
        writeKeyEvent(Keysyms.Meta_R, false);
      if ((lmodifiers & ALT_GRAPH_MASK) != 0)
        writeKeyEvent(Keysyms.ISO_Level3_Shift, false);
      if ((lmodifiers & SUPER_MASK) != 0)
        writeKeyEvent(Keysyms.Super_L, false);
      if ((rmodifiers & SUPER_MASK) != 0)
        writeKeyEvent(Keysyms.Super_R, false);
      lmodifiers = rmodifiers = 0;
    } catch (IOException e) {
      System.out.println("ERROR: Could not send key release events for modifiers:\n       "+
        e.getMessage());
    }
  }


  //
  // Add a raw key event with the given X keysym to eventBuf.
  //

  void writeKeyEvent(int keysym, boolean down) throws IOException {
    int eventBufLen = 0;
//    System.out.println("  writeKeyEvent " + keysym + " " +
//                       (down ? "PRESS" : "release"));
    eventBuf[eventBufLen++] = (byte) KeyboardEvent;
    eventBuf[eventBufLen++] = (byte) (down ? 1 : 0);
    eventBuf[eventBufLen++] = (byte) 0;
    eventBuf[eventBufLen++] = (byte) 0;
    eventBuf[eventBufLen++] = (byte) ((keysym >> 24) & 0xff);
    eventBuf[eventBufLen++] = (byte) ((keysym >> 16) & 0xff);
    eventBuf[eventBufLen++] = (byte) ((keysym >> 8) & 0xff);
    eventBuf[eventBufLen++] = (byte) (keysym & 0xff);
    os.write(eventBuf, 0, eventBufLen);
  }


  //
  // Enable continuous updates for the specified area of the screen (but
  // only if EnableContinuousUpdates message is supported by the server).
  //

  void tryEnableContinuousUpdates(int x, int y, int w, int h)
    throws IOException
  {
    if (!clientMsgCaps.isEnabled(EnableContinuousUpdates)) {
      System.out.println("Continuous updates not supported by the server");
      return;
    }

    if (continuousUpdatesActive) {
      System.out.println("Continuous updates already active");
      return;
    }

    byte[] b = new byte[10];

    b[0] = (byte) EnableContinuousUpdates;
    b[1] = (byte) 1; // enable
    b[2] = (byte) ((x >> 8) & 0xff);
    b[3] = (byte) (x & 0xff);
    b[4] = (byte) ((y >> 8) & 0xff);
    b[5] = (byte) (y & 0xff);
    b[6] = (byte) ((w >> 8) & 0xff);
    b[7] = (byte) (w & 0xff);
    b[8] = (byte) ((h >> 8) & 0xff);
    b[9] = (byte) (h & 0xff);

    os.write(b);

    continuousUpdatesActive = true;
    System.out.println("Continuous updates activated");
  }


  //
  // Disable continuous updates (only if EnableContinuousUpdates message
  // is supported by the server).
  //

  void tryDisableContinuousUpdates() throws IOException
  {
    if (!clientMsgCaps.isEnabled(EnableContinuousUpdates)) {
      System.out.println("Continuous updates not supported by the server");
      return;
    }

    if (!continuousUpdatesActive) {
      System.out.println("Continuous updates already disabled");
      return;
    }

    if (continuousUpdatesEnding)
      return;

    byte[] b = { (byte)EnableContinuousUpdates, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    os.write(b);

    if (!serverMsgCaps.isEnabled(EndOfContinuousUpdates)) {
      // If the server did not advertise support for the
      // EndOfContinuousUpdates message (should not normally happen
      // when EnableContinuousUpdates is supported), then we clear
      // 'continuousUpdatesActive' variable immediately. Normally,
      // it will be reset on receiving EndOfContinuousUpdates message
      // from the server.
      continuousUpdatesActive = false;
    } else {
      // Indicate that we are waiting for EndOfContinuousUpdates.
      continuousUpdatesEnding = true;
    }
  }


  //
  // Process EndOfContinuousUpdates message received from the server.
  //

  void endOfContinuousUpdates()
  {
    continuousUpdatesActive = false;
    continuousUpdatesEnding = false;
  }


  //
  // Check if continuous updates are in effect.
  //

  boolean continuousUpdatesAreActive()
  {
    return continuousUpdatesActive;
  }

  //
  // Compress and write the data into the recorded session file. This
  // method assumes the recording is on (rec != null).
  //

  void recordCompressedData(byte[] data, int off, int len) throws IOException {
    Deflater deflater = new Deflater();
    deflater.setInput(data, off, len);
    int bufSize = len + len / 100 + 12;
    byte[] buf = new byte[bufSize];
    deflater.finish();
    int compressedSize = deflater.deflate(buf);
    recordCompactLen(compressedSize);
    rec.write(buf, 0, compressedSize);
  }

  void recordCompressedData(byte[] data) throws IOException {
    recordCompressedData(data, 0, data.length);
  }

  //
  // Write an integer in compact representation (1..3 bytes) into the
  // recorded session file. This method assumes the recording is on
  // (rec != null).
  //

  void recordCompactLen(int len) throws IOException {
    byte[] buf = new byte[3];
    int bytes = 0;
    buf[bytes++] = (byte)(len & 0x7F);
    if (len > 0x7F) {
      buf[bytes-1] |= 0x80;
      buf[bytes++] = (byte)(len >> 7 & 0x7F);
      if (len > 0x3FFF) {
	buf[bytes-1] |= 0x80;
	buf[bytes++] = (byte)(len >> 14 & 0xFF);
      }
    }
    rec.write(buf, 0, bytes);
  }

  public void startTiming() {
    timing = true;

    // Carry over up to 1s worth of previous rate for smoothing.

    if (timeWaitedIn100us > 10000) {
      timedKbits = timedKbits * 10000 / timeWaitedIn100us;
      timeWaitedIn100us = 10000;
    }
  }

  public void stopTiming() {
    timing = false; 
    if (timeWaitedIn100us < timedKbits/2)
      timeWaitedIn100us = timedKbits/2; // upper limit 20Mbit/s
  }

  public long kbitsPerSecond() {
    return timedKbits * 10000 / timeWaitedIn100us;
  }

  public long timeWaited() {
    return timeWaitedIn100us;
  }

  //
  // Methods for reading data via our DataInputStream member variable (is).
  //
  // In addition to reading data, the readFully() methods updates variables
  // used to estimate data throughput.
  //

  public void readFully(byte b[]) throws IOException {
    readFully(b, 0, b.length);
  }

  public void readFully(byte b[], int off, int len) throws IOException {
    long before = 0;
    if (timing)
      before = System.currentTimeMillis();

    is.readFully(b, off, len);

    if (timing) {
      long after = System.currentTimeMillis();
      long newTimeWaited = (after - before) * 10;
      int newKbits = len * 8 / 1000;

      // limit rate to between 10kbit/s and 40Mbit/s

      if (newTimeWaited > newKbits*1000) newTimeWaited = newKbits*1000;
      if (newTimeWaited < newKbits/4)    newTimeWaited = newKbits/4;

      timeWaitedIn100us += newTimeWaited;
      timedKbits += newKbits;
    }

    numBytesRead += len;
  }

  final int available() throws IOException {
    return is.available();
  }

  // FIXME: DataInputStream::skipBytes() is not guaranteed to skip
  //        exactly n bytes. Probably we don't want to use this method.
  final int skipBytes(int n) throws IOException {
    int r = is.skipBytes(n);
    numBytesRead += r;
    return r;
  }

  final int readU8() throws IOException {
    int r = is.readUnsignedByte();
    numBytesRead++;
    return r;
  }

  final int readU16() throws IOException {
    int r = is.readUnsignedShort();
    numBytesRead += 2;
    return r;
  }

  final int readU32() throws IOException {
    int r = is.readInt();
    numBytesRead += 4;
    return r;
  }
}

